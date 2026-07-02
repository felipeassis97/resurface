# Viabilidade Técnica — F3 `manage-monitored-apps` (Resultado do Council)

> Documento gerado por um council de 5 revisores independentes (blind review + ranking) sobre `proposal.md`, `design.md`, `tasks.md`, `specs/monitored-apps/spec.md` e o `README.md` do projeto, cruzados com o código atual em `app/src/main`.

## Veredito

**Viável — pode construir.** Não há nenhum bloqueio técnico. Cada parte difícil já tem precedente no próprio código:
- `MonitoredAppsRepository` = `OnboardingRepository` (DataStore + `Flow` + `@Singleton` Hilt) quase verbatim.
- Hilt já é usado em todo o app (`@HiltAndroidApp`, `@HiltViewModel`).
- Navegação type-safe (`composable<Route>`) já estabelecida.

O único território novo é (1) injetar Hilt num `AccessibilityService` e (2) `setServiceInfo()` em runtime — ambos problemas conhecidos e resolvidos, não riscos de pesquisa. O plano em `tasks.md` está ~85% correto. Os ajustes abaixo são o que o council convergiu como necessário.

---

## O ponto onde o council discordou (e a decisão)

**Pergunta:** o F3 precisa mesmo do `setServiceInfo(packageNames)` dinâmico agora?

- **Dois membros (Red-teamer, First-principles):** NÃO. O `accessibility_service_config.xml` hoje tem `canRetrieveWindowContent="false"`, `onAccessibilityEvent` é no-op, e não há `packageNames` — logo o serviço já recebe eventos de tudo e **não faz nada** com eles. Escopar em runtime é higiene de privacidade, não um habilitador funcional. Como o set é **congelado** durante o estudo, re-escopo ao vivo é desnecessário e é o caminho menos confiável entre OEMs. Cortar ~1/3 do plano.
- **Três membros (Rigorist, Pragmatist, Generalist):** manter, mas implementar com cuidado — é a "costura" (seam) para o F4, custa pouco, e a reconciliação no connect sai de graça observando o `Flow`.

**Decisão (Chairman):** A crítica do Red-teamer/First-principles lidera e reformula o design — **mas não se corta o serviço, corrige-se o mecanismo.** Implemente como **reconcile-on-connect**, não como "push ao vivo":

> O DataStore é a fonte da verdade; o serviço é uma projeção. Em **todo** `onServiceConnected`, o serviço lê o set persistido e aplica o escopo. Isso é o caminho known-good que todos aceitam. Coletar o `Flow` continua valendo (dá a reconciliação no connect de graça e cobre updates ao vivo como *best-effort*), mas a **integridade do estudo não pode depender** de o re-escopo ao vivo funcionar em todo OEM/Android 16.

Consequência prática: a fallback já citada no `design.md` ("re-escopo só no próximo bind") vira o comportamento **primário garantido**, não o plano B.

---

## Correções ao plano (consenso do council)

### 1. Injeção Hilt no `AccessibilityService`
- `@AndroidEntryPoint` funciona em `AccessibilityService` (estende `Service`). Forma simples da anotação (o projeto usa KSP, não o bytecode-transform de duas assinaturas).
- `@Inject lateinit var repo: MonitoredAppsRepository`. Injeção acontece em `super.onCreate()` → só toque no `repo` depois disso. Seguro em `onServiceConnected`.
- Requer `ResurfaceApplication` com `@HiltAndroidApp` (já é o caso).

### 2. Escopo de corrotina no serviço (bug real no plano)
`AccessibilityService` **não** é `LifecycleService` → não há `lifecycleScope`. Criar o próprio:

