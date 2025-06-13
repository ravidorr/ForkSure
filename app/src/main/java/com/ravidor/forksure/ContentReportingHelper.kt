package com.ravidor.forksure

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class to handle user reports for AI-generated content
 * This satisfies Google Play's AI-Generated Content policy requirement
 */
class ContentReportingHelper {
    
    data class ContentReport(
        val content: String,
        val reason: ReportReason,
        val additionalDetails: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )
    
    enum class ReportReason {
        INAPPROPRIATE,
        OFFENSIVE,
        HARMFUL,
        OTHER
    }
    
    companion object {
        private const val TAG = "ContentReporting"
        private const val DEVELOPER_EMAIL = "ravidor@gmail.com"
        
        /**
         * Submit a content report via email
         */
        suspend fun submitReport(
            context: Context,
            report: ContentReport
        ): Result<Unit> = withContext(Dispatchers.Main) {
            try {
                val emailIntent = createEmailIntent(context, report)
                context.startActivity(Intent.createChooser(emailIntent, "Send Report via Email"))
                
                // Log the report for development purposes
                Log.i(TAG, "Content report initiated: ${report.reason} - ${report.content.take(100)}")
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initiate content report email", e)
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

