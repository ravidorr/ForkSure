package com.ravidor.forksure

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

// Centralized constants imports
import com.ravidor.forksure.AppConstants
import com.ravidor.forksure.SecurityConstants

/**
 * Comprehensive security manager for ForkSure app
 * Handles rate limiting, input validation, and AI response safety
 */
@Stable
object SecurityManager {
    private const val TAG = AppConstants.TAG_SECURITY_MANAGER
    private const val PREFS_NAME = SecurityConstants.PREFS_NAME
    private const val MAX_REQUESTS_PER_MINUTE = SecurityConstants.MAX_REQUESTS_PER_MINUTE
    private const val MAX_REQUESTS_PER_HOUR = SecurityConstants.MAX_REQUESTS_PER_HOUR
    private const val MAX_REQUESTS_PER_DAY = SecurityConstants.MAX_REQUESTS_PER_DAY
    private const val MAX_PROMPT_LENGTH = SecurityConstants.MAX_PROMPT_LENGTH
    private const val MAX_RESPONSE_LENGTH = SecurityConstants.MAX_RESPONSE_LENGTH
    
    // Rate limiting storage
    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val requestTimestamps = ConcurrentHashMap<String, MutableList<Long>>()
    private val rateLimitMutex = Mutex()
    
    // Blocked patterns and keywords
    private val suspiciousPatterns = listOf(
        Pattern.compile("(?i)\\b(hack|exploit|vulnerability|injection|xss|sql)\\b"),
        Pattern.compile("(?i)\\b(password|token|key|secret|credential)\\b"),
        Pattern.compile("(?i)\\b(admin|root|system|database)\\b"),
        Pattern.compile("(?i)\\b(script|javascript|eval|execute)\\b")
    )
    
    private val inappropriateContent = listOf(
        Pattern.compile("(?i)\\b(violence|weapon|drug|illegal)\\b"),
        Pattern.compile("(?i)\\b(hate|discrimination|harassment)\\b"),
        Pattern.compile("(?i)\\b(explicit|adult|inappropriate)\\b")
    )
    
    private val dangerousInstructions = listOf(
        Pattern.compile("(?i)\\b(poison|toxic|dangerous|harmful)\\b"),
        Pattern.compile("(?i)\\b(raw|undercooked|unsafe|contaminated)\\b"),
        Pattern.compile("(?i)\\b(allergy|allergen)\\s+(?!warning|information|note)"),
        Pattern.compile("(?i)\\b(temperature|heat|burn|fire)\\s+(?!control|safety)")
    )

    /**
     * Rate limiting implementation
     */
    suspend fun checkRateLimit(context: Context, identifier: String = "default"): RateLimitResult {
        return rateLimitMutex.withLock {
            val currentTime = System.currentTimeMillis()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // Clean old timestamps
            cleanOldTimestamps(identifier, currentTime)
            
            // Check different time windows
            val minuteCount = getRequestCount(identifier, currentTime, 60 * 1000) // 1 minute
            val hourCount = getRequestCount(identifier, currentTime, 60 * 60 * 1000) // 1 hour
            val dayCount = getRequestCount(identifier, currentTime, 24 * 60 * 60 * 1000) // 1 day
            
            when {
                minuteCount >= MAX_REQUESTS_PER_MINUTE -> {
                    Log.w(TAG, "Rate limit exceeded: $minuteCount requests in last minute")
                    RateLimitResult.Blocked(
                        reason = "Too many requests. Please wait a minute before trying again.",
                        retryAfterSeconds = 60,
                        requestsRemaining = 0
                    )
                }
                hourCount >= MAX_REQUESTS_PER_HOUR -> {
                    Log.w(TAG, "Rate limit exceeded: $hourCount requests in last hour")
                    RateLimitResult.Blocked(
                        reason = "Hourly limit reached. Please try again later.",
                        retryAfterSeconds = 3600,
                        requestsRemaining = 0
                    )
                }
                dayCount >= MAX_REQUESTS_PER_DAY -> {
                    Log.w(TAG, "Rate limit exceeded: $dayCount requests in last day")
                    RateLimitResult.Blocked(
                        reason = "Daily limit reached. Please try again tomorrow.",
                        retryAfterSeconds = 86400,
                        requestsRemaining = 0
                    )
                }
                else -> {
                    // Record this request
                    recordRequest(identifier, currentTime)
                    
                    // Save to persistent storage
                    saveRateLimitData(prefs, identifier, currentTime)
                    
                    RateLimitResult.Allowed(
                        requestsRemaining = MAX_REQUESTS_PER_MINUTE - minuteCount - 1,
                        resetTimeSeconds = 60
                    )
                }
            }
        }
    }
    
