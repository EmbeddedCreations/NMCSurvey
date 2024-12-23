package com.example.myapplication

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class ImageSliderAdapterUpload(
    private val imageList: List<Pair<Uri, String>> // List of Pair<Uri, ImageName>
) : RecyclerView.Adapter<ImageSliderAdapterUpload.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_slider, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val (imageUri, imageName) = imageList[position]

        // Load image from URI
        holder.imageView.setImageURI(imageUri)

    }

    override fun getItemCount(): Int = imageList.size
}
