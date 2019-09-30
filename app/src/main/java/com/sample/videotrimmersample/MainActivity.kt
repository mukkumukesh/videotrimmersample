package com.sample.videotrimmersample

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_VIDEO_TRIMMER = 1
        private const val REQUEST_STORAGE_READ_ACCESS_PERMISSION = 2
        internal const val EXTRA_INPUT_URI = "EXTRA_INPUT_URI"
        private val allowedVideoFileExtensions = arrayOf("mkv", "mp4", "3gp", "mov", "mts")
        private val videosMimeTypes = ArrayList<String>(allowedVideoFileExtensions.size)
    }

    init {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        for (fileExtension in allowedVideoFileExtensions) {
            val mimeTypeFromExtension = mimeTypeMap.getMimeTypeFromExtension(fileExtension)
            if (mimeTypeFromExtension != null)
                videosMimeTypes.add(mimeTypeFromExtension)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_gallery.setOnClickListener { pickFromGallery() }
    }

    private fun pickFromGallery() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                getString(R.string.permission_read_storage_rationale),
                REQUEST_STORAGE_READ_ACCESS_PERMISSION
            )
        } else {
            var intentForChoosingVideos = ThirdPartyIntentsUtil.getPickFileChooserIntent(
                this,
                null,
                false,
                true,
                "video/*",
                videosMimeTypes.toTypedArray(),
                null
            )
            if (intentForChoosingVideos == null)
                intentForChoosingVideos =
                    ThirdPartyIntentsUtil.getPickFileIntent(this, "video/*,", videosMimeTypes.toTypedArray())
            if (intentForChoosingVideos != null)
                startActivityForResult(intentForChoosingVideos, REQUEST_VIDEO_TRIMMER)
        }
    }

    /**
     * Requests given permission.
     * If the permission has been denied previously, a Dialog will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private fun requestPermission(permission: String, rationale: String, requestCode: Int) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.permission_title_rationale))
            builder.setMessage(rationale)
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(permission),
                    requestCode
                )
            }
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_TRIMMER) {
                val uri = data!!.data
                if (uri != null && checkIfUriCanBeUsedForVideo(uri)) {
                    startTrimActivity(uri)
                } else {
                    Toast.makeText(this@MainActivity, R.string.toast_cannot_retrieve_selected_video, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun checkIfUriCanBeUsedForVideo(uri: Uri): Boolean {
        val mimeType = ThirdPartyIntentsUtil.getMimeType(this, uri)
        val identifiedAsVideo = mimeType != null && videosMimeTypes.contains(mimeType)
        if (!identifiedAsVideo)
            return false
        try {
            //check that it can be opened and trimmed using our technique
            val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
            val inputStream = (if (fileDescriptor == null) null else contentResolver.openInputStream(uri))
                ?: return false
            inputStream.close()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun startTrimActivity(uri: Uri) {
        val intent = Intent(this, TrimmerActivity::class.java)
        intent.putExtra(EXTRA_INPUT_URI, uri)
        startActivity(intent)
    }
}