```kotlin
private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
private var collectJob: Job? = null

override fun onServiceConnected() {
    super.onServiceConnected()
    collectJob?.cancel()                       // onServiceConnected pode disparar >1x (rebind)
    collectJob = serviceScope.launch {
        repo.selection
            .catch { /* DataStore emite IOException em corrupção; não deixe crashar
                        — a11y service que crasha se auto-desabilita silenciosamente */ }
            .collect { pkgs -> applyMonitoredScope(pkgs) }
    }
}

override fun onDestroy() {
    serviceScope.cancel()                      // cancelar em onDestroy, NÃO em onUnbind
    super.onDestroy()
}
```

- **`Dispatchers.Main.immediate`**, não `Default`: `setServiceInfo` mexe no estado de conexão, tem que ser na main thread.
- **Cancelar em `onDestroy`, não em `onUnbind`**: o serviço pode fazer unbind→rebind; cancelar no unbind vaza/quebra o collector no rebind.
- `.catch{}` no collect é obrigatório (senão uma leitura corrompida derruba o serviço de acessibilidade).

### 3. `applyMonitoredScope()` — semântica exata do `setServiceInfo`
```kotlin
private fun applyMonitoredScope(pkgs: Set<String>) {
    if (pkgs.isEmpty()) return                 // NUNCA passar array vazio (ver nota)
    val info = serviceInfo ?: return           // getServiceInfo() é null antes de conectar
    info.packageNames = pkgs.toTypedArray()    // reutiliza o info existente
    info.canRetrieveWindowContent = false      // F4 vira isso, não o F3
    serviceInfo = info                         // = setServiceInfo(info)
}
```

- **Copiar o `serviceInfo` existente e mutar só `packageNames`.** Construir um `AccessibilityServiceInfo()` novo apaga `eventTypes`, `feedbackType`, `flags`, `notificationTimeout` vindos do XML — armadilha silenciosa que só detona no PR do F4.
- **`packageNames = null` → escuta TODOS os pacotes.** Array vazio tem semântica ambígua/inconsistente entre versões (foi historicamente tratado como "todos" em várias). Como o repositório rejeita set vazio, **nunca** chegue a passar `emptyArray()`; faça early-return. Este é o ponto de correção mais importante dado o requisito de set congelado.
- Deixar o `accessibility_service_config.xml` **sem** `packageNames` (o runtime sobrescreve).

### 4. Repositório — validação atômica dentro do `edit{}`
`stringSetPreferencesKey("monitored_packages")`. Fazer a validação (≥1 e "pertence ao catálogo") **dentro** de um único `dataStore.edit{}` (read-modify-write atômico), não antes (evita race), e retornar um `Result`/sealed para o ViewModel não flipar o toggle otimisticamente:

```kotlin
suspend fun setSelected(pkg: String, selected: Boolean): Result<Unit> {
    // ... require(pkg in CuratedApps.validPackages); calcula next; require(next.isNotEmpty()) ...
}
```

- Leitura: `prefs[KEY]` é `null` quando nunca foi escrito vs `emptySet` quando limpo. Como empty é rejeitado na escrita, `?: emptySet()` na leitura é seguro; leitura vazia → UI de empty-state.
- **Trave o lock do estudo no NÍVEL DE ESCRITA** (repo rejeita escrita quando `isSelectionLocked`), não só desabilitando toggles na UI. Toggle cinza é reforço cosmético; se o lock viver só na UI, um caminho de código futuro pode mutar o set no meio da coleta → **dataset cientificamente inválido.**

### 5. Navegação — sub-tela, não destino top-level
- Adicionar `@Serializable object MonitoredAppsRoute` **mas NÃO ao enum `Destination`** (o enum alimenta a nav rail/bottom bar — entraria como uma 4ª aba errada).
- `SettingsScreen` hoje não recebe `NavController`. Mudar assinatura para receber um lambda: `SettingsScreen(onNavigateToMonitoredApps: () -> Unit)` (mais testável que passar o `NavController`). **Corrigir o `@Preview`** — ele quebra ao adicionar o parâmetro obrigatório (passar `{}`).
- No host: `composable<SettingsRoute> { SettingsScreen(onNavigateToMonitoredApps = { navController.navigate(MonitoredAppsRoute) }) }` + `composable<MonitoredAppsRoute> { MonitoredAppsScreen(onBack = { navController.popBackStack() }) }`.
- Adicionar affordance de voltar (`TopAppBar` com back) — o plano não menciona.

