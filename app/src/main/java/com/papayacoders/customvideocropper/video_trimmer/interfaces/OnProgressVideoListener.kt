package com.papayacoders.customvideocropper.video_trimmer.interfaces

interface OnProgressVideoListener {
    fun updateProgress(time: Int, max: Int, scale: Float)
}
