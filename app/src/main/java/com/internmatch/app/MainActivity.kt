package com.internmatch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.internmatch.app.core.di.ApplicationViewModelFactory
import com.internmatch.app.core.di.AuthViewModelFactory
import com.internmatch.app.core.di.ProfileViewModelFactory
import com.internmatch.app.core.di.SearchViewModelFactory
import com.internmatch.app.domain.model.Internship
import com.internmatch.app.ui.application.ApplicationDetailDialog
import com.internmatch.app.ui.application.ApplicationDetailsUiState
import com.internmatch.app.ui.application.ApplicationSubmissionUiState
import com.internmatch.app.ui.application.ApplicationViewModel
import com.internmatch.app.ui.application.StudentApplicationsScreen
import com.internmatch.app.ui.application.StudentApplicationsUiState
import com.internmatch.app.ui.auth.AuthScreen
import com.internmatch.app.ui.auth.AuthViewModel
import com.internmatch.app.ui.bookmark.BookmarkScreen
import com.internmatch.app.ui.bookmark.BookmarkUiState
import com.internmatch.app.ui.bookmark.BookmarkViewModel
import com.internmatch.app.ui.profile.StudentProfileScreen
import com.internmatch.app.ui.profile.StudentProfileUiState
import com.internmatch.app.ui.profile.StudentProfileViewModel
import com.internmatch.app.ui.search.InternshipSearchScreen
import com.internmatch.app.ui.search.InternshipSearchUiState
import com.internmatch.app.ui.search.InternshipSearchViewModel
import com.internmatch.app.ui.search.components.ApplyDialog
import com.internmatch.app.ui.theme.InternMatchTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as InternMatchApplication).container

        val authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(appContainer.authRepository)
        )[AuthViewModel::class.java]

        val profileViewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(
                profileRepository = appContainer.studentProfileRepository,
                authRepository = appContainer.authRepository,
                skillRepository = appContainer.skillRepository
            )
        )[StudentProfileViewModel::class.java]

        val searchViewModelFactory = SearchViewModelFactory(
            internshipRepository = appContainer.internshipRepository,
            bookmarkRepository = appContainer.bookmarkRepository,
            studentProfileRepository = appContainer.studentProfileRepository,
            authRepository = appContainer.authRepository,
            matchScoreProvider = appContainer.matchScoreProvider
        )

        val searchViewModel = ViewModelProvider(this, searchViewModelFactory)[InternshipSearchViewModel::class.java]
        val bookmarkViewModel = ViewModelProvider(this, searchViewModelFactory)[BookmarkViewModel::class.java]

        val applicationViewModel = ViewModelProvider(
            this,
            ApplicationViewModelFactory(
                applicationRepository = appContainer.applicationRepository,
                authRepository = appContainer.authRepository
            )
        )[ApplicationViewModel::class.java]

        setContent {
            InternMatchTheme {
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
                val searchUiState by searchViewModel.uiState.collectAsStateWithLifecycle()
                val bookmarkUiState by bookmarkViewModel.uiState.collectAsStateWithLifecycle()

                val submissionUiState by applicationViewModel.submissionState.collectAsStateWithLifecycle()
                val studentApplicationsUiState by applicationViewModel.applicationsState.collectAsStateWithLifecycle()
                val detailsUiState by applicationViewModel.detailsState.collectAsStateWithLifecycle()

                if (authUiState.currentUser == null) {
                    AuthScreen(
                        uiState = authUiState,
                        onSignIn = authViewModel::signIn,
                        onSignUp = authViewModel::signUp,
                        onResetPassword = authViewModel::sendPasswordReset,
                        onClearError = authViewModel::clearError,
                        onClearSuccess = authViewModel::clearSuccess,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MainAppContent(
                        authViewModel = authViewModel,
                        profileViewModel = profileViewModel,
                        profileUiState = profileUiState,
                        searchViewModel = searchViewModel,
                        searchUiState = searchUiState,
                        bookmarkViewModel = bookmarkViewModel,
                        bookmarkUiState = bookmarkUiState,
                        applicationViewModel = applicationViewModel,
                        submissionUiState = submissionUiState,
                        studentApplicationsUiState = studentApplicationsUiState,
                        detailsUiState = detailsUiState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppContent(
    authViewModel: AuthViewModel,
    profileViewModel: StudentProfileViewModel,
    profileUiState: StudentProfileUiState,
    searchViewModel: InternshipSearchViewModel,
    searchUiState: InternshipSearchUiState,
    bookmarkViewModel: BookmarkViewModel,
    bookmarkUiState: BookmarkUiState,
    applicationViewModel: ApplicationViewModel,
    submissionUiState: ApplicationSubmissionUiState,
    studentApplicationsUiState: StudentApplicationsUiState,
    detailsUiState: ApplicationDetailsUiState
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var applyingInternship by remember { mutableStateOf<Internship?>(null) }
    var selectedApplicationId by remember { mutableStateOf<String?>(null) }

    val tabs = listOf("Search", "Bookmarks", "Applications", "Profile")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("InternMatch") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(onClick = authViewModel::signOut) {
                        Text("Sign Out", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index == 0) searchViewModel.loadData()
                            if (index == 1) bookmarkViewModel.loadBookmarks()
                            if (index == 2) applicationViewModel.observeStudentApplications()
                        },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> InternshipSearchScreen(
                        uiState = searchUiState,
                        onQueryChange = searchViewModel::updateQuery,
                        onDomainChange = searchViewModel::updateDomain,
                        onDistrictChange = searchViewModel::updateDistrict,
                        onWorkModeChange = searchViewModel::updateWorkMode,
                        onMinScoreChange = searchViewModel::updateMinMatchScore,
                        onClearFilters = searchViewModel::clearFilters,
                        onToggleBookmark = searchViewModel::toggleBookmark,
                        onRetry = searchViewModel::loadData,
                        onClearError = searchViewModel::clearError,
                        onApply = { internship ->
                            applicationViewModel.resetSubmissionState()
                            applyingInternship = internship
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> BookmarkScreen(
                        uiState = bookmarkUiState,
                        onRemoveBookmark = bookmarkViewModel::removeBookmark,
                        onRetry = bookmarkViewModel::loadBookmarks,
                        onClearError = bookmarkViewModel::clearError,
                        onApply = { internship ->
                            applicationViewModel.resetSubmissionState()
                            applyingInternship = internship
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> StudentApplicationsScreen(
                        uiState = studentApplicationsUiState,
                        onSelectApplication = { appId ->
                            selectedApplicationId = appId
                            applicationViewModel.loadApplicationDetails(appId)
                        },
                        onRetry = applicationViewModel::observeStudentApplications,
                        modifier = Modifier.fillMaxSize()
                    )
                    3 -> StudentProfileScreen(
                        uiState = profileUiState,
                        onStartEditing = profileViewModel::startEditing,
                        onCancelEditing = profileViewModel::cancelEditing,
                        onSaveProfile = profileViewModel::saveProfile,
                        onAddSkillProficiency = profileViewModel::addSkillProficiency,
                        onUpdateSkillProficiency = profileViewModel::updateSkillProficiency,
                        onRemoveSkillProficiency = profileViewModel::removeSkillProficiency,
                        onClearError = profileViewModel::clearError,
                        onClearSuccess = profileViewModel::clearSuccess,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (applyingInternship != null) {
                ApplyDialog(
                    internship = applyingInternship!!,
                    submissionState = submissionUiState,
                    onSubmitApplication = { internshipId, coverLetter ->
                        applicationViewModel.submitApplication(internshipId, coverLetter)
                    },
                    onDismiss = {
                        applyingInternship = null
                        applicationViewModel.resetSubmissionState()
                    }
                )
            }

            if (selectedApplicationId != null) {
                ApplicationDetailDialog(
                    uiState = detailsUiState,
                    onWithdraw = { appId ->
                        applicationViewModel.withdrawApplication(appId)
                    },
                    onDismiss = { selectedApplicationId = null }
                )
            }
        }
    }
}