### 6. DI — provavelmente um no-op
Se `MonitoredAppsRepository` for `@Singleton @Inject constructor(dataStore: DataStore<Preferences>)` (igual `OnboardingRepository`), o Hilt **já provê por constructor injection** — não precisa de `@Provides`/módulo novo. A Task 5 é quase um no-op; só escreva um módulo se introduzir uma interface.

### 7. `studyStarted` sem setter — corrigir, não é "scaffolding aceitável"
O plano persiste `studyStarted` mas não entrega UI/mecanismo para setá-lo. Se `BuildScope.STUDY == true` no build do estudo e não há como ativar `studyStarted`, o **lock nunca engata** → o requisito central de "set congelado" fica sem aplicação. Mínimo: persistir com um `setStudyStarted(true)` chamável (mesmo que via gesto escondido / adb / constante por coorte) para que o freeze seja **alcançável e testável**. Não distribuir o build do estudo com a flag inerte.

### 8. Dependências de teste ausentes (pré-requisito omitido)
`app/build.gradle.kts` só tem `testImplementation(libs.junit)`. Para os testes que o plano pede, adicionar antes: `kotlinx-coroutines-test`, `app.cash.turbine:turbine`, e usar `PreferenceDataStoreFactory.create` com `TemporaryFolder` para o teste do repo. O serviço não é testável em JVM unit (APIs framework-bound) → extrair a lógica pura (`Set<String>` → `Array<String>`) numa função testável e deixar o serviço um adaptador fino.

### 9. Integridade de dados do estudo (não fecham silenciosamente)
- **Drift de package = perda silenciosa de dados.** TikTok é `com.zhiliaoapp.musically` na maioria dos mercados mas `com.ss.android.ugc.trill` em outros; um participante com a variante fica sem monitoramento, sem erro nem log. Mitigação barata: no connect, logar quais packages monitorados estão de fato instalados (`PackageManager.getPackageInfo` em try/catch) e, na tela, mostrar um badge "instalado / não instalado". Como o estudo congela versões no aparelho de referência, o risco é baixo *durante a coleta* — mas o log/badge evita descobrir tarde. Registrar package + versionCode resolvidos no início do estudo.
- **Serviço desabilitado é o caso comum no F3** (sem F4 ainda, o usuário não tem motivo funcional para habilitar). A seleção persiste no DataStore de qualquer jeito; ao habilitar, `onServiceConnected` reconcilia. **Mas** a verificação por logcat (Task 6) **só roda com o serviço habilitado** — adicionar isso como pré-condição explícita do passo de verificação.

---

## Passo a passo de implementação (ordem recomendada)

**Fase 0 — Pré-requisitos**
1. Confirmar `ResurfaceApplication` com `@HiltAndroidApp`.
2. Adicionar deps de teste em `libs.versions.toml` + `build.gradle.kts` (coroutines-test, turbine, datastore test).

**Fase 1 — Data layer** (`data/monitored/`)
3. `enum class SurfaceType { FEED, SHORT_VIDEO, MIXED }`.
4. `data class MonitoredApp(packageName, displayName, surface)`.
5. `CuratedApps`: `val all: List<MonitoredApp>` (IG=MIXED, TikTok=SHORT_VIDEO, YouTube=MIXED, X=FEED, Reddit=FEED), `val validPackages: Set<String>`, `fun surfaceFor(pkg): SurfaceType?`. Considerar variantes conhecidas (ex. TikTok `trill`).
6. `MonitoredAppsRepository` (`@Singleton @Inject constructor(DataStore)`, espelhando `OnboardingRepository`): `stringSetPreferencesKey`; `selection: Flow<Set<String>>`; `setSelected(pkg, Boolean): Result` com validação ≥1 + catálogo **dentro do `edit{}`** e respeitando o lock.
7. Teste unitário do repo (DataStore em `TemporaryFolder`): persistir/restaurar, rejeitar não-catálogo, rejeitar vazio, lock rejeita escrita.

