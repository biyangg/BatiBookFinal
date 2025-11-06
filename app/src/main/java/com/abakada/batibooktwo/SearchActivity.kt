package com.abakada.batibooktwo

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * SearchActivity - Simple Search Functionality
 * 
 * This activity provides search functionality for local stories in the app.
 * Users can search for stories by title or keywords.
 */
class SearchActivity : AppCompatActivity() {
    
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var backButton: ImageView
    private lateinit var noResultsText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        // Initialize UI elements
        searchEditText = findViewById(R.id.search_edit_text)
        searchButton = findViewById(R.id.search_button)
        backButton = findViewById(R.id.back_button)
        noResultsText = findViewById(R.id.no_results_text)
        
        // Get search query from intent (passed from MainActivity)
        val searchQuery = intent.getStringExtra("search_query")
        
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
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
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
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show()
            return
        }
        
        // TODO: Implement local story search functionality
        noResultsText.text = "Search results for: \"$query\"\n\nThis feature will search through local stories."
        noResultsText.visibility = TextView.VISIBLE
    }
}
