package com.abakada.batibooktwo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
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
            performSearch(searchQuery)
        }
        
        // Back button functionality
        backButton.setOnClickListener {
            finish()
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
            return
        }
        
        // TODO: Implement actual search functionality
        // For now, show a message
        noResultsText.text = "Search results for: \"$query\"\n\nThis feature will be implemented in the next update."
        noResultsText.visibility = TextView.VISIBLE
        resultsRecyclerView.visibility = RecyclerView.GONE
    }
}
