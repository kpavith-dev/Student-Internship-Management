package com.internmatch.app.ui.search

import com.internmatch.app.domain.model.Internship

/**
 * Single internship item wrapped with student-specific UI metadata (bookmark state & match score).
 */
data class InternshipItemUiState(
    val internship: Internship,
    val isBookmarked: Boolean = false,
    val matchScore: Double? = null
)

/**
 * Immutable state for the Internship Search & Filters Screen.
 */
data class InternshipSearchUiState(
    val isLoading: Boolean = false,
    val internships: List<InternshipItemUiState> = emptyList(),
    val filterState: SearchFilterUiState = SearchFilterUiState(),
    val availableDomains: List<String> = emptyList(),
    val availableDistricts: List<String> = emptyList(),
    val availableWorkModes: List<String> = listOf("ON_SITE", "HYBRID", "REMOTE"),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && internships.isEmpty() && errorMessage == null
}
