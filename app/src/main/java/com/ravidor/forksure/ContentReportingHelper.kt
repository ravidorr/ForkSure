package com.ravidor.forksure

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Centralized constants imports
import com.ravidor.forksure.AppConstants

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Helper class to handle user reports for AI-generated content
 * This satisfies Google Play's AI-Generated Content policy requirement
 */
@Stable
class ContentReportingHelper {
    
    @Immutable
    data class ContentReport(
        val content: String,
        val reason: ReportReason,
        val additionalDetails: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )
    
    @Stable
    enum class ReportReason {
        INAPPROPRIATE,
        OFFENSIVE,
        HARMFUL,
        OTHER
    }
    
    companion object {
        private const val TAG = AppConstants.TAG_CONTENT_REPORTING
        private const val DEVELOPER_EMAIL = AppConstants.DEVELOPER_EMAIL
        
        /**
         * Submit a content report via webhook (no email client required)
         */
        suspend fun submitReport(
            context: Context,
            report: ContentReport
        ): Result<Unit> = withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🚩 REPORT BUTTON CLICKED - Starting content report submission")
                Log.d(TAG, "Report details: reason=${report.reason}, contentLength=${report.content.length}, hasAdditionalDetails=${report.additionalDetails.isNotBlank()}")
                
                // Try webhook first (silent reporting)
                val webhookResult = submitReportViaWebhook(context, report)
                if (webhookResult.isSuccess) {
                    Log.i(TAG, "✅ Content report sent successfully via webhook")
                    return@withContext Result.success(Unit)
                }
                
                // Fallback to email if webhook fails
                Log.w(TAG, "⚠️ Webhook failed, falling back to email")
                return@withContext submitReportViaEmail(context, report)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to submit content report", e)
                Result.failure(e)
            }
        }
        
        /**
         * Submit report via webhook (silent, no UI)
         */
        private suspend fun submitReportViaWebhook(
            context: Context,
            report: ContentReport
        ): Result<Unit> = withContext(Dispatchers.IO) {
            try {
                // Using Make.com webhook for silent content reporting
                val webhookUrl = "https://hook.eu2.make.com/m6bfqyz9x7thlxis94fj9lte5t7bmvi1"
                
                Log.d(TAG, "🌐 WEBHOOK CALL INITIATED")
                Log.d(TAG, "Webhook URL: $webhookUrl")
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(report.timestamp))
                
                val jsonReport = JSONObject().apply {
                    put("app", "ForkSure")
                    put("timestamp", formattedDate)
                    put("reason", getReasonDisplayName(context, report.reason))
                    put("content", report.content)
                    put("additional_details", report.additionalDetails)
                    put("app_version", getAppVersion(context))
                    put("device_info", "${android.os.Build.MODEL} (${android.os.Build.VERSION.RELEASE})")
                }
                
                Log.d(TAG, "📤 Request payload: ${jsonReport.toString(2)}")
                
                val url = URL(webhookUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "ForkSure-Android/${getAppVersion(context)}")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                
                Log.d(TAG, "📡 Sending HTTP POST request...")
                Log.d(TAG, "Request headers: Content-Type=application/json, Accept=application/json")
                
                // Send the JSON data
                val startTime = System.currentTimeMillis()
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonReport.toString())
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                val requestTime = System.currentTimeMillis() - startTime
                
                Log.d(TAG, "📥 WEBHOOK RESPONSE RECEIVED")
                Log.d(TAG, "Response code: $responseCode")
                Log.d(TAG, "Response message: $responseMessage")
                Log.d(TAG, "Request time: ${requestTime}ms")
                
                // Read response body
                val responseBody = try {
                    if (responseCode in 200..299) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read response body", e)
                    "Failed to read response body: ${e.message}"
                }
                
                Log.d(TAG, "Response body: $responseBody")
                
                if (responseCode in 200..299) {
                    Log.i(TAG, "✅ Webhook report sent successfully (HTTP $responseCode)")
                    Result.success(Unit)
                } else {
                    Log.w(TAG, "⚠️ Webhook returned error code: $responseCode - $responseMessage")
                    Log.w(TAG, "Error response: $responseBody")
                    Result.failure(Exception("Webhook failed with code: $responseCode - $responseMessage"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Webhook submission failed with exception", e)
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Exception message: ${e.message}")
                Result.failure(e)
            }
        }
        
        /**
         * Submit a content report via email (fallback method)
         */
        private suspend fun submitReportViaEmail(
            context: Context,
            report: ContentReport
        ): Result<Unit> = withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "📧 FALLBACK TO EMAIL - Creating email intent")
                val emailIntent = createEmailIntent(context, report)
                context.startActivity(Intent.createChooser(emailIntent, "Send Report via Email"))
                
                Log.i(TAG, "✅ Content report initiated via email fallback")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initiate content report email", e)
                Result.failure(e)
            }
        }
        
        /**
         * Create email intent with report details
         */
        private fun createEmailIntent(context: Context, report: ContentReport): Intent {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(report.timestamp))
            
            val subject = "ForkSure App - Content Report - ${getReasonDisplayName(context, report.reason)}"
            
            val emailBody = buildString {
                appendLine("Content Report for ForkSure App")
                appendLine("=====================================")
                appendLine()
                appendLine("Report Date: $formattedDate")
                appendLine("Report Reason: ${getReasonDisplayName(context, report.reason)}")
                appendLine()
                appendLine("Reported Content:")
                appendLine("─────────────────")
                appendLine(report.content)
                appendLine()
                if (report.additionalDetails.isNotBlank()) {
                    appendLine("Additional Details:")
                    appendLine("──────────────────")
                    appendLine(report.additionalDetails)
                    appendLine()
                }
                appendLine("App Version: ${getAppVersion(context)}")
                appendLine("Device Info: ${android.os.Build.MODEL} (${android.os.Build.VERSION.RELEASE})")
                appendLine()
                appendLine("This report was generated automatically by the ForkSure app's")
                appendLine("content reporting feature to comply with Google Play's")
                appendLine("AI-Generated Content policy.")
            }
            
            Log.d(TAG, "Email subject: $subject")
            Log.d(TAG, "Email recipient: $DEVELOPER_EMAIL")
            
            return Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
            }
        }
        
        /**
         * Get app version for reporting
         */
        private fun getAppVersion(context: Context): String {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                "${packageInfo.versionName} (${packageInfo.longVersionCode})"
            } catch (e: Exception) {
                "Unknown"
            }
        }
        
        /**
         * Get localized string for report reason
         */
        fun getReasonDisplayName(context: Context, reason: ReportReason): String {
            return when (reason) {
                ReportReason.INAPPROPRIATE -> context.getString(R.string.report_reason_inappropriate)
                ReportReason.OFFENSIVE -> context.getString(R.string.report_reason_offensive)
                ReportReason.HARMFUL -> context.getString(R.string.report_reason_harmful)
                ReportReason.OTHER -> context.getString(R.string.report_reason_other)
            }
        }
    }
}

