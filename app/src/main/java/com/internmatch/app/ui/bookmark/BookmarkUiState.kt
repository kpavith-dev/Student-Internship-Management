package com.internmatch.app.ui.bookmark

import com.internmatch.app.ui.search.InternshipItemUiState

/**
 * Immutable state for the Student Bookmarks Screen.
 */
data class BookmarkUiState(
    val isLoading: Boolean = false,
    val bookmarkedInternships: List<InternshipItemUiState> = emptyList(),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && bookmarkedInternships.isEmpty() && errorMessage == null
}
