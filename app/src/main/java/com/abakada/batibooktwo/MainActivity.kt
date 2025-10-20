package com.abakada.batibooktwo

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
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default tab = Home
        binding.bottomNavigationView.selectedItemId = R.id.home
        binding.frameLayout.visibility = View.GONE
        findViewById<View>(R.id.home_content).visibility = View.VISIBLE
        
        // Initialize home content
        initializeHomeContent()

        // Bottom navigation listener
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.library -> {
                    replaceFragment(Library())
                    true
                }
                R.id.profile -> {
                    replaceFragment(Profile())
                    true
                }
                R.id.home -> {
                    showHome()
                    true
                }
                else -> false
            }
        }

        // Hamburger button click → show bottom sheet
        binding.menuIcon.setOnClickListener {
            if (!isBottomSheetVisible) showBottomSheet()
        }

        // Filter button click → show filter bottom sheet
        val filterButton = findViewById<View>(R.id.filter_button)
        filterButton?.setOnClickListener {
            if (!isFilterBottomSheetVisible) showFilterBottomSheet()
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
    }

    private fun showHome() {
        supportFragmentManager.fragments.forEach {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
        binding.frameLayout.visibility = View.GONE
        findViewById<View>(R.id.home_content).visibility = View.VISIBLE
    }

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
            val languages = listOf("Select Language", "English", "Tagalog")

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
                "Tagalog" -> 2
                else -> 0 // default = Select Language
            }
            spinner.setSelection(selectedPosition, false)

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) return // ignore "Select Language"

                    val selected = languages[position]
                    if (selected != savedLang) {
                        sharedPrefs.edit { putString("language", selected) }
                        Toast.makeText(this@MainActivity, "Language set to $selected", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // About Batibook navigation
        val aboutLayout = bottomSheetView?.findViewById<LinearLayout>(R.id.layoutAppInfo)
        aboutLayout?.setOnClickListener {
            hideBottomSheet()

            // Navigate to AboutActivity
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

    private fun initializeFilterUI() {
        val readingLevelGrid = filterBottomSheetView?.findViewById<GridLayout>(R.id.reading_level_grid)
        val categoriesGrid = filterBottomSheetView?.findViewById<GridLayout>(R.id.categories_grid)
        val btnClear = filterBottomSheetView?.findViewById<Button>(R.id.btn_clear)
        val btnShowBooks = filterBottomSheetView?.findViewById<Button>(R.id.btn_show_books)
        val btnClose = filterBottomSheetView?.findViewById<ImageView>(R.id.cancelButton)

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
                } else {
                    selectedReadingLevels.add(i)
                    button.isSelected = true
                }
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
                } else {
                    selectedCategories.add(category)
                    button.isSelected = true
                }
            }
            categoriesGrid?.addView(categoryView)
        }

        // Clear button
        btnClear?.setOnClickListener {
            selectedReadingLevels.clear()
            selectedCategories.clear()
            resetFilterButtons(readingLevelGrid, categoriesGrid)
        }

        // Show Books button
        btnShowBooks?.setOnClickListener {
            applyFilters()
            hideFilterBottomSheet()
        }

        // Close (X) button
        btnClose?.setOnClickListener { hideFilterBottomSheet() }
    }

    private fun resetFilterButtons(readingLevelGrid: GridLayout?, categoriesGrid: GridLayout?) {
        // Reset reading level buttons
        for (i in 0 until (readingLevelGrid?.childCount ?: 0)) {
            val child = readingLevelGrid?.getChildAt(i)
            val button = child?.findViewById<Button>(R.id.btn_reading_level)
            button?.isSelected = false
        }

        // Reset category buttons
        for (i in 0 until (categoriesGrid?.childCount ?: 0)) {
            val child = categoriesGrid?.getChildAt(i)
            val button = child?.findViewById<Button>(R.id.btn_category)
            button?.isSelected = false
        }
    }

    private fun applyFilters() {
        // TODO: Apply filters to book data
        // This is where you would filter the books based on selectedReadingLevels and selectedCategories
        // For now, just show a toast
        val message = "Filtered by: Reading Levels ${selectedReadingLevels.joinToString()}, Categories: ${selectedCategories.joinToString()}"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
