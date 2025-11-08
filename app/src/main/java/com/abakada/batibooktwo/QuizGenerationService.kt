package com.abakada.batibooktwo

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import org.json.JSONObject
import java.util.*

/**
 * Quiz Generation API Service
 * 
 * This service generates simple comprehension quizzes based on story content.
 * It can use OpenAI API for intelligent quiz generation or provide simple
 * rule-based quiz generation as a fallback.
 * 
 * INTEGRATION PROCESS:
 * Option 1 - OpenAI API (Recommended):
 * 1. Get OpenAI API key from https://platform.openai.com/api-keys
 * 2. Replace OPENAI_API_KEY constant below with your key
 * 3. Ensure you have internet connection
 * 
 * Option 2 - Simple Local Generation (Fallback):
 * 1. No API key needed
 * 2. Generates basic quizzes using keyword extraction
 * 3. Works offline
 * 
 * HOW IT WORKS:
 * - Takes story text as input
 * - Analyzes content to identify key concepts
 * - Generates multiple-choice questions
 * - Creates answer options with correct and incorrect answers
 * - Returns quiz questions in structured format
 * 
 * API KEY LOCATION:
 * - OpenAI API Key: Replace OPENAI_API_KEY constant in this file (line 39)
 * - Get your key at: https://platform.openai.com/api-keys
 * 
 * FEATURES:
 * - Multiple choice questions
 * - Automatic answer generation
 * - Difficulty level selection
 * - Question count customization
 * - Fallback to simple generation if API unavailable
 */
class QuizGenerationService private constructor(private val context: android.content.Context) {
    
    companion object {
        private const val TAG = "QuizGenerationService"
        
        // OpenAI API Configuration
        // TODO: Replace with your actual OpenAI API key
        // Get your key from: https://platform.openai.com/api-keys
        private const val OPENAI_API_KEY = "YOUR_OPENAI_API_KEY_HERE"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        
        @Volatile
        private var INSTANCE: QuizGenerationService? = null
        
        fun getInstance(context: android.content.Context): QuizGenerationService {
            return INSTANCE ?: synchronized(this) {
                val instance = QuizGenerationService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }
    
    private val gson = Gson()
    
    /**
     * Generate quiz using OpenAI API
     * 
     * @param storyText The story content to generate questions from
     * @param questionCount Number of questions to generate (default: 5)
     * @param difficulty Difficulty level: "easy", "medium", "hard" (default: "medium")
     * @param callback Callback to handle result
     * 
     * Process:
     * 1. Validates API key is set
     * 2. Creates prompt for OpenAI
     * 3. Sends request to OpenAI API
     * 4. Parses JSON response
     * 5. Returns quiz questions
     */
    fun generateQuizWithOpenAI(
        storyText: String,
        questionCount: Int = 5,
        difficulty: String = "medium",
        callback: QuizCallback
    ) {
        // Check if API key is configured
        if (OPENAI_API_KEY == "YOUR_OPENAI_API_KEY_HERE" || OPENAI_API_KEY.isBlank()) {
            Log.w(TAG, "OpenAI API key not configured, using simple generation")
            generateSimpleQuiz(storyText, questionCount, callback)
            return
        }
        
        // Create prompt for OpenAI
        val prompt = """
            Generate $questionCount multiple-choice comprehension questions for children based on this story.
            Difficulty level: $difficulty
            Make questions appropriate for children learning to read.
            Return ONLY a JSON array with this exact format:
            [
              {
                "question": "Question text?",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctAnswer": 0
              }
            ]
            
            Story:
            $storyText
        """.trimIndent()
        
        // Create request body
        val requestBody = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 1000)
        }
        
        // Create request
        val jsonRequest = object : JsonObjectRequest(
            Request.Method.POST,
            OPENAI_API_URL,
            requestBody,
            { response ->
                try {
                    val choices = response.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        val content = message.getString("content")
                        
                        // Parse JSON from content
                        val questionsJson = org.json.JSONArray(content.trim())
                        val questions = parseQuizQuestions(questionsJson)
                        
                        callback.onSuccess(questions)
                    } else {
                        throw Exception("No response from OpenAI")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing OpenAI response", e)
                    // Fallback to simple generation
                    generateSimpleQuiz(storyText, questionCount, callback)
                }
            },
            { error ->
                Log.e(TAG, "OpenAI API error", error)
                // Fallback to simple generation
                generateSimpleQuiz(storyText, questionCount, callback)
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "Bearer $OPENAI_API_KEY"
                return headers
            }
        }
        
        requestQueue.add(jsonRequest)
    }
    
