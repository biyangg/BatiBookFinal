package com.abakada.batibooktwo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchActivity : AppCompatActivity() {
    
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var backButton: ImageView
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var noResultsText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        // Initialize UI elements
        searchEditText = findViewById(R.id.search_edit_text)
        searchButton = findViewById(R.id.search_button)
        backButton = findViewById(R.id.back_button)
        resultsRecyclerView = findViewById(R.id.results_recycler_view)
        noResultsText = findViewById(R.id.no_results_text)
        
        // Set up RecyclerView
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Get search query from intent
        val searchQuery = intent.getStringExtra("search_query")
        val searchSource = intent.getStringExtra("search_source")
        
        if (!searchQuery.isNullOrEmpty()) {
            searchEditText.setText(searchQuery)
            // Toast notification for search from intent - SearchActivity
            val context = getApplicationContext()
            val txt = "Searching from $searchSource: $searchQuery"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            performSearch(searchQuery)
        }
        
        // Back button functionality
        backButton.setOnClickListener {
            finish()
            // Toast notification for back navigation - SearchActivity
            val context = getApplicationContext()
            val txt = "Returning to previous screen"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
        
        // Search button functionality
        searchButton.setOnClickListener {
            performSearch()
        }
        
        // Search on Enter key press
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
        
        // Focus on search field when activity starts
        searchEditText.requestFocus()
    }
    
    private fun performSearch() {
        val query = searchEditText.text.toString().trim()
        performSearch(query)
    }
    
    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            // Toast notification for empty search - SearchActivity
            val context = getApplicationContext()
            val txt = "Please enter a search term"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            return
        }
        
        // Toast notification for search execution - SearchActivity
        val context = getApplicationContext()
        val txt = "Searching for: $query"
        val time = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, txt, time)
        toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
        toast.show()
        
        // TODO: Implement actual search functionality
        // For now, show a message
        noResultsText.text = "Search results for: \"$query\"\n\nThis feature will be implemented in the next update."
        noResultsText.visibility = TextView.VISIBLE
        resultsRecyclerView.visibility = RecyclerView.GONE
    }
}
