package com.apple.quickscan.generator

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrGeneratorHelper {

    fun generateQrBitmap(
        content: String,
        size: Int = 1024,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 2)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else lightColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun formatWifi(ssid: String, password: String, encryption: String = "WPA"): String {
        return "WIFI:T:$encryption;S:$ssid;P:$password;;"
    }

    fun formatContactVCard(name: String, phone: String, email: String): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCARD\n")
        sb.append("VERSION:3.0\n")
        sb.append("FN:").append(name).append("\n")
        if (phone.isNotBlank()) sb.append("TEL:").append(phone).append("\n")
        if (email.isNotBlank()) sb.append("EMAIL:").append(email).append("\n")
        sb.append("END:VCARD")
        return sb.toString()
    }

    fun formatPhone(phone: String): String = if (phone.startsWith("tel:")) phone else "tel:$phone"

    fun formatEmail(email: String, subject: String = ""): String {
        return if (subject.isNotBlank()) "mailto:$email?subject=${android.net.Uri.encode(subject)}" else "mailto:$email"
    }

    fun formatSms(phone: String, body: String = ""): String {
        return if (body.isNotBlank()) "smsto:$phone:$body" else "smsto:$phone"
    }
}