    /**
     * Generate simple quiz using local algorithm (kid-friendly)
     * 
     * @param storyText The story content
     * @param questionCount Number of questions to generate
     * @param callback Callback to handle result
     * 
     * This method creates simple, story-aligned questions for children:
     * - Uses simple words from the story
     * - Creates easy multiple-choice questions
     * - Matches questions directly to story content
     * - Works offline, no API needed
     */
    fun generateSimpleQuiz(
        storyText: String,
        questionCount: Int = 5,
        callback: QuizCallback
    ) {
        try {
            // Split into sentences and clean them
            val sentences = storyText.split(Regex("[.!?]+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.length > 5 } // Filter very short sentences
            
            if (sentences.isEmpty()) {
                callback.onError("Story text is too short to generate quiz")
                return
            }
            
            val questions = mutableListOf<QuizQuestion>()
            
            // Extract key words (nouns, main verbs) from all sentences
            val allWords = sentences.flatMap { sentence ->
                sentence.split(Regex("\\s+"))
                    .map { it.lowercase().replace(Regex("[^a-z]"), "") }
                    .filter { it.length >= 3 } // Filter short words
            }
            
            // Get unique important words (characters, objects)
            val importantWords = allWords
                .groupBy { it }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(10)
                .map { it.first }
            
            // Generate simple questions based on story content
            sentences.take(questionCount).forEachIndexed { index, sentence ->
                if (questions.size >= questionCount) return@forEachIndexed
                
                val words = sentence.split(Regex("\\s+")).map { it.lowercase().replace(Regex("[^a-z]"), "") }
                
                // Find the main subject/object in the sentence
                val mainWord = importantWords.firstOrNull { word ->
                    words.any { it.contains(word, ignoreCase = true) }
                } ?: words.firstOrNull { it.length >= 4 } ?: "something"
                
                // Create simple, kid-friendly questions
                val questionText = when {
                    sentence.contains("dog", ignoreCase = true) -> "What animal is in this story?"
                    sentence.contains("cat", ignoreCase = true) -> "What animal is in this story?"
                    sentence.contains("run", ignoreCase = true) || sentence.contains("runs", ignoreCase = true) -> "What did the character do?"
                    sentence.contains("jump", ignoreCase = true) || sentence.contains("jumps", ignoreCase = true) -> "What did the character do?"
                    sentence.contains("together", ignoreCase = true) -> "What happened at the end?"
                    else -> "What word did you see in the story?"
                }
                
                // Generate simple options
                val correctOption = when {
                    sentence.contains("dog", ignoreCase = true) -> "Dog"
                    sentence.contains("cat", ignoreCase = true) -> "Cat"
                    sentence.contains("run", ignoreCase = true) || sentence.contains("runs", ignoreCase = true) -> "Run"
                    sentence.contains("jump", ignoreCase = true) || sentence.contains("jumps", ignoreCase = true) -> "Jump"
                    sentence.contains("together", ignoreCase = true) -> "They played together"
                    else -> mainWord.replaceFirstChar { it.uppercaseChar() }
                }
                
                // Create wrong answers (simple distractors)
                val wrongOptions = when {
                    sentence.contains("dog", ignoreCase = true) -> listOf("Cat", "Bird", "Fish")
                    sentence.contains("cat", ignoreCase = true) -> listOf("Dog", "Bird", "Fish")
                    sentence.contains("run", ignoreCase = true) || sentence.contains("runs", ignoreCase = true) -> listOf("Sleep", "Eat", "Sit")
                    sentence.contains("jump", ignoreCase = true) || sentence.contains("jumps", ignoreCase = true) -> listOf("Walk", "Sleep", "Eat")
                    sentence.contains("together", ignoreCase = true) -> listOf("They went home", "They were sad", "They were alone")
                    else -> listOf("Something else", "I don't know", "Maybe")
                }
                
                val allOptions = (listOf(correctOption) + wrongOptions).shuffled()
                val correctIndex = allOptions.indexOf(correctOption)
                
                questions.add(
                    QuizQuestion(
                        question = questionText,
                        options = allOptions.take(4),
                        correctAnswer = correctIndex
                    )
                )
            }
            
            // Fill remaining questions with simple story comprehension questions
            while (questions.size < questionCount) {
                val storyHasDog = storyText.contains("dog", ignoreCase = true)
                val storyHasCat = storyText.contains("cat", ignoreCase = true)
                
                questions.add(
                    QuizQuestion(
                        question = when {
                            storyHasDog && storyHasCat -> "How many animals were in the story?"
                            storyHasDog -> "What animal did you read about?"
                            storyHasCat -> "What animal did you read about?"
                            else -> "Did you enjoy reading this story?"
                        },
                        options = when {
                            storyHasDog && storyHasCat -> listOf("Two", "One", "Three", "Four")
                            storyHasDog -> listOf("Dog", "Cat", "Bird", "Fish")
                            storyHasCat -> listOf("Cat", "Dog", "Bird", "Fish")
                            else -> listOf("Yes", "No", "Maybe", "I don't know")
                        },
                        correctAnswer = 0
                    )
                )
            }
            
            callback.onSuccess(questions.take(questionCount))
        } catch (e: Exception) {
            Log.e(TAG, "Error generating simple quiz", e)
            callback.onError("Failed to generate quiz: ${e.message}")
        }
    }
    
    /**
     * Parse quiz questions from JSON array
     */
    private fun parseQuizQuestions(jsonArray: org.json.JSONArray): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        
        for (i in 0 until jsonArray.length()) {
            try {
                val item = jsonArray.getJSONObject(i)
                val question = item.getString("question")
                val optionsArray = item.getJSONArray("options")
                val options = mutableListOf<String>()
                
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }
                
                val correctAnswer = item.getInt("correctAnswer")
                
                questions.add(
                    QuizQuestion(
                        question = question,
                        options = options,
                        correctAnswer = correctAnswer
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing question $i", e)
            }
        }
        
        return questions
    }
    
    /**
     * Data model for quiz question
     */
    data class QuizQuestion(
        val question: String,
        val options: List<String>,
        val correctAnswer: Int // Index of correct option (0-based)
    )
    
    /**
     * Callback interface
     */
    interface QuizCallback {
        fun onSuccess(questions: List<QuizQuestion>)
        fun onError(error: String)
    }
}


