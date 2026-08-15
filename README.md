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

## 5. Trådløs feilsøking (Valgfritt)
Dersom du ønsker å feilsøke over Wi-Fi uten USB-kabel:
1. Sørg for at PC og telefon er på samme Wi-Fi-nettverk.
2. Gå til **Innstillinger** > **System** > **Utvikleralternativer** > **Trådløs feilsøking** (Wireless debugging) og slå den på.
3. Velg *"Koble til enhet med parkoblingskode"* og kjør i terminalen:
   ```bash
   adb pair <IP-ADRESSE>:<PORT> <PARKOBLINGSKODE>
   ```
4. Deretter koble til:
   ```bash
   adb connect <IP-ADRESSE>:<PORT>
   ```
