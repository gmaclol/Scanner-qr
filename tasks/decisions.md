# Decisioni Architetturali e Vincoli di Progetto

## Stack e Vincoli — QuickScan (Scanner QR & Barcode)

### Stack Tecnologico
- **Piattaforma**: Android Nativo (Kotlin 2.1.0)
- **Min SDK**: 26 (Android 8.0 Oreo) | **Target SDK**: 35 (Android 15)
- **Build System**: Gradle 9.3.1 + Android Gradle Plugin (AGP) 8.13.2
- **Scansione Fotocamera**: CameraX (v1.4.1) + Google ML Kit Barcode Scanning (v17.3.0) — elaborazione on-device istantanea a zero-costo
- **Generazione Codici QR**: ZXing Core (v3.5.3) — elaborazione bitmap locale e offline
- **UI / Design System**: Material 3 + Custom Glassmorphism Apple-style (OLED Black, Blur, Dynamic Island notification pill, Spring animations)
- **Persistenza Cronologia & Impostazioni**: SharedPreferences + GSON
- **Rete & Auto-Update**: OkHttp + GitHub Releases API
- **CI/CD**: GitHub Actions con firma release keystore automatica su push di tag *

### Vincoli
- **Budget**: Zero-costo totale (ML Kit on-device, ZXing locale, GitHub Releases gratuite per hosting APK e aggiornamenti)
- **Privacy & Prestazioni**: Scansione 100% offline e locale, nessuna telemetria invasiva, massima fluidità a 60/120fps.
