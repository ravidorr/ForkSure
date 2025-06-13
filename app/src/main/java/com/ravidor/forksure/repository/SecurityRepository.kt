package com.ravidor.forksure.repository

import android.content.Context
import android.content.SharedPreferences
import com.ravidor.forksure.RateLimitResult
import com.ravidor.forksure.SecurityEnvironmentResult
import com.ravidor.forksure.SecurityManager
import com.ravidor.forksure.UserInputValidationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for security-related operations
 */
interface SecurityRepository {
    suspend fun checkRateLimit(identifier: String): RateLimitResult
    suspend fun checkSecurityEnvironment(): SecurityEnvironmentResult
    suspend fun validateUserInput(input: String): UserInputValidationResult
}

/**
 * Implementation of SecurityRepository using SecurityManager
 */
@Singleton
class SecurityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences
) : SecurityRepository {

    override suspend fun checkRateLimit(identifier: String): RateLimitResult {
        return SecurityManager.checkRateLimit(context, identifier)
    }

    override suspend fun checkSecurityEnvironment(): SecurityEnvironmentResult {
        return SecurityManager.checkSecurityEnvironment(context)
    }

    override suspend fun validateUserInput(input: String): UserInputValidationResult {
        val validationResult = SecurityManager.validateInput(input)
        return when (validationResult) {
            is com.ravidor.forksure.InputValidationResult.Valid -> {
                UserInputValidationResult.Valid(validationResult.sanitizedInput)
            }
            is com.ravidor.forksure.InputValidationResult.Invalid -> {
                UserInputValidationResult.Invalid(validationResult.reason)
            }
        }
    }
} 