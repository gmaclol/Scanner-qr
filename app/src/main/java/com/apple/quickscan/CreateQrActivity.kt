package com.apple.quickscan

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.apple.quickscan.databinding.ActivityCreateQrBinding
import com.apple.quickscan.generator.QrGeneratorHelper
import com.apple.quickscan.history.HistoryRepository
import com.apple.quickscan.history.ScanHistoryItem
import com.apple.quickscan.utils.ClipboardHelper
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class CreateQrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateQrBinding
    private lateinit var historyRepository: HistoryRepository
    private var currentBitmap: Bitmap? = null
    private var currentContent: String = ""

    private enum class QrType {
        URL, TEXT, WIFI, CONTACT, PHONE, EMAIL, SMS
    }

    private var selectedType: QrType = QrType.TEXT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        historyRepository = HistoryRepository(this)

        setupChips()
        setupTextWatchers()
        setupActionButtons()

        // Azione preferita di default: Testo libero (vuoto)
        binding.chipText.isChecked = true
        binding.etInput1.setText("")
        updateInputsForType(QrType.TEXT)
    }

    private fun setupChips() {
        binding.chipGroupTypes.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                R.id.chipUrl -> updateInputsForType(QrType.URL)
                R.id.chipText -> updateInputsForType(QrType.TEXT)
                R.id.chipWifi -> updateInputsForType(QrType.WIFI)
                R.id.chipContact -> updateInputsForType(QrType.CONTACT)
                R.id.chipPhone -> updateInputsForType(QrType.PHONE)
                R.id.chipEmail -> updateInputsForType(QrType.EMAIL)
                R.id.chipSms -> updateInputsForType(QrType.SMS)
            }
        }
    }

    private fun updateInputsForType(type: QrType) {
        selectedType = type
        binding.etInput1.visibility = View.VISIBLE
        binding.etInput2.visibility = View.GONE
        binding.etInput3.visibility = View.GONE

        when (type) {
            QrType.URL -> {
                binding.etInput1.hint = getString(R.string.input_url_hint)
                binding.etInput1.inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            }
            QrType.TEXT -> {
                binding.etInput1.hint = getString(R.string.input_text_hint)
                binding.etInput1.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
            QrType.WIFI -> {
                binding.etInput1.hint = getString(R.string.input_wifi_ssid)
                binding.etInput1.inputType = android.text.InputType.TYPE_CLASS_TEXT
                binding.etInput2.visibility = View.VISIBLE
                binding.etInput2.hint = getString(R.string.input_wifi_password)
                binding.etInput2.inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            QrType.CONTACT -> {
                binding.etInput1.hint = getString(R.string.input_contact_name)
                binding.etInput1.inputType = android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                binding.etInput2.visibility = View.VISIBLE
                binding.etInput2.hint = getString(R.string.input_contact_phone)
                binding.etInput2.inputType = android.text.InputType.TYPE_CLASS_PHONE
                binding.etInput3.visibility = View.VISIBLE
                binding.etInput3.hint = getString(R.string.input_contact_email)
                binding.etInput3.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            QrType.PHONE -> {
                binding.etInput1.hint = getString(R.string.input_phone_hint)
                binding.etInput1.inputType = android.text.InputType.TYPE_CLASS_PHONE
            }
            QrType.EMAIL -> {
                binding.etInput1.hint = getString(R.string.input_email_hint)
                binding.etInput1.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                binding.etInput2.visibility = View.VISIBLE
                binding.etInput2.hint = getString(R.string.input_email_subject)
                binding.etInput2.inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
            QrType.SMS -> {
                binding.etInput1.hint = getString(R.string.input_sms_number)
                binding.etInput1.inputType = android.text.InputType.TYPE_CLASS_PHONE
                binding.etInput2.visibility = View.VISIBLE
                binding.etInput2.hint = getString(R.string.input_sms_body)
                binding.etInput2.inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
        }

        regenerateQr()
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                regenerateQr()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etInput1.addTextChangedListener(watcher)
        binding.etInput2.addTextChangedListener(watcher)
        binding.etInput3.addTextChangedListener(watcher)
    }

    private fun regenerateQr() {
        val in1 = binding.etInput1.text.toString().trim()
        val in2 = binding.etInput2.text.toString().trim()
        val in3 = binding.etInput3.text.toString().trim()

        currentContent = when (selectedType) {
            QrType.URL -> in1
            QrType.TEXT -> in1
            QrType.WIFI -> if (in1.isNotBlank()) QrGeneratorHelper.formatWifi(in1, in2) else ""
            QrType.CONTACT -> if (in1.isNotBlank()) QrGeneratorHelper.formatContactVCard(in1, in2, in3) else ""
            QrType.PHONE -> if (in1.isNotBlank()) QrGeneratorHelper.formatPhone(in1) else ""
            QrType.EMAIL -> if (in1.isNotBlank()) QrGeneratorHelper.formatEmail(in1, in2) else ""
            QrType.SMS -> if (in1.isNotBlank()) QrGeneratorHelper.formatSms(in1, in2) else ""
        }

        if (currentContent.isNotBlank()) {
            val bitmap = QrGeneratorHelper.generateQrBitmap(currentContent, size = 600)
            currentBitmap = bitmap
            binding.ivQrPreview.setImageBitmap(bitmap)
        } else {
            currentBitmap = null
            binding.ivQrPreview.setImageDrawable(null)
        }
    }

    private fun setupActionButtons() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShareQr.setOnClickListener {
            if (currentBitmap == null || currentContent.isBlank()) {
                Toast.makeText(this, "Inserisci prima del testo per generare il QR", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            shareQrImageAndText()
        }

        binding.btnSaveGallery.setOnClickListener {
            if (currentBitmap == null || currentContent.isBlank()) {
                Toast.makeText(this, "Inserisci prima del testo per generare il QR", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveQrToGallery()
        }

        binding.btnCopyText.setOnClickListener {
            if (currentContent.isNotBlank()) {
                ClipboardHelper.copyToClipboard(this, currentContent)
                Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                saveToHistory()
            } else {
                Toast.makeText(this, "Inserisci prima del testo da copiare", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareQrImageAndText() {
        val bitmap = currentBitmap ?: return
        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "quickscan_qr.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, currentContent)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
            saveToHistory()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore nella condivisione: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveQrToGallery() {
        val bitmap = currentBitmap ?: return
        val filename = "QuickScan_QR_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QuickScan")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/QuickScan"
                val file = File(imagesDir)
                if (!file.exists()) {
                    file.mkdir()
                }
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(this, getString(R.string.qr_saved_success), Toast.LENGTH_LONG).show()
                saveToHistory()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore salvataggio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToHistory() {
        if (currentContent.isNotBlank()) {
            historyRepository.addItem(
                ScanHistoryItem(
                    content = currentContent,
                    formatName = "QR Generato (${selectedType.name})",
                    isGenerated = true
                )
            )
        }
    }
}
