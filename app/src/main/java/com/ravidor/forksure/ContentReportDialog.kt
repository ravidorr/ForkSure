package com.ravidor.forksure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun ContentReportDialog(
    content: String,
    onDismiss: () -> Unit,
    onReportSubmitted: (ContentReportingHelper.ContentReport) -> Unit
) {
    val context = LocalContext.current
    var selectedReason by remember { mutableStateOf(ContentReportingHelper.ReportReason.INAPPROPRIATE) }
    var additionalDetails by remember { mutableStateOf("") }
    
    val reportReasons = listOf(
        ContentReportingHelper.ReportReason.INAPPROPRIATE,
        ContentReportingHelper.ReportReason.OFFENSIVE,
        ContentReportingHelper.ReportReason.HARMFUL,
        ContentReportingHelper.ReportReason.OTHER
    )
    
    val dialogTitle = stringResource(R.string.report_dialog_title)
    val dialogMessage = stringResource(R.string.report_dialog_message)
    val additionalDetailsLabel = stringResource(R.string.report_additional_details)
    val submitButtonText = stringResource(R.string.action_submit_report)
    val cancelButtonText = stringResource(R.string.action_cancel)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics {
                    contentDescription = "Report content dialog title"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Content reporting form"
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = dialogMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics {
                        contentDescription = "Report instructions"
                    }
                )
                
                // Report reason selection
                Text(
                    text = "Reason for reporting:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.semantics {
                        contentDescription = "Select reason for reporting section"
                    }
                )
                
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .semantics {
                            contentDescription = "Report reason options"
                        }
                ) {
                    reportReasons.forEach { reason ->
                        val reasonDisplayName = ContentReportingHelper.getReasonDisplayName(context, reason)
                        val isSelected = selectedReason == reason
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    onClick = { 
                                        selectedReason = reason
                                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                                    }
                                )
                                .padding(vertical = 4.dp)
                                .semantics {
                                    contentDescription = if (isSelected) {
                                        "$reasonDisplayName, selected"
                                    } else {
                                        "$reasonDisplayName, not selected"
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null, // handled by selectable modifier
                                modifier = Modifier.semantics {
                                    contentDescription = "" // Handled by Row
                                }
                            )
                            Text(
                                text = reasonDisplayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .semantics {
                                        contentDescription = "" // Handled by Row
                                    }
                            )
                        }
                    }
                }
                
                // Additional details field
                OutlinedTextField(
                    value = additionalDetails,
                    onValueChange = { additionalDetails = it },
                    label = { 
                        Text(
                            additionalDetailsLabel,
                            modifier = Modifier.semantics {
                                contentDescription = "" // Handled by TextField
                            }
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Additional details text field. Optional"
                        },
                    maxLines = 3,
                    singleLine = false,
                    placeholder = {
                        Text(
                            "Optional: Provide additional details about your report",
                            modifier = Modifier.semantics {
                                contentDescription = "" // Handled by TextField
                            }
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val report = ContentReportingHelper.ContentReport(
                        content = content,
                        reason = selectedReason,
                        additionalDetails = additionalDetails.trim()
                    )
                    onReportSubmitted(report)
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                },
                modifier = Modifier.semantics {
                    contentDescription = "Submit report button"
                }
            ) {
                Text(submitButtonText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                },
                modifier = Modifier.semantics {
                    contentDescription = "Cancel button"
                }
            ) {
                Text(cancelButtonText)
            }
        },
        modifier = Modifier.semantics {
            contentDescription = "Content report dialog"
        }
    )
}

