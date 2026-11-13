package com.internmatch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.internmatch.app.core.di.AuthViewModelFactory
import com.internmatch.app.core.di.ProfileViewModelFactory
import com.internmatch.app.ui.auth.AuthScreen
import com.internmatch.app.ui.auth.AuthViewModel
import com.internmatch.app.ui.profile.StudentProfileScreen
import com.internmatch.app.ui.profile.StudentProfileUiState
import com.internmatch.app.ui.profile.StudentProfileViewModel
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

        setContent {
            InternMatchTheme {
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
                val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()

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
                        profileUiState = profileUiState
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
    profileUiState: StudentProfileUiState
) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            StudentProfileScreen(
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
}
