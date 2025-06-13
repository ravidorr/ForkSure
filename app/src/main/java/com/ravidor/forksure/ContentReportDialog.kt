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
import androidx.compose.ui.semantics.Role
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.report_dialog_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.report_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Report reason selection
                Text(
                    text = "Reason for reporting:",
                    style = MaterialTheme.typography.labelLarge
                )
                
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    reportReasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedReason == reason),
                                    onClick = { selectedReason = reason },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedReason == reason),
                                onClick = null // handled by selectable modifier
                            )
                            Text(
                                text = ContentReportingHelper.getReasonDisplayName(context, reason),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                // Additional details field
                OutlinedTextField(
                    value = additionalDetails,
                    onValueChange = { additionalDetails = it },
                    label = { Text(stringResource(R.string.report_additional_details)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    singleLine = false
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
                }
            ) {
                Text(stringResource(R.string.action_submit_report))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

