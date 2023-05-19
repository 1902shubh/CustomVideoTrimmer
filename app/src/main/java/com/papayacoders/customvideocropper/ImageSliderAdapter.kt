package com.papayacoders.customvideocropper

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.papayacoders.customvideocropper.databinding.ItemImageViewBinding

class ImageSliderAdapter(
    private val imageList: List<String>,
    private val context: Context
) : PagerAdapter() {
    override fun getCount(): Int {
        return imageList.size
    }

    override fun isViewFromObject(
        view: View,
        `object`: Any
    ): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imageSliderItemBinding = ItemImageViewBinding.inflate(LayoutInflater.from(context))
        Glide.with(context)
            .load("file:///${imageList[position]}")
            .into(imageSliderItemBinding.momentImage)
        container.addView(imageSliderItemBinding.root, 0)
        return imageSliderItemBinding.root
    }

    override fun destroyItem(
        container: ViewGroup,
        position: Int,
        `object`: Any
    ) {
        container.removeView(`object` as View)
    }
}
