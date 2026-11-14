# InternMatch – Day 7 Application Management Documentation

## Document Metadata

| Attribute | Details |
| :--- | :--- |
| **Project Name** | InternMatch |
| **Course** | EC5208 – Mobile Application Development |
| **Package Name** | `com.internmatch.app` |
| **Module Architecture** | Single Gradle Module (`:app`), Manual Dependency Injection |
| **Tech Stack** | Kotlin, Jetpack Compose, Material 3, MVVM, Repository Pattern, Firebase Auth, Cloud Firestore |
| **Branch** | `feature/day-7-applications` |
| **Base Commit** | `e66c269d3a93be072637ca27ff0d2fd95152fef9` |
| **Status** | **IMPLEMENTED & UNIT TESTED** |

---

## 1. Day 7 Objectives

Day 7 establishes the complete domain foundation, data architecture, security rules, ViewModel state management, and Jetpack Compose user experience for Student Application Workflows (FR18, FR19, and FR21):

1. **FR18 — Application Submission**: Enable authenticated students to apply for approved internships in 3 taps or fewer, capturing an immutable snapshot of student qualifications and the calculated match score.
2. **FR19 — Application Tracking & Audit History**: Provide students with real-time status tracking and a full chronological audit timeline of application state transitions.
3. **FR21 — Application Withdrawal**: Allow authenticated students to withdraw active applications prior to a final decision (`ACCEPTED` or `REJECTED`), updating application status atomically and logging the event in the audit trail.

---

## 2. Shared Contract Alignment & Status

| Requirement / Component | Implementation Status | Verification Label |
| :--- | :--- | :--- |
| **Application Domain Model** | `com.internmatch.app.domain.model.Application` | **IMPLEMENTED / UNIT TESTED** |
| **Profile Snapshot** | `com.internmatch.app.domain.model.StudentProfileSnapshot` | **IMPLEMENTED / UNIT TESTED** |
| **Status Audit History** | `com.internmatch.app.domain.model.ApplicationStatusHistory` | **IMPLEMENTED / UNIT TESTED** |
| **Application Status Enum** | Reused `com.internmatch.app.domain.model.ApplicationStatus` | **VERIFIED** |
| **Application Repository Contract** | `com.internmatch.app.domain.repository.ApplicationRepository` | **IMPLEMENTED** |
| **Firestore Client & Repository** | `FirestoreApplicationRepository`, `DefaultFirebaseApplicationClient` | **IMPLEMENTED / UNIT TESTED** |
| **Deterministic Duplicate Prevention** | Doc ID `${internshipId}_${studentId}` + Transaction check | **IMPLEMENTED / UNIT TESTED** |
| **Real Match Score Integration** | `DefaultMatchScoreProvider` score captured at submission | **IMPLEMENTED / UNIT TESTED** |
| **ViewModel State Management** | `ApplicationViewModel`, `ApplicationViewModelFactory` | **IMPLEMENTED / UNIT TESTED** |
| **Compose UI Workflows** | `StudentApplicationsScreen`, `ApplyDialog`, `ApplicationDetailDialog` | **IMPLEMENTED / VERIFIED** |
| **Firestore Security Rules** | `firestore.rules` updated for `/applications/{id}` & `/history` | **IMPLEMENTED / VERIFIED** |

---

## 3. Core Technical Architecture & Design

### 3.1 Domain Models
- **`Application`**: Contains application metadata (`id`, `internshipId`, `studentId`, `companyName`, `internshipTitle`, `status`, `submittedAt`, `updatedAt`, `matchScore`, `coverLetter`, `resumeUrl`, `profileSnapshot`). Includes computed property `isWithdrawable` returning true for `SUBMITTED`, `UNDER_REVIEW`, and `SHORTLISTED`.
- **`StudentProfileSnapshot`**: Typed, immutable snapshot capturing student profile attributes (academic details, GPA, bio, contact details, skill proficiencies, projects, certificates) at the exact moment of application submission.
- **`ApplicationStatusHistory`**: Represents chronological status audit entries with `status`, `timestamp`, `actorUid`, and `note`.

### 3.2 Firestore Path & Transaction Design
- **Application Path**: `/applications/{applicationId}` where `applicationId = "${internshipId}_${studentId}"`.
- **History Subcollection Path**: `/applications/{applicationId}/history/{historyId}`.
- **Atomic Submissions**: `FirebaseApplicationClient.submitApplicationTransaction()` uses `firestore.runTransaction` to atomically create the application document and initial `SUBMITTED` history event. If document already exists, throws `ApplicationException.AlreadyApplied`.
- **Atomic Withdrawal**: `FirebaseApplicationClient.withdrawApplicationTransaction()` reads the application document inside a transaction, verifies ownership and eligibility (`status in ['SUBMITTED', 'UNDER_REVIEW', 'SHORTLISTED']`), updates status to `WITHDRAWN`, and appends a `WITHDRAWN` history event atomically.

