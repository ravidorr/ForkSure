package com.ravidor.forksure.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ravidor.forksure.AccessibilityHelper
import com.ravidor.forksure.Dimensions
import com.ravidor.forksure.HapticFeedbackType
import com.ravidor.forksure.R

/**
 * Layout and general UI components for the main screen
 * Contains header, instructional text, and action button components
 */

@Composable
fun MainScreenHeader() {
    Text(
        text = stringResource(R.string.baking_title),
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(Dimensions.PADDING_STANDARD)
            .semantics {
                contentDescription = "ForkSure - Baking with AI, main heading"
            }
    )
}

@Composable
fun InstructionalTextSection() {
    val welcomeText = stringResource(R.string.results_placeholder)
    val welcomeMessageDescription = stringResource(R.string.accessibility_welcome_message, welcomeText)
    Text(
        text = welcomeText,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.PADDING_STANDARD, vertical = Dimensions.PADDING_MEDIUM)
            .semantics {
                contentDescription = welcomeMessageDescription
            }
    )
}

@Composable
fun AnalyzeButtonSection(
    isAnalyzeEnabled: Boolean,
    onAnalyzeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val enabledDescription = stringResource(R.string.accessibility_analyze_button_enabled)
    val disabledDescription = stringResource(R.string.accessibility_analyze_button_disabled)
    val actionGoText = stringResource(R.string.action_go)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = Dimensions.PADDING_STANDARD),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                onAnalyzeClick()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            enabled = isAnalyzeEnabled,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(64.dp)
                .semantics {
                    contentDescription = if (isAnalyzeEnabled) {
                        enabledDescription
                    } else {
                        disabledDescription
                    }
                }
        ) {
            Text(
                text = actionGoText,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
} 