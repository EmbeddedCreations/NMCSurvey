package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.google.android.material.datepicker.MaterialDatePicker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UploadedItemsAdapter
    private val allUploadedItems = mutableListOf<UploadedItem>() // Store all uploaded items
    private val filteredItems = mutableListOf<UploadedItem>() // Store items to be displayed
    private var currentPage = 1
    private val itemsPerPage = 10
    private lateinit var progressBarLoader: ProgressBar
    private var isLoading = false
    private lateinit var selectedDateText: TextView
    private lateinit var buttonClearFilter: Button
    private lateinit var noDataTextView: TextView
    private var selectedDate: String? = null // Variable to store the selected date
    private lateinit var cardCountTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UploadedItemsAdapter(filteredItems) { uploadedItem, imageUrl, imageId ->
            // Handle edit action here
            onEditItem(uploadedItem, imageUrl, imageId)
        }

        recyclerView.adapter = adapter
        progressBarLoader = findViewById(R.id.progressBarLoader)
        selectedDateText = findViewById(R.id.textViewSelectedDate)
        buttonClearFilter = findViewById(R.id.buttonClearFilter)
        noDataTextView = findViewById(R.id.textViewNoData)
        cardCountTextView = findViewById(R.id.textViewCardCount)

        val buttonSelectDate: Button = findViewById(R.id.buttonSelectDate)

        // Set up the Material Date Picker
        buttonSelectDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Clear filter button
        buttonClearFilter.setOnClickListener {
            clearDateFilter()
        }
        // Retrieve intent extras
        val username = intent.getStringExtra("username") ?: "Unknown User"
        val lat = intent.getDoubleExtra("lat", Double.NaN)
        val long = intent.getDoubleExtra("long", Double.NaN)

        // Check if lat/long are valid
        if (!lat.isNaN() && !long.isNaN()) {
            fetchSurveyDataFromServerNearby(username, lat, long) // Fetch nearby locations
        } else {
            fetchSurveyDataFromServer(username) // Fetch all data
        }

        // Pagination scroll listener
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // Trigger load more if the last visible item is within the threshold
                if (!isLoading && lastVisibleItem + 1 >= totalItemCount) {
                    loadMoreItems()
                }
            }
        })
    }
    private fun showLoader(show: Boolean) {
        progressBarLoader.visibility = if (show) View.VISIBLE else View.GONE
    }
    private fun onEditItem(item: UploadedItem, imageUrl: String, imageId: String) {
        // Navigate to the EditActivity with the selected image details
        showLoader(true)

        val intent = Intent(this, EditActivity::class.java).apply {
            putExtra("IMAGE_URL", imageUrl)
            putExtra("IMAGE_ID", imageId) // Ensure the key matches
            putExtra("DESCRIPTION", item.description)
            putExtra("LATITUDE", item.latitude)
            putExtra("LONGITUDE", item.longitude)
            putExtra("CLICKED_DATE", item.clickedDate)
            putExtra("CLICKED_TIME", item.clickedTime)
            putExtra("ACCURACY", item.accurracy)
            val entryBy =intent.getStringExtra("username") ?: "Unknown User"
            putExtra("EntryBy", entryBy)
        }
        startActivity(intent)
        showLoader(false)

    }


    private fun showDatePickerDialog() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()

        datePicker.show(supportFragmentManager, "DATE_PICKER")

        datePicker.addOnPositiveButtonClickListener { selection ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = Date(selection)
            selectedDate = dateFormat.format(date)

            // Update the selected date text
            selectedDateText.text = "Selected Date: $selectedDate"

            // Filter items by the selected date
            filterItemsByDate()
        }
    }

    private fun clearDateFilter() {
        selectedDate = null
        selectedDateText.text = "Selected Date: None"
        buttonClearFilter.visibility = Button.GONE
        currentPage = 1

        filteredItems.clear()
        loadItemsForPage()
    }

    private fun filterItemsByDate() {
        buttonClearFilter.visibility = Button.VISIBLE
        currentPage = 1
        filteredItems.clear()

        val dateFilteredItems = allUploadedItems.filter { it.clickedDate == selectedDate }

        if (dateFilteredItems.isEmpty()) {
            noDataTextView.visibility = TextView.VISIBLE
        } else {
            noDataTextView.visibility = TextView.GONE
            filteredItems.addAll(dateFilteredItems.take(itemsPerPage))
            updateCardCount(dateFilteredItems.size, filteredItems.size)
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadMoreItems() {
        showLoader(true)
        isLoading = true
        currentPage++

        val itemsToLoad = if (selectedDate != null) {
            allUploadedItems.filter { it.clickedDate == selectedDate }
        } else {
            allUploadedItems
        }

        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, itemsToLoad.size)

        if (startIndex < itemsToLoad.size) {
            filteredItems.addAll(itemsToLoad.subList(startIndex, endIndex))
            updateCardCount(itemsToLoad.size, filteredItems.size)
            adapter.notifyDataSetChanged()
        } else if (filteredItems.isEmpty()) {
            cardCountTextView.text = "No results found"
        }

        isLoading = false
        showLoader(false)

    }

    private fun fetchSurveyDataFromServer(username: String) {
        isLoading = true

        ApiClient.apiService.fetchSurveyData(username).enqueue(object : Callback<List<SurveyItem>> {
            override fun onResponse(call: Call<List<SurveyItem>>, response: Response<List<SurveyItem>>) {
                showLoader(false)

                if (response.isSuccessful && response.body() != null) {
                    val surveyItems = response.body()!!

                    // Log deserialized SurveyItems for debugging
                    Log.d("APIResponse", "Deserialized Response: $surveyItems")

                    try {
                        // Safely map SurveyItems to UploadedItems
                        val newItems = surveyItems.map { surveyItem ->
                            UploadedItem(
                                id = surveyItem.ids ?: emptyList(), // Directly use the List<String> from the API response
                                imageUrls = surveyItem.imageUrls ?: emptyList(),
                                description = surveyItem.remark ?: "No description available",
                                latitude = surveyItem.latitude ?: "Not available",
                                longitude = surveyItem.longitude ?: "Not available",
                                clickedDate = surveyItem.clickedDate ?: "Not specified",
                                clickedTime = surveyItem.clickedTime ?: "Not specified",
                                accurracy = surveyItem.accurracy ?: "N/A",
                                distance = surveyItem.distance
                            )
                        }


                        allUploadedItems.addAll(newItems)
                        loadItemsForPage()
                        updateCardCount(allUploadedItems.size, filteredItems.size)

                    } catch (e: Exception) {
                        Log.e("MappingError", "Error while mapping API response", e)
                        Toast.makeText(this@HistoryActivity, "Error processing data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("APIResponse", "Failed to fetch data: ${response.errorBody()?.string()}")
                    Toast.makeText(this@HistoryActivity, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }

                isLoading = false
            }

            override fun onFailure(call: Call<List<SurveyItem>>, t: Throwable) {
                showLoader(false)
                Log.e("APIResponse", "Error fetching data: ${t.message}", t)
                Toast.makeText(this@HistoryActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        })
    }


    private fun fetchSurveyDataFromServerNearby(username: String, lat: Double? = null, long: Double? = null) {
        isLoading = true
        showLoader(true)

        // Fetch data using the API service
        ApiClient.apiService.fetchSurveyDataNearby(username, lat, long).enqueue(object : Callback<List<SurveyItem>> {
            override fun onResponse(call: Call<List<SurveyItem>>, response: Response<List<SurveyItem>>) {
                showLoader(false)

                if (response.isSuccessful && response.body() != null) {
                    val surveyItems = response.body()!!

                    Log.d("APIResponse", "Deserialized Response: $surveyItems")

                    try {
                        // Map SurveyItems to UploadedItems
                        val newItems = surveyItems.map { surveyItem ->
                            UploadedItem(
                                id = surveyItem.ids ?: emptyList(),
                                imageUrls = surveyItem.imageUrls ?: emptyList(),
                                description = surveyItem.remark ?: "No description available",
                                latitude = surveyItem.latitude ?: "Not available",
                                longitude = surveyItem.longitude ?: "Not available",
                                clickedDate = surveyItem.clickedDate ?: "Not specified",
                                clickedTime = surveyItem.clickedTime ?: "Not specified",
                                accurracy = surveyItem.accurracy ?: "N/A",
                                distance = surveyItem.distance
                            )
                        }

                        allUploadedItems.clear()
                        allUploadedItems.addAll(newItems)
                        loadItemsForPage()
                        updateCardCount(allUploadedItems.size, filteredItems.size)

                    } catch (e: Exception) {
                        Log.e("MappingError", "Error while mapping API response", e)
                        Toast.makeText(this@HistoryActivity, "Error processing data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("APIResponse", "Failed to fetch data: ${response.errorBody()?.string()}")
                    Toast.makeText(this@HistoryActivity, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }

                isLoading = false
            }

            override fun onFailure(call: Call<List<SurveyItem>>, t: Throwable) {
                showLoader(false)
                Log.e("APIResponse", "Error fetching data: ${t.message}", t)
                Toast.makeText(this@HistoryActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        })
    }


    private fun loadItemsForPage() {
        noDataTextView.visibility = TextView.GONE
        filteredItems.clear()
        val endIndex = minOf(itemsPerPage, allUploadedItems.size)
        filteredItems.addAll(allUploadedItems.subList(0, endIndex))
        updateCardCount(allUploadedItems.size, filteredItems.size)
        adapter.notifyDataSetChanged()
    }

    private fun updateCardCount(totalCount: Int, displayedCount: Int) {
        cardCountTextView.text = "Showing $displayedCount out of $totalCount results"
    }
}
