package com.apple.quickscan.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(
    private val onBarcodeDetected: (barcode: Barcode) -> Unit
) : ImageAnalysis.Analyzer {

    // Configure ML Kit to recognize all formats (QR, Data Matrix, Aztec, EAN-13, EAN-8, UPC, Code 128, Code 39, etc.)
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    
    @Volatile
    var isScanningEnabled: Boolean = true

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanningEnabled) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && isScanningEnabled) {
                        val firstBarcode = barcodes[0]
                        if (!firstBarcode.rawValue.isNullOrBlank()) {
                            isScanningEnabled = false
                            onBarcodeDetected(firstBarcode)
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignore transient frame errors
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
