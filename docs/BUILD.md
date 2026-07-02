# BUILD — Mindless Scroll (app Android + firmware ESP32)

Guia de implementação. Companheiro de engenharia da proposta acadêmica ([proposal_final.md](proposal_final.md)) — aqui é **como construir e rodar**, não o porquê. Toda escolha de API/permissão segue as correções verificadas da proposta (FGS via Companion Device Manager, firmware RTP, Android 16/API 36, `connectedDevice` sem time-cap, AccessibilityService sem `foregroundServiceType`).

> Status: hardware é **V2** (Estudo 2). O app V0/V1 funciona sem a pulseira — o módulo BLE é opcional e detrás de uma flag. Construa o app primeiro; a pulseira depois.

---

## 0. Pré-requisitos

| Ferramenta | Versão | Para quê                                                    |
|---|---|-------------------------------------------------------------|
| Android Studio | Ladybug+ (AGP 8.5+) | App                                                         |
| JDK | 17 | Build do app                                                |
| Kotlin | 2.0+ | App                                                         |
| `minSdk` / `targetSdk` / `compileSdk` | 34 / 36 / 36 | Android 14 baseline (BLE runtime perms) → Android 16 target |
| Arduino IDE **ou** PlatformIO | Arduino-ESP32 core 3.x | Firmware                                                    |
| Device de referência | Pixel 9/10-classe, Android 16, Advanced Protection **OFF**, OS+apps congelados | Sideload de pesquisa                                        |

**Bibliotecas do firmware** (Library Manager / `lib_deps`):
- `NimBLE-Arduino` (h2zero) — stack BLE leve
- `Adafruit DRV2605 Library`
- `Adafruit BusIO` (dep do DRV2605)

---

## 1. Hardware — BOM e montagem (V2)

### 1.1 Lista de peças (single-motor, congelada)

