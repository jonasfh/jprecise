# JPrecise

**JPrecise** er et teknisk *proof-of-concept* (POC) for Android som undersøker muligheten for å gi mer finkornet volumkontroll enn standard Android-trinn, spesielt i det laveste lydområdet. Dette er nyttig for lytting på svært lavt volum (f.eks. ved innsovning), både på telefonens innebygde høyttaler og over Bluetooth-hodetelefoner.

- **Målenhet:** Motorola Edge 50 Pro (XT2403-2)
- **Målversjon:** Android 16 (API-nivå 36)
- **Språk:** Java (Android SDK)

---

## 1. Forbered telefonen (Utviklervalg)

Før du kan installere appen direkte fra datamaskinen via USB eller ADB:

1. **Aktiver Utvikleralternativer (Developer Options):**
   - Åpne **Innstillinger** (Settings) på telefonen.
   - Gå til **Om telefonen** (About phone).
   - Trykk 7 ganger på **Byggnummer** (Build number) til du får beskjed om at du er en utvikler.
2. **Aktiver USB-feilsøking (USB Debugging):**
   - Gå tilbake til **Innstillinger** > **System** > **Utvikleralternativer** (eller søk etter *Utvikleralternativer*).
   - Slå på **USB-feilsøking** (USB debugging).
3. **Koble til telefonen:**
   - Koble telefonen til datamaskinen med USB-kabel.
   - På telefonen dukker det opp en dialogboks: *"Vil du tillate USB-feilsøking?"* / *"Allow USB debugging?"*.
   - Huk av for *"Alltid tillat fra denne datamaskinen"* og trykk **Tillat**.

---

## 2. Bygge APK

### Alternativ A: Inne i Devcontainer / Linux-miljø
Kjør følgende kommando i prosjektets rotmappe:
```bash
./gradlew assembleDebug
```
Den ferdige APK-filen vil ligge i:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### Alternativ B: Bygg via Docker (uten devcontainer-sesjon)
Dersom du står på vertsmaskinen med Docker installert:
```bash
docker run --rm -v "$PWD":/workspace -w /workspace jprecise-devcontainer \
    bash -lc "./gradlew --no-daemon assembleDebug"
```

---

## 3. Installere APK på telefonen

### Metode 1: Direkte installasjon via Gradle (Anbefalt under utvikling)
Med telefonen tilkoblet via USB (og godkjent via ADB), kan du bygge og installere i ett trinn:
```bash
./gradlew installDebug
```

### Metode 2: Installere via ADB
Dersom du allerede har bygget APK-en:
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
*(Parameteren `-r` overskriver/oppdaterer en eksisterende installasjon uten å slette app-data).*

### Metode 3: Manuell overføring (Sideloading)
1. Bygg APK-en med `./gradlew assembleDebug`.
2. Overfør `app/build/outputs/apk/debug/app-debug.apk` til telefonen (f.eks. via USB-filoverføring, Google Drive, e-post, Discord eller lignende).
3. Åpne filen i en filbehandler på telefonen og trykk **Installer**.
4. Godkjenn eventuell advarsel om installasjon fra ukjente kilder/apper.

---

## 4. Starte appen og se logger

### Starte appen fra kommandolinjen
```bash
adb shell am start -n com.jprecise/.MainActivity
```

### Lese logger i sanntid
For å følge med på appens loggmeldinger og eventuelle feil:
```bash
adb logcat -s JPrecise:V AndroidRuntime:E
```
Eller filtrer på pakkenavn:
```bash
adb logcat | grep com.jprecise
```

---

## 5. Trådløs feilsøking (Wi-Fi Debugging)

Dersom du ønsker å feilsøke og installere trådløst over Wi-Fi (fra vertsmaskinen):

1. **Sørg for at PC og telefon er på samme Wi-Fi-nettverk.**
2. **Aktiver trådløs feilsøking på telefonen:**
   - Gå til **Innstillinger** > **System** > **Utvikleralternativer**.
   - Slå på **Trådløs feilsøking** (Wireless debugging).
   - Trykk direkte på selve teksten/linjen *Trådløs feilsøking* for å åpne detaljsiden.
3. **Parkoble enheten (første gang):**
   - Trykk på **Koble til enhet med parkoblingskode** (Pair device with pairing code).
   - Dialogen viser en IP-adresse, en parkoblingsport og en 6-sifret parkoblingskode.
   - Kjør følgende fra terminalen på vertsmaskinen:
     ```bash
     adb pair <IP-ADRESSE>:<PARKOBLINGSPORT> <PARKOBLINGSKODE>
     # Eksempel:
     # adb pair 192.168.68.53:39679 913416
     ```
