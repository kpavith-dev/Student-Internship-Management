package com.internmatch.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.internmatch.app.domain.model.ProficiencyLevel
import com.internmatch.app.domain.model.Skill
import com.internmatch.app.domain.model.StudentProfile
import com.internmatch.app.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    uiState: StudentProfileUiState,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onSaveProfile: (
        regNum: String,
        university: String,
        degree: String,
        year: Int,
        semester: Int,
        gpa: Double?,
        bio: String,
        phone: String,
        linkedin: String,
        github: String,
        portfolio: String
    ) -> Unit,
    onAddSkillProficiency: (skillId: String, level: ProficiencyLevel) -> Unit,
    onUpdateSkillProficiency: (skillId: String, level: ProficiencyLevel) -> Unit,
    onRemoveSkillProficiency: (skillId: String) -> Unit,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSkillDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            onClearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { success ->
            snackbarHostState.showSnackbar(success)
            onClearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (uiState.profile != null && !uiState.isEditing) {
                        TextButton(onClick = onStartEditing) {
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.profile == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.isEditing || uiState.isCreatingProfile -> {
                    EditProfileContent(
                        profile = uiState.profile,
                        validationErrors = uiState.validationErrors,
                        isLoading = uiState.isLoading,
                        onCancel = onCancelEditing,
                        onSave = onSaveProfile,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.profile != null -> {
                    ProfileDetailsContent(
                        profile = uiState.profile,
                        user = uiState.currentUser,
                        availableSkills = uiState.availableSkills,
                        onOpenAddSkillDialog = { showAddSkillDialog = true },
                        onUpdateSkillProficiency = onUpdateSkillProficiency,
                        onRemoveSkill = onRemoveSkillProficiency,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    EmptyProfileContent(
                        user = uiState.currentUser,
                        onCreateProfile = onStartEditing,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (uiState.isLoading && uiState.profile != null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(24.dp)
                )
            }
        }
    }

    if (showAddSkillDialog) {
        AddSkillDialog(
            availableSkills = uiState.availableSkills,
            existingSkillIds = uiState.profile?.skillProficiencies?.map { it.skillId } ?: emptyList(),
            onDismiss = { showAddSkillDialog = false },
            onAddSkill = { skillId, level ->
                onAddSkillProficiency(skillId, level)
                showAddSkillDialog = false
            }
        )
    }
}

@Composable
private fun ProfileDetailsContent(
    profile: StudentProfile,
    user: User?,
    availableSkills: List<Skill>,
    onOpenAddSkillDialog: () -> Unit,
    onUpdateSkillProficiency: (skillId: String, level: ProficiencyLevel) -> Unit,
    onRemoveSkill: (skillId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Personal Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (user?.displayName?.firstOrNull() ?: 'S').uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = user?.displayName ?: "Student",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profile.registrationNumber.isNotBlank()) {
                        Text(
                            text = "Reg No: ${profile.registrationNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Academic Details Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Academic Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                InfoRow(label = "University", value = profile.university.ifBlank { "Not set" })
                InfoRow(label = "Degree Programme", value = profile.degreeProgram.ifBlank { "Not set" })
                InfoRow(label = "Academic Year", value = "Year ${profile.academicYear}")
                InfoRow(label = "Semester", value = "Semester ${profile.semester}")
                if (profile.gpa != null) {
                    InfoRow(label = "GPA", value = String.format("%.2f", profile.gpa))
                }
                if (profile.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Biography",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Contact Information Card
        profile.contactDetails?.let { contact ->
            if (contact.phone.isNotBlank() || contact.linkedinUrl.isNotBlank() ||
                contact.githubUrl.isNotBlank() || contact.portfolioUrl.isNotBlank()
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Contact Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (contact.phone.isNotBlank()) InfoRow(label = "Phone", value = contact.phone)
                        if (contact.linkedinUrl.isNotBlank()) InfoRow(label = "LinkedIn", value = contact.linkedinUrl)
                        if (contact.githubUrl.isNotBlank()) InfoRow(label = "GitHub", value = contact.githubUrl)
                        if (contact.portfolioUrl.isNotBlank()) InfoRow(label = "Portfolio", value = contact.portfolioUrl)
                    }
                }
            }
        }

        // Skills & Proficiency Section
        SkillsProficiencyCard(
            skillProficiencies = profile.skillProficiencies,
            availableSkills = availableSkills,
            onOpenAddSkillDialog = onOpenAddSkillDialog,
            onUpdateProficiency = onUpdateSkillProficiency,
            onRemoveSkill = onRemoveSkill
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsProficiencyCard(
    skillProficiencies: List<com.internmatch.app.domain.model.SkillProficiency>,
    availableSkills: List<Skill>,
    onOpenAddSkillDialog: () -> Unit,
    onUpdateProficiency: (skillId: String, level: ProficiencyLevel) -> Unit,
    onRemoveSkill: (skillId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Skills & Proficiencies",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = onOpenAddSkillDialog,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("+ Add Skill")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (skillProficiencies.isEmpty()) {
                Text(
                    text = "No skills selected yet. Tap '+ Add Skill' to add your proficiencies.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    skillProficiencies.forEach { sp ->
                        val skillName = availableSkills.find { it.id == sp.skillId }?.name ?: sp.skillId
                        SkillProficiencyChip(
                            skillName = skillName,
                            level = sp.level,
                            onLevelChange = { newLevel -> onUpdateProficiency(sp.skillId, newLevel) },
                            onRemove = { onRemoveSkill(sp.skillId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillProficiencyChip(
    skillName: String,
    level: ProficiencyLevel,
    onLevelChange: (ProficiencyLevel) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    AssistChip(
        onClick = { expanded = true },
        label = {
            Text("$skillName: ${level.name}")
        },
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(18.dp)
            ) {
                Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = when (level) {
                ProficiencyLevel.EXPERT -> MaterialTheme.colorScheme.primaryContainer
                ProficiencyLevel.ADVANCED -> MaterialTheme.colorScheme.secondaryContainer
                ProficiencyLevel.INTERMEDIATE -> MaterialTheme.colorScheme.tertiaryContainer
                ProficiencyLevel.BEGINNER -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    )

    if (expanded) {
        ProficiencyMenu(
            currentLevel = level,
            onLevelSelected = { newLevel ->
                onLevelChange(newLevel)
                expanded = false
            },
            onDismiss = { expanded = false }
        )
    }
}

@Composable
private fun ProficiencyMenu(
    currentLevel: ProficiencyLevel,
    onLevelSelected: (ProficiencyLevel) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        ProficiencyLevel.entries.forEach { level ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = level.name,
                        fontWeight = if (level == currentLevel) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onLevelSelected(level) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSkillDialog(
    availableSkills: List<Skill>,
    existingSkillIds: List<String>,
    onDismiss: () -> Unit,
    onAddSkill: (skillId: String, level: ProficiencyLevel) -> Unit
) {
    val selectableSkills = availableSkills.filterNot { existingSkillIds.contains(it.id) }
    var selectedSkill by remember { mutableStateOf(selectableSkills.firstOrNull()) }
    var selectedLevel by remember { mutableStateOf(ProficiencyLevel.INTERMEDIATE) }
    var skillDropdownExpanded by remember { mutableStateOf(false) }
    var levelDropdownExpanded by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Skill Proficiency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (selectableSkills.isEmpty()) {
                    Text("All catalogue skills have already been added.")
                } else {
                    // Skill Dropdown
                    ExposedDropdownMenuBox(
                        expanded = skillDropdownExpanded,
                        onExpandedChange = { skillDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSkill?.name ?: "Select Skill",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Skill") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = skillDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = skillDropdownExpanded,
                            onDismissRequest = { skillDropdownExpanded = false }
                        ) {
                            selectableSkills.forEach { skill ->
                                DropdownMenuItem(
                                    text = { Text(skill.name) },
                                    onClick = {
                                        selectedSkill = skill
                                        skillDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Level Dropdown
                    ExposedDropdownMenuBox(
                        expanded = levelDropdownExpanded,
                        onExpandedChange = { levelDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLevel.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Proficiency Level") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = levelDropdownExpanded,
                            onDismissRequest = { levelDropdownExpanded = false }
                        ) {
                            ProficiencyLevel.entries.forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level.name) },
                                    onClick = {
                                        selectedLevel = level
                                        levelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedSkill != null,
                onClick = {
                    selectedSkill?.let { skill ->
                        onAddSkill(skill.id, selectedLevel)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditProfileContent(
    profile: StudentProfile?,
    validationErrors: Map<String, String>,
    isLoading: Boolean,
    onCancel: () -> Unit,
    onSave: (
        regNum: String,
        university: String,
        degree: String,
        year: Int,
        semester: Int,
        gpa: Double?,
        bio: String,
        phone: String,
        linkedin: String,
        github: String,
        portfolio: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var regNum by remember { mutableStateOf(profile?.registrationNumber ?: "") }
    var university by remember { mutableStateOf(profile?.university ?: "") }
    var degreeProgram by remember { mutableStateOf(profile?.degreeProgram ?: "") }
    var academicYearText by remember { mutableStateOf(profile?.academicYear?.toString() ?: "1") }
    var semesterText by remember { mutableStateOf(profile?.semester?.toString() ?: "1") }
    var gpaText by remember { mutableStateOf(profile?.gpa?.toString() ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }

    var phone by remember { mutableStateOf(profile?.contactDetails?.phone ?: "") }
    var linkedinUrl by remember { mutableStateOf(profile?.contactDetails?.linkedinUrl ?: "") }
    var githubUrl by remember { mutableStateOf(profile?.contactDetails?.githubUrl ?: "") }
    var portfolioUrl by remember { mutableStateOf(profile?.contactDetails?.portfolioUrl ?: "") }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (profile == null) "Create Profile" else "Edit Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = regNum,
            onValueChange = { regNum = it },
            label = { Text("Registration Number *") },
            isError = validationErrors.containsKey("registrationNumber"),
            supportingText = validationErrors["registrationNumber"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = university,
            onValueChange = { university = it },
            label = { Text("University *") },
            isError = validationErrors.containsKey("university"),
            supportingText = validationErrors["university"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = degreeProgram,
            onValueChange = { degreeProgram = it },
            label = { Text("Degree Programme *") },
            isError = validationErrors.containsKey("degreeProgram"),
            supportingText = validationErrors["degreeProgram"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = academicYearText,
                onValueChange = { academicYearText = it },
                label = { Text("Year (1-6)") },
                isError = validationErrors.containsKey("academicYear"),
                supportingText = validationErrors["academicYear"]?.let { { Text(it) } },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = semesterText,
                onValueChange = { semesterText = it },
                label = { Text("Semester (1-12)") },
                isError = validationErrors.containsKey("semester"),
                supportingText = validationErrors["semester"]?.let { { Text(it) } },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = gpaText,
            onValueChange = { gpaText = it },
            label = { Text("GPA (Optional, 0.0 - 4.0)") },
            isError = validationErrors.containsKey("gpa"),
            supportingText = validationErrors["gpa"]?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Biography / Short Bio") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Contact Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = linkedinUrl,
            onValueChange = { linkedinUrl = it },
            label = { Text("LinkedIn URL") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = githubUrl,
            onValueChange = { githubUrl = it },
            label = { Text("GitHub Profile URL") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = portfolioUrl,
            onValueChange = { portfolioUrl = it },
            label = { Text("Portfolio Website") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (profile != null) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Button(
                enabled = !isLoading,
                onClick = {
                    val year = academicYearText.toIntOrNull() ?: 1
                    val sem = semesterText.toIntOrNull() ?: 1
                    val gpaVal = gpaText.toDoubleOrNull()

                    onSave(
                        regNum,
                        university,
                        degreeProgram,
                        year,
                        sem,
                        gpaVal,
                        bio,
                        phone,
                        linkedinUrl,
                        githubUrl,
                        portfolioUrl
                    )
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save Profile")
            }
        }
    }
}

@Composable
private fun EmptyProfileContent(
    user: User?,
    onCreateProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "🎓",
                    fontSize = 36.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome, ${user?.displayName ?: "Student"}!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You have not set up your student profile yet. Create your profile to showcase your academic details and skill proficiencies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onCreateProfile) {
            Text("+ Create Student Profile")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
