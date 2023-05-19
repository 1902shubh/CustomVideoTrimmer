package com.papayacoders.customvideocropper

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.papayacoders.customvideocropper.databinding.ActivityMainBinding
import com.papayacoders.customvideocropper.utils.Constants.allowedVideoFileExtensions
import com.papayacoders.customvideocropper.utils.FileUtils
import com.papayacoders.customvideocropper.utils.ThirdPartyIntentsUtil
import com.papayacoders.customvideocropper.video_trimmer.interfaces.OnProgressVideoListener
import com.papayacoders.customvideocropper.video_trimmer.interfaces.OnRangeSeekBarListener
import com.papayacoders.customvideocropper.video_trimmer.interfaces.VideoTrimmingListener
import com.papayacoders.customvideocropper.video_trimmer.utils.BackgroundExecutor
import com.papayacoders.customvideocropper.video_trimmer.utils.TrimVideoUtils
import com.papayacoders.customvideocropper.video_trimmer.view.RangeSeekBarView
import com.papayacoders.customvideocropper.video_trimmer.view.TimeLineView
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity(), VideoTrimmingListener {
    private lateinit var binding: ActivityMainBinding

    private var duration = 0
    private var timeVideo = 0
    private var startPosition = 0
    private var endPosition = 0
    private var originSizeFile: Long = 0
    private var resetSeekBar = true
    private lateinit var playView: View

    private var listeners = ArrayList<OnProgressVideoListener>()
    private var videoViewContainer: View? = null
    private lateinit var videoView: VideoView
    private var maxDurationInMs: Int = 0
    private lateinit var rangeSeekBarView: RangeSeekBarView

    private var timeLineView: TimeLineView? = null
    private var src: Uri? = null
    private var dstFile: File? = null
    private var videoTrimmingListener: VideoTrimmingListener? = null
    private lateinit var imageFile: File
    private lateinit var videoFile: File
    private lateinit var lastCapturedImageUri: Uri
    private lateinit var lastCapturedVideoUri: Uri

    private var videoList = arrayListOf<String>()

    companion object {
        private val videosMimeTypes = ArrayList<String>(allowedVideoFileExtensions.size)
        private val STORAGE_PERMISSION_CODE = 1
        const val REQUEST_SELECT_VIDEO = 0
        const val REQUEST_CAPTURE_VIDEO = 1
        private const val MIN_TIME_FRAME = 1000
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setReadStoragePermission()

        videoList.clear()
        videoTrimmingListener = this

        val parentFolder = getExternalFilesDir(null)!!
        parentFolder.mkdirs()
        val fileName = "trimmedVideo_${System.currentTimeMillis()}.mp4"
        dstFile = File(parentFolder, fileName)

        videoViewContainer = binding.videoView2
        videoView = binding.videoView2
        rangeSeekBarView = binding.rangeSeekBarView
        timeLineView = binding.timeLineView
        playView = binding.playIndicatorView

        setMaxDurationInMs(10 * 10000)

        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView2)
        binding.videoView2.setMediaController(mediaController)

        binding.fileCV.setOnClickListener { pickVideo() }
        binding.cameraCV.setOnClickListener { selectVideo(true) }

        setUpListeners()
        setUpMargins()

        binding.button.setOnClickListener {
            initiateTrimming()
        }


        binding.rangeSeekBarView.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                // Do nothing
            }

            override fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                // Do nothing
            }

            override fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })


    }

    private fun setReadStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                    REQUEST_SELECT_VIDEO
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_CAPTURE_VIDEO)
            }
        }
    }

    private fun selectVideo(isCamera: Boolean) {
        try {
            videoFile = FileUtils.getVideoFilePath(this)
            lastCapturedVideoUri = FileProvider.getUriForFile(
                this, FileUtils.getAuthorities(this), videoFile
            )
            val intent = if (isCamera) {
                Intent(MediaStore.ACTION_VIDEO_CAPTURE).putExtra(
                    MediaStore.EXTRA_OUTPUT, lastCapturedVideoUri
                )
            } else {
                val videoPickIntent = Intent(Intent.ACTION_PICK)
                videoPickIntent.type = FileUtils.INTENT_TYPE_VIDEO
                videoPickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                videoPickIntent
            }
            videoIntentLauncher.launch(intent)
        } catch (e: Exception) {
            return
        }
    }

    private var videoIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (!::lastCapturedVideoUri.isInitialized) {
                    videoFile = FileUtils.getVideoFilePath(this)
                    lastCapturedVideoUri = FileProvider.getUriForFile(
                        this, FileUtils.getAuthorities(this), videoFile
                    )
                }
                lifecycleScope.launch {
                    if (result.data?.clipData != null) {
                        val count: Int = result.data?.clipData?.itemCount ?: 0
                        for (i in 0 until count) {

                            val filePath = FileUtils.getImageFile(
                                this@MainActivity, result.data?.clipData?.getItemAt(i)?.uri
                            )
                            if (filePath!!.exists()) {
                                videoList.add(filePath.toString())
                                Log.d("SHUBH", "filepath: $filePath")
                            }
                        }
                    } else {
                        result.data?.data?.let {

                            videoFile = FileUtils.getImageFile(this@MainActivity, it)!!
                        }
                        if (videoFile.exists()) {
                            videoList.add(videoFile.toString())
                            Log.d("SHUBH", "videoFile: $videoFile")

                        }
                    }
                }
            }
        }


    //Pick a video file from device
    private fun pickVideo() {
        val intent = Intent()
        intent.apply {
            type = "video/*"
            action = Intent.ACTION_PICK
        }
        // intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Select video"), REQUEST_SELECT_VIDEO)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {

        if (resultCode == Activity.RESULT_OK)
            if (requestCode == REQUEST_SELECT_VIDEO || requestCode == REQUEST_CAPTURE_VIDEO) {
                handleResult(intent)
            }

        super.onActivityResult(requestCode, resultCode, intent)
    }

    private fun handleResult(data: Intent?) {
        if (data != null && data.data != null) {
            val uri = data.data
            if (uri != null && checkIfUriCanBeUsedForVideo(uri)) {
                setVideoURI(uri, this)

                binding.videoView2.setVideoURI(uri)
                binding.timeLineView.setVideo(uri)
                binding.timeLineView.getBitmap(1080, 110)

                notifyProgressUpdate(true)

                Log.d("MICODERRR", "handleResult: ${uri.toString()}")
            } else {
                Toast.makeText(
                    this@MainActivity,
                    R.string.toast_cannot_retrieve_selected_video,
                    Toast.LENGTH_SHORT
                ).show()
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
            val inputStream =
                (if (fileDescriptor == null) null else contentResolver.openInputStream(uri))
                    ?: return false
            inputStream.close()
            return true
        } catch (e: Exception) {
            return false
        }
    }


    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            RangeSeekBarView.ThumbType.LEFT.index -> {
                startPosition = (duration * value / 100L).toInt()
                binding.videoView2.seekTo(startPosition)
            }

            RangeSeekBarView.ThumbType.RIGHT.index -> {
                endPosition = (duration * value / 100L).toInt()
            }
        }