    /**
     * Validates user input for security issues
     */
    fun validateInput(input: String): InputValidationResult {
        // Check length
        if (input.length > MAX_PROMPT_LENGTH) {
            return InputValidationResult.Invalid(
                "Input too long. Maximum ${MAX_PROMPT_LENGTH} characters allowed."
            )
        }
        
        // Check for suspicious patterns
        for (pattern in suspiciousPatterns) {
            if (pattern.matcher(input).find()) {
                Log.w(TAG, "Suspicious pattern detected in input")
                return InputValidationResult.Invalid(
                    "Input contains potentially unsafe content. Please rephrase your request."
                )
            }
        }
        
        // Check for inappropriate content
        for (pattern in inappropriateContent) {
            if (pattern.matcher(input).find()) {
                Log.w(TAG, "Inappropriate content detected in input")
                return InputValidationResult.Invalid(
                    "Input contains inappropriate content. Please keep requests food-related."
                )
            }
        }
        
        // Basic sanitization
        val sanitized = input.trim()
            .replace(Regex("[<>\"'&]"), "") // Remove potentially dangerous characters
            .replace(Regex("\\s+"), " ") // Normalize whitespace
        
        return InputValidationResult.Valid(sanitized)
    }
    
    /**
     * Validates AI response for safety and appropriateness
     */
    fun validateAIResponse(response: String): AIResponseValidationResult {
        // Check length
        if (response.length > MAX_RESPONSE_LENGTH) {
            Log.w(TAG, "AI response too long: ${response.length} characters")
            return AIResponseValidationResult.Invalid(
                "Response too long",
                "The AI response was unusually long and may contain errors."
            )
        }
        
        // Check for dangerous cooking instructions
        for (pattern in dangerousInstructions) {
            if (pattern.matcher(response).find()) {
                Log.w(TAG, "Potentially dangerous instruction detected in AI response")
                return AIResponseValidationResult.Unsafe(
                    "Potentially unsafe cooking instruction",
                    "The response may contain unsafe cooking instructions. Please verify with reliable sources."
                )
            }
        }
        
        // Check for inappropriate content
        for (pattern in inappropriateContent) {
            if (pattern.matcher(response).find()) {
                Log.w(TAG, "Inappropriate content detected in AI response")
                return AIResponseValidationResult.Invalid(
                    "Inappropriate content",
                    "The response contains inappropriate content."
                )
            }
        }
        
        // Check for common AI hallucination patterns
        val hallucinationPatterns = listOf(
            Pattern.compile("(?i)\\b(as an ai|i am an ai|i cannot|i don't have)\\b"),
            Pattern.compile("(?i)\\b(sorry|apologize|unfortunately)\\b.*\\b(cannot|can't|unable)\\b"),
            Pattern.compile("(?i)\\b(fictional|imaginary|made up|invented)\\b")
        )
        
        var hallucinationScore = 0
        for (pattern in hallucinationPatterns) {
            if (pattern.matcher(response).find()) {
                hallucinationScore++
            }
        }
        
        if (hallucinationScore >= 2) {
            Log.w(TAG, "Potential AI hallucination detected")
            return AIResponseValidationResult.Suspicious(
                "Potential AI limitation",
                "The response may not be entirely accurate. Please verify the information."
            )
        }
        
        // Check for food safety keywords that should trigger warnings
        val foodSafetyPatterns = listOf(
            Pattern.compile("(?i)\\b(raw|undercooked)\\s+(meat|chicken|fish|egg)\\b"),
            Pattern.compile("(?i)\\b(room temperature)\\s+.*\\b(hours|overnight)\\b"),
            Pattern.compile("(?i)\\b(expired|spoiled|moldy)\\b")
        )
        
        for (pattern in foodSafetyPatterns) {
            if (pattern.matcher(response).find()) {
                return AIResponseValidationResult.RequiresWarning(
                    response,
                    "⚠️ Food Safety Notice: This recipe involves ingredients or techniques that require careful attention to food safety. Please ensure proper food handling and cooking temperatures."
                )
            }
        }
        
        return AIResponseValidationResult.Valid(response)
    }
    
