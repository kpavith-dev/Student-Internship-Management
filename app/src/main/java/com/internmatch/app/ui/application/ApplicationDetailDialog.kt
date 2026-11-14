package com.internmatch.app.ui.application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.internmatch.app.domain.model.ApplicationStatusHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApplicationDetailDialog(
    uiState: ApplicationDetailsUiState,
    onWithdraw: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWithdrawConfirmation by remember { mutableStateOf(false) }
    val app = uiState.application
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = app?.internshipTitle ?: "Application Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            } else if (app != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = app.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusBadge(status = app.status)
                    }

                    if (app.matchScore != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Match Score at Submission:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "${app.matchScore.toInt()}% Match",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Profile Snapshot Summary
                    app.profileSnapshot?.let { snapshot ->
                        Text(
                            text = "Profile Snapshot (Submitted)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (snapshot.university.isNotBlank()) {
                            Text("University: ${snapshot.university}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (snapshot.degreeProgram.isNotBlank()) {
                            Text("Degree: ${snapshot.degreeProgram}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (snapshot.gpa != null) {
                            Text("GPA: ${snapshot.gpa}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (snapshot.skillProficiencies.isNotEmpty()) {
                            Text(
                                "Skills: ${snapshot.skillProficiencies.joinToString { "${it.skillId} (${it.level})" }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Status History Timeline
                    Text(
                        text = "Status History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.history.isEmpty()) {
                        Text("No history recorded.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        uiState.history.forEach { historyEvent ->
                            HistoryEventItem(event = historyEvent, dateFormat = dateFormat)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text("Application information unavailable.")
            }
        },
        confirmButton = {
            if (app != null && app.isWithdrawable) {
                Button(
                    onClick = { showWithdrawConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !uiState.isWithdrawing
                ) {
                    if (uiState.isWithdrawing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onError)
                    } else {
                        Text("Withdraw Application")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = modifier
    )

    if (showWithdrawConfirmation && app != null) {
        AlertDialog(
            onDismissRequest = { showWithdrawConfirmation = false },
            title = { Text("Confirm Withdrawal") },
            text = {
                Text("Are you sure you want to withdraw your application for '${app.internshipTitle}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWithdrawConfirmation = false
                        onWithdraw(app.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Withdraw")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HistoryEventItem(
    event: ApplicationStatusHistory,
    dateFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusBadge(status = event.status)
                Text(
                    text = if (event.timestamp > 0L) dateFormat.format(Date(event.timestamp)) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (!event.note.isNull_or_blank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.note ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.isBlank()
}
