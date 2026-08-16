# Mappa della Struttura del Progetto

### MainActivity.kt
Responsabilità: Schermata principale dell'app. Scanner live con CameraX e ML Kit.
Funzioni:
- startCamera(): Avvia il preview e ImageAnalysis di CameraX.
- onBarcodeDetected(): Esegue feedback sonoro/aptico, copia negli appunti, mostra la pill animata 'Dynamic Island' e apre il bottom sheet dei risultati.
- setupDrawer(): Configura la navigazione laterale con stile frosted glass.

### CreateQrActivity.kt
Responsabilità: Generazione di codici QR per 7 tipologie (Testo, URL, WiFi, Contatto vCard, Telefono, Email, SMS).
Funzioni:
- generateQr(): Genera la bitmap in tempo reale tramite ZXing.
- shareQr(): Condivide l'immagine del codice QR tramite Intent ACTION_SEND universale.
- saveQrToGallery(): Salva l'immagine PNG in Pictures/QuickScan.

### HistoryActivity.kt
Responsabilità: Visualizzazione e gestione della cronologia dei codici scansionati.

### SettingsActivity.kt
Responsabilità: Gestione preferenze utente (suono, vibrazione, copia automatica, apertura automatica URL) e verifica aggiornamenti remoti.

### UpdateManager.kt
Responsabilità: Controllo aggiornamenti su GitHub Releases e download/installazione APK.

### scanner/QrCodeAnalyzer.kt
Responsabilità: ImageAnalysis Analyzer per analizzare i frame della fotocamera con Google ML Kit.

### scanner/ViewfinderView.kt
Responsabilità: Disegno del mirino scanner Apple-style con angoli curvi e laser animato.

### generator/QrGeneratorHelper.kt
Responsabilità: Helper ZXing per encoding di stringhe, WiFi, vCard, mailto in bitmap QR ad alta risoluzione.
