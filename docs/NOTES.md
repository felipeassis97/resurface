# Mindless Scroll — Proposta Final de Projeto de Pesquisa

### Detecção e Interrupção Gentil do "Modo Zumbi" em Redes Sociais — App Android + Pulseira Háptica ESP32 + IA on-device

*Documento final, sintetizado por revisão multi-perspectiva (3 rodadas de council + síntese). Mantém as correções técnicas das rodadas anteriores e incorpora as correções factuais da rodada final (mecanismo de FGS via Companion Device Manager; firmware RTP; baseline Android 16/risco 17; Gemini Nano em Pixel 9/10 + ML Kit Prompt API; localização re-justificada por privacidade). Substitui `proposal_v2.md` e a documentação técnica de `resurface/README.md`.*

---

## Resumo

Pessoas frequentemente descrevem "se perder" rolando feeds de redes sociais — sessões longas, rápidas e passivas que terminam sem que tenham decidido começá-las e que deixam uma sensação de tempo desperdiçado. Este projeto investiga se é possível **reconhecer comportamentalmente** esse estado, no próprio dispositivo e sem ler o conteúdo consumido, e **devolver gentilmente a decisão ao usuário** — sem bloquear, julgar ou impedir o uso intencional.

Construímos um sistema Android completo — um "aliado", não um bloqueador — que: observa *como* se rola dentro dos apps escolhidos pelo usuário (velocidade, continuidade, duração, passividade), distingue lazer intencional de uso mecânico, e responde com um lembrete gentil e configurável, personalizado por IA generativa local (Gemini Nano), no tom que o usuário escolhe. Numa segunda versão, uma **pulseira háptica ESP32** entrega o lembrete como um toque no pulso.

A pesquisa é entregue em **dois estudos encaixados**: **Estudo 1 (V1, app-only)** valida o detector comportamental e mede se uma interrupção on-screen/on-phone deconfundida encurta a rolagem continuada, contra um contrafactual intra-pessoa (sham); **Estudo 2 (V2, pulseira)** — condicional ao V1 ter embarcado e ao acúmulo de episódios ser suficiente — adiciona o braço háptico no pulso e testa a pergunta distintiva: **o *locus* da entrega importa — um canal háptico novo no pulso (off-body) supera os canais já habituados do telefone?** Tudo roda on-device; nenhum dado comportamental deixa o aparelho.

Posiciona-se na interseção de JITAI (Just-In-Time Adaptive Interventions), Self-Determination Theory (suporte à autonomia) e microintervenções de atrito (one-sec, ScrollStop), estendendo-as a um canal háptico vestível e a personalização generativa local.

> *Este documento descreve um produto completo e o primeiro estudo rigoroso que se pode extrair dele. Lemos as duas coisas juntas de propósito: a **visão** dá a razão de o trabalho importar; o **estudo** dá a razão de a banca poder acreditar nele.*

---

## 1. Motivação e contexto

O uso de redes sociais não é, em si, o problema — e tratá-lo como tal (bloqueadores, controle parental, contadores de tempo punitivos) falha justamente com quem mais quer mudar: produz reatância, vergonha e abandono do app. O alvo deste trabalho é mais estreito e mais honesto: o **episódio de rolagem passiva e automática** — a sessão que a pessoa não escolheu conscientemente começar nem continuar, e que ela mesma, depois, descreve como "tempo perdido no piloto automático".

A intuição central: não se trata de impedir o uso, mas de **devolver um momento de escolha** — um toque suave que pergunta, sem julgar, *"ainda é isto que você quer agora?"*. A aposta de pesquisa é que o **canal sensorial** desse toque importa, e que um canal novo, fora do corpo (o pulso), pode quebrar o transe melhor do que o telefone — o mesmo aparelho onde o transe acontece e cujas vibrações o usuário aprendeu a ignorar.

---

## 2. Trabalho relacionado e posicionamento

Três corpos de trabalho convergem sobre este problema; nenhum o cobre por inteiro.

**2.1 Uso ativo vs. passivo de redes sociais (Verduyn et al.).** Estabelece *que* a rolagem passiva se associa a piores desfechos de bem-estar, mas opera em nível de questionário e pós-hoc. Não há detecção comportamental, em tempo real, no dispositivo, do estado passivo. Usamos essa distinção como âncora teórica da nossa definição operacional (uso ativo *suprime* a intervenção; uso mecânico a *sinaliza*).

**2.2 JITAI e Self-Determination Theory.** Frameworks de Just-In-Time Adaptive Intervention (Nahum-Shani et al.) prescrevem entregar o estímulo certo no momento de receptividade; a SDT fundamenta a postura **autônomo-suportiva** (informar e devolver a escolha, em vez de controlar — evitando reatância). Operamos *dentro* desses frameworks; não os inventamos.

**2.3 Microintervenções de atrito (one-sec, ScrollStop, linha CHI).** Demonstram que pequenas fricções alteram comportamento. Mas **todas atuam na tela e no telefone** — o canal já saturado de notificações ao qual o usuário está cronicamente habituado.

**2.4 Háptica como canal de nudge.** Notificações hápticas e toques vestíveis para alerta são um subcampo maduro de HCI. O recorte novo é "háptico *off-body* para quebrar o transe de rolagem", comparado de forma controlada contra os canais on-screen/on-phone que a literatura assume por padrão.

**2.5 A lacuna que ocupamos.** A interseção das três: uma intervenção JITAI, entregue por um canal háptico *off-body* deliberadamente não-saturado, disparada por um detector comportamental que roda *no dispositivo, em tempo real*, sobre o estado de rolagem passiva — comparada de forma controlada contra os canais que a literatura existente assume. A pergunta não é "intervenções funcionam?" (sabe-se que sim), mas "**o locus da entrega importa, e um canal háptico novo no pulso supera os canais habituados do telefone?**".

---

## 3. Lacuna e perguntas de pesquisa

**Geral:** investigar se é possível detectar comportamentalmente, on-device e sem ler conteúdo, episódios de rolagem passiva prolongada, e qual *modalidade* de interrupção respeitosa melhor os encerra — dentro de um modelo autônomo-suportivo.

A pesquisa é **encaixada em dois estudos** (ver §5 e §11 para a justificativa do encaixe). As perguntas em ordem de prioridade:

