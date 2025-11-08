package com.abakada.batibooktwo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.abakada.batibooktwo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isBottomSheetVisible = false
    private var bottomSheetView: View? = null
    private var isFilterBottomSheetVisible = false
    private var filterBottomSheetView: View? = null
    private val selectedReadingLevels = mutableSetOf<Int>()
    private val selectedCategories = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before setting content view
        val savedTheme = ThemeManager.getCurrentTheme(this)
        ThemeManager.applyTheme(savedTheme)
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default tab = Home
        binding.bottomNavigationView.selectedItemId = R.id.home
        binding.frameLayout.visibility = View.GONE
        findViewById<View>(R.id.home_content).visibility = View.VISIBLE
        
        // Initialize home content
        initializeHomeContent()

        // Initialize search functionality
        initializeSearchFunctionality()
        
        // Initialize see more buttons
        initializeSeeMoreButtons()
        
        // Toast notification for app startup - MainActivity
        val context = applicationContext
        val txt = "Welcome to BatiBook! Start your reading journey"
        val time = Toast.LENGTH_LONG
        val toast = Toast.makeText(context, txt, time)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
        toast.show()

        // Bottom navigation listener
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.library -> {
                    replaceFragment(Library())
                    updateBottomNavigationVisibility()
                    true
                }
                R.id.profile -> {
                    replaceFragment(Profile())
                    updateBottomNavigationVisibility()
                    true
                }
                R.id.home -> {
                    showHome()
                    updateBottomNavigationVisibility()
                    true
                }
                else -> false
            }
        }
        
        // Check initial visibility
        updateBottomNavigationVisibility()

        // Hamburger button click → show bottom sheet
        binding.menuIcon.setOnClickListener {
            if (!isBottomSheetVisible) {
                showBottomSheet()
                // Toast notification for menu opening - MainActivity
                val context = applicationContext
                val txt = "Opening Settings Menu"
                val time = Toast.LENGTH_SHORT
                val toast = Toast.makeText(context, txt, time)
                toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
                toast.show()
            }
        }

        // Filter button click → show filter bottom sheet
        val filterButton = findViewById<View>(R.id.filter_button)
        filterButton?.setOnClickListener {
            if (!isFilterBottomSheetVisible) {
                showFilterBottomSheet()
                // Toast notification for filter opening - MainActivity
                val context = applicationContext
                val txt = "Opening Book Filters"
                val time = Toast.LENGTH_SHORT
                val toast = Toast.makeText(context, txt, time)
                toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
                toast.show()
            }
        }

        // Tap outside bottom sheet to close
        binding.bottomSheetContainer.setOnClickListener {
            if (isBottomSheetVisible) hideBottomSheet()
            if (isFilterBottomSheetVisible) hideFilterBottomSheet()
        }

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isBottomSheetVisible) {
                    hideBottomSheet()
                } else if (isFilterBottomSheetVisible) {
                    hideFilterBottomSheet()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        binding.frameLayout.visibility = View.VISIBLE
        findViewById<View>(R.id.home_content).visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
        
        // Update navigation visibility after fragment transaction
        binding.root.post {
            updateBottomNavigationVisibility()
        }
    }
    
    fun updateBottomNavigationVisibility() {
        // Check if Profile fragment is showing login layout
        val fragmentManager = supportFragmentManager
        val profileFragment = fragmentManager.findFragmentById(R.id.frame_layout)
        
        if (profileFragment is Profile) {
            val authService = FirebaseAuthService.getInstance()
            // Hide navigation bar if user is not authenticated (showing login)
            binding.bottomNavigationView.visibility = 
                if (authService.isUserAuthenticated()) View.VISIBLE else View.GONE
        } else {
            // Show navigation bar for other fragments
            binding.bottomNavigationView.visibility = View.VISIBLE
        }
    }

    private fun showHome() {
        supportFragmentManager.fragments.forEach {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
        binding.frameLayout.visibility = View.GONE
        findViewById<View>(R.id.home_content).visibility = View.VISIBLE
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun showBottomSheet() {
        val inflater = LayoutInflater.from(this)
        bottomSheetView = inflater.inflate(R.layout.bottom_sheet_layout, binding.bottomSheetContainer, false)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.BOTTOM
        bottomSheetView?.layoutParams = params

        bottomSheetView?.setOnClickListener { /* prevent close on click */ }

        binding.bottomSheetContainer.addView(bottomSheetView)
        binding.bottomSheetContainer.visibility = View.VISIBLE

        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in)
        bottomSheetView?.startAnimation(slideIn)
        isBottomSheetVisible = true

        // ✅ LANGUAGE SPINNER SETUP
        val spinner = bottomSheetView?.findViewById<Spinner>(R.id.spinner)
        if (spinner != null) {
            val languages = listOf("Select Language", "English")

            val adapter = object : ArrayAdapter<String>(this, R.layout.spinner_item, languages) {
                override fun isEnabled(position: Int): Boolean = position != 0

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent) as TextView
                    if (position == 0) view.setTextColor(android.graphics.Color.GRAY)
                    return view
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent) as TextView
                    view.setTextColor(
                        if (position == 0) android.graphics.Color.GRAY else android.graphics.Color.WHITE
                    )
                    return view
                }
            }
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            spinner.adapter = adapter

            // ✅ Load saved language
            val sharedPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
            val savedLang = sharedPrefs.getString("language", null)

            // Set correct spinner position based on saved language
            val selectedPosition = when (savedLang) {
                "English" -> 1
                else -> 0 // default = Select Language
            }
            spinner.setSelection(selectedPosition, false)

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) return // ignore "Select Language"

                    val selected = languages[position]
                    if (selected != savedLang) {
                        sharedPrefs.edit { putString("language", selected) }
                        val context = applicationContext
                        val txt = "Language set to $selected"
                        val time = Toast.LENGTH_SHORT
                        val toast = Toast.makeText(context, txt, time)
                        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
                        toast.show()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // ✅ DARK MODE SWITCH SETUP
        val darkModeSwitch = bottomSheetView?.findViewById<Switch>(R.id.switcher)
        if (darkModeSwitch != null) {
            val sharedPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
            val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
            darkModeSwitch.isChecked = isDarkMode

            darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit { putBoolean("dark_mode", isChecked) }
                // Toast notification for dark mode toggle - MainActivity
                val context = applicationContext
                val txt = if (isChecked) "Dark mode enabled" else "Dark mode disabled"
                val time = Toast.LENGTH_SHORT
                val toast = Toast.makeText(context, txt, time)
                toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
                toast.show()
            }
        }

        // About Batibook navigation
        val aboutLayout = bottomSheetView?.findViewById<LinearLayout>(R.id.layoutAppInfo)
        aboutLayout?.setOnClickListener {
            hideBottomSheet()
            
            // Toast notification for about navigation - MainActivity
            val context = applicationContext
            val txt = "Opening About BatiBook"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()

            // Intent to navigate to AboutActivity
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        // Cancel button
        val cancelButton = bottomSheetView?.findViewById<View>(R.id.cancelButton)
        cancelButton?.setOnClickListener { hideBottomSheet() }
    }


    private fun hideBottomSheet() {
        bottomSheetView?.let { view ->
            val slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out)
            slideOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    binding.bottomSheetContainer.removeView(view)
                    binding.bottomSheetContainer.visibility = View.GONE
                    isBottomSheetVisible = false
                }
            })
            view.startAnimation(slideOut)
        }
    }

    private fun initializeHomeContent() {
        // Initialize Featured Stories
        initializeFeaturedStories()
        
        // Initialize Recommended Books
        initializeRecommendedBooks()
        
        // Initialize Categories
        initializeCategories()
    }

    private fun initializeFeaturedStories() {
        val container = findViewById<LinearLayout>(R.id.featured_stories_container)
        val books = listOf(
            BookData("Dog and Cat", R.drawable.book1, R.string.dog_and_cat),
            BookData("Ana and the Ball", R.drawable.book2, R.string.ana_and_the_ball),
            BookData("Lito's Umbrella", R.drawable.book3, R.string.lito_s_umbrella),
            BookData("Mila and the Butterfly", R.drawable.book4, R.string.mila_and_the_butterfly)
        )
        
        books.forEach { book ->
            val bookView = LayoutInflater.from(this).inflate(R.layout.item_book, container, false)
            val imageView = bookView.findViewById<ImageView>(R.id.book_image)
            val titleView = bookView.findViewById<TextView>(R.id.book_title)
            
            imageView.setImageResource(book.imageRes)
            titleView.text = getString(book.titleRes)
            
            // Add click listener for book
            bookView.setOnClickListener {
                showBookDetails(book)
            }
            
            container.addView(bookView)
        }
    }

    private fun initializeRecommendedBooks() {
        val container = findViewById<LinearLayout>(R.id.recommended_container)
        val books = listOf(
            BookData("Toto and His Friends in Forest", R.drawable.book5, R.string.toto_and_his_friends_in_forest),
            BookData("Bituin and the Little Fish", R.drawable.book6, R.string.bituin_and_the_little_fish)
        )
        
        books.forEach { book ->
            val bookView = LayoutInflater.from(this).inflate(R.layout.item_book, container, false)
            val imageView = bookView.findViewById<ImageView>(R.id.book_image)
            val titleView = bookView.findViewById<TextView>(R.id.book_title)
            
            imageView.setImageResource(book.imageRes)
            titleView.text = getString(book.titleRes)
            
            // Add click listener for book
            bookView.setOnClickListener {
                showBookDetails(book)
            }
            
            container.addView(bookView)
        }
    }

    private fun initializeCategories() {
        val grid = findViewById<GridLayout>(R.id.categories_grid)
        val categories = listOf(
            CategoryData("Folktales", R.drawable.cat1, R.string.folktales),
            CategoryData("Legends and Myths", R.drawable.cat2, R.string.legends_and_myths),
            CategoryData("Animal Tales", R.drawable.cat3, R.string.animal_tales),
            CategoryData("Nature and Environment", R.drawable.cat4, R.string.nature_and_environment),
            CategoryData("Family and Friends", R.drawable.cat5, R.string.family_and_friends),
            CategoryData("Fantasy and Adventure", R.drawable.cat6, R.string.fantasy_and_adventure),
            CategoryData("Values and Morals", R.drawable.cat7, R.string.values_and_morals),
            CategoryData("Heroes and Inspirations", R.drawable.cat8, R.string.heroes_and_inspirations),
            CategoryData("Everyday Stories", R.drawable.cat9, R.string.everyday_stories)
        )
        
        categories.forEachIndexed { index, category ->
            val categoryView = LayoutInflater.from(this).inflate(R.layout.item_category, grid, false)
            val iconView = categoryView.findViewById<ImageView>(R.id.category_icon)
            val titleView = categoryView.findViewById<TextView>(R.id.category_title)
            
            iconView.setImageResource(category.iconRes)
            titleView.text = getString(category.titleRes)
            
            val layoutParams = GridLayout.LayoutParams()
            layoutParams.columnSpec = GridLayout.spec(index % 3, 1f)
            layoutParams.rowSpec = GridLayout.spec(index / 3, 1f)
            categoryView.layoutParams = layoutParams
            
            // Add click listener for category
            categoryView.setOnClickListener {
                showCategoryBooks(category)
            }
            
            grid.addView(categoryView)
        }
    }

    data class BookData(
        val title: String,
        val imageRes: Int,
        val titleRes: Int
    )

    data class CategoryData(
        val title: String,
        val iconRes: Int,
        val titleRes: Int
    )

    private fun showFilterBottomSheet() {
        val inflater = LayoutInflater.from(this)
        filterBottomSheetView = inflater.inflate(R.layout.filter_bottom_sheet, binding.bottomSheetContainer, false)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.BOTTOM
        filterBottomSheetView?.layoutParams = params

        filterBottomSheetView?.setOnClickListener { /* prevent close on click */ }

        binding.bottomSheetContainer.addView(filterBottomSheetView)
        binding.bottomSheetContainer.visibility = View.VISIBLE

        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in)
        filterBottomSheetView?.startAnimation(slideIn)
        isFilterBottomSheetVisible = true

        // Initialize filter UI
        initializeFilterUI()
    }

    private fun hideFilterBottomSheet() {
        filterBottomSheetView?.let { view ->
            val slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out)
            slideOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    binding.bottomSheetContainer.removeView(view)
                    binding.bottomSheetContainer.visibility = View.GONE
                    isFilterBottomSheetVisible = false
                }
            })
            view.startAnimation(slideOut)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initializeFilterUI() {
        val readingLevelGrid = filterBottomSheetView?.findViewById<GridLayout>(R.id.reading_level_grid)
        val categoriesGrid = filterBottomSheetView?.findViewById<GridLayout>(R.id.categories_grid)
        val btnClear = filterBottomSheetView?.findViewById<Button>(R.id.btn_clear)
        val btnShowBooks = filterBottomSheetView?.findViewById<Button>(R.id.btn_show_books)
        val btnClose = filterBottomSheetView?.findViewById<ImageView>(R.id.cancelButton)

        // Update CTA state and label to reflect current selections
        val updateCta: () -> Unit = {
            val totalSelected = selectedReadingLevels.size + selectedCategories.size
            if (btnShowBooks != null) {
                if (totalSelected > 0) {
                    btnShowBooks.isEnabled = true
                    btnShowBooks.isClickable = true
                    btnShowBooks.alpha = 1f
                    btnShowBooks.text = "Show Books (${totalSelected})"
                    btnShowBooks.contentDescription = "Show Books, $totalSelected filters selected"
                } else {
                    btnShowBooks.isEnabled = false
                    btnShowBooks.isClickable = false
                    btnShowBooks.alpha = 0.5f
                    btnShowBooks.text = getString(R.string.show_books)
                    btnShowBooks.contentDescription = getString(R.string.show_books)
                }
            }
        }

        // Initialize reading levels (0-9)
        for (i in 0..9) {
            val levelView = LayoutInflater.from(this).inflate(R.layout.item_reading_level, readingLevelGrid, false)
            val button = levelView.findViewById<Button>(R.id.btn_reading_level)
            button.text = i.toString()
            button.gravity = Gravity.CENTER
            button.minHeight = 52
            button.setPadding(0, 0, 0, 0)
            // ripple/pressed feedback is default for Buttons; selected state shows via selector
            button.setOnClickListener {
                if (selectedReadingLevels.contains(i)) {
                    selectedReadingLevels.remove(i)
                    button.isSelected = false
                    button.setTextColor(android.graphics.Color.BLACK)
                } else {
                    selectedReadingLevels.add(i)
                    button.isSelected = true
                    button.setTextColor(android.graphics.Color.WHITE)
                }
                updateCta()
            }
            readingLevelGrid?.addView(levelView)
        }

        // Initialize categories
        val categories = listOf(
            "Folktales", "Legends and Myths", "Animal Tales", "Nature and Environment",
            "Family and Friends", "Fantasy and Adventure", "Values and Morals", "Heroes and Inspirations"
        )

        categories.forEach { category ->
            val categoryView = LayoutInflater.from(this).inflate(R.layout.item_filter_category, categoriesGrid, false)
            val button = categoryView.findViewById<Button>(R.id.btn_category)
            button.isAllCaps = false
            button.setPadding(24, 0, 24, 0)
            button.text = category
            // make widths equal per column by using 0dp + columnWeight in layout
            button.setOnClickListener {
                if (selectedCategories.contains(category)) {
                    selectedCategories.remove(category)
                    button.isSelected = false
                    button.setTextColor(android.graphics.Color.BLACK)
                } else {
                    selectedCategories.add(category)
                    button.isSelected = true
                    button.setTextColor(android.graphics.Color.WHITE)
                }
                updateCta()
            }
            categoriesGrid?.addView(categoryView)
        }

        // Clear button
        btnClear?.setOnClickListener {
            selectedReadingLevels.clear()
            selectedCategories.clear()
            resetFilterButtons(readingLevelGrid, categoriesGrid)
            updateCta()
        }

        // Show Books button
        btnShowBooks?.setOnClickListener {
            applyFilters()
            hideFilterBottomSheet()
        }

        // Close (X) button
        btnClose?.setOnClickListener { hideFilterBottomSheet() }

        // Initialize CTA state on open
        updateCta()
    }

    private fun resetFilterButtons(readingLevelGrid: GridLayout?, categoriesGrid: GridLayout?) {
        // Reset reading level buttons
        for (i in 0 until (readingLevelGrid?.childCount ?: 0)) {
            val child = readingLevelGrid?.getChildAt(i)
            val button = child?.findViewById<Button>(R.id.btn_reading_level)
            button?.isSelected = false
            button?.setTextColor(android.graphics.Color.BLACK)
        }

        // Reset category buttons
        for (i in 0 until (categoriesGrid?.childCount ?: 0)) {
            val child = categoriesGrid?.getChildAt(i)
            val button = child?.findViewById<Button>(R.id.btn_category)
            button?.isSelected = false
            button?.setTextColor(android.graphics.Color.BLACK)
        }
    }

    private fun applyFilters() {
        if (selectedReadingLevels.isEmpty() && selectedCategories.isEmpty()) {
            val context = applicationContext
            val txt = "Please select at least one filter option"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            return
        }
        
        // Show filtered books dialog
        showFilteredBooks()
        
        val message = "Filtered by: Reading Levels ${selectedReadingLevels.joinToString()}, Categories: ${selectedCategories.joinToString()}"
        val context = applicationContext
        val txt = message
        val time = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, txt, time)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
        toast.show()
    }
    
    @SuppressLint("SetTextI18n")
    private fun showFilteredBooks() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.filtered_books_dialog, null)
        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val booksGrid = dialogView.findViewById<GridLayout>(R.id.filtered_books_grid)
        val closeButton = dialogView.findViewById<ImageView>(R.id.close_button)
        
        title.text = "Filtered Books"
        
        // Sample filtered books based on selections
        val filteredBooks = mutableListOf<BookData>()
        
        // Add books based on selected categories
        if (selectedCategories.contains("Folktales")) {
            filteredBooks.add(BookData("The Magic Carpet", R.drawable.book1, R.string.dog_and_cat))
            filteredBooks.add(BookData("The Wise Turtle", R.drawable.book2, R.string.ana_and_the_ball))
        }
        if (selectedCategories.contains("Animal Tales")) {
            filteredBooks.add(BookData("Dog and Cat", R.drawable.book1, R.string.dog_and_cat))
            filteredBooks.add(BookData("Forest Friends", R.drawable.book5, R.string.toto_and_his_friends_in_forest))
        }
        if (selectedCategories.contains("Legends and Myths")) {
            filteredBooks.add(BookData("The Legend of Mount Apo", R.drawable.book3, R.string.lito_s_umbrella))
            filteredBooks.add(BookData("The Golden Fish", R.drawable.book4, R.string.mila_and_the_butterfly))
        }
        
        // If no categories selected, show books based on reading levels
        if (selectedCategories.isEmpty() && selectedReadingLevels.isNotEmpty()) {
            filteredBooks.addAll(listOf(
                BookData("Level ${selectedReadingLevels.first()} Book 1", R.drawable.book1, R.string.dog_and_cat),
                BookData("Level ${selectedReadingLevels.first()} Book 2", R.drawable.book2, R.string.ana_and_the_ball)
            ))
        }
        
        // If no filters selected, show all books
        if (filteredBooks.isEmpty()) {
            filteredBooks.addAll(listOf(
                BookData("Sample Book 1", R.drawable.book1, R.string.dog_and_cat),
                BookData("Sample Book 2", R.drawable.book2, R.string.ana_and_the_ball),
                BookData("Sample Book 3", R.drawable.book3, R.string.lito_s_umbrella),
                BookData("Sample Book 4", R.drawable.book4, R.string.mila_and_the_butterfly)
            ))
        }
        
        // Populate books grid
        filteredBooks.forEachIndexed { index, book ->
            val bookView = LayoutInflater.from(this).inflate(R.layout.item_book, booksGrid, false)
            val imageView = bookView.findViewById<ImageView>(R.id.book_image)
            val titleView = bookView.findViewById<TextView>(R.id.book_title)
            
            imageView.setImageResource(book.imageRes)
            titleView.text = getString(book.titleRes)
            
            bookView.setOnClickListener {
                showBookDetails(book)
            }
            
            val layoutParams = GridLayout.LayoutParams()
            layoutParams.columnSpec = GridLayout.spec(index % 2, 1f)
            layoutParams.rowSpec = GridLayout.spec(index / 2, 1f)
            bookView.layoutParams = layoutParams
            
            booksGrid.addView(bookView)
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun initializeSearchFunctionality() {
        val searchBar = findViewById<EditText>(R.id.search_bar)
        val searchIcon = findViewById<ImageView>(R.id.search_icon)
        
        // Make search bar clickable and focusable
        searchBar.setOnClickListener {
            searchBar.isFocusableInTouchMode = true
            searchBar.requestFocus()
            // Show cursor by making it focusable
        }
        
        // Search functionality
        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchBar.text.toString())
                searchBar.clearFocus()
                // Hide cursor by clearing focus
                true
            } else {
                false
            }
        }
        
        // Search icon click
        searchIcon.setOnClickListener {
            performSearch(searchBar.text.toString())
        }
    }
    
    private fun performSearch(query: String) {
        if (query.isBlank()) {
            val context = applicationContext
            val txt = "Please enter a search term"
            val time = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, txt, time)
            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            return
        }
        
        // Toast notification for search execution - MainActivity
        val context = applicationContext
        val txt = "Searching for: $query"
        val time = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, txt, time)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
        toast.show()
        
        // Intent to open SearchActivity
        val intent = Intent(this, SearchActivity::class.java).apply {
            putExtra("search_query", query)
        }
        startActivity(intent)
    }
    
    private fun showBookDetails(book: BookData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.book_details_dialog, null)
        val bookImage = dialogView.findViewById<ImageView>(R.id.dialog_book_image)
        val bookTitle = dialogView.findViewById<TextView>(R.id.dialog_book_title)
        val downloadButton = dialogView.findViewById<Button>(R.id.download_button)
        val favoriteButton = dialogView.findViewById<Button>(R.id.favorite_button)
        val readButton = dialogView.findViewById<Button>(R.id.read_button)
        val closeButton = dialogView.findViewById<ImageView>(R.id.close_button)
        
        bookImage.setImageResource(book.imageRes)
        bookTitle.text = getString(book.titleRes)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // Initialize Library Service
        val libraryService = LibraryService.getInstance()
        val authService = FirebaseAuthService.getInstance()
        
        // Check if user is authenticated
        if (!authService.isUserAuthenticated()) {
            downloadButton.setOnClickListener {
                Toast.makeText(this, "Please sign in to download books", Toast.LENGTH_SHORT).show()
            }
            favoriteButton.setOnClickListener {
                Toast.makeText(this, "Please sign in to add favorites", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Check book status
            val bookId = "book_${book.titleRes}"
            libraryService.checkBookStatus(bookId, object : LibraryService.BookStatusCallback {
                override fun onSuccess(isFavorite: Boolean, isDownloaded: Boolean) {
                    // Update button states based on current status
                    favoriteButton.text = if (isFavorite) "Remove from Favorites" else "Add to Favorites"
                    downloadButton.text = if (isDownloaded) "Downloaded" else "Download"
                }
            })
            
            downloadButton.setOnClickListener {
                val bookId = "book_${book.titleRes}"
                val bookTitle = getString(book.titleRes)
                
                libraryService.downloadBook(
                    bookId = bookId,
                    bookTitle = bookTitle,
                    bookAuthor = "Unknown Author",
                    bookImageRes = book.imageRes,
                    bookTitleRes = book.titleRes,
                    object : LibraryService.LibraryCallback {
                        override fun onSuccess() {
                            Toast.makeText(this@MainActivity, "Book downloaded successfully!", Toast.LENGTH_SHORT).show()
                            downloadButton.text = "Downloaded"
                            downloadButton.isEnabled = false
                            dialog.dismiss()
                            // Note: Library fragment will refresh automatically when user navigates to it
                        }
                        override fun onError(error: String) {
                            Toast.makeText(this@MainActivity, "Download failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            
            favoriteButton.setOnClickListener {
                val bookId = "book_${book.titleRes}"
                val bookTitle = getString(book.titleRes)
                val isCurrentlyFavorite = favoriteButton.text.toString().contains("Remove")
                
                if (isCurrentlyFavorite) {
                    // Remove from favorites
                    libraryService.removeFromFavorites(bookId, object : LibraryService.LibraryCallback {
                        override fun onSuccess() {
                            Toast.makeText(this@MainActivity, "Removed from favorites", Toast.LENGTH_SHORT).show()
                            favoriteButton.text = "Add to Favorites"
                        }
                        override fun onError(error: String) {
                            Toast.makeText(this@MainActivity, "Failed to remove: $error", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    // Add to favorites
                    libraryService.addToFavorites(
                        bookId = bookId,
                        bookTitle = bookTitle,
                        bookAuthor = "Unknown Author",
                        bookImageRes = book.imageRes,
                        bookTitleRes = book.titleRes,
                        object : LibraryService.LibraryCallback {
                            override fun onSuccess() {
                                Toast.makeText(this@MainActivity, "Added to favorites!", Toast.LENGTH_SHORT).show()
                                favoriteButton.text = "Remove from Favorites"
                                // Note: Library fragment will refresh automatically when user navigates to it
                            }
                            override fun onError(error: String) {
                                Toast.makeText(this@MainActivity, "Failed to add: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
        
        readButton.setOnClickListener {
            // Open StoryReaderActivity for page-by-page reading
            val intent = Intent(this, StoryReaderActivity::class.java).apply {
                putExtra("story_title", getString(book.titleRes))
                putExtra("story_id", "story_${book.titleRes}")
                // You can pass actual story content here, or it will load sample story
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
    
    private fun showCategoryBooks(category: CategoryData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.category_books_dialog, null)
        val categoryTitle = dialogView.findViewById<TextView>(R.id.category_title)
        val booksGrid = dialogView.findViewById<GridLayout>(R.id.category_books_grid)
        val closeButton = dialogView.findViewById<ImageView>(R.id.close_button)
        
        categoryTitle.text = getString(category.titleRes)
        
        // Sample books for each category
        val categoryBooks = when (category.title) {
            "Folktales" -> listOf(
                BookData("The Magic Carpet", R.drawable.book1, R.string.dog_and_cat),
                BookData("The Wise Turtle", R.drawable.book2, R.string.ana_and_the_ball)
            )
            "Legends and Myths" -> listOf(
                BookData("The Legend of Mount Apo", R.drawable.book3, R.string.lito_s_umbrella),
                BookData("The Golden Fish", R.drawable.book4, R.string.mila_and_the_butterfly)
            )
            "Animal Tales" -> listOf(
                BookData("Dog and Cat", R.drawable.book1, R.string.dog_and_cat),
                BookData("Forest Friends", R.drawable.book5, R.string.toto_and_his_friends_in_forest)
            )
            else -> listOf(
                BookData("Sample Book 1", R.drawable.book1, R.string.dog_and_cat),
                BookData("Sample Book 2", R.drawable.book2, R.string.ana_and_the_ball)
            )
        }
        
        // Populate books grid
        categoryBooks.forEachIndexed { index, book ->
            val bookView = LayoutInflater.from(this).inflate(R.layout.item_book, booksGrid, false)
            val imageView = bookView.findViewById<ImageView>(R.id.book_image)
            val titleView = bookView.findViewById<TextView>(R.id.book_title)
            
            imageView.setImageResource(book.imageRes)
            titleView.text = getString(book.titleRes)
            
            bookView.setOnClickListener {
                showBookDetails(book)
            }
            
            val layoutParams = GridLayout.LayoutParams()
            layoutParams.columnSpec = GridLayout.spec(index % 2, 1f)
            layoutParams.rowSpec = GridLayout.spec(index / 2, 1f)
            bookView.layoutParams = layoutParams
            
            booksGrid.addView(bookView)
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun initializeSeeMoreButtons() {
        // Find see more buttons in home content
        val featuredSeeMore = findViewById<TextView>(R.id.featured_see_more)
        val recommendedSeeMore = findViewById<TextView>(R.id.recommended_see_more)
        
        // Featured Stories See More
        featuredSeeMore?.setOnClickListener {
            showAllFeaturedStories()
        }
        
        // Recommended Books See More
        recommendedSeeMore?.setOnClickListener {
            showAllRecommendedBooks()
        }
    }
    
    @SuppressLint("SetTextI18n")
    private fun showAllFeaturedStories() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.all_books_dialog, null)
        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val booksGrid = dialogView.findViewById<GridLayout>(R.id.books_grid)
        val closeButton = dialogView.findViewById<ImageView>(R.id.close_button)
        
        title.text = "All Featured Stories"
        
        val allFeaturedBooks = listOf(
            BookData("Dog and Cat", R.drawable.book1, R.string.dog_and_cat),
            BookData("Ana and the Ball", R.drawable.book2, R.string.ana_and_the_ball),
            BookData("Lito's Umbrella", R.drawable.book3, R.string.lito_s_umbrella),
            BookData("Mila and the Butterfly", R.drawable.book4, R.string.mila_and_the_butterfly),
            BookData("The Magic Forest", R.drawable.book5, R.string.toto_and_his_friends_in_forest),
            BookData("Ocean Adventures", R.drawable.book6, R.string.bituin_and_the_little_fish)
        )
        
        populateBooksGrid(booksGrid, allFeaturedBooks)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    @SuppressLint("SetTextI18n")
    private fun showAllRecommendedBooks() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.all_books_dialog, null)
        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val booksGrid = dialogView.findViewById<GridLayout>(R.id.books_grid)
        val closeButton = dialogView.findViewById<ImageView>(R.id.close_button)
        
        title.text = "All Recommended Books"
        
        val allRecommendedBooks = listOf(
            BookData("Toto and His Friends in Forest", R.drawable.book5, R.string.toto_and_his_friends_in_forest),
            BookData("Bituin and the Little Fish", R.drawable.book6, R.string.bituin_and_the_little_fish),
            BookData("Adventure Tales", R.drawable.book1, R.string.dog_and_cat),
            BookData("Nature Stories", R.drawable.book2, R.string.ana_and_the_ball),
            BookData("Friendship Stories", R.drawable.book3, R.string.lito_s_umbrella),
            BookData("Learning Adventures", R.drawable.book4, R.string.mila_and_the_butterfly)
        )
        
        populateBooksGrid(booksGrid, allRecommendedBooks)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun populateBooksGrid(grid: GridLayout, books: List<BookData>) {
        books.forEachIndexed { index, book ->
            val bookView = LayoutInflater.from(this).inflate(R.layout.item_book, grid, false)
            val imageView = bookView.findViewById<ImageView>(R.id.book_image)
            val titleView = bookView.findViewById<TextView>(R.id.book_title)
            
            imageView.setImageResource(book.imageRes)
            titleView.text = getString(book.titleRes)
            
            bookView.setOnClickListener {
                showBookDetails(book)
            }
            
            val layoutParams = GridLayout.LayoutParams()
            layoutParams.columnSpec = GridLayout.spec(index % 2, 1f)
            layoutParams.rowSpec = GridLayout.spec(index / 2, 1f)
            bookView.layoutParams = layoutParams
            
            grid.addView(bookView)
        }
    }
}
