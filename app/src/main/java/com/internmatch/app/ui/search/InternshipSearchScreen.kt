package com.internmatch.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.internmatch.app.ui.search.components.InternshipCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InternshipSearchScreen(
    uiState: InternshipSearchUiState,
    onQueryChange: (String) -> Unit,
    onDomainChange: (String) -> Unit,
    onDistrictChange: (String) -> Unit,
    onWorkModeChange: (String) -> Unit,
    onMinScoreChange: (Double) -> Unit,
    onClearFilters: () -> Unit,
    onToggleBookmark: (String) -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
    onApply: ((com.internmatch.app.domain.model.Internship) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var domainExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.filterState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by title, company, or keyword...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filters Section
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Domain Dropdown
                ExposedDropdownMenuBox(
                    expanded = domainExpanded,
                    onExpandedChange = { domainExpanded = !domainExpanded }
                ) {
                    OutlinedTextField(
                        value = if (uiState.filterState.domain.isBlank()) "All Domains" else uiState.filterState.domain,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = domainExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(0.48f),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(
                        expanded = domainExpanded,
                        onDismissRequest = { domainExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Domains") },
                            onClick = {
                                onDomainChange("")
                                domainExpanded = false
                            }
                        )
                        uiState.availableDomains.forEach { domain ->
                            DropdownMenuItem(
                                text = { Text(domain) },
                                onClick = {
                                    onDomainChange(domain)
                                    domainExpanded = false
                                }
                            )
                        }
                    }
                }

                // District Dropdown
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded }
                ) {
                    OutlinedTextField(
                        value = if (uiState.filterState.district.isBlank()) "All Districts" else uiState.filterState.district,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Districts") },
                            onClick = {
                                onDistrictChange("")
                                districtExpanded = false
                            }
                        )
                        uiState.availableDistricts.forEach { district ->
                            DropdownMenuItem(
                                text = { Text(district) },
                                onClick = {
                                    onDistrictChange(district)
                                    districtExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Work Mode Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                FilterChip(
                    selected = uiState.filterState.workMode.isBlank(),
                    onClick = { onWorkModeChange("") },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) }
                )
                uiState.availableWorkModes.forEach { mode ->
                    FilterChip(
                        selected = uiState.filterState.workMode.equals(mode, ignoreCase = true),
                        onClick = { onWorkModeChange(mode) },
                        label = { Text(mode, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Min Score Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Min Score:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                listOf(0.0, 25.0, 50.0, 75.0).forEach { score ->
                    val labelText = if (score == 0.0) "Any" else "${score.toInt()}%+"
                    FilterChip(
                        selected = uiState.filterState.minMatchScore == score,
                        onClick = { onMinScoreChange(score) },
                        label = { Text(labelText, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (uiState.filterState.hasActiveFilters) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClearFilters) {
                        Text("Clear All Filters")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Internship Results List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No matching internships found.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting your search keywords or active filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.internships,
                        key = { it.internship.id }
                    ) { item ->
                        InternshipCard(
                            itemUiState = item,
                            onToggleBookmark = onToggleBookmark,
                            onApply = onApply
                        )
                    }
                }
            }
        }

        if (uiState.errorMessage != null && !uiState.isLoading && uiState.internships.isNotEmpty()) {
            Snackbar(
                action = { TextButton(onClick = onClearError) { Text("Dismiss") } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(uiState.errorMessage)
            }
        }
    }
}