- **RQ1 (primária — Estudo 1, app-only):** Uma interrupção deconfundida na tela/telefone (overlay e háptico do telefone) reduz a rolagem continuada versus a ausência de intervenção (sham), num desenho within-subjects com contrafactual intra-pessoa?
- **RQ2 (Estudo 1):** É possível caracterizar e validar um detector comportamental on-device de episódios de rolagem passiva prolongada, contra ground truth de autorrelato diferido?
- **RQ3 (stretch, offline):** Um classificador LiteRT leve, treinado sobre os dados rotulados coletados, supera a linha de base heurística?
- **RQ-pulseira (Estudo 2 — V2, condicional):** Adicionando um braço de háptico no pulso (ESP32), o **locus off-body** supera o háptico do telefone e o overlay na quebra do episódio? Roda **somente se** o V1 embarcou, o acúmulo de episódios da Fase 1 limpar o mínimo pré-registrado, e o hardware passar na bancada.
- **RQ-nudge (secundária condicional):** Dentro do braço de overlay, um nudge personalizado gerado por LLM on-device (Gemini Nano, tom escolhido pelo usuário) melhora desengajamento e aceitação versus um banco curado? Roda só sob as mesmas condições de folga + disponibilidade de hardware/modelo.

**O princípio do desacoplamento (carga estrutural):** a comparação de modalidades exige que o gatilho seja *consistente entre os braços*, não *correto*. A mesma heurística dispara independentemente da condição; só a modalidade de resposta é randomizada. RQ1 sobrevive mesmo se a acurácia de detecção (RQ2) for modesta. A recíproca não vale — por isso a comparação de modalidades é a espinha.

**Decisão de escopo (síntese do council — ver §5):** a pergunta *distintiva* do projeto é a do locus háptico (off-body). Mas, ao acúmulo de episódios realista (~5/braço/participante), o contraste telefone-vs-pulso é estatisticamente frágil e confundido por **novidade de canal** (§17). A claim que **sobrevive** é "*qualquer* háptica deconfundida supera não-intervenção" — um resultado **on-phone, sem pulseira**. Por isso a espinha da tese é o **Estudo 1 (app-only)**, e a pulseira é um **segundo estudo que se ganha o direito de rodar** — exatamente o que "uma segunda versão" significa no plano do autor.

---

## 4. A visão: um aliado, não um bloqueador (produto completo)

Antes de carvar o estudo, a visão inteira, no presente — é isto que se constrói ao longo das versões:

1. **Sempre presente, nunca intrusivo.** O app fica *armado* de forma leve e só fica *ativo* quando você entra num app que *você* escolheu monitorar, num horário que *você* permitiu.
2. **Aprende quem você é.** Um onboarding curto traça um perfil on-device — rotina, hobbies, objetivos, e o **tom** em que você quer ser abordado (amigável / direto / neutro).
3. **Fala na sua voz.** Quando percebe o piloto automático, a IA local gera um lembrete que ressoa com *seus* objetivos — *"você comentou que queria correr mais — a noite tá boa"* — em vez de "você usou demais".
4. **Um toque calmo.** Numa segunda versão, uma pulseira no pulso entrega o lembrete como um toque háptico — você nem precisa olhar a tela.
5. **Um espelho gentil.** Um "retrato de atenção" mostra *quando* e *onde* esses episódios se agrupam (madrugada, ao acordar) — em linguagem de apoio, nunca de julgamento.
6. **Totalmente seu.** Apps monitorados, dias, faixas de horário, sensibilidade, horas de silêncio, modalidade — tudo configurável. Deixar você traçar os próprios limites *é* a decisão autônomo-suportiva central.
7. **Privado por construção.** Detecção, analytics e IA generativa rodam no aparelho. Nenhum dado comportamental sai. Nunca lê o conteúdo que você consome — só *como* você rola.

**A separação fundamental, mantida em todo o documento:** o sistema detecta um **envelope comportamental** (rápido + contínuo + longo + passivo) que *correlaciona* com mindless scrolling — **nunca o estado cognitivo em si**. Telemetria idêntica é produzida por estados opostos (imersão intencional prazerosa vs. transe), e o modo zumbi verdadeiro pode ser *lento* (o drift da madrugada). Nenhum sensor de scroll vê "mindlessness"; afirmar o contrário é indefensável diante de uma banca.

---

## 5. Como ler escopo de produto vs. estudo (a tensão central, resolvida)

Este projeto tem **dois artefatos** que coexistem de propósito, e quase toda confusão em revisões anteriores veio de tratá-los como um só:

1. **A especificação de produto/sistema** (§4, §6–9): o sistema completo da visão do autor. É o que se **constrói**, ao longo de versões.
2. **O escopo de pesquisa** (§3, §10–13): um subconjunto deliberadamente reduzido e *deconfundido*, instrumentado para responder a perguntas publicáveis. É o que se **mede**.

**Regra de ouro:** nada da visão é descartado. Todo componente que ameaça a validade do estudo é **mantido no produto e congelado/desligado/reduzido na janela de medição** — com o trade-off exato nomeado na §11. O produto completo é o destino; o estudo é o primeiro passo defensável até lá.

**O encaixe em versões (decisão de roadmap):**

| Versão | O que é | Papel na pesquisa |
|---|---|---|
| **V0 — artefato de estudo** | App-only: heurística + banco curado + 3 braços (overlay / háptico telefone / sham) | **Estudo 1** (RQ1, RQ2). A espinha. Sem hardware, sem Nano, sem localização. |
| **V1 — aliado com IA** | + Gemini Nano (Tier 3 livre), perfil rico (hobbies/objetivos), personalização por localização | Produto. Demonstrado; partes entram no estudo só de forma controlada (RQ-nudge). |
| **V2 — pulseira** | + pulseira ESP32 háptica (BLE), refinada (LRA) | **Estudo 2** (RQ-pulseira), condicional. A pergunta distintiva, ganha como upside. |

Por que a pulseira é V2 e não V1: (a) é o pedido explícito do autor ("uma segunda versão"); (b) põe todo risco de hardware (flakiness BLE, decisão ERM→LRA, matching de intensidade, logística de empréstimo) *fora* do caminho crítico da tese; (c) a aritmética de acúmulo de episódios (§10) prevê que o contraste de 4 braços fica subdimensionado, e a própria análise de limitações (§17) mostra que a claim que sobrevive é a on-phone. Liderar com a claim mais fraca seria apostar a tese no ponto mais frágil do documento.

---

## 6. APIs Android, recursos de dispositivo e plataforma

Tabela precisa. Caveats verificados contra a trajetória Android **16 (baseline de meados de 2026) / 17 (risco vivo)**. *Nota de plataforma: o Android 16 embarcou em meados de 2025; em meados de 2026 ele é o alvo maduro e o Android 17 é a release iminente — daí o congelamento de OS durante a coleta (§15, Risco 8) ser não-opcional.*