| Componente | Parte | Nota |
|---|---|---|
| MCU | ESP32-S3 Feather (Adafruit #5477) | BLE, USB-C, carregador LiPo, STEMMA QT |
| Driver háptico | DRV2605L (Adafruit #2305) | I2C `0x5A` (fixo); modos ERM e LRA; controle RTP de amplitude |
| Motor | ERM coin (#1201) — **upgrade LRA recomendado** (Precision Microdrives C10-100, ~175 Hz) | Decisão de bancada (ver §1.3) |
| Bateria | LiPo 400 mAh (#3898) | Carga via USB-C do Feather |
| Cabo | STEMMA QT JST-SH 4 pinos | Feather ↔ DRV2605L (sem solda) |

**Cortado:** IMU da pulseira, mux I2C/multi-motor (#5626).

### 1.2 Fiação

```
ESP32-S3 Feather                 DRV2605L
  STEMMA QT  ───── JST-SH 4p ───── STEMMA QT     (SDA/SCL/3V3/GND, sem solda)

DRV2605L OUT+ ───── motor +
DRV2605L OUT- ───── motor -

LiPo 400mAh ───── conector JST do Feather   (CONFERIR POLARIDADE)
Carga: USB-C do Feather
```

- I2C do DRV2605L: endereço **`0x5A`** fixo.
- ERM: open-loop. **LRA: malha fechada com auto-ressonância** (a vantagem que motiva o upgrade — e ~20–30 ms de spin-up vs ~50–80 ms do ERM).
- ⚠️ **Polaridade da LiPo:** confira antes de plugar; inversão queima o Feather.

### 1.3 Decisão ERM ↔ LRA (bancada, Fase 1)

Caracterize ambos os atuadores com acelerômetro. Use LRA se o orçamento permitir: o telefone-alvo usa LRA, e um ERM-pulso vs LRA-telefone embute confound de qualidade de estímulo no contraste do estudo. No firmware é só trocar `drv.useERM()` → `drv.useLRA()`.

---

## 2. Firmware ESP32

### 2.1 Princípio crítico — modo RTP, não waveform-ROM

Intensidade contínua (necessária para casar intensidade percebida por participante) exige **RTP** (`setRealtimeValue`). A biblioteca de waveforms ROM tem amplitude **fixa no efeito** — incompatível com controle de intensidade. Os dois modos são **mutuamente exclusivos**; não misturar.

### 2.2 Sketch completo

```cpp
#include <NimBLEDevice.h>
#include <Adafruit_DRV2605.h>
#include <Wire.h>

// --- UUIDs (GERAR os seus: `uuidgen`) ---
#define SVC_UUID    "0000feed-0000-1000-8000-00805f9b34fb"
#define CMD_UUID    "0000fee1-0000-1000-8000-00805f9b34fb"
#define STATUS_UUID "0000fee2-0000-1000-8000-00805f9b34fb"
#define ACK_UUID    "0000fee3-0000-1000-8000-00805f9b34fb"

Adafruit_DRV2605 drv;
NimBLECharacteristic *cmdChar, *statusChar, *ackChar;

// Toca um efeito por amplitude RTP contínua (0–255) durante `dur` ms.
// Produção: trocar delay() por timer não-bloqueante (millis()/ticker).
void playEffect(uint8_t effect_id, uint8_t intensity, uint16_t dur) {
  // effect_id define a FORMA (envelope); intensity é a amplitude.
  // MVP: amplitude plana; refinar envelopes por effect_id depois.
  drv.setRealtimeValue(intensity);
  delay(dur);
  drv.setRealtimeValue(0);
}

void notifyAck(uint8_t effect_id) {
  uint16_t ts = (uint16_t)(millis() & 0xFFFF);
  uint8_t payload[3] = { effect_id, (uint8_t)(ts & 0xFF), (uint8_t)(ts >> 8) };
  ackChar->setValue(payload, 3);
  ackChar->notify();
}

class CmdCallback : public NimBLECharacteristicCallbacks {
  void onWrite(NimBLECharacteristic *c) override {
    std::string v = c->getValue();
    if (v.size() < 4) return;                          // validação de tamanho
    uint8_t  effect_id = (uint8_t)v[0];
    uint8_t  intensity = (uint8_t)v[1];
    uint16_t dur       = (uint8_t)v[2] | ((uint8_t)v[3] << 8);  // little-endian
    if (effect_id == 0 || effect_id > 0x03) return;    // validação de id
    if (dur > 2000) dur = 2000;                        // clamp de segurança
    playEffect(effect_id, intensity, dur);
    notifyAck(effect_id);                              // latência E2E
  }
};

void setup() {
  Wire.begin();                          // STEMMA QT I2C
  if (!drv.begin()) {                    // checar falha de I2C
    // sinalizar erro (LED), não prosseguir silenciosamente
    while (1) { delay(1000); }
  }
  drv.useERM();                          // drv.useLRA() no upgrade
  drv.setMode(DRV2605_MODE_REALTIME);    // RTP — NÃO misturar com setWaveform
  // DATA_FORMAT_RTP unsigned p/ 0–255 mapear amplitude cheia (default signed limita a 127):
  drv.writeRegister8(DRV2605_REG_CONTROL3, drv.readRegister8(DRV2605_REG_CONTROL3) | 0x08);

  NimBLEDevice::init("resurface-band");
  NimBLEDevice::setPower(ESP_PWR_LVL_P9);
  NimBLEServer *srv = NimBLEDevice::createServer();
  NimBLEService *svc = srv->createService(SVC_UUID);

  cmdChar = svc->createCharacteristic(CMD_UUID, NIMBLE_PROPERTY::WRITE_NR);
  cmdChar->setCallbacks(new CmdCallback());

  statusChar = svc->createCharacteristic(
      STATUS_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY);

  ackChar = svc->createCharacteristic(ACK_UUID, NIMBLE_PROPERTY::NOTIFY);

  svc->start();
  NimBLEAdvertising *adv = NimBLEDevice::getAdvertising();
  adv->addServiceUUID(SVC_UUID);
  adv->start();
}

void loop() {
  // Status/bateria por Notify on-change (ler ADC do Feather p/ bateria).
  // Light-sleep entre eventos: ver §2.4.
  delay(100);
}
```

### 2.3 Contrato GATT (referência)

| Característica | UUID | Propriedade | Payload (bytes) |
|---|---|---|---|
| Command | `CMD_UUID` | Write No Response | `[effect_id:1][intensity:1][duration_ms:2 LE]` |
| Status/Battery | `STATUS_UUID` | Read / Notify | `[battery_pct:1][state:1]` (state: 0=idle,1=playing,2=charging) |
| Effect-Ack | `ACK_UUID` | Notify | `[effect_id:1][played_ts:2 LE]` |

`effect_id`: `0x01` suave · `0x02` firme · `0x03` ramp (reservado, não usado no estudo). Amplitude vem sempre do byte `intensity`.

### 2.4 Energia

- **Light-sleep mantendo BLE não é automático.** Habilitar BT modem-sleep + automatic light-sleep (`esp_pm_configure` / padrão `power_save` do NimBLE). Sem isso o rádio fica acordado.
- **Deep-sleep dropa a conexão** → reconexão de segundos. Usar **light-sleep**.
- Pulseira conecta só durante sessões de app monitorado → consumo médio baixo. **Medir e documentar** o worst-case na bancada (idle/advertising, conectado-light-sleep no connection-interval, buzz×n) contra a célula de 400 mAh → horas-de-sessão-por-carga + cadência de carga.

### 2.5 Flash

```bash
# Arduino IDE: Board = "Adafruit Feather ESP32-S3", porta USB, Upload.
# PlatformIO:
pio run -t upload && pio device monitor
```

`platformio.ini` mínimo:
```ini
[env:adafruit_feather_esp32s3]
platform = espressif32
board = adafruit_feather_esp32s3
framework = arduino
lib_deps =
    h2zero/NimBLE-Arduino
    adafruit/Adafruit DRV2605 Library
monitor_speed = 115200
```

---

## 3. App Android

### 3.1 Estrutura de módulos

```
app/
 ├─ capture/        AccessibilityService + UsageStats wrapper  (texto de nó SCRUBBED na origem)
 ├─ detection/      extração de features + heurística (envelope) + supressão por intencionalidade
 ├─ decision/       CONFIG GATE (janelas binárias) + JITAI (randomização por episódio no build de estudo)
 ├─ intervention/   Dispatcher: overlay / Vibrator / (V2) comando BLE
 ├─ ai/             NudgeSource = NanoGenerator | CuratedBank  (fallback obrigatório)
 ├─ ble/            [V2] BleClient + connectedDevice FGS + CDM pairing
 ├─ data/           Room (logs por episódio) + DataStore (config/perfil)
 ├─ onboarding/     consentimento, permissões, seleção de apps, perfil, (V2) pareamento
 └─ ui/             dashboard (template-frozen na coleta), config
```

### 3.2 Permissões (`AndroidManifest.xml`)

```xml
<!-- Special-access (concedidas em telas do sistema, não runtime) -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
                 tools:ignore="ProtectedPermissions"/>
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>

<!-- Runtime -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

<!-- FGS p/ link BLE (V2) -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"/>

<!-- BLE via Companion Device Manager (V2) — CDM dispensa BLUETOOTH_SCAN/localização -->
<uses-feature android:name="android.software.companion_device_setup"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND"/>

<!-- Reboot (complemento; nada sobe FGS no boot) -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
```

⚠️ **AccessibilityService NÃO leva `foregroundServiceType`** — é serviço system-bound. O `connectedDevice` é do serviço de BLE, separado.

### 3.3 Declaração dos serviços

```xml
<!-- Sempre-armado: sem foregroundServiceType, sem notificação -->
<service
    android:name=".capture.ScrollAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService"/>
    </intent-filter>
    <meta-data android:name="android.accessibilityservice"
               android:resource="@xml/accessibility_config"/>
</service>

<!-- [V2] FGS do link BLE -->
<service
    android:name=".ble.BleConnectionService"
    android:foregroundServiceType="connectedDevice"
    android:exported="false"/>
```

`res/xml/accessibility_config.xml` — escopar aos apps monitorados e pedir os tipos de evento certos:
```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeViewScrolled|typeWindowStateChanged|typeWindowContentChanged|typeViewTextChanged|typeViewFocused"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:packageNames="com.instagram.android,com.zhiliaoapp.musically"
    android:canRetrieveWindowContent="true"/>
```
> `packageNames` é setável em runtime via `setServiceInfo()` conforme a seleção do usuário. **Scrub do texto na origem** — extrair só métricas numéricas, nunca strings de conteúdo.

### 3.4 Ciclo de vida armado → ativo

```kotlin
// ScrollAccessibilityService — gatilho sempre-armado
override fun onAccessibilityEvent(e: AccessibilityEvent) {
    when (e.eventType) {
        TYPE_WINDOW_STATE_CHANGED -> {
            val pkg = e.packageName?.toString() ?: return
            if (config.isMonitored(pkg)) {
                session.start(pkg)
                if (config.bleEnabled) startBleService()   // V2: FGS sobe via CDM exemption
            } else session.end()
        }
        TYPE_VIEW_SCROLLED -> features.onScroll(e)          // velocidade/continuidade
        TYPE_VIEW_TEXT_CHANGED, TYPE_VIEW_FOCUSED ->
            intent.markIntentional()                        // supressão (digitação/foco)
        TYPE_WINDOW_CONTENT_CHANGED -> features.onChurn(e)  // churn vídeo-curto
    }
    detector.evaluate(features.snapshot())?.let { dispatcher.maybeIntervene(it) }
}
```

- O cliente BLE vive **no processo do AccessibilityService** (já vivo). O FGS `connectedDevice` é iniciado do background via **isenção do CDM** (`REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND`) — **não** por "AccessibilityService habilitado" (essa isenção não existe).
- `ACTION_USER_PRESENT` (primeiro desbloqueio) hospedado aqui (não pode ser receiver de manifesto).
- **Duração de sessão por timer próprio** na abertura — `UsageStatsManager` é agregado/atrasado.

### 3.5 Pareamento da pulseira via CDM (V2)

```kotlin
val cdm = getSystemService(CompanionDeviceManager::class.java)
val filter = BluetoothLeDeviceFilter.Builder()
    .setNamePattern(Pattern.compile("resurface-band"))
    .build()
val req = AssociationRequest.Builder().addDeviceFilter(filter).setSingleDevice(true).build()
cdm.associate(req, object : CompanionDeviceManager.Callback() {
    override fun onAssociationPending(chooser: IntentSender) {
        startIntentSenderForResult(chooser, REQ_CDM, null, 0, 0, 0)  // UI do sistema pareia
    }
    override fun onAssociationCreated(info: AssociationInfo) {
        prefs.saveBandAddress(info.associatedDevice?.bleDevice?.scanResult?.device?.address)
    }
    override fun onFailure(error: CharSequence?) { /* log */ }
}, null)
```
CDM faz a descoberta (UI do sistema) → **dispensa `BLUETOOTH_SCAN` e localização**. Guardar o endereço; reconectar na abertura do app monitorado com `BluetoothGatt` direto.

### 3.6 Envio do comando háptico (V2)

```kotlin
// payload [effect_id][intensity][duration_ms LE] — Write No Response (menor latência)
fun buzz(effectId: Int, intensity: Int, durMs: Int) {
    val p = byteArrayOf(
        effectId.toByte(), intensity.toByte(),
        (durMs and 0xFF).toByte(), ((durMs shr 8) and 0xFF).toByte()
    )
    cmdChar.value = p
    cmdChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
    gatt.writeCharacteristic(cmdChar)
    log.bleSent(effectId, System.nanoTime())   // p/ latência E2E com o Effect-Ack
}
```

### 3.7 Camada de IA — interface única

```kotlin
sealed interface NudgeSource { fun nudge(ctx: EpisodeContext, tone: Tone): String }

class CuratedBank(...) : NudgeSource { /* banco curado por tom — SEMPRE disponível */ }
class NanoGenerator(...) : NudgeSource { /* ML Kit GenAI Prompt API, checada em runtime */ }

// Fallback OBRIGATÓRIO — Prompt API varia por device/rollout/storage mesmo em "suportado"
val source: NudgeSource =
    if (NanoGenerator.isAvailableAtRuntime()) NanoGenerator(...) else CuratedBank(...)
```
- **Build de estudo:** `CuratedBank` (Nano só no sub-experimento RQ-nudge, gerado **ao vivo**, nunca cacheado). Slots de template = só hora+app.
- **Produto:** Nano livre (Tier 3) + localização (geofence rotulado pelo usuário). Cache só no produto.

### 3.8 Flag produto-vs-estudo

```kotlin
object BuildScope {
    const val STUDY = true   // true = build de estudo (deconfundido)
    // STUDY: 1 limiar standard | janela gateia só intervenção | sem calibração de limiar
    //        | dashboard template-frozen | sem localização | sem hobbies | randomização por episódio
}
```

---

## 4. Setup no device (sequência de permissões)

Cada uma é tela do sistema, sem callback automático — guiar o usuário em ordem:

1. **AccessibilityService** → `Settings.ACTION_ACCESSIBILITY_SETTINGS`
2. **Usage Access** → `Settings.ACTION_USAGE_ACCESS_SETTINGS`
3. **Overlay** → `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
4. **Notificações** (runtime) → `POST_NOTIFICATIONS`
5. **Isenção de otimização de bateria** (OEMs agressivos) → `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
6. **(V2) Pareamento da pulseira** → fluxo CDM (§3.5)

> Verificar cada grant com `Settings.canDrawOverlays()`, `AccessibilityManager.getEnabledAccessibilityServiceList()`, etc. Cronometrar e screenshotar falhas no device real.

---

## 5. Build e run

```bash
# App
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.resurface.mindless/.onboarding.OnboardingActivity

# Logs de captura/detecção
adb logcat -s Mindless:V

# Firmware (PlatformIO)
pio run -t upload && pio device monitor
```

---

## 6. Verificação (ordem de teste)

1. **Spike de instrumentação (Fase 0, GATE):** rodar o AccessibilityService descartável; logar eventos crus de IG Feed, IG Reels, TikTok. Confirmar Pass/Partial/Fail (§8.3 da proposta). **Não construir o resto antes disso.**
2. **Captura → detecção:** abrir app monitorado, rolar; confirmar features e disparo da heurística no logcat. Confirmar supressão ao digitar.
3. **Intervenção on-phone:** overlay aparece sobre o app; `Vibrator` dispara.
4. **(V2) BLE:** parear via CDM; `buzz()` → motor vibra; medir latência decisão→Effect-Ack.
5. **Reboot:** AccessibilityService re-habilita sozinho; nada sobe FGS no boot.
6. **Bateria:** rodar sessão longa, medir dreno do telefone e da pulseira.

---

## 7. Pegadinhas (do design review)

- ❌ Não declarar `foregroundServiceType` no AccessibilityService.
- ❌ Não iniciar FGS do `BOOT_COMPLETED` (Android 15+ bloqueia). A11y re-habilita sozinho.
- ❌ Não confiar no `UsageStatsManager` para duração ao vivo — timer próprio.
- ❌ Não misturar RTP e waveform-ROM no firmware — intensidade some.
- ❌ Não usar deep-sleep com BLE conectado — dropa a conexão.
- ❌ Não persistir texto de nó nem coordenadas de localização.
- ⚠️ Fixar versões de IG/TikTok no device — builds novos mudam os eventos emitidos.
- ⚠️ Polaridade da LiPo antes de plugar.
- ⚠️ Advanced Protection OFF no device de pesquisa (senão o a11y pode ser revogado).

---

*Detalhes de método, ética e desenho do estudo: [proposal_final.md](proposal_final.md).*
