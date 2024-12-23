package com.example.myapplication

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class UploadedItemsAdapter(
    private val uploadedItems: List<UploadedItem>,
    private val onEditClick: (UploadedItem, String, String) -> Unit
) : RecyclerView.Adapter<UploadedItemsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewPager: ViewPager2 = view.findViewById(R.id.viewPagerImages)
        val descriptionText: TextView = view.findViewById(R.id.textViewDescription)
        val latitudeText: TextView = view.findViewById(R.id.textViewLatitude)
        val longitudeText: TextView = view.findViewById(R.id.textViewLongitude)
        val accuracyText: TextView = view.findViewById(R.id.textViewAccuracy)
        val clickedDate: TextView = view.findViewById(R.id.textViewClickedDate)
        val clickedTime: TextView = view.findViewById(R.id.textViewClickedTime)
        val distanceText: TextView = view.findViewById(R.id.textViewDistance) // Distance field
        val editButton: ImageView = view.findViewById(R.id.imageViewEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_uploaded, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = uploadedItems[position]

        // Set textual data
        holder.descriptionText.text = item.description ?: "No description"
        holder.latitudeText.text = "Lat: ${item.latitude ?: "N/A"}"
        holder.longitudeText.text = "Lon: ${item.longitude ?: "N/A"}"
        holder.accuracyText.text = "Accuracy: ${item.accurracy ?: "N/A"}"
        holder.clickedDate.text = "Date: ${item.clickedDate ?: "N/A"}"
        holder.clickedTime.text = "Time: ${item.clickedTime ?: "N/A"}"

        // Handle distance visibility
        if (item.distance != null) {
            holder.distanceText.text = "Distance: ${item.distance} m"
            holder.distanceText.visibility = View.VISIBLE
        } else {
            holder.distanceText.visibility = View.GONE
        }

        // Prepare the list of pairs (image URL, image ID) for the ImageSliderAdapter
        val imageData = item.imageUrls.zip(item.id) // Creates Pair<URL, ID> for each image

        // Pass the data to the ImageSliderAdapter
        val imageSliderAdapter = ImageSliderAdapter(imageData)
        holder.viewPager.adapter = imageSliderAdapter

        // Update the current image index on page change
        holder.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                item.currentImageIndex = position // Update the current image index
            }
        })

        // Handle edit button click
        holder.editButton.setOnClickListener {
            val currentIndex = item.currentImageIndex
            val selectedImageUrl = item.imageUrls[currentIndex]
            val selectedImageId = item.id[currentIndex]

            // Call the edit action with the selected image details
            onEditClick(item, selectedImageUrl, selectedImageId)
        }
    }

    override fun getItemCount(): Int = uploadedItems.size
}