### 3.3 Security Rules (`firestore.rules`)
```rules
match /applications/{applicationId} {
  allow read: if request.auth != null && (
    resource.data.studentId == request.auth.uid ||
    resource.data.providerId == request.auth.uid
  );

  allow create: if request.auth != null &&
    request.resource.data.studentId == request.auth.uid &&
    request.resource.data.status == 'SUBMITTED';

  allow update: if request.auth != null &&
    resource.data.studentId == request.auth.uid &&
    resource.data.status in ['SUBMITTED', 'UNDER_REVIEW', 'SHORTLISTED'] &&
    request.resource.data.status == 'WITHDRAWN';

  allow delete: if false;

  match /history/{historyId} {
    allow read: if request.auth != null;
    allow create: if request.auth != null &&
      request.resource.data.actorUid == request.auth.uid;
    allow update, delete: if false;
  }
}
```

---

## 4. Test Results & Verification

### 4.1 Automated Unit Tests
Executed via Gradle task `.\gradlew.bat testDebugUnitTest`:

```
BUILD SUCCESSFUL in 1s
26 actionable tasks: 26 up-to-date
```

Key Unit Test Suites:
1. `com.internmatch.app.domain.model.ApplicationTest`: 4/4 PASSED
2. `com.internmatch.app.data.repository.FirestoreApplicationRepositoryTest`: 9/9 PASSED
3. `com.internmatch.app.ui.application.ApplicationViewModelTest`: 6/6 PASSED
4. `com.internmatch.app.data.repository.FirestoreStudentProfileRepositoryTest`: 63/63 PASSED
5. `com.internmatch.app.data.repository.FirebaseAuthRepositoryTest`: 17/17 PASSED
6. `com.internmatch.app.data.repository.FirestoreBookmarkRepositoryTest`: 26/26 PASSED

### 4.2 Build Verification
Executed via Gradle task `.\gradlew.bat assembleDebug`:

```
BUILD SUCCESSFUL in 6s
37 actionable tasks: 3 executed, 34 up-to-date
```

### 4.3 Git Check
Executed `git diff --check`: Clean (Zero whitespace warnings).

---

## 5. Summary of Changed Files

- `app/src/main/java/com/internmatch/app/domain/model/Application.kt` [NEW]
- `app/src/main/java/com/internmatch/app/domain/model/StudentProfileSnapshot.kt` [NEW]
- `app/src/main/java/com/internmatch/app/domain/model/ApplicationStatusHistory.kt` [NEW]
- `app/src/main/java/com/internmatch/app/domain/model/ApplicationException.kt` [NEW]
- `app/src/main/java/com/internmatch/app/domain/repository/ApplicationRepository.kt` [NEW]
- `app/src/main/java/com/internmatch/app/data/repository/FirebaseApplicationClient.kt` [NEW]
- `app/src/main/java/com/internmatch/app/data/repository/DefaultFirebaseApplicationClient.kt` [NEW]
- `app/src/main/java/com/internmatch/app/data/repository/FirestoreApplicationRepository.kt` [NEW]
- `app/src/main/java/com/internmatch/app/core/di/ApplicationViewModelFactory.kt` [NEW]
- `app/src/main/java/com/internmatch/app/core/di/AppContainer.kt` [MODIFY]
- `app/src/main/java/com/internmatch/app/ui/application/ApplicationUiState.kt` [NEW]
- `app/src/main/java/com/internmatch/app/ui/application/ApplicationViewModel.kt` [NEW]
- `app/src/main/java/com/internmatch/app/ui/application/StudentApplicationsScreen.kt` [NEW]
- `app/src/main/java/com/internmatch/app/ui/application/ApplicationDetailDialog.kt` [NEW]
- `app/src/main/java/com/internmatch/app/ui/search/components/ApplyDialog.kt` [NEW]
- `app/src/main/java/com/internmatch/app/ui/search/components/InternshipCard.kt` [MODIFY]
- `app/src/main/java/com/internmatch/app/ui/search/InternshipSearchScreen.kt` [MODIFY]
- `app/src/main/java/com/internmatch/app/ui/bookmark/BookmarkScreen.kt` [MODIFY]
- `app/src/main/java/com/internmatch/app/MainActivity.kt` [MODIFY]
- `firestore.rules` [MODIFY]
- `app/src/test/java/com/internmatch/app/domain/model/ApplicationTest.kt` [NEW]
- `app/src/test/java/com/internmatch/app/data/repository/FirestoreApplicationRepositoryTest.kt` [NEW]
- `app/src/test/java/com/internmatch/app/ui/application/ApplicationViewModelTest.kt` [NEW]
- `docs/DAY_07_APPLICATION_MANAGEMENT.md` [NEW]

---

## 6. Suggested Git Commits

1. `feat(domain): define application models, exceptions and repository contract`
2. `feat(data): implement firestore application repository with atomic transactions`
3. `feat(viewmodel): implement student application state management and factories`
4. `feat(ui): add student application screens, apply modal, and detail audit timeline`
5. `security(rules): enforce student application privacy, creation and withdrawal security rules`
6. `test(application): add domain, repository and viewmodel test suites`
7. `docs(application): add Day 7 application management documentation`

---

## 7. Pull Request Instructions

Create Pull Request:
- **Title**: `feat(applications): Day 7 student application submission, tracking, and withdrawal`
- **Head Branch**: `feature/day-7-applications`
- **Base Branch**: `main-dev`
- **Summary**: Implements FR18, FR19, and FR21 student application workflows with immutable profile snapshots, atomic Firestore transactions, chronological status history audit timelines, strict security rules, ViewModels, Compose UI screens, and comprehensive unit tests.