| API / Recurso | Para quê | Permissão / declaração | Caveat (Android 16, mid-2026) |
|---|---|---|---|
| `UsageStatsManager` | App em foco, duração de sessão, hora do dia, contexto de primeiro desbloqueio | `PACKAGE_USAGE_STATS` (special-access, via `Settings.ACTION_USAGE_ACCESS_SETTINGS`) | Dados agregados/atrasados (buckets) — **não confiar para duração ao vivo**; usar timer próprio na abertura. Robusto e durável a políticas. |
| `AccessibilityService` | Scroll (`TYPE_VIEW_SCROLLED`), janela (`TYPE_WINDOW_STATE_CHANGED`), conteúdo (`TYPE_WINDOW_CONTENT_CHANGED`), texto/foco (`TYPE_VIEW_TEXT_CHANGED`, `TYPE_VIEW_FOCUSED`) — escopado por `packageNames` | `BIND_ACCESSIBILITY_SERVICE`; habilitado manualmente. Uso não-acessibilidade → sideload de pesquisa | **NÃO tem `foregroundServiceType`** — é serviço *system-bound* (corrige erro de v1). Mantido vivo sem notificação; re-habilitado após reboot. |
| `SYSTEM_ALERT_WINDOW` | Overlay de consciência (braço A) | special-access, via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` | Overlay *visível* é uma das isenções reais de FGS-start em background (Android 15+). Cobre o braço A. **Não cobre os braços hápticos** (que não mostram overlay) — ver Companion Device Manager abaixo. |
| **Companion Device Manager (CDM)** | Parear e manter o link com a pulseira; **iniciar o FGS de BLE do background** | `CompanionDeviceManager.associate()` + **`REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND`** | **Correção factual da rodada final.** A isenção de FGS-start é via *CDM*, **não** via "AccessibilityService habilitado" (esse item **não existe** na lista oficial de isenções). CDM também **dispensa `BLUETOOTH_SCAN`/localização** (descoberta via UI de pareamento do sistema). |
| FGS tipo `connectedDevice` | Manter o link BLE vivo durante a sessão | `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_CONNECTED_DEVICE`; notificação persistente | **Não é time-capped** (ao contrário de `dataSync` ≈ 6h e `shortService` ≈ 3min — a evitar). Uma vez iniciado por uma isenção válida (CDM), roda em background. |
| BLE / GATT (`BluetoothGatt`) | Conversar com o ESP32 | `BLUETOOTH_CONNECT` (runtime, API 31+). *Com CDM, `BLUETOOTH_SCAN` é dispensável.* | Alvo API 36 (Android 16). Permissões legadas `BLUETOOTH`/`BLUETOOTH_ADMIN` com `maxSdkVersion="30"`. |
| `SensorManager` (IMU do telefone) | Proxy de flick — **opcional**, só se a Fase 0 mostrar surfaces pobres em eventos | Nenhuma para acelerômetro base | Hedge, não sinal primário. IMU da pulseira: cortado. |
| `Vibrator` / `VibratorManager` | Háptico do telefone (braço B). `VibratorManager.getDefaultVibrator()`; `VibrationEffect` (primitivas `PRIMITIVE_TICK`/`CLICK` quando suportadas) | Nenhuma | A maioria dos telefones-alvo usa **LRA**; isto **é** o confound ERM-vs-LRA (§10, §13). |
| Notificações | Notificação do FGS + nudges | `POST_NOTIFICATIONS` (runtime, API 33+) | Conceder no onboarding. |
| ML Kit GenAI / **Gemini Nano** (AICore / Private Compute Core) | IA generativa on-device: nudges (RQ-nudge) e resumos | Sem permissão; gated por disponibilidade de dispositivo + AICore + rollout do modelo + storage | **Correção factual:** o Nano mais recente embarca em **Pixel 10** (ago/2025); a geração de texto livre que a RQ-nudge precisa é a **ML Kit GenAI *Prompt* API** (distinta de Summarization/Rewriting), com matriz de dispositivos própria (Pixel 9/10-classe). **Nano ≠ sucessor do NNAPI.** Checagem em runtime + fallback obrigatório. |
| **LiteRT** (ex-TFLite) | Modelo leve de classificação (RQ3, offline) | Nenhuma | **NNAPI deprecado no Android 15**; aceleração via **LiteRT GPU delegate** ou **Google Play Services (`AccelerationService`)**. NNAPI era delegate de aceleração para LiteRT — categoria distinta do Nano (runtime de LLM). |
| `DataStore` / `Room` | Config, perfil, logs estruturados on-device | Nenhuma | Logs exportáveis = dado bruto da avaliação. |
| `BroadcastReceiver` `BOOT_COMPLETED` | Reagir a reboot — **complemento apenas** | `RECEIVE_BOOT_COMPLETED` | Android 15+ restringe iniciar FGS do boot. O re-habilitar do AccessibilityService é a resiliência real; nada sobe no boot. |
| `FusedLocationProvider` — **coarse, opcional, PRODUTO apenas** | Contexto de localização para personalização (V1) | `ACCESS_COARSE_LOCATION` (runtime); **nunca** fine nem background-location | **Fora do build de estudo** (§11). No produto: coarse, on-device, **geofence rotulado pelo próprio usuário** (pin "casa"), nunca inferido, nunca coordenadas persistidas. |

### Correções de plataforma mantidas explicitamente (não reintroduzir)

1. **`AccessibilityService` NÃO tem `foregroundServiceType`** — é serviço system-bound; o tipo `connectedDevice` pertence ao FGS de BLE, que é serviço separado.
2. **O FGS de BLE é iniciado do background via Companion Device Manager.** Quando o app monitorado vem ao foreground, o **processo deste app está em background**. O start do FGS é permitido porque a pulseira é um *companion device associado* e o app declara `REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND` — uma isenção **que existe** na lista oficial. *(A afirmação anterior de "isenção por AccessibilityService habilitado" estava errada e é retirada.)* Como hedge, o cliente BLE também vive no processo do AccessibilityService (manter um socket aberto num processo já vivo é legal e distinto de *iniciar* um FGS).
3. **Gemini Nano ≠ sucessor do NNAPI.** NNAPI (delegate de aceleração para LiteRT, deprecado no Android 15 → sucessor LiteRT GPU delegate / Play Services) vs. Gemini Nano (runtime de LLM no AICore). Camadas não relacionadas.

---

## 7. Funcionalidades do app e fluxos

### 7.1 Onboarding (supervisionado no estudo; ~30–40 min)

1. Consentimento informado + termo (CEP/IRB).
2. Concessão de permissões, em sequência (cada uma é tela do sistema, sem callback — por isso supervisionado): AccessibilityService → Usage Access → Overlay → Notificações → (V2) pareamento da pulseira via **CDM** (a UI do sistema descobre e pareia; dispensa permissão de scan).
3. Seleção de apps monitorados (define `packageNames`).
4. Questionário de perfil (5–10 itens, ~3 min): rotina + janelas declaradas + tom preferido. *(Hobbies/objetivos: produto V1 — §11.)*
5. (V2) Matching de intensidade percebida por participante (telefone vs. pulseira) — passo de deconfounding.

### 7.2 Pipeline de decisão (produto completo — JITAI)

```
1. CONFIG GATE      → app monitorado? dentro de janela ativa? fora de horas de silêncio?
2. DETECÇÃO         → estado mecânico (envelope)?
3. INTENCIONALIDADE → uso claramente ativo? (digitação/dwell alto → suprime)
4. DECISÃO JITAI    → timing/contexto + perfil + histórico
5. PERSONALIZAÇÃO   → IA gera a mensagem (perfil+contexto), tom autônomo-suportivo
6. DISPATCH         → overlay / háptico do telefone / (V2) comando BLE → pulseira
7. LOG + APRENDIZADO→ registra resposta → calibra limiar pessoal
```

**No build de estudo este pipeline é cirurgicamente reduzido** (§11): passo 1 gateia **só intervenções** (nunca medição); passo 4 vira randomização por episódio; passo 5 é congelado (banco curado), exceto no sub-experimento RQ-nudge; passo 7 **não calibra limiar** (gatilho mantido constante).

### 7.3 Intervenção (single-step, autônomo-suportiva)

- **Overlay (braço A):** cartão de escolha — *"Você está rolando há X minutos — ainda é isto que você quer agora?"* com Continuar / Pausa / Ver-meu-tempo. Continuar é sempre um toque, nunca penalizado. **Único braço com texto informacional.**
- **Háptico do telefone (braço B) / pulseira (braço C, V2):** um toque *envelope-matched*. Sem escada de escalada no estudo (escalada adiciona reatância e confunde; trabalho futuro).
- **UI auditada:** nunca rotula o comportamento como "mindless"/"zumbi"; enquadramento tempo-e-intenção, nunca julgamento.

---

## 8. Metodologia de detecção

### 8.1 Sinais e fusão

- **`UsageStatsManager` (fundação — robusta, durável):** app em foco, duração de sessão, hora do dia, primeiro desbloqueio.
- **`AccessibilityService` (enriquecimento — mais rico, mais frágil):** eventos de scroll/página, estrutura de pausas, churn de conteúdo. **Todo texto de nó é descartado na captura; só métricas numéricas/categóricas persistem.**
- **IMU do telefone:** opcional, só se a Fase 0 mostrar surfaces de vídeo-curto pobres em eventos.

Quanto mais a detecção se apoia no `UsageStatsManager`, mais à prova de políticas/futuro ela é; Accessibility é enriquecimento cuja perda *degrada*, mas não destrói, o detector.

### 8.2 Supressão por intencionalidade (a camada lazer-vs-zumbi)

Mesmo dentro de uma janela ativa, sinaliza-se **só o estado mecânico**:
- **Suprimem:** digitação/busca (`TYPE_VIEW_TEXT_CHANGED`, foco de input), **dwell alto** num conteúdo, navegação deliberada, sessões curtas.
- **Sinalizam:** scroll rápido + alta continuidade (poucas pausas) + sessão longa + consumo passivo.
- Âncora: distinção **uso ativo vs. passivo** (Verduyn et al.). **Caveat honesto:** dwell alto também ocorre no transe (alguém parado num vídeo hipnótico). A supressão por dwell reduz falsos positivos sobre uso ativo *deliberado*; não fecha o gap de construto.

### 8.3 O problema Feed-vs-vídeo-curto (gate da Fase 0)

Risco fatal #1, razão de a Fase 0 ser go/no-go **antes de comprometer arquitetura**:
- **Feed (Instagram Home):** lista rolável → `TYPE_VIEW_SCROLLED` frequente; scrolls/min e velocidade recuperáveis.
- **Vídeo curto (TikTok/Reels):** **pagers full-screen** (SurfaceView/Compose), **não** listas. Podem emitir `TYPE_VIEW_SCROLLED` esparso/nenhum; deltas de scroll podem vir não-preenchidos (só frequência como proxy). **O sinal mais rico está na surface *menos* importante (feed) e pode faltar na *mais* importante (Reels/TikTok).** Medir, não assumir.

Resultados: *Pass* (sinal por-swipe/vídeo recuperável → segue) · *Partial* (só churn "vídeos/min" → features re-escopadas para duração+churn+pausas) · *Fail* (nada usável → fallback UsageStats e enquadramento ajustado **antes** de finalizar). **Adicionalmente: fixar as versões dos apps monitorados no device** — IG/TikTok mudam de build mensalmente e um evento que existe hoje pode sumir.

### 8.4 Definição operacional (envelope, não estado)

**Episódio de rolagem passiva prolongada:** consumo sustentado (sem digitação/busca), alta continuidade (pausas poucas/curtas), sessão além de um limiar de duração, com peso de contexto (ex.: madrugada). Reivindicado explicitamente como **proxy comportamental**. Limiares fixados só após a Fase 0.

### 8.5 Estágios do classificador

1. **Heurística** (build de estudo): limiares combinados transparentes + supressão por intencionalidade. Sem dataset, depurável, suficiente — o estudo precisa de gatilho *consistente*, não perfeito.
2. **LiteRT** (offline, pós-estudo): treinado sobre rótulos das fases, avaliado contra a heurística. **Nenhum LLM no caminho de detecção.**

### 8.6 Ground truth

Probes retrospectivos diferidos em fronteiras naturais de sessão, sobre episódios sinalizados E aleatórios (controle). **Regra de atribuição probe↔episódio:** refere-se só ao episódio mais recente; se vários compartilham uma fronteira, alveja o último e os anteriores ficam "unprobed" (nunca herdam rótulo). Limitações reportadas: erro de recall, reatividade residual, esparsidade de fronteira, e recall plausivelmente *correlacionado ao construto* (transe mais profundo → pior recall, enviesando precisão/recall não-conservadoramente).

---

## 9. Camada de IA / personalização (três tiers)

A IA generativa preserva a visão — comunicação personalizada, no tom do usuário, on-device — sem contaminar o experimento.

**Tier 1 — Resumos reflexivos (CONGELADOS durante a coleta).** Um dashboard reflexivo é **co-intervenção de automonitoramento** (técnica de mudança de comportamento robusta): não enviesa o contraste *entre braços*, mas move a linha de base, emaranha a habituação e *prima as respostas dos probes da Fase 1*. **Durante a coleta:** resumos-template idênticos para todos (ou desabilitado); aberturas logadas como covariável; insights personalizados só no **debrief de saída**. Resumos Nano embarcam no produto.

**Tier 2 — Sub-experimento curado-vs-Nano (RQ-nudge, condicional).** Só no braço de overlay. Texto randomizado curado vs. Nano-gerado **ao vivo** (não cacheado — cache não condiciona no contexto do episódio atual, derrotando a própria manipulação). Texto exato logado. **Condições (expectativa honesta: provavelmente não roda):** funil comporta o split (a ~5 episódios/overlay/participante as células esvaziam) + frota passa na checagem da **Prompt API** em runtime + folga de cronograma. Senão, reverte a protótipo demonstrado.

**Tier 3 — Geração livre (produto V1) — inclui localização.** Geração Nano irrestrita por tom/contexto, incluindo personalização por **geofence rotulado pelo usuário** (*"você comentou que queria correr — a noite tá boa, e você marcou que está em casa"*). **Fora do build de estudo**, demonstrado N=1 self, firewalled de participantes.

**Engenharia (todos os tiers):** interface única `NudgeSource = NanoGenerator | CuratedBank` (fallback **obrigatório**, verificado em runtime — disponibilidade da Prompt API varia por dispositivo/rollout/storage mesmo em hardware "suportado"); pré-geração+cache **só no produto** (Tier 3), nunca no Tier 2; restrições de prompt impõem tom/comprimento/registro autônomo-suportivo, sem vocabulário de vergonha, dados de perfil só por slots definidos (estudo: hora+app; produto: +hobby/rotina/local); tudo no AICore/Private Compute Core. Esforço: **3–4 semanas elapsed**, agendado **depois** da integração do Estudo 1 — nunca no caminho crítico.

---

## 10. Desenho do estudo

### Fase 0 — Gate + IRB paralelo (semanas 1–3)

1. **Spike de instrumentação (go/no-go):** AccessibilityService descartável loga eventos crus de IG Feed, IG Reels e TikTok no device real (§8.3), com versões dos apps fixadas.
2. **Protocolo IRB/CEP submetido agora**, cobrindo **ambos os estudos** (V1 app-only e V2 pulseira) — latência de revisão é caminho crítico oculto.
3. **Logger piloto passivo (self, N=1)** para depurar captura/logging — firewalled de toda avaliação.

### Fase 1 — Estudo 1: validação de detecção + comparação on-phone (inicia na aprovação do IRB — gate duro)

**Sub-fase 1a (só-logging, sem window-gating):** mede o stream *não-filtrado*; probes diferidos em fronteira sobre episódios sinalizados + aleatórios. Saída: precisão/recall da heurística contra rótulos externos + dataset rotulado (RQ3) + inputs do funil. N≈10–20; duração ditada por **acúmulo de episódios** (1–3 semanas como estimativa).

**Sub-fase 1b (comparação de modalidades on-phone, 3 braços):** within-subjects, randomizado por episódio, gatilho constante:

| Braço | Estímulo |
|---|---|
| A | Overlay (cartão de escolha; único com texto) |
| B | Háptico do telefone (`Vibrator`) |
| D | Sham / withhold (detectado, sem intervenção — contrafactual) |

**Coorte separada:** participantes da Fase 1 não participam do Estudo 2 (evita contaminar baseline).

**Em paralelo (trilha build/bench, semanas 4–8):** firmware ESP32, cliente BLE + reconexão (CDM), verificação de overlay Android 16, caracterização por acelerômetro dos hápticos, matching de intensidade percebida. Alimenta o **Estudo 2**, não o crítico.

### Gate: simulação de funil (fim da Fase 1)

Análise ~1h sobre dados passivos: aplica janelas + limiar + split de braços + restrições de contexto; projeta **episódios/braço/participante**. Prossegue só se limpar mínimo pré-registrado (**alvo ≥8–12/braço/participante**). Aritmética realista (~2 episódios/dia → ~5/braço em 2 semanas) **provavelmente não limpa o 4-braços** — daí o Estudo 1 ser 3 braços (on-phone) e a pulseira ser condicional. **Fallbacks quantitativos pré-registrados agora:** estender uso até X semanas, relaxar limiar de duração para Y, ou abandonar a secundária de habituação — decidido aqui, não sob pressão.

### Fase 2 — Estudo 2: pulseira (V2, condicional ao funil + V1 embarcado)

Adiciona o **braço C (pulseira)** ao desenho. **Controles de deconfounding:**

- **Envelope-matching (não waveform-matching):** o atuador do telefone (**LRA**) e o motor da pulseira (**ERM**) são mecanismos diferentes — no ERM frequência e amplitude são **acopladas**, então waveform-matching rigoroso é **fisicamente impossível**. Casa-se **envelope** (onset, duração, intensidade percebida), caracteriza-se ambos com acelerômetro, e adiciona-se **matching de intensidade percebida por participante**. **Decisão da Fase 1: upgrade da pulseira para LRA** (Precision Microdrives C10-100; DRV2605L suporta LRA) — barato relativo ao dano de um ERM-pulso vs. LRA-telefone no contraste-cabeça.
- **Contexto de coleta casado:** ou todos os braços free-living, ou o contraste B-vs-C só em **blocos supervisionados casados** rodados identicamente para ambos. Episódios free-living de telefone nunca comparados a episódios de lab da pulseira.
- **Entrega da pulseira por intention-to-treat (ITT):** um toque disparado-mas-não-entregue (drop BLE) **permanece no denominador do braço C** (analisado ITT *e* per-protocol), com taxa de entrega reportada como figura de confiabilidade-cabeça e covariável. *(Excluir só as falhas de C flacharia o braço C por sobrevivência — assimetria que infla exatamente o contraste sob suspeita.)*
- **Sem texto nos braços hápticos** (locus-vs-locus, não informação-vs-buzz); **latência BLE logada por evento**.

**Desfecho primário (pré-especificado):** tempo de scroll continuado após o cue (cue-to-disengagement) vs. o sham intra-pessoa. Secundários (exploratórios): pausa-vs-dispensa, latência, aceitação/conforto, habituação (braço × índice-de-episódio). **N≈10–15; episódios/braço/participante — não n — é a restrição vinculante.** Dashboard congelado durante a coleta.

---

## 11. Matriz Produto-vs-Estudo — o que está ligado e o que é congelado

*Nada nesta tabela é cortado do projeto. Cada linha descreve o que está **ligado no produto** e o que é **temporariamente congelado** para que o estudo possa atribuir causa. O produto completo é o destino; o estudo é o primeiro passo defensável.*

| Componente | No produto (V1/V2) | Na janela de medição | Por quê |
|---|---|---|---|
| Sensibilidade por janela | Configurável | **Um limiar standard** | Limiar variável por hora-da-semana quebra o gatilho-constante e torna RQ2 mistura de operating points. |
| Nudge IA livre | Nano livre (Tier 3) | **Banco curado** (+ RQ-nudge controlado, condicional) | LLM livre como estímulo primário gera confounds; re-entra como secundária controlada e logada. |
| Localização | Geofence rotulado pelo usuário | **Desligada** | Adiciona variável não-controlada ao estímulo e superfície de privacidade/CEP, sem consumidor no experimento. |
| Dashboard Tier-1 | Resumos Nano sempre-on | **Template congelado** | Co-intervenção de automonitoramento: move baseline, prima probes. |
| Contexto lab-vs-campo | Free-living | **Casado entre braços** | Pulseira-em-lab vs. telefone free-living quebra o contrafactual. |
| Atuador (ERM/LRA) | Qualquer um | **LRA recomendado** | ERM-pulso vs. LRA-telefone embute o confound de qualidade no contraste-cabeça. |
| Aprendizado por feedback (Camada 3) | Calibra limiar pessoal | **Desligado** | Calibração por-usuário ao longo do tempo viola o gatilho-constante. |
| Hobbies/objetivos no perfil | Slots de personalização | **Cortados** (só hora+app) | Único consumidor era a personalização Tier-2/3. |
| Pulseira | V2 | **Estudo 2 condicional** | Tira o risco de hardware do caminho crítico da tese. |

---

## 12. Plano estatístico (pré-registrado)

- **Modelo primário (Estudo 1, 3 braços):** cue-to-disengagement (duração, com censura). Para n≈10–15 e desfecho de duração, **modelo linear misto sobre log-tempo** com **participante como intercepto aleatório** (slope para braço se convergir) é suficiente e defensável; o modelo de sobrevivência (Cox frailty) fica reservado para se a densidade de episódios permitir.
- **Estimando:** redução no tempo de scroll continuado vs. os próprios episódios sham (braço D = referência).
- **Covariáveis:** sequência/carryover (braço anterior), tempo-no-estudo, hora do dia, surface do app, flag in-window. *(Estudo 2 acrescenta: taxa de entrega BLE, contexto de coleta.)*
- **Multiplicidade:** um contraste primário (háptico pooled vs. sham); secundários exploratórios e não-corrigidos. *(Estudo 2: B-vs-C como contraste-cabeça nomeado.)*
- **Exclusões:** edições de perfil mid-estudo como covariável. *(Estudo 2: falhas BLE por ITT + per-protocol, não exclusão simples.)*

---

## 13. Pulseira ESP32 — build concreto (V2)

### 13.1 Peças (single-motor, congelada)

| Componente | Parte | Função |
|---|---|---|
| MCU | **ESP32-S3 Feather** (Adafruit #5477) | GATT server BLE, USB-C, carregador LiPo, STEMMA QT (I2C) |
| Driver háptico | **DRV2605L** (Adafruit #2305), I2C | Efeitos; modos ERM **e** LRA; controle RTP de amplitude |
| Motor | **ERM coin** (#1201) — **upgrade LRA é decisão da Fase 1, recomendado** (Precision Microdrives C10-100, ~175 Hz) | Atuador único |
| Bateria | **LiPo 400 mAh** (#3898) | Carga via USB-C do Feather |
| Cabo | STEMMA QT JST-SH 4 pinos | Feather ↔ DRV2605L |

**Cortado:** IMU da pulseira, mux I2C/multi-motor (#5626).

### 13.2 Fiação / I2C

DRV2605L via **STEMMA QT** (SDA/SCL/3V3/GND, sem solda); endereço I2C **0x5A** (fixo). Motor nos terminais OUT+/OUT− do DRV2605L. ERM em open-loop; **LRA usa malha fechada com auto-ressonância** (a vantagem que motiva o upgrade). LiPo no JST do Feather — **conferir polaridade**; carga pela USB-C.

### 13.3 Contrato BLE / GATT

Serviço GATT customizado (UUIDs de 128 bits — gerar e substituir placeholders).

| Característica | Propriedade | Payload (bytes) | Descrição |
|---|---|---|---|
| **Command** | Write No Response | `[effect_id:1][intensity:1][duration_ms:2 LE]` | Dispara háptico. `intensity` 0–255 → amplitude RTP (ver firmware). WNR = menor latência. |
| **Status/Battery** | Read / Notify | `[battery_pct:1][state:1]` | Bateria (0–100), estado (0=idle,1=playing,2=charging). |
| **Effect-Ack** | Notify | `[effect_id:1][played_ts:2 LE]` | Firmware sinaliza efeito-tocado → contabilidade de latência E2E. |
| **Device Info** | Read (padrão 0x180A) | — | Fabricante/firmware. |

Patterns vivem no firmware. **Validação no firmware:** rejeitar `effect_id` desconhecido, clampar `intensity`/`duration_ms`.

### 13.4 Firmware (NimBLE + DRV2605 — modo RTP coerente)

**Correção factual da rodada final:** o controle de **intensidade contínua** (necessário para o matching de intensidade percebida por participante — um deconfound primário) exige **RTP** (`setRealtimeValue`), **não** a biblioteca de waveforms ROM (cuja amplitude é fixa no efeito). Os dois modos são **mutuamente exclusivos**; misturá-los faz a intensidade ser silenciosamente ignorada.

```cpp
#include <NimBLEDevice.h>
#include <Adafruit_DRV2605.h>

