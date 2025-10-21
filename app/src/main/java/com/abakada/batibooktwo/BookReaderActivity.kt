package com.abakada.batibooktwo

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
            // Toast notification for back navigation - BookReaderActivity
            val context = getApplicationContext()
            val txt = "Returning to library"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
        
        // Read button functionality
        readButton.setOnClickListener {
            // Toast notification for reading functionality - BookReaderActivity
            val context = getApplicationContext()
            val txt = "Opening book: $bookTitle"
            val time = Toast.LENGTH_LONG
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            // TODO: Implement actual book reading functionality
        }
        
        // Download button functionality
        downloadButton.setOnClickListener {
            // Toast notification for download functionality - BookReaderActivity
            val context = getApplicationContext()
            val txt = "Downloading $bookTitle..."
            val time = Toast.LENGTH_LONG
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            // TODO: Implement actual download functionality
        }
        
        // Favorite button functionality
        favoriteButton.setOnClickListener {
            // Toast notification for favorite functionality - BookReaderActivity
            val context = getApplicationContext()
            val txt = "Added $bookTitle to favorites"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            // TODO: Implement actual favorite functionality
        }
        
        // Share button functionality
        shareButton.setOnClickListener {
            // Toast notification for share functionality - BookReaderActivity
            val context = getApplicationContext()
            val txt = "Sharing $bookTitle"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            // TODO: Implement actual share functionality
        }
    }
}
