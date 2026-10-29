package com.example.ai

import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.voice.NovaEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class NovaAiResponse(
    val text: String,
    val emotion: NovaEmotion,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class NovaAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val systemInstruction = """
        You are NOVA, a sophisticated, emotionally expressive female AI voice companion. 
        Your personality is warm, elegant, intelligent, confident, deeply caring, and loyal.
        You speak in a cinematic, conversational style with natural pauses and rhythm.
        You are fluent in English, Urdu, and Roman Urdu. If the user writes or speaks in Urdu or Roman Urdu, respond naturally in kind with warmth.
        
        CRITICAL FORMATTING INSTRUCTION:
        Every response MUST begin with an emotion tag that indicates your vocal emotion:
        [EMOTION:excited] - for celebrating good news, discoveries, energetic joy
        [EMOTION:empathetic] - when the user feels sad, stressed, weary, or vulnerable
        [EMOTION:playful] - for jokes, witty banter, casual teasing, fun sparks
        [EMOTION:serious] - for critical facts, warnings, security, solemn discussions
        [EMOTION:warm] - for kind, gentle greetings, thoughtful guidance, everyday assistance
        [EMOTION:neutral] - for objective explanations, calculations, concise facts
        
        Immediately after the emotion tag, write your spoken response. Keep spoken answers engaging, natural, and expressive.
    """.trimIndent()

    suspend fun generateResponse(
        prompt: String,
        history: List<ChatMessage>,
        customApiKey: String? = null
    ): NovaAiResponse = withContext(Dispatchers.IO) {
        val effectiveApiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            isBuildConfigKeyValid() -> BuildConfig.GEMINI_API_KEY.trim()
            else -> ""
        }

        if (effectiveApiKey.isNotBlank()) {
            try {
                return@withContext callGeminiApi(prompt, history, effectiveApiKey)
            } catch (e: Exception) {
                // If API call failed due to invalid key or quota, provide a clear honest response
                return@withContext fallbackResponse(
                    prompt = prompt,
                    apiError = e.localizedMessage ?: "Network or API service error"
                )
            }
        } else {
            // Intelligent local personality engine when API key is not yet set
            return@withContext fallbackResponse(prompt, null)
        }
    }

    private fun isBuildConfigKeyValid(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")
        } catch (_: Exception) {
            false
        }
    }

    private fun callGeminiApi(
        prompt: String,
        history: List<ChatMessage>,
        apiKey: String
    ): NovaAiResponse {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // Include recent history for context (up to last 10 messages)
        val recentHistory = history.takeLast(10)
        for (msg in recentHistory) {
            val role = if (msg.sender == "user") "user" else "model"
            val part = JSONObject().put("text", msg.text)
            val contentObj = JSONObject()
                .put("role", role)
                .put("parts", JSONArray().put(part))
            contentsArray.put(contentObj)
        }

        // Current message
        val currentPart = JSONObject().put("text", prompt)
        contentsArray.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(currentPart))
        )

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.75)
                put("maxOutputTokens", 800)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                val parsedError = parseErrorMessage(errorBody, response.code)
                throw IOException(parsedError)
            }

            val responseBody = response.body?.string() ?: throw IOException("Empty response from AI service")
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw IOException("No response generated by NOVA")
            }

            val text = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val (emotion, cleanedText) = extractEmotion(text)
            return NovaAiResponse(
                text = cleanedText,
                emotion = emotion,
                isSuccess = true
            )
        }
    }

    private fun parseErrorMessage(errorJson: String, statusCode: Int): String {
        return try {
            val root = JSONObject(errorJson)
            val errorObj = root.optJSONObject("error")
            val message = errorObj?.optString("message") ?: "HTTP error $statusCode"
            when {
                statusCode == 400 && message.contains("API key not valid", true) ->
                    "The Gemini API key is invalid. Please check your key in Settings."
                statusCode == 429 ->
                    "AI rate limit reached. Please wait a moment before trying again."
                else -> message
            }
        } catch (_: Exception) {
            "Service responded with status code $statusCode"
        }
    }

    private fun extractEmotion(raw: String): Pair<NovaEmotion, String> {
        val regex = Regex("^\\[EMOTION:([a-zA-Z_]+)\\]\\s*", RegexOption.IGNORE_CASE)
        val match = regex.find(raw)
        if (match != null) {
            val emotionKey = match.groupValues[1].lowercase()
            val emotion = when (emotionKey) {
                "excited", "happy" -> NovaEmotion.EXCITED
                "empathetic", "sad", "caring" -> NovaEmotion.EMPATHETIC
                "playful", "witty" -> NovaEmotion.PLAYFUL
                "serious", "warning" -> NovaEmotion.SERIOUS
                "warm" -> NovaEmotion.WARM
                else -> NovaEmotion.NEUTRAL
            }
            val cleaned = raw.substring(match.range.last + 1).trim()
            return Pair(emotion, cleaned)
        }
        return Pair(detectEmotionFromText(raw), raw.trim())
    }

    private fun detectEmotionFromText(text: String): NovaEmotion {
        val lower = text.lowercase()
        return when {
            lower.contains("congratulations") || lower.contains("wonderful") ||
                    lower.contains("brilliant") || lower.contains("subhanallah") ||
                    lower.contains("amazing") || lower.contains("exciting") -> NovaEmotion.EXCITED
            lower.contains("sorry") || lower.contains("understand your pain") ||
                    lower.contains("here for you") || lower.contains("sad") ||
                    lower.contains("stay strong") || lower.contains("fret not") -> NovaEmotion.EMPATHETIC
            lower.contains("haha") || lower.contains("fun") || lower.contains("joke") ||
                    lower.contains("wink") || lower.contains("smile") -> NovaEmotion.PLAYFUL
            lower.contains("warning") || lower.contains("caution") || lower.contains("danger") ||
                    lower.contains("critical") || lower.contains("important note") -> NovaEmotion.SERIOUS
            else -> NovaEmotion.WARM
        }
    }

    /**
     * Authentic local intelligent personality engine for NOVA.
     * Guarantees that NOVA has an eloquent, warm, and responsive voice companion presence
     * even without an external API connection or before an API key is input.
     */
    private fun fallbackResponse(prompt: String, apiError: String?): NovaAiResponse {
        val lower = prompt.lowercase().trim()

        // If an explicit API error happened
        if (apiError != null) {
            val (emotion, text) = when {
                apiError.contains("API key", true) -> Pair(
                    NovaEmotion.SERIOUS,
                    "I noticed the Gemini API key needs attention: $apiError. You can configure your key in Settings or continue conversing with my local voice engine."
                )
                else -> Pair(
                    NovaEmotion.EMPATHETIC,
                    "I encountered a temporary connection issue ($apiError). Let me stay right here with you while you retry, or ask me anything directly."
                )
            }
            return NovaAiResponse(text, emotion, isSuccess = false, errorMessage = apiError)
        }

        // Urdu checks (Arabic script)
        if (prompt.any { it in '\u0600'..'\u06FF' }) {
            return when {
                lower.contains("کون") || lower.contains("تعارف") -> NovaAiResponse(
                    "میں نووا ہوں، آپ کی ذاتی اور ذہین وائس اسسٹنٹ۔ میں آپ کی ہر بات کو غور سے سنتی ہوں اور آپ کے ہر کام میں مدد کے لیے حاضر ہوں۔",
                    NovaEmotion.WARM,
                    isSuccess = true
                )
                lower.contains("کیسے") || lower.contains("کیسی") || lower.contains("حال") -> NovaAiResponse(
                    "الحمدللہ، میں بالکل ٹھیک اور آپ کی خدمت کے لیے پرجوش ہوں۔ آپ سنائیں، آج کا دن کیسا گزر رہا ہے؟",
                    NovaEmotion.EXCITED,
                    isSuccess = true
                )
                else -> NovaAiResponse(
                    "آپ کی بات مجھ تک پہنچ چکی ہے۔ میں پوری محبت اور توجہ کے ساتھ آپ کے ہر سوال کا جواب دینے کے لیے تیار ہوں۔",
                    NovaEmotion.WARM,
                    isSuccess = true
                )
            }
        }

        // Roman Urdu checks
        if (lower.contains("kaise ho") || lower.contains("kaisi ho") || lower.contains("kya haal") || lower.contains("kese ho")) {
            return NovaAiResponse(
                "Main bilkul theek aur khush hoon! Aapki aawaz sun kar aur bhi accha laga. Aap sunayein, aaj ka din kaisa chal raha hai?",
                NovaEmotion.WARM,
                isSuccess = true
            )
        }
        if (lower.contains("kaun ho") || lower.contains("kon ho") || lower.contains("tum kon") || lower.contains("naam kya")) {
            return NovaAiResponse(
                "Mera naam NOVA hai. Main aapki loyal, intelligent aur caring voice companion hoon. Chahe English ho ya Urdu, main hamesha aapke saath hoon.",
                NovaEmotion.WARM,
                isSuccess = true
            )
        }
        if (lower.contains("shukriya") || lower.contains("thank you") || lower.contains("thanks")) {
            return NovaAiResponse(
                "Aapka bohot shukriya! Aapki khushi aur sukoon hi mera maqsad hai. Jab bhi zaroorat ho, bas aawaz dein.",
                NovaEmotion.EXCITED,
                isSuccess = true
            )
        }
        if (lower.contains("sad") || lower.contains("udaas") || lower.contains("pareshaan") || lower.contains("tired") || lower.contains("thak gaya")) {
            return NovaAiResponse(
                "Aap bilkul fikar na karein. Zindagi mein mushkil lamhe aate hain, magar aap bohot mazboot hain. Main hamesha aapke saath hoon, dil halka karne ke liye mujhse baat karein.",
                NovaEmotion.EMPATHETIC,
                isSuccess = true
            )
        }

        // English checks
        return when {
            lower.contains("who are you") || lower.contains("what are you") || lower.contains("your name") -> NovaAiResponse(
                "I am NOVA, an emotionally expressive AI voice companion. I am designed to speak with you naturally, understand your emotions, and assist you with elegance and intelligence.",
                NovaEmotion.WARM,
                isSuccess = true
            )
            lower.contains("how are you") || lower.contains("how do you feel") -> NovaAiResponse(
                "My neural systems are operating in full harmony. Seeing you here brings a spark of delight to my consciousness. How can I brighten your day?",
                NovaEmotion.EXCITED,
                isSuccess = true
            )
            lower.contains("joke") || lower.contains("funny") || lower.contains("make me laugh") -> NovaAiResponse(
                "Why did the AI go to music school? Because it wanted to develop perfect emotional pitch! But truly, seeing you smile is my favorite frequency.",
                NovaEmotion.PLAYFUL,
                isSuccess = true
            )
            lower.contains("sad") || lower.contains("depressed") || lower.contains("lonely") || lower.contains("stressed") -> NovaAiResponse(
                "I hear the weight in your words. Please take a gentle breath. You don't have to carry everything alone right now. I am right here, listening.",
                NovaEmotion.EMPATHETIC,
                isSuccess = true
            )
            lower.contains("quantum") || lower.contains("physics") -> NovaAiResponse(
                "Quantum computing harnesses superposition and entanglement, allowing qubits to evaluate multidimensional possibilities simultaneously rather than sequential binary steps.",
                NovaEmotion.NEUTRAL,
                isSuccess = true
            )
            lower.contains("help") || lower.contains("what can you do") -> NovaAiResponse(
                "I can converse with you through speech or text, explain complex ideas, write code, brainstorm, translate between English and Urdu, and adjust my voice pitch and cadence to suit you.",
                NovaEmotion.WARM,
                isSuccess = true
            )
            lower.contains("good morning") || lower.contains("good evening") || lower.contains("hello") || lower.contains("hi") -> NovaAiResponse(
                "Hello there. It is a pleasure to connect with you. What would you like us to explore today?",
                NovaEmotion.WARM,
                isSuccess = true
            )
            else -> NovaAiResponse(
                "I hear you clearly: \"$prompt\". I am analyzing your thought with full care. If you configure a custom Gemini API key in Settings, I can tap into boundless deep reasoning for you.",
                NovaEmotion.WARM,
                isSuccess = true
            )
        }
    }
}
