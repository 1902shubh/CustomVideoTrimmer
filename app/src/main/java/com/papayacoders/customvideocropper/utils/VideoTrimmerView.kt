package com.papayacoders.customvideocropper.utils

import android.content.Context
import android.text.format.Formatter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.VideoView

class VideoTrimmerView @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0)
//    : BaseVideoTrimmerView(context, attrs, defStyleAttr)
{

//    private lateinit var binding: VideoTrimmerBinding

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

//    override fun initRootView() {
//        binding = VideoTrimmerBinding.inflate(LayoutInflater.from(context), this, true)
//        binding.fab.setOnClickListener { initiateTrimming() }
//    }

//    override fun getTimeLineView(): TimeLineView = binding.timeLineView
//
//    override fun getTimeInfoContainer(): View = binding.timeTextContainer
//
//    override fun getPlayView(): View = binding.playIndicatorView
//
//    override fun getVideoView(): VideoView = binding.videoView
//
//    override fun getVideoViewContainer(): View = binding.videoViewContainer
//
//    override fun getRangeSeekBarView(): RangeSeekBarView = binding.rangeSeekBarView
//
//    override fun onRangeUpdated(startTimeInMs: Int, endTimeInMs: Int) {
//        val seconds = context.getString(R.string.short_seconds)
//        binding.trimTimeRangeTextView.text = "${stringForTime(startTimeInMs)} $seconds - ${stringForTime(endTimeInMs)} $seconds"
//    }
//
//    override fun onVideoPlaybackReachingTime(timeInMs: Int) {
//        val seconds = context.getString(R.string.short_seconds)
//        binding.playbackTimeTextView.text = "${stringForTime(timeInMs)} $seconds"
//    }
//
//    override fun onGotVideoFileSize(videoFileSize: Long) {
//        binding.videoFileSizeTextView.text = Formatter.formatShortFileSize(context, videoFileSize)
//    }
}