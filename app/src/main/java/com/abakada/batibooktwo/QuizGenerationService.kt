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
     * Generate simple quiz using local algorithm (fallback)
     * 
     * @param storyText The story content
     * @param questionCount Number of questions to generate
     * @param callback Callback to handle result
     * 
     * This method:
     * - Extracts key sentences from story
     * - Creates simple "who/what/where" questions
     * - Generates multiple choice options
     * - Works offline, no API needed
     */
    fun generateSimpleQuiz(
        storyText: String,
        questionCount: Int = 5,
        callback: QuizCallback
    ) {
        try {
            val sentences = storyText.split(Regex("[.!?]+"))
                .map { it.trim() }
                .filter { it.length > 20 } // Filter short sentences
                .take(questionCount * 2) // Take more sentences than needed
            
            val questions = mutableListOf<QuizQuestion>()
            
            sentences.forEachIndexed { index, sentence ->
                if (questions.size >= questionCount) return@forEachIndexed
                
                // Extract key words from sentence
                val words = sentence.split(Regex("\\s+"))
                    .filter { it.length > 3 }
                    .take(4)
                
                // Create question
                val questionText = when {
                    sentence.contains("who", ignoreCase = true) ->
                        "Who ${sentence.split(Regex("who", RegexOption.IGNORE_CASE)).lastOrNull()?.trim() ?: "was in the story?"}"
                    sentence.contains("what", ignoreCase = true) ->
                        "What ${sentence.split(Regex("what", RegexOption.IGNORE_CASE)).lastOrNull()?.trim() ?: "happened in the story?"}"
                    sentence.contains("where", ignoreCase = true) ->
                        "Where ${sentence.split(Regex("where", RegexOption.IGNORE_CASE)).lastOrNull()?.trim() ?: "did this happen?"}"
                    else -> "What happened: ${sentence.take(50)}...?"
                }
                
                // Generate options
                val correctAnswer = sentence
                val wrongAnswers = sentences
                    .filter { it != sentence }
                    .take(3)
                    .map { it.take(40) }
                
                val allOptions = (listOf(correctAnswer) + wrongAnswers).shuffled()
                val correctIndex = allOptions.indexOf(correctAnswer)
                
                questions.add(
                    QuizQuestion(
                        question = questionText,
                        options = allOptions.take(4),
                        correctAnswer = correctIndex
                    )
                )
            }
            
            // If we didn't generate enough questions, fill with simple ones
            while (questions.size < questionCount) {
                questions.add(
                    QuizQuestion(
                        question = "What is the main idea of this story?",
                        options = listOf(
                            "A story about friendship",
                            "A story about adventure",
                            "A story about learning",
                            "A story about discovery"
                        ),
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