Adafruit_DRV2605 drv;
NimBLECharacteristic *cmdChar, *statusChar, *ackChar;

void playEffect(uint8_t intensity, uint16_t dur) {
  // RTP: amplitude contínua (0–255). Casa intensidade percebida por participante.
  drv.setRealtimeValue(intensity);
  delay(dur);                 // produção: timer não-bloqueante
  drv.setRealtimeValue(0);    // para
}

class CmdCallback : public NimBLECharacteristicCallbacks {
  void onWrite(NimBLECharacteristic *c) {
    std::string v = c->getValue();
    if (v.size() < 4) return;                          // validação de tamanho
    uint8_t  effect_id = v[0];
    uint8_t  intensity = v[1];
    uint16_t dur       = v[2] | (v[3] << 8);           // little-endian
    if (effect_id == 0 || effect_id > 0x03) return;    // validação de id
    dur = min(dur, (uint16_t)2000);                    // clamp de segurança
    playEffect(intensity, dur);
    // notifyAck(effect_id) p/ latência E2E
  }
};

void setup() {
  Wire.begin();
  if (!drv.begin()) { /* tratar falha de I2C */ }
  drv.useERM();                          // drv.useLRA() no upgrade
  drv.setMode(DRV2605_MODE_REALTIME);    // RTP — NÃO misturar com setWaveform
  // DATA_FORMAT_RTP unsigned p/ 0–255 mapear amplitude cheia (default signed limita a 127)

  NimBLEDevice::init("resurface-band");
  NimBLEServer *srv = NimBLEDevice::createServer();
  NimBLEService *svc = srv->createService(SVC_UUID);
  cmdChar = svc->createCharacteristic(CMD_UUID, NIMBLE_PROPERTY::WRITE_NR);
  cmdChar->setCallbacks(new CmdCallback());
  statusChar = svc->createCharacteristic(STATUS_UUID,
                 NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY);
  ackChar = svc->createCharacteristic(ACK_UUID, NIMBLE_PROPERTY::NOTIFY);
  svc->start();
  srv->getAdvertising()->start();
}
```

`effect_id` no esquema RTP define a *forma* (envelope: ataque/sustain), com a **amplitude vindo do byte `intensity`**: `0x01` toque suave (envelope curto, baixa amplitude default) · `0x02` firme (envelope mais longo) · `0x03` ramp (reservado, não usado no estudo).

### 13.5 Energia / sleep

**Correção factual:** manter BLE através de light-sleep **não é automático** — exige habilitar explicitamente *BT modem-sleep + automatic light-sleep* (padrão `power_save` do NimBLE / `esp_pm_configure`). Sem isso o rádio fica acordado. Deep-sleep dropa a conexão (reintroduz latência de reconexão de segundos) — por isso **light-sleep, não deep**.

**Orçamento de bateria (worst-case, a fazer na bancada):** medir corrente de idle/advertising, corrente conectada em light-sleep no connection-interval escolhido, e corrente de buzz × buzzes/sessão, contra a célula de 400 mAh → render um número de *horas-de-sessão-ativa-por-carga* + cadência ("carga noturna"). O autor declara: a pulseira conecta só durante sessões de app monitorado, então o consumo médio é baixo — **mas o número vai na tese**, não a hand-wave.

### 13.6 Orçamento de latência para o "toque imediato"

| Etapa | Latência típica | Nota |
|---|---|---|
| Pré-conexão BLE (na abertura do app, via CDM) | ~1–3 s | **Pago uma vez na abertura**, não por toque. |
| Decisão → escrita GATT (WNR) | ~7,5–30 ms | 1 connection interval. |
| Parse firmware + `setRealtimeValue` | < 5 ms | I2C 100–400 kHz. |
| Spin-up mecânico | ERM ~50–80 ms; **LRA ~20–30 ms** | Vantagem extra do LRA. |
| **Total por-toque (já conectado)** | **~30–115 ms** | Dentro de "imediato" perceptual. |

Latência logada por evento (decisão → Effect-Ack) para reporte; drops por ITT (§10). Esta é a métrica que torna o contraste B-vs-C interpretável: se a pulseira "perde", é preciso distinguir locus de latência.

---

## 14. Privacidade, segurança e ética

- **Minimização, imposta tecnicamente:** só métricas numéricas/categóricas; texto de nó descartado na captura — protege participantes e terceiros incidentais (mensagens, usernames). Localização cortada do estudo.
- **On-device apenas:** detecção, analytics, IA generativa — tudo local. Sem servidores.
- **Consentimento:** informado, específico ("observa *como* você rola — velocidade, pausas, duração — nunca *o que* você vê"), revogável, com deleção sob demanda. Cobre o perfil on-device. Perfil **editável com log** (não congelado — edições viram covariável).
- **IRB/CEP:** submetido na Fase 0, cobrindo ambos os estudos; **aprovação é gate duro na coleta**. Endereça a pergunta difícil: o sistema intervém com base num estado inferido, em população que pode incluir pessoas em sofrimento com o próprio uso — intervenções únicas, gentis, dispensáveis num toque, qualquer modalidade desabilitável.
- **Justificativa do braço sham:** detectar e deliberadamente não intervir é aceitável porque (a) as intervenções são não-comprovadas e não-terapêuticas — reter não retém benefício estabelecido; (b) episódios sham são indistinguíveis de uso comum; (c) todo participante recebe todas as modalidades (within-subjects); (d) debrief na saída. Sem o sham, não há contrafactual e o estudo não estima efeito algum.
- **Localização (produto), re-justificada por privacidade — não por Play:** como o sistema é sideload-only (abaixo), revisão do Play é irrelevante; a minimização se justifica por **privacidade do participante e CEP**. Localização é **coarse, geofence rotulado pelo próprio usuário** (nunca inferência de "casa"/"trabalho", nunca coordenadas persistidas), **demonstrada N=1 self**, firewalled — nenhum device de participante tem localização ligada (ou exige emenda de CEP separada).
- **Ética de linguagem:** "modo zumbi" é shorthand interno; UI e consentimento usam fraseado neutro, autônomo-suportivo.
- **Honestidade de plataforma:** `AccessibilityService` para fins não-acessibilidade é território **sideload-only de pesquisa**; Advanced Protection e a trajetória do **Android 17 (risco presente, não futuro)** podem auto-revogar o serviço. Device de pesquisa mantém esse modo desligado, OS e apps congelados. **Este sistema não é shippável no Google Play sob a política atual** — declarado, não escondido.

---

## 15. Riscos, ranqueados

| # | Risco | Severidade | Mitigação |
|---|---|---|---|
| 1 | TikTok/Reels emitem sinal de acessibilidade insuficiente | Fatal se não tratado | Gate da Fase 0 antes de comprometer; fallback (UsageStats+churn+IMU); versões de apps fixadas; enquadramento ajustado pré-proposta |
| 2 | Fraqueza/circularidade do ground truth | Fatal à RQ2 | Probes diferidos sobre sinalizados+aleatórios; alegações de envelope (não estado); limitações reportadas |
| 3 | Overrun de escopo → sem dados | Alto | **Pulseira é V2/Estudo 2**; build de estudo só-heurística; nudges curados; LiteRT pós-hoc |
| 4 | Funil de episódios não limpa (subdimensionado) | **Alto** | Estudo 1 = 3 braços (on-phone); funil como gate duro; fallbacks quantitativos pré-registrados; pulseira condicional |
| 5 | Comparação de modalidades confundida (locus vs. novidade/qualidade) | Alto (Estudo 2) | Envelope-matching, upgrade LRA, contexto casado, ITG/ITT, braços sem texto, sham; novidade declarada em §17 |
| 6 | Latência do IRB trava cronograma | Médio-alto | Submetido na Fase 0, cobre ambos os estudos; trilha build/bench e self-pilot absorvem a espera |
| 7 | Flakiness de reconexão BLE | Médio | CDM + reconexão na abertura com endereço cacheado, retries limitados, ITT |
| 8 | Atualização de plataforma muda comportamento mid-estudo | Médio (Android 17 iminente) | Device de referência com OS **e apps** congelados durante a coleta |
| 9 | Nano/Prompt API indisponível nos devices | Baixo (por design) | Fallback `CuratedBank` obrigatório; checagem em runtime; RQ-nudge condicional; dashboard só-template |

---

## 16. Contribuições esperadas e cronograma

**Contribuições:**
1. **Empírica (primária):** comparação controlada, within-subjects, de uma interrupção on-phone deconfundida vs. ausência de intervenção, com contrafactual intra-pessoa — e, condicionalmente (Estudo 2), a primeira comparação de *locus* háptico off-body (pulso) vs. on-body (telefone) para desengajar rolagem passiva.
2. **Metodológica:** detector comportamental on-device de rolagem passiva, definido como *envelope* (não estado), validado contra autorrelato diferido, com caracterização honesta de seus limites de construto.
3. **Artefato/sistema:** sistema integrado funcional — app sempre-ativo, IA generativa local, pulseira ESP32 (V2) — demonstrando um modelo de intervenção privado, não-punitivo, autônomo-suportivo, com processamento on-device.
4. **De design:** conjunto reutilizável de padrões de deconfounding para nudges hápticos vestíveis (envelope-matching, matching de intensidade percebida, gatilho-constante, separação produto-vs-estudo).
5. **Dataset:** corpus rotulado de features de rolagem + julgamentos de intencionalidade, on-device, preservando privacidade.

**Cronograma:**

| Semanas | Fase | Entregas |
|---|---|---|
| 1–3 | Fase 0 | Spike de instrumentação (go/no-go); submissão IRB (ambos estudos); self-pilot N=1 |
| 4–8 | Build/bench (paralela ao IRB) | Firmware ESP32 + NimBLE (RTP); cliente BLE + CDM + reconexão; verificação overlay Android 16; caracterização acelerômetro; **decisão ERM↔LRA**; protocolo de intensidade percebida |
| ~4+ (no IRB) | Fase 1a/1b | Build só-logging + probes; comparação on-phone 3 braços; N≈10–20 |
| fim Fase 1 | Gate de funil | Simulação de acúmulo (~1h); go/no-go do Estudo 2 |
| (condicional) | Fase 2 / Estudo 2 | Pulseira (braço C, V2); N≈10–15 |
| 16–24 | Fase 3 | Análise; RQ3 (LiteRT offline); RQ-nudge / protótipo Nano; retrato de atenção (descritivo) |

**Próximas 2 semanas (ações concretas):** (1) redigir+submeter o protocolo IRB/CEP cobrindo ambos os estudos — maior latência, zero dependência de código; (2) construir o spike de instrumentação e decidir Pass/Partial/Fail; (3) prototipar o fluxo de permissões no device real (4 grants V1 + exenção de otimização de bateria), cronometrar e screenshotar falhas; (4) esqueleto do app + logger Room com self-pilot N=1.

---

## 17. Premissas e a objeção mais forte

**Premissas:** device de referência **Pixel 9/10-classe**, Android 16 (API 36), Advanced Protection desligado, OS+apps congelados na coleta; a isenção de FGS-start via CDM é o mecanismo (verificado na Fase 0); Fase 0 retorna Pass/Partial; o funil limpa o mínimo para o Estudo 1 de 3 braços (Estudo 2 condicional); hardware emprestado com dono de budget nomeado; n limitado pela **logística** (sideload + permissões manuais + hardware) mais que pela estatística.

**A objeção mais forte à própria proposta:** *mesmo com todos os deconfounds, o contraste-cabeça do Estudo 2 (telefone-vs-pulso) permanece confundido por **novidade de canal** de um jeito que nenhum controle no escopo de um mestrado solo remove.* A pulseira é um objeto novo, vestido "para o experimento"; uma vibração nova-e-saliente no pulso atrai atenção por **orientação a novidade**, não necessariamente por vantagem de *locus off-body* — e a análise de habituação que tentaria capturar isso é estatisticamente impotente a ~5–12 episódios/braço. O telefone, por contraste, é um canal háptico totalmente habituado. Então o desenho compara um canal háptico **novo** contra um **saturado** e atribui a diferença a *locus*. O envelope-matching casa a *forma física* do estímulo, mas não sua *história psicológica*.

**Por que ainda vale a pena:** (a) é uma limitação **honestamente reportável** que fortalece a tese — o enquadramento de feasibility/effect-size já não reivindica generalização; (b) **é exatamente por isso que a espinha é o Estudo 1** — a claim que sobrevive ("*qualquer* háptica deconfundida supera não-intervenção", o contraste pooled-háptico-vs-sham, que a novidade infla igualmente em todos os braços) é **on-phone e não precisa da pulseira**; (c) a confounding de novidade é, ela própria, uma **descoberta de design para o produto** — se a pulseira só vence por novidade, isso prediz declínio de eficácia e informa a estratégia anti-habituação. A banca **vai** levantar isto; a proposta o admite no lugar de esperar ser pega — e estrutura o projeto (Estudo 1 lidera; pulseira é upside ganho) para que a tese não dependa do ponto mais frágil.

---

*Fim da proposta.*
