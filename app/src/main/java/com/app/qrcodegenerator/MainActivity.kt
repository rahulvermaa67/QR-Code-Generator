package com.app.qrcodegenerator

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var adView: AdView
    private lateinit var editText: EditText
    private lateinit var generateButton: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var placeholderText: TextView
    private lateinit var downloadButton: Button
    private lateinit var shareButton: Button
    private lateinit var qrCodeBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this) {}

        // Find views
        adView = findViewById(R.id.adView)
        editText = findViewById(R.id.editText)
        generateButton = findViewById(R.id.generateButton)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        placeholderText = findViewById(R.id.placeholderText)
        downloadButton = findViewById(R.id.downloadButton)
        shareButton = findViewById(R.id.shareButton)

        // Load Ad
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        // Generate QR Code
        generateButton.setOnClickListener {
            val text = editText.text.toString()
            if (text.isNotEmpty()) {
                generateQRCode(text)
                downloadButton.isEnabled = true
                shareButton.isEnabled = true
                qrCodeImageView.visibility = ImageView.VISIBLE
                placeholderText.visibility = TextView.GONE
            } else {
                showAlertDialog("Please Enter Any Text or Link")
            }
        }

        // Request Storage Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }

        // Download QR Code
        downloadButton.setOnClickListener {
            saveQRCode()
        }

        // Share QR Code
        shareButton.setOnClickListener {
            shareQRCode()
        }

        // Initially disable the download and share buttons
        downloadButton.isEnabled = false
        shareButton.isEnabled = false
    }

    private fun generateQRCode(text: String) {
        val qrCodeWriter = QRCodeWriter()
        try {
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 400, 400)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) -0x1000000 else -0x1)
                }
            }
            qrCodeBitmap = bitmap
            qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun saveQRCode() {
        if (::qrCodeBitmap.isInitialized) {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val qrCodeFile = File(path, "QRCode.png")
            try {
                val outputStream = FileOutputStream(qrCodeFile)
                qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                MediaStore.Images.Media.insertImage(contentResolver, qrCodeFile.absolutePath, qrCodeFile.name, null)
                showAlertDialog("Download Successful")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            showAlertDialog("No QR Code Generated To Save.")
        }
    }

    private fun shareQRCode() {
        if (::qrCodeBitmap.isInitialized) {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val qrCodeFile = File(path, "QRCode.png")
            try {
                val outputStream = FileOutputStream(qrCodeFile)
                qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", qrCodeFile)
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/png"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            showAlertDialog("No QR Code generated to share.")
        }
    }


    private fun showAlertDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() })
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveQRCode()
        } else {
            showAlertDialog("Permission denied to write to storage.")
        }
    }
}
