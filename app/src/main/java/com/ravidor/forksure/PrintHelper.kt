package com.ravidor.forksure

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintJob
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import androidx.compose.runtime.Stable
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for printing recipes using Android's printing framework
 * Converts markdown recipe content to HTML and handles print job creation
 */
@Stable
object PrintHelper {
    private const val TAG = "PrintHelper"
    
    /**
     * Print a recipe by converting markdown to HTML and using WebView printing
     */
    fun printRecipe(
        context: Context,
        recipeContent: String,
        recipeName: String = "ForkSure Recipe"
    ): PrintJob? {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            
            // Create a WebView for printing
            val webView = WebView(context)
            
            // Convert markdown to HTML
            val htmlContent = convertRecipeToHtml(recipeContent, recipeName)
            
            // Load HTML content
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            
            // Create print adapter
            val printAdapter = webView.createPrintDocumentAdapter(generateJobName(recipeName))
            
            // Create print attributes
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                .setResolution(PrintAttributes.Resolution("default", "default", 300, 300))
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
            
            // Start print job
            val printJob = printManager.print(
                generateJobName(recipeName),
                printAdapter,
                printAttributes
            )
            
            Log.d(TAG, "Print job started: ${printJob.info.label}")
            printJob
            
        } catch (e: Exception) {
            Log.e(TAG, "Error printing recipe", e)
            null
        }
    }
    
    /**
     * Convert markdown recipe content to formatted HTML for printing
     */
    fun convertRecipeToHtml(markdownContent: String, recipeName: String): String {
        val timestamp = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date())
        
        // Basic markdown to HTML conversion
        var htmlContent = markdownContent
            .replace("# ", "<h1>")
            .replace("## ", "<h2>")
            .replace("### ", "<h3>")
            .replace("**", "<strong>", ignoreCase = true)
            .replace("*", "<em>", ignoreCase = true)
            .replace("\n- ", "<li>")
            .replace("\n• ", "<li>")
            .replace("\n\n", "</p><p>")
        
        // Wrap list items in ul tags - simplified
        if (htmlContent.contains("<li>")) {
            htmlContent = htmlContent.replace("<li>", "<ul><li>")
            htmlContent = htmlContent.replace("</p><p>", "</ul></p><p>")
        }
        
        // Simple tag closing - just ensure basic tags are closed
        val lines = htmlContent.split("\n")
        val processedLines = lines.map { line ->
            when {
                line.contains("<h1>") && !line.contains("</h1>") -> "$line</h1>"
                line.contains("<h2>") && !line.contains("</h2>") -> "$line</h2>"
                line.contains("<h3>") && !line.contains("</h3>") -> "$line</h3>"
                line.contains("<strong>") && !line.contains("</strong>") -> "$line</strong>"
                line.contains("<em>") && !line.contains("</em>") -> "$line</em>"
                else -> line
            }
        }
        htmlContent = processedLines.joinToString("\n")
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>$recipeName</title>
                <style>
                    body {
                        font-family: 'Times New Roman', serif;
                        font-size: 12pt;
                        line-height: 1.6;
                        margin: 0.5in;
                        color: #333;
                    }
                    h1 {
                        color: #2c3e50;
                        border-bottom: 2px solid #3498db;
                        padding-bottom: 10px;
                        margin-bottom: 20px;
                        font-size: 18pt;
                    }
                    h2 {
                        color: #34495e;
                        margin-top: 25px;
                        margin-bottom: 15px;
                        font-size: 14pt;
                    }
                    h3 {
                        color: #7f8c8d;
                        margin-top: 20px;
                        margin-bottom: 10px;
                        font-size: 12pt;
                    }
                    ul {
                        margin: 10px 0;
                        padding-left: 20px;
                    }
                    li {
                        margin-bottom: 5px;
                    }
                    p {
                        margin: 10px 0;
                        text-align: justify;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        border-bottom: 1px solid #bdc3c7;
                        padding-bottom: 15px;
                    }
                    .footer {
                        margin-top: 30px;
                        padding-top: 15px;
                        border-top: 1px solid #bdc3c7;
                        text-align: center;
                        font-size: 10pt;
                        color: #7f8c8d;
                    }
                    .recipe-content {
                        margin: 20px 0;
                    }
                    strong {
                        color: #2c3e50;
                    }
                    @media print {
                        body {
                            margin: 0.5in;
                        }
                        .no-print {
                            display: none;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>$recipeName</h1>
                    <p>Generated by ForkSure - AI-Powered Recipe Assistant</p>
                </div>
                
                <div class="recipe-content">
                    <p>$htmlContent</p>
                </div>
                
                <div class="footer">
                    <p>Printed on $timestamp</p>
                    <p>ForkSure App - Bake with Confidence</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Generate a unique job name for the print job
     */
    fun generateJobName(recipeName: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanName = recipeName.replace(Regex("[^a-zA-Z0-9\\s]"), "").take(20)
        return "ForkSure_${cleanName}_$timestamp"
    }
    
    /**
     * Check if printing is available on the device
     */
    fun isPrintingAvailable(context: Context): Boolean {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            printManager != null
        } catch (e: Exception) {
            Log.w(TAG, "Error checking print availability", e)
            false
        }
    }
    
    /**
     * Extract recipe title from markdown content
     */
    fun extractRecipeTitle(markdownContent: String): String {
        return try {
            // Look for first heading or use first line
            val lines = markdownContent.split("\n")
            val titleLine = lines.find { it.startsWith("#") || it.startsWith("**") }
                ?: lines.firstOrNull { it.isNotBlank() }
                ?: "Recipe"
            
            // Clean up the title
            titleLine.replace(Regex("^#+\\s*"), "")
                .replace("**", "")
                .replace("*", "")
                .trim()
                .take(50)
                .ifEmpty { "ForkSure Recipe" }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting recipe title", e)
            "ForkSure Recipe"
        }
    }
} 