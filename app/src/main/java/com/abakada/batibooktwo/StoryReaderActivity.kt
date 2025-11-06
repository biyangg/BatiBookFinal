package com.abakada.batibooktwo

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * StoryReaderActivity - Page-by-page story reader with quiz at the end
 * 
 * This activity displays stories page by page, allowing users to navigate
 * through pages. At the end, it generates and displays a quiz based on the story.
 * 
 * Features:
 * - Page-by-page navigation
 * - Audio controls for text-to-speech (optional)
 * - Progress tracking
 * - Quiz generation at the end using QuizGenerationService
 */
class StoryReaderActivity : AppCompatActivity() {

    private lateinit var storyImage: ImageView
    private lateinit var storyText: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var pageIndicator: TextView
    private lateinit var backButton: ImageView
    private lateinit var storyPageContainer: View
    private lateinit var quizContainer: ScrollView
    private lateinit var quizQuestionsContainer: LinearLayout
    private lateinit var btnSubmitQuiz: Button
    private lateinit var quizInstruction: TextView

    // Story data
    private var storyPages: List<StoryPage> = emptyList()
    private var currentPageIndex = 0
    private var storyTitle: String = ""
    private var storyId: String = ""

    // Quiz
    private var quizQuestions: List<QuizGenerationService.QuizQuestion> = emptyList()
    private val selectedAnswers = mutableMapOf<Int, Int>() // question index -> selected option index