4. **Koble til enheten:**
   - Gå tilbake til hovedsiden for *Trådløs feilsøking* på telefonen. Merk at tilkoblingsporten under *IP-adresse og port* er **forskjellig** fra parkoblingsporten.
   - Kjør fra terminalen:
     ```bash
     adb connect <IP-ADRESSE>:<TILKOBLINGSPORT>
     # Eksempel:
     # adb connect 192.168.68.53:41695
     ```
5. **Verifiser tilkoblingen:**
   ```bash
   adb devices -l
   # Viser f.eks: 192.168.68.53:41695 device product:eqe_ge model:motorola_edge_50_pro device:eqe transport_id:1
   ```
6. **Installere og feilsøke fra host:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb logcat -s JPrecise:V AndroidRuntime:E
   ```

---

## 6. Teste Accessibility Service POC (Volumknapp-avskjæring)

Denne POC-en undersøker om vi kan observere og avskjære fysiske volumknapper via Android `AccessibilityService`.

### 1. Aktivere Accessibility Service på telefonen
- **Metode A (via UI i appen):**
  1. Åpne appen **JPrecise** på telefonen.
  2. Trykk på **Open Accessibility Settings**.
  3. Finn **JPrecise Volume Service** under Nedlastede apper / Tilgjengelighetstjenester og slå den **PÅ**.
  *(Merk på Android 13/14/15/16: Hvis tjenesten er grået ut pga. "Begrensede innstillinger", gå til App-info for JPrecise > tre prikker øverst til høyre > "Tillat begrensede innstillinger").*

- **Metode B (rask aktivering via ADB):**
  ```bash
  adb shell settings put secure enabled_accessibility_services com.jprecise/.VolumeAccessibilityService
  ```

### 2. Kjøre live-logging
Kjør logcat filtrert på `JPrecise`:
```bash
adb logcat -s JPrecise:V AndroidRuntime:E
```

### 3. Teste volumknapper
1. **Passiv observasjon (Consume = OFF):**
   - Trykk på fysiske volumknapper (opp/ned).
   - Verifiser at du ser logger som:
     ```text
     VolumeKeyEvent -> Key: KEYCODE_VOLUME_UP (24) | Action: ACTION_DOWN | Repeat: 0 | StreamMusic: 5/15 (min: 0) | MusicActive: false | Consumed: false
     ```
   - Standard Android-volumpanel vises som normalt.
2. **Aktiv avskjæring / Intercept (Consume = ON):**
   - Slå PÅ bryteren *"Consume Volume Key Events (return true)"* i appen.
   - Trykk på volumknappene.
   - Verifiser i logcat at `Consumed: true`. Androids standard volumpanel skal **ikke** dukke opp, og standard volum skal **ikke** endres av systemet.
3. **Syntetiske tastetrykk via ADB:**
   ```bash
   adb shell input keyevent 24  # Volum OPP
   adb shell input keyevent 25  # Volum NED
   ```

---

## 7. Teste høyoppløselig volumkontroll (POC #2 - Sub-Step Volume Engine)

Denne POC-en demonstrerer **finkornet volumkontroll med vesentlig høyere oppløsning** enn standard Android 1/15-trinn, spesielt på lavt lyttevolum (mellom 0 og 1).

### 1. Bygg og installer på telefonen (via Wi-Fi / USB)
```bash
./gradlew installDebug
```

### 2. Testprosedyre på telefonen (Motorola Edge 50 Pro)

1. **Start test-lyd (Audio Benchmark):**
   - Trykk på **Start Test Tone (440 Hz)** i appen.
   - En ren, jevn sinustone spilles av via `AudioTrack`.
2. **Test finkornet demping med slider:**
   - Dra slideren sakte i det lave området mellom `0.00` og `1.00`.
   - Legg merke til at hvert deltrinn (f.eks. `0.10`, `0.20`, `0.30` ... `1.00`) gir en jevn, gradvis og målbar volumendring (demping fra `-24.0 dB` opp til `0.0 dB`).
   - Standard Android hopper direkte fra stillhet (0) til et relativt høyt trinn (1), mens JPrecise gir 10–20 hørbare mikro-trinn i dette intervallet!
3. **Test fysiske volumknapper:**
   - Slå PÅ bryteren *"Route Hardware Volume Keys to Sub-Steps"*.
   - Velg ønsket trinnstørrelse (f.eks. `0.10` for 10 mikrosteg, eller `0.05` for 20 mikrosteg).
   - Trykk på de fysiske volumknappene på telefonen.
   - Verifiser at volumet endres med det valgte finkornede deltrinnet for hvert knappetrykk, mens systemets standard volumpanel forblir skjult.
4. **Live logcat-overvåking:**
   ```bash
   adb logcat -s JPrecise:V
   ```
   Logger viser sanntidsverdiene:
   ```text
   SubStepVolume -> Level: 0.30 / 15.0 | Base STREAM_MUSIC: 1 | Attenuation: -16.8 dB | Gain: 0.300
   ```


