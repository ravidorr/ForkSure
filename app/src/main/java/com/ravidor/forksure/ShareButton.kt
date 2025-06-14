package com.ravidor.forksure

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Share button components for sharing recipes to external apps
 */

/**
 * Simple share button that opens Android's share chooser
 */
@Composable
fun ShareButton(
    outputText: String,
    modifier: Modifier = Modifier,
    onShareComplete: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    
    Button(
        onClick = { 
            val recipeTitle = PrintHelper.extractRecipeTitle(outputText)
            val shared = RecipeSharingHelper.shareRecipeToKeep(
                context = context,
                recipeContent = outputText,
                recipeName = recipeTitle
            )
            if (shared) {
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
            } else {
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
            }
            onShareComplete?.invoke(shared)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        ),
        modifier = modifier.semantics {
            contentDescription = context.getString(R.string.accessibility_share_button)
        }
    ) {
        Text(stringResource(R.string.action_share))
    }
}

/**
 * Advanced share button with dropdown menu for different sharing options
 */
@Composable
fun AdvancedShareButton(
    outputText: String,
    modifier: Modifier = Modifier,
    onShareComplete: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        // Main share button
        Button(
            onClick = { showShareMenu = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier.semantics {
                contentDescription = context.getString(R.string.accessibility_share_button)
            }
        ) {
            Text(stringResource(R.string.action_share))
        }
        
        // Dropdown menu with share options
        DropdownMenu(
            expanded = showShareMenu,
            onDismissRequest = { showShareMenu = false }
        ) {
            // Share to Google Keep
            if (RecipeSharingHelper.isAppAvailable(context, ShareTarget.GOOGLE_KEEP)) {
                DropdownMenuItem(
                    text = { Text("📝 Save to Google Keep") },
                    onClick = {
                        showShareMenu = false
                        val recipeTitle = PrintHelper.extractRecipeTitle(outputText)
                        val shared = RecipeSharingHelper.shareRecipeToApp(
                            context = context,
                            recipeContent = outputText,
                            recipeName = recipeTitle,
                            targetApp = ShareTarget.GOOGLE_KEEP
                        )
                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                        onShareComplete?.invoke(shared)
                    }
                )
            }
            
            // Share to Gmail
            if (RecipeSharingHelper.isAppAvailable(context, ShareTarget.GMAIL)) {
                DropdownMenuItem(
                    text = { Text("✉️ Email Recipe") },
                    onClick = {
                        showShareMenu = false
                        val recipeTitle = PrintHelper.extractRecipeTitle(outputText)
                        val shared = RecipeSharingHelper.shareRecipeToApp(
                            context = context,
                            recipeContent = outputText,
                            recipeName = recipeTitle,
                            targetApp = ShareTarget.GMAIL
                        )
                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                        onShareComplete?.invoke(shared)
                    }
                )
            }
            
            // Share to Google Docs
            if (RecipeSharingHelper.isAppAvailable(context, ShareTarget.GOOGLE_DOCS)) {
                DropdownMenuItem(
                    text = { Text("📄 Save to Google Docs") },
                    onClick = {
                        showShareMenu = false
                        val recipeTitle = PrintHelper.extractRecipeTitle(outputText)
                        val shared = RecipeSharingHelper.shareRecipeToApp(
                            context = context,
                            recipeContent = outputText,
                            recipeName = recipeTitle,
                            targetApp = ShareTarget.GOOGLE_DOCS
                        )
                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                        onShareComplete?.invoke(shared)
                    }
                )
            }
            
            // General share option (always available)
            DropdownMenuItem(
                text = { Text("📤 Share to other apps...") },
                onClick = {
                    showShareMenu = false
                    val recipeTitle = PrintHelper.extractRecipeTitle(outputText)
                    val shared = RecipeSharingHelper.shareRecipeToKeep(
                        context = context,
                        recipeContent = outputText,
                        recipeName = recipeTitle
                    )
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                    onShareComplete?.invoke(shared)
                }
            )
        }
    }
}

/**
 * Share button specifically for Google Keep with fallback to general share
 */
@Composable
fun ShareToKeepButton(
    outputText: String,
    modifier: Modifier = Modifier,
    onShareComplete: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    
    Button(
        onClick = { 
            val recipeTitle = PrintHelper.extractRecipeTitle(outputText)
            val shared = if (RecipeSharingHelper.isAppAvailable(context, ShareTarget.GOOGLE_KEEP)) {
                RecipeSharingHelper.shareRecipeToApp(
                    context = context,
                    recipeContent = outputText,
                    recipeName = recipeTitle,
                    targetApp = ShareTarget.GOOGLE_KEEP
                )
            } else {
                // Fallback to general share if Keep isn't available
                RecipeSharingHelper.shareRecipeToKeep(
                    context = context,
                    recipeContent = outputText,
                    recipeName = recipeTitle
                )
            }
            
            if (shared) {
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
            } else {
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
            }
            onShareComplete?.invoke(shared)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        ),
        modifier = modifier.semantics {
            contentDescription = context.getString(R.string.accessibility_share_to_keep_button)
        }
    ) {
        Text(stringResource(R.string.action_share_to_keep))
    }
} 