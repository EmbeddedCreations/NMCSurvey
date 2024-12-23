package com.example.myapplication

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageSliderAdapter(
    private val imageData: List<Pair<String, String>> // Pair<ImageURL, ID>
) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_slider, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val (imageUrl, id) = imageData[position]

        // Use Glide to load the image
        Glide.with(holder.imageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.image_placeholder)
            .into(holder.imageView)

        // Log the image ID (optional, for debugging or later use)
        Log.d("ImageSliderAdapter", "Image ID: $id, URL: $imageUrl")
    }

    override fun getItemCount(): Int = imageData.size
}