//        Log.d("SHUBH", "onSeekThumbs: $startPosition  $endPosition")
        setProgressBarPosition(startPosition)

        onRangeUpdated(startPosition, endPosition)
        timeVideo = endPosition - startPosition
    }

    private fun onRangeUpdated(startTimeInMs: Int, endTimeInMs: Int) {
        val seconds = getString(R.string.short_seconds)
        binding.trimTimeRangeTextView.text =
            "${stringForTime(startTimeInMs)} $seconds - ${stringForTime(endTimeInMs)} $seconds"
    }

    private fun onGotVideoFileSize(videoFileSize: Long) {
        binding.videoFileSizeTextView.text = Formatter.formatShortFileSize(this, videoFileSize)
    }

    private fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        val timeFormatter = java.util.Formatter()
        return if (hours > 0)
            timeFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        else
            timeFormatter.format("%02d:%02d", minutes, seconds).toString()
    }

    private fun onStopSeekThumbs() {
        pauseVideo()
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (duration == 0) return
        val position = binding.videoView2.currentPosition
        if (all)
            for (item in listeners)
                item.updateProgress(position, duration, position * 100f / duration)
        else
            listeners[1].updateProgress(position, duration, position * 100f / duration)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpListeners() {
        listeners.add(object : OnProgressVideoListener {
            override fun updateProgress(time: Int, max: Int, scale: Float) {
                updateVideoProgress(time)
            }
        })

        videoView.setOnErrorListener { _, what, extra ->
            false
        }

        videoView.setOnTouchListener { v, event ->
            true
        }

        rangeSeekBarView.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                // Do nothing
            }

            override fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                // Do nothing
            }

            override fun onSeekStop(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })
        binding.videoView2.setOnPreparedListener { this.onVideoPrepared(it) }
        binding.videoView2.setOnCompletionListener { onVideoCompleted() }
    }

    private fun onVideoCompleted() {
        videoView!!.seekTo(startPosition)
    }

    @UiThread
    private fun onVideoPrepared(mp: MediaPlayer) {
        val lp = binding.videoView2!!.layoutParams

        videoView.layoutParams = lp
        playView.visibility = View.VISIBLE
        duration = videoView.duration
        setSeekBarPosition()
        onVideoPlaybackReachingTime(0)
    }

    private fun updateVideoProgress(time: Int) {
        if (time >= endPosition) {
            pauseVideo()
            resetSeekBar = true
            return
        }
        setProgressBarPosition(time)
        onVideoPlaybackReachingTime(time)

    }

    private fun onVideoPlaybackReachingTime(timeInMs: Int) {
        val seconds = getString(R.string.short_seconds)
        binding.playbackTimeTextView.text = "${stringForTime(timeInMs)} $seconds"
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun pauseVideo() {
        binding.videoView2.pause()
        playView.visibility = View.VISIBLE
    }

    private fun setProgressBarPosition(position: Int) {
        if (duration > 0) {
        }
    }

    private fun setUpMargins() {
        val marge = rangeSeekBarView!!.thumbWidth
        val lp: ViewGroup.MarginLayoutParams =
            timeLineView!!.layoutParams as ViewGroup.MarginLayoutParams
        lp.setMargins(marge, lp.topMargin, marge, lp.bottomMargin)
        timeLineView!!.layoutParams = lp
    }

    private fun setSeekBarPosition() {
        if (duration >= maxDurationInMs) {
            startPosition = duration / 2 - maxDurationInMs / 2
            endPosition = duration / 2 + maxDurationInMs / 2
            rangeSeekBarView.setThumbValue(0, startPosition * 100f / duration)
            rangeSeekBarView.setThumbValue(1, endPosition * 100f / duration)
        } else {
            startPosition = 0
            endPosition = duration
        }
        Log.d("SHUBH", "setSeekBarPosition: $startPosition  $endPosition")
        setProgressBarPosition(startPosition)
        videoView.seekTo(startPosition)
        timeVideo = duration
        rangeSeekBarView.initMaxWidth()
    }

    private fun setMaxDurationInMs(maxDurationInMs: Int) {
        this.maxDurationInMs = maxDurationInMs
    }

    private fun setVideoURI(videoURI: Uri, context: Context) {
        src = videoURI
        if (originSizeFile == 0L) {
            val cursor = context.contentResolver.query(videoURI, null, null, null, null)
            if (cursor != null) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                originSizeFile = cursor.getLong(sizeIndex)
                cursor.close()
                onGotVideoFileSize(originSizeFile)
            }
        }
        videoView.setVideoURI(src)
        videoView.requestFocus()
        timeLineView!!.setVideo(src!!)
    }

    @Suppress("unused")
    @UiThread
    fun initiateTrimming() {
        pauseVideo()
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(this, src)
        val metadataKeyDuration =
            java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
        if (timeVideo < MIN_TIME_FRAME) {
            if (metadataKeyDuration - endPosition > MIN_TIME_FRAME - timeVideo) {
                endPosition += MIN_TIME_FRAME - timeVideo
            } else if (startPosition > MIN_TIME_FRAME - timeVideo) {
                startPosition -= MIN_TIME_FRAME - timeVideo
            }
        }
        //notify that video trimming started
        if (videoTrimmingListener != null)
            videoTrimmingListener!!.onTrimStarted()
        BackgroundExecutor.execute(
            object : BackgroundExecutor.Task(null, 0L, null) {
                override fun execute() {
                    try {
                        TrimVideoUtils.startTrim(
                            this@MainActivity,
                            src!!,
                            dstFile!!,
                            startPosition.toLong(),
                            endPosition.toLong(),
                            duration.toLong(),
                            videoTrimmingListener!!
                        )
                    } catch (e: Throwable) {
                        Thread.getDefaultUncaughtExceptionHandler()
                            .uncaughtException(Thread.currentThread(), e)
                    }
                }
            }
        )
    }

    override fun onVideoPrepared() {
        Log.d("SHUBH", "onVideoPrepared: ")

    }

    override fun onTrimStarted() {
        Log.d("SHUBH", "onTrimStarted: ")
    }

    override fun onFinishedTrimming(uri: Uri?) {
        Log.d("SHUBH", "onFinishedTrimming: $uri ")
        Toast.makeText(this, "Trimmed", Toast.LENGTH_SHORT).show()
    }

    override fun onErrorWhileViewingVideo(what: Int, extra: Int) {
        Log.d("SHUBH", "onErrorWhileViewingVideo: ")
    }

}