**Fase 2 — BuildScope** (`config/BuildScope.kt`)
8. `const val STUDY`. Persistir `studyStarted` (DataStore, `booleanPreferencesKey`) com `setStudyStarted()` chamável + `studyStarted: Flow<Boolean>`. `isSelectionLocked = STUDY && studyStarted`. Testar a tabela-verdade das 4 combinações.

**Fase 3 — DI**
9. Constructor injection (sem módulo novo). Confirmar que `@AndroidEntryPoint` no serviço resolve contra `SingletonComponent`.

**Fase 4 — Serviço (reconcile-on-connect)**
10. `@AndroidEntryPoint` no `ResurfaceAccessibilityService`; `@Inject repo`; `serviceScope`; coletar `selection` em `onServiceConnected` (guardando double-launch); cancelar em `onDestroy`.
11. `applyMonitoredScope()`: copiar `serviceInfo`, `packageNames = toTypedArray()`, early-return se vazio, `canRetrieveWindowContent = false`, sem detecção. Logar escopo aplicado + check de instalados.

**Fase 5 — UI** (`ui/screens/monitoredapps/`)
12. `MonitoredAppsViewModel` (`@HiltViewModel`): combinar `CuratedApps.all` + `repo.selection` + `isSelectionLocked` num `StateFlow<UiState>` (por-app selecionado, contagem, último-app-travado, tudo-travado-se-locked); `toggle()` chama repo e trata rejeição.
13. `MonitoredAppsScreen`: `TopAppBar` com back, lista com `Switch`, contagem, empty-state, último toggle desabilitado, tudo desabilitado se locked, badge instalado/não.
14. Nav: `MonitoredAppsRoute` (fora do enum), registrar no host, `SettingsScreen` com lambda de navegação, corrigir `@Preview`, adicionar string resources.
15. Teste do ViewModel (`MainDispatcherRule` + fake/temp repo): toggle, enforce ≥1, lock desabilita.

**Fase 6 — Verificação no dispositivo**
16. Selecionar apps → matar/reabrir → confirmar persistência. **Habilitar o serviço** → confirmar `packageNames` no logcat no connect. Confirmar ≥1 e rejeição de vazio. Se `STUDY`, ativar `studyStarted` (hook de teste) e confirmar toggles travados. Rodar o check do re-escopo ao vivo em ≥1 aparelho OEM não-Pixel; tratar falha do live-update como **aceitável** desde que a reconciliação no connect funcione.

---

## Notas do council
- **Concordância:** F3 é viável; Hilt no serviço OK; repo = padrão `OnboardingRepository` (sem `@Provides`); rota fora do enum `Destination`; validação atômica no `edit{}`; nunca passar array vazio ao `setServiceInfo`; `studyStarted` sem setter é lacuna real.
- **Discordância (resolvida):** re-escopo dinâmico ao vivo — o council divide entre "cortar do F3" e "manter". Chairman ficou no meio baseado em evidência: **manter o serviço mas como reconcile-on-connect** (DataStore = fonte da verdade), não depender de live re-scope entre OEMs.
- **Ranking agregado dos revisores:** Rigorist > Pragmatist > Red-teamer > First-principles > Generalist.
- **Overrides do Chairman:** elevei dois pontos que só parte do council pegou — (1) lock do estudo no nível de escrita, não só na UI (integridade científica); (2) check de instalado/drift de package (evita coleta silenciosamente vazia). Rejeitei o corte total do serviço proposto pelo First-principles, porque deixaria a "tightening" de privacidade prometida aos participantes sem efeito real caso o F4 atrase.
