package com.internmatch.app.ui.search

/**
 * Immutable UI state representing active search and filter parameters.
 */
data class SearchFilterUiState(
    val query: String = "",
    val domain: String = "",
    val district: String = "",
    val workMode: String = "",
    val minMatchScore: Double = 0.0
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || domain.isNotBlank() || district.isNotBlank() || workMode.isNotBlank() || minMatchScore > 0.0
}
