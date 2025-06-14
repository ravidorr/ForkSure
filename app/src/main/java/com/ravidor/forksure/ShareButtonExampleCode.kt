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
 * Example code showing how to add share functionality to your existing RecipeResultsSection
 * Copy the relevant parts into your MainScreen.kt and BakingScreen.kt files
 */

// Add this to your existing action buttons row in RecipeResultsSection:

/*
// In your RecipeResultsSection, replace the action buttons row with this:
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = Dimensions.PADDING_STANDARD),
    horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL, Alignment.CenterHorizontally)
) {
    // Share button - NEW!
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
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        ),
        modifier = Modifier.semantics {
            contentDescription = context.getString(R.string.accessibility_share_button)
        }
    ) {
        Text(stringResource(R.string.action_share))
    }
    
    // Print button - EXISTING
    Button(
        onClick = { 
            onPrintRecipe()
            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
        },
        modifier = Modifier.semantics {
            contentDescription = context.getString(R.string.accessibility_print_button)
        }
    ) {
        Text(stringResource(R.string.action_print))
    }
    
    // Report button - EXISTING
    Button(
        onClick = { 
            onReportContent()
            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        ),
        modifier = Modifier.semantics {
            contentDescription = context.getString(R.string.accessibility_report_button)
        }
    ) {
        Text(stringResource(R.string.action_report_content))
    }
}
*/

// Alternative: More advanced share button with dropdown options
@Composable
fun AdvancedShareButton(
    outputText: String,
    modifier: Modifier = Modifier
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
            DropdownMenuItem(
                text = { Text("📝 Save to Google Keep") },
                onClick = {
                    showShareMenu = false
                    val recipeTitle = PrintHelper.extractRecipeTitle(outputText)
                    RecipeSharingHelper.shareRecipeToApp(
                        context = context,
                        recipeContent = outputText,
                        recipeName = recipeTitle,
                        targetApp = ShareTarget.GOOGLE_KEEP
                    )
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                }
            )
            
            // Share to any app
            DropdownMenuItem(
                text = { Text("📤 Share to...") },
                onClick = {
                    showShareMenu = false
                    val recipeTitle = PrintHelper.extractRecipeTitle(outputText)
                    RecipeSharingHelper.shareRecipeToKeep(
                        context = context,
                        recipeContent = outputText,
                        recipeName = recipeTitle
                    )
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                }
            )
            
            // Share to Gmail
            DropdownMenuItem(
                text = { Text("✉️ Email Recipe") },
                onClick = {
                    showShareMenu = false
                    val recipeTitle = PrintHelper.extractRecipeTitle(outputText)
                    RecipeSharingHelper.shareRecipeToApp(
                        context = context,
                        recipeContent = outputText,
                        recipeName = recipeTitle,
                        targetApp = ShareTarget.GMAIL
                    )
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                }
            )
        }
    }
}

// Don't forget to add this import to your MainScreen.kt and BakingScreen.kt:
// import com.ravidor.forksure.RecipeSharingHelper
// import com.ravidor.forksure.ShareTarget 