    // Services
    private lateinit var quizService: QuizGenerationService
    private lateinit var progressService: ProgressTrackingService
    private lateinit var ttsService: TextToSpeechService
    private var isTtsPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_reader)

        // Get story data from intent
        storyTitle = intent.getStringExtra("story_title") ?: "Unknown Story"
        storyId = intent.getStringExtra("story_id") ?: "story_${System.currentTimeMillis()}"
        val storyContent = intent.getStringExtra("story_content") ?: ""

        // Initialize services
        quizService = QuizGenerationService.getInstance(this)
        progressService = ProgressTrackingService.getInstance()
        ttsService = TextToSpeechService.getInstance(this)

        // Initialize views
        initializeViews()

        // Load story pages
        if (storyContent.isNotEmpty()) {
            loadStoryPages(storyContent)
        } else {
            // Load sample story for demo
            loadSampleStory()
        }

        // Initialize TTS
        ttsService.initialize { success ->
            if (success) {
                // TTS ready
            }
        }

        // Setup click listeners
        setupListeners()

        // Display first page
        showPage(0)
    }

    private fun initializeViews() {
        storyImage = findViewById(R.id.story_image)
        storyText = findViewById(R.id.story_text)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        pageIndicator = findViewById(R.id.page_indicator)
        backButton = findViewById(R.id.back_button)
        storyPageContainer = findViewById(R.id.story_page_container)
        quizContainer = findViewById(R.id.quiz_container)
        quizQuestionsContainer = findViewById(R.id.quiz_questions_container)
        btnSubmitQuiz = findViewById(R.id.btn_submit_quiz)
        quizInstruction = findViewById(R.id.quiz_instruction)

        // Audio controls
        val btnRewind = findViewById<ImageButton>(R.id.btn_rewind)
        val btnPlayPause = findViewById<ImageButton>(R.id.btn_play_pause)
        val btnForward = findViewById<ImageButton>(R.id.btn_forward)

        btnPlayPause.setOnClickListener {
            if (isTtsPlaying) {
                ttsService.stop()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                isTtsPlaying = false
            } else {
                val currentText = storyPages[currentPageIndex].text
                ttsService.speak(currentText)
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                isTtsPlaying = true
            }
        }

        ttsService.onSpeechDone = {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            isTtsPlaying = false
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        btnPrev.setOnClickListener {
            if (currentPageIndex > 0) {
                showPage(currentPageIndex - 1)
            }
        }

        btnNext.setOnClickListener {
            if (currentPageIndex < storyPages.size - 1) {
                showPage(currentPageIndex + 1)
            } else {
                // Reached end - show quiz
                showQuiz()
            }
        }

        btnSubmitQuiz.setOnClickListener {
            submitQuiz()
        }
    }

    private fun loadStoryPages(content: String) {
        // Split content into pages (simple implementation)
        // You can enhance this to use actual page data
        val sentences = content.split(Regex("[.!?]+"))
        storyPages = sentences.mapIndexed { index, sentence ->
            StoryPage(
                imageRes = when (index % 4) {
                    0 -> R.drawable.book1
                    1 -> R.drawable.book2
                    2 -> R.drawable.book3
                    else -> R.drawable.book4
                },
                text = sentence.trim()
            )
        }
    }

    private fun loadSampleStory() {
        // Sample "Dog and Cat" story
        storyPages = listOf(
            StoryPage(R.drawable.book1, "Dog."),
            StoryPage(R.drawable.book2, "Cat."),
            StoryPage(R.drawable.book3, "Dog and cat."),
            StoryPage(R.drawable.book1, "The dog runs."),
            StoryPage(R.drawable.book2, "The cat jumps."),
            StoryPage(R.drawable.book3, "Dog! Cat!"),
            StoryPage(R.drawable.book1, "Together.")
        )
    }

    private fun showPage(index: Int) {
        if (index < 0 || index >= storyPages.size) return

        currentPageIndex = index
        val page = storyPages[index]

        // Update image
        storyImage.setImageResource(page.imageRes)

        // Update text
        storyText.text = page.text

        // Update page indicator
        pageIndicator.text = "Page ${index + 1} / ${storyPages.size}"

        // Update navigation buttons
        btnPrev.isEnabled = index > 0
        btnNext.text = if (index == storyPages.size - 1) "Quiz" else "→"

        // Save progress
        progressService.saveProgress(
            storyId = storyId,
            storyTitle = storyTitle,
            currentPage = index,
            totalPages = storyPages.size,
            timeSpent = 0
        )
    }

    private fun showQuiz() {
        // Hide story page, show quiz
        storyPageContainer.visibility = View.GONE
        quizContainer.visibility = View.VISIBLE

        // Generate quiz from story content
        val fullStoryText = storyPages.joinToString(" ") { it.text }
        quizInstruction.text = "Answer the questions based on \"$storyTitle\""

        quizService.generateQuizWithOpenAI(
            storyText = fullStoryText,
            questionCount = 5,
            difficulty = "easy",
            object : QuizGenerationService.QuizCallback {
                override fun onSuccess(questions: List<QuizGenerationService.QuizQuestion>) {
                    quizQuestions = questions
                    displayQuizQuestions(questions)
                }

                override fun onError(error: String) {
                    // Fallback to simple quiz
                    quizService.generateSimpleQuiz(
                        storyText = fullStoryText,
                        questionCount = 5,
                        object : QuizGenerationService.QuizCallback {
                            override fun onSuccess(questions: List<QuizGenerationService.QuizQuestion>) {
                                quizQuestions = questions
                                displayQuizQuestions(questions)
                            }

                            override fun onError(error: String) {
                                Toast.makeText(this@StoryReaderActivity, "Failed to generate quiz: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        )
    }

    private fun displayQuizQuestions(questions: List<QuizGenerationService.QuizQuestion>) {
        quizQuestionsContainer.removeAllViews()

        questions.forEachIndexed { questionIndex, question ->
            val questionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 32)
                }
            }

            // Question text
            val questionText = TextView(this).apply {
                text = "${questionIndex + 1}. ${question.question}"
                textSize = 18f
                setTextColor(resources.getColor(R.color.black, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
            }

            questionLayout.addView(questionText)



            // Options (RadioButtons)
            val radioGroup = RadioGroup(this).apply {
                orientation = RadioGroup.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            question.options.forEachIndexed { optionIndex, option ->
                val radioButton = RadioButton(this).apply {
                    text = option
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.black, null))
                    id = View.generateViewId()
                    layoutParams = RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.MATCH_PARENT,
                        RadioGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                }

                radioButton.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedAnswers[questionIndex] = optionIndex
                        checkQuizCompletion()
                    }
                }

                radioGroup.addView(radioButton)
            }

            questionLayout.addView(radioGroup)
            quizQuestionsContainer.addView(questionLayout)
        }

        checkQuizCompletion()
    }

    private fun checkQuizCompletion() {
        val allAnswered = selectedAnswers.size == quizQuestions.size
        btnSubmitQuiz.isEnabled = allAnswered
    }

    private fun submitQuiz() {
        var correctCount = 0
        val totalQuestions = quizQuestions.size

        quizQuestions.forEachIndexed { index, question ->
            val selectedAnswer = selectedAnswers[index]
            if (selectedAnswer == question.correctAnswer) {
                correctCount++
            }
        }

        val score = (correctCount * 100) / totalQuestions
        val message = "You got $correctCount out of $totalQuestions correct!\nScore: $score%"

        AlertDialog.Builder(this)
            .setTitle("Quiz Results")
            .setMessage(message)
            .setPositiveButton("Great Job!") { _, _ ->
                // Mark story as completed
                progressService.saveProgress(
                    storyId = storyId,
                    storyTitle = storyTitle,
                    currentPage = storyPages.size,
                    totalPages = storyPages.size,
                    timeSpent = 0
                )
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsService.shutdown()
    }

    data class StoryPage(
        val imageRes: Int,
        val text: String
    )
}

