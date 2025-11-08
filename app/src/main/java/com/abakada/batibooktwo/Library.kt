package com.abakada.batibooktwo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import android.graphics.drawable.GradientDrawable
import android.content.res.Resources
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Button
import android.widget.EditText

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Library.newInstance] factory method to
 * create an instance of this fragment.
 */
class Library : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    
    private lateinit var bookGrid: GridLayout
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBooks = view.findViewById<CardView>(R.id.btn_books)
        val btnFavorites = view.findViewById<CardView>(R.id.btn_favorites)
        val btnDownloads = view.findViewById<CardView>(R.id.btn_downloads)
        val ivSearch = view.findViewById<ImageView>(R.id.ivSearch)
        bookGrid = view.findViewById<GridLayout>(R.id.bookGrid)
        
        // Initialize Library Service
        val libraryService = LibraryService.getInstance()
        val authService = FirebaseAuthService.getInstance()
        
        // Check if user is authenticated
        if (!authService.isUserAuthenticated()) {
            // Show message to sign in
            bookGrid.removeAllViews()
            val messageView = TextView(requireContext()).apply {
                text = "Please sign in to view your library"
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                setTextColor(resources.getColor(R.color.black, null))
                layoutParams = GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(0, 2)
                    rowSpec = GridLayout.spec(0)
                }
            }
            bookGrid.addView(messageView)
        } else {
            // Load books from Firestore - default to "reading" section
            currentFilter = "reading"
            loadBooksFromFirestore("reading")
        }

        // Initial state: Reading selected (changed from Books)
        updateTopNavState(
            selected = TopNav.READING,
            btnBooks = btnBooks,
            btnFavorites = btnFavorites,
            btnDownloads = btnDownloads
        )

        btnBooks.setOnClickListener {
            updateTopNavState(TopNav.READING, btnBooks, btnFavorites, btnDownloads)
            if (authService.isUserAuthenticated()) {
                currentFilter = "reading"
                loadBooksFromFirestore("reading")
            }
        }

        btnFavorites.setOnClickListener {
            updateTopNavState(TopNav.FAVORITES, btnBooks, btnFavorites, btnDownloads)
            if (authService.isUserAuthenticated()) {
                currentFilter = "favorites"
                loadBooksFromFirestore("favorites")
            }
        }

        btnDownloads.setOnClickListener {
            updateTopNavState(TopNav.DOWNLOADS, btnBooks, btnFavorites, btnDownloads)
            if (authService.isUserAuthenticated()) {
                currentFilter = "downloads"
                loadBooksFromFirestore("downloads")
            }
        }

        ivSearch.setOnClickListener {
            showSearchDialog()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh library when fragment resumes (e.g., after adding a book)
        val authService = FirebaseAuthService.getInstance()
        if (authService.isUserAuthenticated() && ::bookGrid.isInitialized) {
            loadBooksFromFirestore(currentFilter)
        }
    }
    
    /**
     * Load books from Firestore
     * Filter types: "reading" (books with progress), "favorites", "downloads"
     */
    private fun loadBooksFromFirestore(filter: String) {
        bookGrid.removeAllViews()
        
        // Show loading indicator
        val loadingView = TextView(requireContext()).apply {
            text = "Loading..."
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setTextColor(resources.getColor(R.color.black, null))
            layoutParams = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(0, 2)
                rowSpec = GridLayout.spec(0)
            }
        }
        bookGrid.addView(loadingView)
        
        val libraryService = LibraryService.getInstance()
        val progressService = ProgressTrackingService.getInstance()
        
        if (filter == "reading") {
            // Load books with reading progress
            progressService.getAllProgress(object : ProgressTrackingService.ProgressListCallback {
                override fun onSuccess(progressList: List<ProgressTrackingService.ReadingProgress>) {
                    if (progressList.isEmpty()) {
                        showEmptyMessage("No reading progress yet. Start reading a story!")
                        return
                    }
                    
                    // Get book IDs from progress
                    val bookIds = progressList.map { it.storyId }.toSet()
                    
                    // Load all library books and filter by those with progress
                    libraryService.getLibraryBooks("all", object : LibraryService.LibraryListCallback {
                        override fun onSuccess(books: List<LibraryService.LibraryBook>) {
                            // Filter books that have reading progress
                            val readingBooks = books.filter { book ->
                                // Match by bookId or storyId pattern
                                bookIds.contains(book.bookId) || 
                                bookIds.any { it.contains(book.bookId) || book.bookId.contains(it) }
                            }
                            
                            if (readingBooks.isEmpty()) {
                                // Try to create books from progress data
                                val progressBooks = progressList.map { progress ->
                                    LibraryBookData(
                                        title = progress.storyTitle,
                                        imageRes = R.drawable.book1,
                                        titleRes = R.string.dog_and_cat,
                                        status = BookStatus.DOWNLOADED,
                                        isFavorite = false
                                    )
                                }
                                populateGrid(bookGrid, progressBooks)
                            } else {
                                val libraryBooks = readingBooks.map { book ->
                                    LibraryBookData(
                                        title = book.bookTitle,
                                        imageRes = book.bookImageRes.toIntOrNull() ?: R.drawable.book1,
                                        titleRes = book.bookTitleRes.toIntOrNull() ?: R.string.dog_and_cat,
                                        status = if (book.isDownloaded) BookStatus.DOWNLOADED else BookStatus.DOWNLOAD,
                                        isFavorite = book.isFavorite
                                    )
                                }
                                populateGrid(bookGrid, libraryBooks)
                            }
                        }
                        
                        override fun onError(error: String) {
                            // If library service fails, try to show progress books directly
                            val progressBooks = progressList.map { progress ->
                                LibraryBookData(
                                    title = progress.storyTitle,
                                    imageRes = R.drawable.book1,
                                    titleRes = R.string.dog_and_cat,
                                    status = BookStatus.DOWNLOADED,
                                    isFavorite = false
                                )
                            }
                            if (progressBooks.isNotEmpty()) {
                                populateGrid(bookGrid, progressBooks)
                            } else {
                                showErrorMessage("Error loading reading progress: $error")
                            }
                        }
                    })
                }
                
                override fun onError(error: String) {
                    showErrorMessage("Error loading reading progress: $error")
                }
            })
        } else {
            // Load favorites or downloads
            libraryService.getLibraryBooks(filter, object : LibraryService.LibraryListCallback {
                override fun onSuccess(books: List<LibraryService.LibraryBook>) {
                    bookGrid.removeAllViews()
                    
                    if (books.isEmpty()) {
                        showEmptyMessage(when (filter) {
                            "favorites" -> "No favorite books yet"
                            "downloads" -> "No downloaded books yet"
                            else -> "Your library is empty"
                        })
                    } else {
                        // Convert Firestore books to LibraryBookData
                        val libraryBooks = books.map { book ->
                            LibraryBookData(
                                title = book.bookTitle,
                                imageRes = book.bookImageRes.toIntOrNull() ?: R.drawable.book1,
                                titleRes = book.bookTitleRes.toIntOrNull() ?: R.string.dog_and_cat,
                                status = if (book.isDownloaded) BookStatus.DOWNLOADED else BookStatus.DOWNLOAD,
                                isFavorite = book.isFavorite
                            )
                        }
                        populateGrid(bookGrid, libraryBooks)
                    }
                }
                
                override fun onError(error: String) {
                    showErrorMessage("Error loading library: $error")
                }
            })
        }
    }
    
    private fun showEmptyMessage(message: String) {
        bookGrid.removeAllViews()
        val emptyView = TextView(requireContext()).apply {
            text = message
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setTextColor(resources.getColor(R.color.black, null))
            layoutParams = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(0, 2)
                rowSpec = GridLayout.spec(0)
            }
        }
        bookGrid.addView(emptyView)
    }
    
    private fun showErrorMessage(message: String) {
        bookGrid.removeAllViews()
        val errorView = TextView(requireContext()).apply {
            text = message
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.RED)
            layoutParams = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(0, 2)
                rowSpec = GridLayout.spec(0)
            }
        }
        bookGrid.addView(errorView)
    }

    private enum class TopNav { READING, FAVORITES, DOWNLOADS }

    private fun updateTopNavState(
        selected: TopNav,
        btnBooks: CardView,
        btnFavorites: CardView,
        btnDownloads: CardView
    ) {
        val selectedColor = resources.getColor(R.color.golden_sand, null)
        val unselectedColor = resources.getColor(R.color.filter_color, null)

        // Update all buttons to unselected state first
        btnBooks.setCardBackgroundColor(if (selected == TopNav.READING) selectedColor else unselectedColor)
        btnFavorites.setCardBackgroundColor(if (selected == TopNav.FAVORITES) selectedColor else unselectedColor)
        btnDownloads.setCardBackgroundColor(if (selected == TopNav.DOWNLOADS) selectedColor else unselectedColor)

        // Reset elevation for all
        btnBooks.cardElevation = 0f
        btnFavorites.cardElevation = 0f
        btnDownloads.cardElevation = 0f
    }


    private fun populateGrid(grid: GridLayout, books: List<LibraryBookData>) {
        books.forEachIndexed { index, book ->
            val bookView = LayoutInflater.from(requireContext()).inflate(R.layout.item_library_book, grid, false)
            val imageView = bookView.findViewById<ImageView>(R.id.book_image)
            val titleView = bookView.findViewById<TextView>(R.id.book_title)
            val statusIcon = bookView.findViewById<ImageView>(R.id.book_status_icon)
            
            imageView.setImageResource(book.imageRes)
            titleView.text = getString(book.titleRes)
            
            // Set status icon: favorite takes precedence for a heart icon
            if (book.isFavorite) {
                statusIcon.setImageResource(R.drawable.heart)
            } else {
                when (book.status) {
                    BookStatus.DOWNLOAD -> statusIcon.setImageResource(R.drawable.download2)
                    BookStatus.DOWNLOADED -> statusIcon.setImageResource(R.drawable.check)
                }
            }
            
            val layoutParams = GridLayout.LayoutParams()
            layoutParams.columnSpec = GridLayout.spec(index % 2, 1f)
            // add vertical spacing between rows
            layoutParams.rowSpec = GridLayout.spec(index / 2, 1f)
            // horizontal spacing handled by item margin; keep row spacing only
            layoutParams.setMargins(0, if (index / 2 > 0) (12f.dp).toInt() else 0, 0, 0)
            bookView.layoutParams = layoutParams
            
            // Add click listener for book
            bookView.setOnClickListener {
                showLibraryBookDetails(book)
            }
            
            grid.addView(bookView)
        }
    }

    data class LibraryBookData(
        val title: String,
        val imageRes: Int,
        val titleRes: Int,
        val status: BookStatus,
        val isFavorite: Boolean = false
    )

    enum class BookStatus {
        DOWNLOAD, DOWNLOADED
    }

    private val Float.dp: Float
        get() = this * Resources.getSystem().displayMetrics.density

    private fun showSearchDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.search_dialog, null)
        val searchEditText = dialogView.findViewById<EditText>(R.id.search_edit_text)
        val searchButton = dialogView.findViewById<Button>(R.id.search_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                // Intent to open SearchActivity
                val intent = Intent(requireContext(), SearchActivity::class.java).apply {
                    putExtra("search_query", query)
                    putExtra("search_source", "library")
                }
                startActivity(intent)
                dialog.dismiss()
            } else {
                // Handle empty search
            }
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun performLibrarySearch(query: String) {
        // TODO: Implement actual search functionality for library books
    }
    
    private fun showLibraryBookDetails(book: LibraryBookData) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.library_book_details_dialog, null)
        val bookImage = dialogView.findViewById<ImageView>(R.id.dialog_book_image)
        val bookTitle = dialogView.findViewById<TextView>(R.id.dialog_book_title)
        val bookStatus = dialogView.findViewById<TextView>(R.id.dialog_book_status)
        val downloadButton = dialogView.findViewById<Button>(R.id.download_button)
        val favoriteButton = dialogView.findViewById<Button>(R.id.favorite_button)
        val readButton = dialogView.findViewById<Button>(R.id.read_button)
        val closeButton = dialogView.findViewById<ImageView>(R.id.close_button)
        
        bookImage.setImageResource(book.imageRes)
        bookTitle.text = getString(book.titleRes)
        bookStatus.text = if (book.isFavorite) "Favorite" else when (book.status) {
            BookStatus.DOWNLOAD -> "Available for Download"
            BookStatus.DOWNLOADED -> "Downloaded"
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Initialize Library Service
        val libraryService = LibraryService.getInstance()
        val authService = FirebaseAuthService.getInstance()
        
        if (!authService.isUserAuthenticated()) {
            downloadButton.setOnClickListener {
                android.widget.Toast.makeText(requireContext(), "Please sign in to download books", android.widget.Toast.LENGTH_SHORT).show()
            }
            favoriteButton.setOnClickListener {
                android.widget.Toast.makeText(requireContext(), "Please sign in to manage favorites", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            // Update button states
            downloadButton.text = if (book.status == BookStatus.DOWNLOADED) "Downloaded" else "Download"
            favoriteButton.text = if (book.isFavorite) "Remove from Favorites" else "Add to Favorites"
            
            val bookId = "book_${book.titleRes}"
            
            downloadButton.setOnClickListener {
                if (book.status == BookStatus.DOWNLOAD) {
                    libraryService.downloadBook(
                        bookId = bookId,
                        bookTitle = getString(book.titleRes),
                        bookAuthor = "Unknown Author",
                        bookImageRes = book.imageRes,
                        bookTitleRes = book.titleRes,
                        object : LibraryService.LibraryCallback {
                            override fun onSuccess() {
                                android.widget.Toast.makeText(requireContext(), "Book downloaded successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                downloadButton.text = "Downloaded"
                                downloadButton.isEnabled = false
                                dialog.dismiss()
                                // Refresh library
                                loadBooksFromFirestore(currentFilter)
                            }
                            override fun onError(error: String) {
                                android.widget.Toast.makeText(requireContext(), "Download failed: $error", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    android.widget.Toast.makeText(requireContext(), "Book already downloaded", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            favoriteButton.setOnClickListener {
                if (book.isFavorite) {
                    // Remove from favorites
                    libraryService.removeFromFavorites(bookId, object : LibraryService.LibraryCallback {
                        override fun onSuccess() {
                            android.widget.Toast.makeText(requireContext(), "Removed from favorites", android.widget.Toast.LENGTH_SHORT).show()
                            favoriteButton.text = "Add to Favorites"
                            dialog.dismiss()
                            // Refresh library
                            loadBooksFromFirestore(currentFilter)
                        }
                        override fun onError(error: String) {
                            android.widget.Toast.makeText(requireContext(), "Failed to remove: $error", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    // Add to favorites
                    libraryService.addToFavorites(
                        bookId = bookId,
                        bookTitle = getString(book.titleRes),
                        bookAuthor = "Unknown Author",
                        bookImageRes = book.imageRes,
                        bookTitleRes = book.titleRes,
                        object : LibraryService.LibraryCallback {
                            override fun onSuccess() {
                                android.widget.Toast.makeText(requireContext(), "Added to favorites!", android.widget.Toast.LENGTH_SHORT).show()
                                favoriteButton.text = "Remove from Favorites"
                            }
                            override fun onError(error: String) {
                                android.widget.Toast.makeText(requireContext(), "Failed to add: $error", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
        
        readButton.setOnClickListener {
            // Open StoryReaderActivity for page-by-page reading
            val intent = Intent(requireContext(), StoryReaderActivity::class.java).apply {
                putExtra("story_title", getString(book.titleRes))
                putExtra("story_id", "book_${book.titleRes}")
                putExtra("story_content", "") // Empty will trigger sample story
            }
            startActivity(intent)
            dialog.dismiss()
        }
        
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Library.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Library().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}