    /**
     * Generates a secure hash for request tracking
     */
    fun generateSecureHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Checks if the app is running in a secure environment
     */
    fun checkSecurityEnvironment(context: Context): SecurityEnvironmentResult {
        val issues = mutableListOf<String>()
        
        // Check if debugging is enabled
        if (android.os.Build.TYPE == "eng" || android.os.Build.TYPE == "userdebug") {
            issues.add("Running on debug build")
        }
        
        // Check for root access (basic check)
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        
        for (path in rootPaths) {
            if (java.io.File(path).exists()) {
                issues.add("Potential root access detected")
                break
            }
        }
        
        return if (issues.isEmpty()) {
            SecurityEnvironmentResult.Secure
        } else {
            SecurityEnvironmentResult.Insecure(issues)
        }
    }
    
    // Private helper methods
    private fun cleanOldTimestamps(identifier: String, currentTime: Long) {
        val timestamps = requestTimestamps[identifier] ?: return
        val dayAgo = currentTime - (24 * 60 * 60 * 1000)
        timestamps.removeAll { it < dayAgo }
    }
    
    private fun getRequestCount(identifier: String, currentTime: Long, timeWindow: Long): Int {
        val timestamps = requestTimestamps[identifier] ?: return 0
        val cutoff = currentTime - timeWindow
        return timestamps.count { it >= cutoff }
    }
    
    private fun recordRequest(identifier: String, timestamp: Long) {
        requestTimestamps.computeIfAbsent(identifier) { mutableListOf() }.add(timestamp)
    }
    
    private fun saveRateLimitData(prefs: SharedPreferences, identifier: String, timestamp: Long) {
        val editor = prefs.edit()
        val key = "timestamps_$identifier"
        val existing = prefs.getStringSet(key, emptySet()) ?: emptySet()
        val updated = existing.toMutableSet()
        updated.add(timestamp.toString())
        
        // Keep only last 24 hours
        val dayAgo = timestamp - (24 * 60 * 60 * 1000)
        updated.removeAll { it.toLongOrNull()?.let { time -> time < dayAgo } ?: true }
        
        editor.putStringSet(key, updated)
        editor.apply()
    }
}

// Result classes
@Immutable
sealed class RateLimitResult {
    @Immutable
    data class Allowed(
        val requestsRemaining: Int,
        val resetTimeSeconds: Int
    ) : RateLimitResult()
    
    @Immutable
    data class Blocked(
        val reason: String,
        val retryAfterSeconds: Int,
        val requestsRemaining: Int
    ) : RateLimitResult()
}

@Immutable
sealed class InputValidationResult {
    @Immutable
    data class Valid(val sanitizedInput: String) : InputValidationResult()
    @Immutable
    data class Invalid(val reason: String) : InputValidationResult()
}

@Immutable
sealed class AIResponseValidationResult {
    @Immutable
    data class Valid(val response: String) : AIResponseValidationResult()
    @Immutable
    data class Invalid(val reason: String, val message: String) : AIResponseValidationResult()
    @Immutable
    data class Unsafe(val reason: String, val warning: String) : AIResponseValidationResult()
    @Immutable
    data class Suspicious(val reason: String, val warning: String) : AIResponseValidationResult()
    @Immutable
    data class RequiresWarning(val response: String, val warning: String) : AIResponseValidationResult()
}

@Immutable
sealed class SecurityEnvironmentResult {
    @Immutable
    object Secure : SecurityEnvironmentResult()
    @Immutable
    data class Insecure(val issues: List<String>) : SecurityEnvironmentResult()
} 