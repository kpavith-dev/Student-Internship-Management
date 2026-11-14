package com.internmatch.app.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.internmatch.app.domain.model.Internship
import com.internmatch.app.ui.search.InternshipItemUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InternshipCard(
    itemUiState: InternshipItemUiState,
    onToggleBookmark: (String) -> Unit,
    onApply: ((Internship) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val internship = itemUiState.internship

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = internship.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = internship.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (itemUiState.matchScore != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "${itemUiState.matchScore.toInt()}% Match",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (internship.location.isNotBlank() || internship.district.isNotBlank()) {
                val locText = listOfNotNull(
                    internship.location.ifBlank { null },
                    internship.district.ifBlank { null }
                ).joinToString(", ")
                Text(
                    text = "📍 $locText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (internship.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = internship.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (internship.domain.isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(internship.domain) }
                        )
                    }
                    if (internship.workMode.isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(internship.workMode) }
                        )
                    }
                    if (internship.employmentType.isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(internship.employmentType) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onApply != null) {
                        androidx.compose.material3.Button(
                            onClick = { onApply(internship) }
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { onToggleBookmark(internship.id) }
                    ) {
                        Text(
                            text = if (itemUiState.isBookmarked) "★ Saved" else "☆ Save",
                            fontWeight = if (itemUiState.isBookmarked) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
