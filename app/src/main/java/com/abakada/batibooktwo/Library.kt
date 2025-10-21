package com.abakada.batibooktwo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
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
        val bookGrid = view.findViewById<GridLayout>(R.id.bookGrid)

        // Initialize with sample books
        initializeBooks(bookGrid)

        // Initial state: Books selected
        updateTopNavState(
            selected = TopNav.BOOKS,
            btnBooks = btnBooks,
            btnFavorites = btnFavorites,
            btnDownloads = btnDownloads
        )

        btnBooks.setOnClickListener {
            updateTopNavState(TopNav.BOOKS, btnBooks, btnFavorites, btnDownloads)
            filterBooks(bookGrid, TopNav.BOOKS)
        }

        btnFavorites.setOnClickListener {
            updateTopNavState(TopNav.FAVORITES, btnBooks, btnFavorites, btnDownloads)
            filterBooks(bookGrid, TopNav.FAVORITES)
        }

        btnDownloads.setOnClickListener {
            updateTopNavState(TopNav.DOWNLOADS, btnBooks, btnFavorites, btnDownloads)
            filterBooks(bookGrid, TopNav.DOWNLOADS)
        }

        ivSearch.setOnClickListener {
            showSearchDialog()
        }
    }

    private enum class TopNav { BOOKS, FAVORITES, DOWNLOADS }

    private fun updateTopNavState(
        selected: TopNav,
        btnBooks: CardView,
        btnFavorites: CardView,
        btnDownloads: CardView
    ) {
        val selectedColor = 0xFFE3AC6C.toInt() // #E3AC6C
        val unselectedColor = 0xFFFDFDFC.toInt() // #FDFDFC

        // Update all buttons to unselected state first
        btnBooks.setCardBackgroundColor(if (selected == TopNav.BOOKS) selectedColor else unselectedColor)
        btnFavorites.setCardBackgroundColor(if (selected == TopNav.FAVORITES) selectedColor else unselectedColor)
        btnDownloads.setCardBackgroundColor(if (selected == TopNav.DOWNLOADS) selectedColor else unselectedColor)

        // Reset elevation for all
        btnBooks.cardElevation = 0f
        btnFavorites.cardElevation = 0f
        btnDownloads.cardElevation = 0f
    }

    private fun initializeBooks(grid: GridLayout) {
        val books = listOf(
            // EXACTLY as in provided design
            LibraryBookData("Dog and Cat", R.drawable.book1, R.string.dog_and_cat, BookStatus.DOWNLOAD, isFavorite = false),
            LibraryBookData("Lito's Umbrella", R.drawable.book3, R.string.lito_s_umbrella, BookStatus.DOWNLOAD, isFavorite = false),
            LibraryBookData("Mila and the Butterfly", R.drawable.book4, R.string.mila_and_the_butterfly, BookStatus.DOWNLOADED, isFavorite = false),
            LibraryBookData("Ana and the Ball", R.drawable.book2, R.string.ana_and_the_ball, BookStatus.DOWNLOAD, isFavorite = true)
        )
        
        populateGrid(grid, books)
    }

    private fun filterBooks(grid: GridLayout, filter: TopNav) {
        // Clear existing views
        grid.removeAllViews()
        
        val allBooks = listOf(
            LibraryBookData("Dog and Cat", R.drawable.book1, R.string.dog_and_cat, BookStatus.DOWNLOAD, isFavorite = false),
            LibraryBookData("Lito's Umbrella", R.drawable.book3, R.string.lito_s_umbrella, BookStatus.DOWNLOAD, isFavorite = false),
            LibraryBookData("Mila and the Butterfly", R.drawable.book4, R.string.mila_and_the_butterfly, BookStatus.DOWNLOADED, isFavorite = false),
            LibraryBookData("Ana and the Ball", R.drawable.book2, R.string.ana_and_the_ball, BookStatus.DOWNLOAD, isFavorite = true)
        )
        
        val filteredBooks = when (filter) {
            TopNav.BOOKS -> allBooks
            TopNav.FAVORITES -> allBooks.filter { it.isFavorite }
            TopNav.DOWNLOADS -> allBooks.filter { it.status == BookStatus.DOWNLOADED }
        }
        
        populateGrid(grid, filteredBooks)
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
                // Toast notification for library search - Library Fragment
                val context = requireContext().applicationContext
                val txt = "Searching library for: $query"
                val time = Toast.LENGTH_SHORT
                val toast = Toast.makeText(context, txt, time)
                toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
                toast.show()
                
                // Intent to open SearchActivity
                val intent = Intent(requireContext(), SearchActivity::class.java).apply {
                    putExtra("search_query", query)
                    putExtra("search_source", "library")
                }
                startActivity(intent)
                dialog.dismiss()
            } else {
                // Toast notification for empty search - Library Fragment
                val context = requireContext().applicationContext
                val txt = "Please enter a search term"
                val time = Toast.LENGTH_SHORT
                val toast = Toast.makeText(context, txt, time)
                toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
                toast.show()
            }
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun performLibrarySearch(query: String) {
        Toast.makeText(requireContext(), "Searching for: $query", Toast.LENGTH_SHORT).show()
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
        
        downloadButton.setOnClickListener {
            if (book.status == BookStatus.DOWNLOAD) {
                Toast.makeText(requireContext(), "Downloading ${getString(book.titleRes)}...", Toast.LENGTH_SHORT).show()
                // TODO: Implement actual download functionality
            } else {
                Toast.makeText(requireContext(), "Book already downloaded", Toast.LENGTH_SHORT).show()
            }
        }
        
        favoriteButton.setOnClickListener {
            val message = if (book.isFavorite) "Removed from favorites" else "Added to favorites"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            // TODO: Implement actual favorite toggle functionality
        }
        
        readButton.setOnClickListener {
            // Toast notification for library book reading - Library Fragment
            val context = requireContext().applicationContext
            val txt = "Opening ${getString(book.titleRes)} for reading"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            
            // Intent to open BookReaderActivity
            val intent = Intent(requireContext(), BookReaderActivity::class.java).apply {
                putExtra("book_title", getString(book.titleRes))
                putExtra("book_author", "Unknown Author")
                putExtra("book_status", book.status.name)
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