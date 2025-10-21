package com.abakada.batibooktwo

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BookReaderActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_reader)
        
        // Get book data from intent
        val bookTitle = intent.getStringExtra("book_title") ?: "Unknown Book"
        val bookAuthor = intent.getStringExtra("book_author") ?: "Unknown Author"
        
        // Initialize UI elements
        val backButton = findViewById<ImageView>(R.id.back_button)
        val bookTitleText = findViewById<TextView>(R.id.book_title)
        val bookAuthorText = findViewById<TextView>(R.id.book_author)
        val readButton = findViewById<Button>(R.id.read_button)
        val downloadButton = findViewById<Button>(R.id.download_button)
        val favoriteButton = findViewById<Button>(R.id.favorite_button)
        val shareButton = findViewById<Button>(R.id.share_button)
        
        // Set book information
        bookTitleText.text = bookTitle
        bookAuthorText.text = bookAuthor
        
        // Back button functionality
        backButton.setOnClickListener {
            finish()
        }
        
        // Read button functionality
        readButton.setOnClickListener {
            // TODO: Implement actual book reading functionality
        }
        
        // Download button functionality
        downloadButton.setOnClickListener {
            // TODO: Implement actual download functionality
        }
        
        // Favorite button functionality
        favoriteButton.setOnClickListener {
            // TODO: Implement actual favorite functionality
        }
        
        // Share button functionality
        shareButton.setOnClickListener {
            // TODO: Implement actual share functionality
        }
    }
}
