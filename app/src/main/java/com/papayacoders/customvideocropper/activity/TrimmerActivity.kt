package com.papayacoders.customvideocropper.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.papayacoders.customvideocropper.MainActivity
import com.papayacoders.customvideocropper.R
import com.papayacoders.customvideocropper.databinding.ActivityTrimmerBinding
import com.papayacoders.customvideocropper.utils.Constants.EXTRA_INPUT_URI
import com.papayacoders.customvideocropper.video_trimmer.interfaces.VideoTrimmingListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.log10
import kotlin.math.pow


class TrimmerActivity : AppCompatActivity(), VideoTrimmingListener {
//    private var progressDialog: ProgressDialog? = null

    private lateinit var binding: ActivityTrimmerBinding

    private val uris = mutableListOf<Uri>()

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding = ActivityTrimmerBinding.inflate(layoutInflater)
//        setContentView(binding.root)

//        val inputVideoUri: Uri? = intent?.getParcelableExtra(   EXTRA_INPUT_URI)
//        Log.d("MICODERRR", "onCreate: $inputVideoUri")
//
//        if (inputVideoUri == null) {
//            finish()
//            return
//        }
//        //setting progressbar
//        progressDialog = ProgressDialog(this)
//        progressDialog.setCancelable(false)

//        binding.videoTrimmerView.setMaxDurationInMs(10 * 10000)
//        binding.videoTrimmerView.setOnK4LVideoListener(this)
//        val parentFolder = getExternalFilesDir(null)!!
//        parentFolder.mkdirs()
//        Log.d("MICODERRR", "location: $parentFolder")
//        val fileName = "trimmedVideo_${System.currentTimeMillis()}.mp4"
//        val trimmedVideoFile = File(parentFolder, fileName)
//        binding.videoTrimmerView.setDestinationFile(trimmedVideoFile)
//        binding.videoTrimmerView.setVideoURI(inputVideoUri)
//        binding.videoTrimmerView.setVideoInformationVisibility(true)
    }

    override fun onTrimStarted() {
        binding.trimmingProgressView.visibility = View.VISIBLE
    }

    override fun onFinishedTrimming(uri: Uri?) {
        binding.trimmingProgressView.visibility = View.GONE
        if (uri == null) {
            Toast.makeText(this@TrimmerActivity, "failed trimming", Toast.LENGTH_SHORT).show()
        } else {
            ///storage/emulated/0/Android/data/com.micoder.videomers/files/trimmedVideo_1683899855398.mp4
            //uris.clear()

            Log.d("MICODERRR", "uri: $uri")
            Log.d("MICODERRR", "contentUri: ${uriToContentUri(this@TrimmerActivity, uri)}")
            //uris.add(Uri.parse("content://com.mixplorer.file/502!/com.micoder.videomers/files/trimmedVideo_1683976090858.mp4"))
            uris.add(uriToContentUri(this@TrimmerActivity, uri)!!)

            Toast.makeText(this@TrimmerActivity, uriToContentUri(this@TrimmerActivity, uri).toString(), Toast.LENGTH_LONG).show()


        }
        //finish()
    }

    private fun uriToContentUri(context: Context, uri: Uri): Uri? {
        return if ("content" == uri.scheme) {
            uri
        } else {
            val file = File(uri.path!!)
            FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        }
    }



    override fun onErrorWhileViewingVideo(what: Int, extra: Int) {
        binding.trimmingProgressView.visibility = View.GONE
        Toast.makeText(this@TrimmerActivity, "error while previewing video", Toast.LENGTH_SHORT).show()
    }

    override fun onVideoPrepared() {
        //        Toast.makeText(TrimmerActivity.this, "onVideoPrepared", Toast.LENGTH_SHORT).show();
    }
}