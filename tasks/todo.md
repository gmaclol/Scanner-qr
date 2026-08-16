# Task Attivi & Checklist

## CI/CD & Auto-Update GitHub Releases
- [x] Configurare `app/build.gradle.kts` con signingConfig release per `keystore.properties`
- [x] Creare workflow GitHub Actions `.github/workflows/release.yml`
- [x] Creare `UpdateManager.kt` per il controllo automatico degli aggiornamenti via GitHub Releases
- [x] Aggiungere pulsante 'Verifica Aggiornamenti' in `SettingsActivity`
- [x] Inizializzare repository Git con `.gitignore` protetto
- [ ] Configurare GitHub Secrets (Keystore & Passwords) su `gmaclol/Scanner-qr`
- [ ] Push iniziale e creazione tag `v1.0.0`
