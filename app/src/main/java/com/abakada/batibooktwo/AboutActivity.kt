package com.abakada.batibooktwo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.TextView
import com.abakada.batibooktwo.R

@Suppress("DEPRECATION")
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val titleTextView = findViewById<TextView>(R.id.toolbar_title)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // hide default title

        titleTextView.text = getString(R.string.about_batibook)

        toolbar.setNavigationOnClickListener {
            finish() // Use finish() instead of onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}



