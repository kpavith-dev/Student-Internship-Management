# InternMatch – Shared Domain Contract

## Document Metadata

| Attribute | Details |
| :--- | :--- |
| **Project Name** | InternMatch |
| **Package Name** | `com.internmatch.app` |
| **Module Architecture** | Single Gradle Module (`:app`), Manual Dependency Injection (No Hilt / Koin) |
| **Tech Stack** | Kotlin, Jetpack Compose, Material 3, MVVM, Repository Pattern, Firebase Auth, Cloud Firestore, WorkManager |
| **Current Branch** | `Pavi-dev` |
| **Document Stage** | Day 2, Task 4 – Consolidated Shared Domain Contract (Technical Correction Pass) |
| **Target Document Path** | `docs/SHARED_DOMAIN_CONTRACT.md` |
| **Status** | **CONSOLIDATED BY PROJECT OWNER (PENDING TECHNICAL SPECIFICATIONS)** |

---

## 1. Document Purpose and Scope

This document serves as the **Shared Domain Contract** for the InternMatch Android application. It consolidates the eight architectural decisions accepted by the project owner during the Day 2 Team Contract Agreement.

### Scope and Governance
- **Single Source of Truth**: Establishes shared domain models, repository contracts, application workflow state machines, matching logic, Firestore structural layout, security principles, and team ownership split.
- **Strict Distinction of Status**:
  - `[ACCEPTED]`: Formally agreed architectural decisions.
  - `[PROPOSED]`: Baseline field definitions and structural recommendations subject to technical specification.
  - `[PENDING]`: Unresolved technical details requiring lower-level specification before coding.
- **Zero Hallucination Policy**: Consolidates accepted decisions without inventing unapproved features, modifying accepted workflows, or claiming unauthorized team sign-offs.

---

## 2. Accepted Architectural Decisions

### Accepted Decision 1 – User Account and Student Profile Separation `[ACCEPTED]`
- **Concept**: Authentication and generic account information are strictly separated from student-specific profile data.
- **Identity Model**: `User` represents generic authenticated account credentials across all user roles (`STUDENT`, `PROVIDER`, `ADMIN`).
- **Profile Extension**: `StudentProfile` contains specialized student data and applies exclusively to `STUDENT` account holders.
- **Role Immutability**: A user role must not be self-editable to `ADMIN`. Role changes must be managed securely via backend/admin mechanisms.

### Accepted Decision 2 – Skill Catalogue and Skill Proficiency `[ACCEPTED]`
- **Catalogue Centralization**: `Skill` instances belong to a central master catalogue managed strictly by Administrators.
- **Referential Integrity**: `SkillProficiency` references a skill via `skillId` rather than duplicating text strings.
- **Proficiency Levels**: Standardized enum values: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`.
- **Scoring Isolation**: Skill proficiency is stored as student profile metadata only. It does **NOT** influence or alter the core matching formula score.
- **Module Ownership**: Jeevithan owns master skill catalogue management and student skill selection features; Paviththiran integrates selected skill profiles into the student profile view.

### Accepted Decision 3 – Internship and Required Skills `[ACCEPTED]`
- **Internship Management**: Represents posted internship opportunities with metadata, requirements, application deadlines, and publication status.
- **Required Skill Definition**: Each `RequiredSkill` links a `skillId` from the master catalogue with an assigned numerical `weight`.
- **Equal Weight Allocation**: The system assigns equal weights to all required skills. Provider-configurable custom weights are **not currently approved**. (The exact numerical value, representation, and storage precision remain `[PENDING]`).
- **Visibility Restriction**: Only internships with `status = APPROVED` appear in student search results, filters, and recommendations.
- **Module Ownership**: Jeevithan owns internship posting, editing, and approval workflows. Paviththiran owns student search, filtering, and bookmarking.

### Accepted Decision 4 – Pure Kotlin Matching Formula `[ACCEPTED]`
- **Formula Definition**:
  $$\text{Match Score} = \left( \frac{\text{Sum of Matched Required Skill Weights}}{\text{Total Required Skill Weights}} \right) \times 100$$
- **Core Rules**:
  1. Only required skills specified on the internship are evaluated.
  2. A required skill is counted as "matched" if present in the student's selected skills list.
  3. Skill proficiency levels do **NOT** affect the match score calculation.
  4. Non-skill factors (GPA, location, degree program, preferred skills) do **NOT** contribute to the match score.
  5. The system assigns equal weights to all required skills. Provider-configurable custom weights are **not currently approved**. (The exact numerical value, representation, and storage precision remain `[PENDING]`).
  6. **Zero-Weight Rule**: No match score is calculated when total required-skill weight is zero. (The exact return representation in Kotlin code is `[PENDING]`).
  7. Implemented as a pure Kotlin domain service (`MatchingEngine`) with no Android or Firebase framework dependencies, making it 100% unit-testable.
- **Scoring Constraints**: No recommendation tiers, multipliers, or additional weighted factors may be added without formal agreement.

### Accepted Decision 5 – Application Workflow and State Transitions `[ACCEPTED]`
- **Allowed Statuses**: `SUBMITTED`, `UNDER_REVIEW`, `SHORTLISTED`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`.
- **Submission Uniqueness**: One student may submit only one application per internship.
- **Withdrawal Rules**: Students may withdraw an application prior to reaching a final decision (`ACCEPTED` or `REJECTED`).
- **Resubmission Policy**: The resubmission policy after `WITHDRAWN` remains `[PENDING]`. No automatic replacement or resubmission should be implemented until this policy is decided. Do not claim that resubmission is either allowed or permanently prohibited.
- **Provider Workflow Rules**: Providers must move applications through `UNDER_REVIEW` before making final decisions; direct transition from `SUBMITTED` to `ACCEPTED` or `REJECTED` is prohibited.
- **Module Ownership**: Paviththiran owns student-side application submission and status tracking. Jeevithan owns provider-side application review and status update workflows.

### Accepted Decision 6 – Bookmark Storage Structure `[ACCEPTED]`
- **Firestore Path**: `users/{studentId}/bookmarks/{internshipId}`
- **Document ID Strategy**: Using `internshipId` as the Firestore document ID guarantees atomic uniqueness and prevents duplicate bookmarks for the same internship.
- **Module Ownership**: Paviththiran owns bookmark creation, deletion, and query operations.

### Accepted Decision 7 – Repository Interfaces and Architecture `[ACCEPTED]`
- **Architecture**: Single Gradle module (`:app`), clean architecture with MVVM and Manual Dependency Injection.
- **Repository Interface Location**: `domain/repository` package.
- **Repository Implementation Location**: `data/repository` package.
- **Service Classification**: `MatchingEngine` is classified as a pure Kotlin **Domain Service**, NOT a Repository.
- **The 7 Accepted Repository Interfaces**:
  1. `AuthRepository`
  2. `StudentProfileRepository`
  3. `SkillRepository`
  4. `InternshipRepository`
  5. `ApplicationRepository`
  6. `BookmarkRepository`
  7. `NotificationRepository`

### Accepted Decision 8 – Firestore Security Principles `[ACCEPTED]`
- **Role Integrity**: Users cannot change their own role to `ADMIN`.
- **Profile Ownership**: Students can edit only their own profiles.
- **Bookmark Isolation**: Students can access only their own bookmarks (`users/{studentId}/bookmarks/{internshipId}`).
- **Student Application Privacy**: Students can submit and view only their own applications.
- **Provider Application Access**: Providers can review applications belonging to their own internships.
- **Catalogue & Approval Security**: Only Admin can approve internships and manage the central skill catalogue.
- **Authorization Boundary**: Match scores must never be used for authorization.
- **Applicant Data Minimization**: Providers receive only necessary applicant profile information for evaluation.
- **Firestore Security Limitations Note**: The above items represent conceptual security principles, **not** deployable Firestore Security Rules syntax. Cloud Firestore Security Rules evaluate access at the document level (allow/deny read) and **cannot** perform field-level redaction or filtering within a returned document.

---

## 3. Domain Model Catalogue

Below are the domain model definitions distinguishing accepted decisions from proposed details.

### 3.1 User `[ACCEPTED / PROPOSED]`
Represents common authenticated account credentials and system role.

```kotlin
// Location: domain/model/User.kt
enum class UserRole { STUDENT, PROVIDER, ADMIN } // [ACCEPTED]

data class User(
    val id: String,                  // [ACCEPTED] Unique Auth User ID (UID)
    val email: String,               // [ACCEPTED] User email address
    val displayName: String,         // [ACCEPTED] User display name
    val role: UserRole,              // [ACCEPTED] User role
    val createdAt: Long = 0L,        // [PROPOSED] Epoch milliseconds
    val updatedAt: Long = 0L         // [PROPOSED] Epoch milliseconds
)
```

### 3.2 StudentProfile `[ACCEPTED / PROPOSED]`
Student-specific profile details linked to a `User` account (`role = STUDENT`).

```kotlin
// Location: domain/model/StudentProfile.kt
data class StudentProfile(
    val studentId: String,                    // [ACCEPTED] Foreign key matching User.id
    val university: String,                   // [ACCEPTED] Name of university/institution
    val degreeProgram: String,                // [ACCEPTED] Field of study/degree
    val graduationYear: Int,                  // [ACCEPTED] Expected graduation year
    val gpa: Double? = null,                  // [ACCEPTED] Optional GPA
    val bio: String = "",                     // [ACCEPTED] Short biography/summary
    val contactDetails: ContactInfo? = null,  // [PROPOSED] Contact information details
    val projects: List<Project> = emptyList(), // [PROPOSED] List of student projects
    val certificates: List<Certificate> = emptyList(), // [PROPOSED] List of certifications
    val skillProficiencies: List<SkillProficiency> = emptyList() // [PROPOSED / PENDING] Selected skills with proficiency
)

// [PROPOSED] Data structures requiring implementation-level specification
data class ContactInfo(
    val phone: String = "",
    val linkedinUrl: String = "",
    val githubUrl: String = "",
    val portfolioUrl: String = ""
)

data class Project(
    val title: String,
    val description: String,
    val projectUrl: String = ""
)

data class Certificate(
    val title: String,
    val issuer: String,
    val issueDate: String = ""
)
```

> [!NOTE]
> **Skill Proficiency Storage (`[PENDING]`)**: Whether student `skillProficiencies` are stored embedded inside the `StudentProfile` document or managed/queried separately via `SkillRepository` is an unresolved architectural choice marked as `[PENDING]`.

### 3.3 Skill & SkillProficiency `[ACCEPTED / PROPOSED]`
Central skill catalogue items and student proficiency mappings.

```kotlin
// Location: domain/model/Skill.kt
data class Skill(
    val id: String,                  // [ACCEPTED] Unique skill ID
    val name: String,                // [ACCEPTED] Canonical skill name
    val category: String = ""        // [PROPOSED] Skill category string
)

enum class ProficiencyLevel {        // [ACCEPTED] Standardized proficiency levels
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

data class SkillProficiency(
    val skillId: String,             // [ACCEPTED] References Skill.id
    val level: ProficiencyLevel      // [ACCEPTED] Proficiency level (profile metadata only)
)
```

### 3.4 Internship & RequiredSkill `[ACCEPTED / PROPOSED]`
Internship listings created by providers and approved by admins.

```kotlin
// Location: domain/model/Internship.kt
enum class InternshipStatus {
    DRAFT,            // [PROPOSED] Lifecycle state
    PENDING_APPROVAL, // [PROPOSED] Lifecycle state
    APPROVED,         // [ACCEPTED] Required state (only APPROVED visible to students)
    REJECTED,         // [PROPOSED] Lifecycle state
    CLOSED            // [PROPOSED] Lifecycle state
}

data class RequiredSkill(
    val skillId: String,             // [ACCEPTED] References Skill.id
    val weight: Double               // [PROPOSED implementation detail; exact numerical value, representation, and storage precision are PENDING]
)

data class Internship(
    val id: String,                  // [PROPOSED] Unique Internship ID field & String type
    val providerId: String,          // [PROPOSED] Owner provider user ID field & String type
    val companyName: String,         // [PROPOSED] Company name field & String type
    val title: String,               // [PROPOSED] Job title field & String type
    val description: String,         // [PROPOSED] Description field & String type
    val location: String,            // [PROPOSED] Location string field & String type
    val employmentType: String,      // [PROPOSED] Employment type representation & field
    val requiredSkills: List<RequiredSkill>, // [PROPOSED] Field & List representation (required-skill relationship is [ACCEPTED])
    val status: InternshipStatus,    // [PROPOSED] Field & InternshipStatus enum type (APPROVED visibility rule is [ACCEPTED])
    val applicationDeadline: Long,   // [PROPOSED] Deadline timestamp field & Long type
    val createdAt: Long,             // [PROPOSED] Creation timestamp field & Long type
    val updatedAt: Long              // [PROPOSED] Update timestamp field & Long type
)
```

> [!IMPORTANT]
> **Equal Weights Rule**: The system assigns equal weights to all required skills. Provider-configurable custom weights are **not currently approved**. The exact numerical value, representation, and storage precision remain `[PENDING]`.

### 3.5 Application `[ACCEPTED / PROPOSED]`
Application submitted by a student for a specific internship.

```kotlin
// Location: domain/model/Application.kt
enum class ApplicationStatus {        // [ACCEPTED] Workflow statuses
    SUBMITTED,
    UNDER_REVIEW,
    SHORTLISTED,
    ACCEPTED,
    REJECTED,
    WITHDRAWN
}

data class Application(
    val id: String,                  // [PROPOSED] Unique Application ID
    val internshipId: String,        // [ACCEPTED] References Internship.id
    val studentId: String,           // [ACCEPTED] References User.id (Student)
    val status: ApplicationStatus,   // [ACCEPTED] Workflow status
    val submittedAt: Long = 0L,      // [PROPOSED] Submission timestamp
    val updatedAt: Long = 0L,        // [PROPOSED] Last update timestamp
    val coverLetter: String = "",    // [PROPOSED] Optional cover letter text
    val resumeUrl: String = ""       // [PROPOSED] Storage URL for resume
)
```

### 3.6 Bookmark `[ACCEPTED]`
Bookmarked internship saved by a student.

```kotlin
// Location: domain/model/Bookmark.kt
data class Bookmark(
    val internshipId: String,        // [ACCEPTED] Used as Firestore Document ID
    val savedAt: Long                // [ACCEPTED] Timestamp when saved
)
```

### 3.7 Notification `[PROPOSED / PENDING]`
In-app system notification structure.

```kotlin
// Location: domain/model/Notification.kt
data class Notification(
    val id: String,                  // [PROPOSED]
    val userId: String,              // [PROPOSED]
    val title: String,               // [PROPOSED]
    val message: String,             // [PROPOSED]
    val isRead: Boolean = false,     // [PROPOSED]
    val createdAt: Long = 0L         // [PROPOSED]
)
```

---

## 4. Repository Catalogue

The 7 accepted repository interfaces define data access boundaries.

| Interface Name | Package Location | Primary Responsibilities | Primary Owner |
| :--- | :--- | :--- | :--- |
| `AuthRepository` | `domain/repository` | Authentication credentials, session management, user role lookup. | Paviththiran |
| `StudentProfileRepository` | `domain/repository` | Student profile CRUD operations, bio, projects, certificates, contact info. | Paviththiran |
| `SkillRepository` | `domain/repository` | Master skill catalogue query, creation, skill metadata lookup. | Jeevithan |
| `InternshipRepository` | `domain/repository` | Internship posting, approval management, search, filtering, detail fetching. | Jeevithan (Posting/Admin) <br> Paviththiran (Search/Filter) |
| `ApplicationRepository` | `domain/repository` | Application submission, tracking, provider applicant review, status updates. | Paviththiran (Submit/Track) <br> Jeevithan (Review/Update) |
| `BookmarkRepository` | `domain/repository` | Bookmark saving, deletion, listing student bookmarks. | Paviththiran |
| `NotificationRepository` | `domain/repository` | Notification data access (fetching, mark as read, persisting notification records). | Paviththiran |

> [!NOTE]
> - `MatchingEngine` is **NOT** a repository interface. It is a pure Kotlin Domain Service residing in `domain/service/MatchingEngine.kt`.
> - **Notification Architecture Separation**: `NotificationRepository` handles notification data access (reading/writing Firestore notification documents). `WorkManager` handles background scheduling. `Worker` handles background task execution. Exact persistence and scheduling integration remains `[PENDING]`.
> - Detailed method signatures for repository interfaces are marked `[PENDING]` and require implementation-level definition.

---

## 5. Firestore Collection Structure

```
Cloud Firestore Root
│
├── /users/{userId}                           [User Document - PROPOSED Path]
│   ├── id: string
│   ├── email: string
│   ├── displayName: string
│   ├── role: "STUDENT" | "PROVIDER" | "ADMIN"
│   ├── createdAt: timestamp
│   └── updatedAt: timestamp
│       │
│       └── /bookmarks/{internshipId}        [ACCEPTED Bookmark Path]
│           ├── internshipId: string         (Matches Document ID)
│           └── savedAt: timestamp
│
├── /studentProfiles/{studentId}             [PROPOSED Profile Collection Path]
│   ├── studentId: string                    (Matches userId)
│   ├── university: string
│   ├── degreeProgram: string
│   ├── graduationYear: number
│   ├── gpa: number (optional)
│   ├── bio: string
│   ├── contactDetails: map                  [PROPOSED]
│   ├── projects: array of maps              [PROPOSED]
│   ├── certificates: array of maps          [PROPOSED]
│   └── skillProficiencies: array of maps    [PROPOSED / PENDING Storage Location]
│
├── /skills/{skillId}                         [Skill Catalogue Document - PROPOSED Path]
│   ├── id: string
│   ├── name: string
│   └── category: string
│
├── /internships/{internshipId}               [Internship Document - PROPOSED Path]
│   ├── id: string
│   ├── providerId: string
│   ├── companyName: string
│   ├── title: string
│   ├── description: string
│   ├── location: string
│   ├── employmentType: string
│   ├── requiredSkills: array of maps        [{ skillId, weight }]
│   ├── status: "DRAFT" | "PENDING_APPROVAL" | "APPROVED" | "REJECTED" | "CLOSED"
│   ├── applicationDeadline: timestamp
│   ├── createdAt: timestamp
│   └── updatedAt: timestamp
│
├── /applications/{applicationId}             [Application Document - PROPOSED Path]
│   ├── id: string
│   ├── internshipId: string
│   ├── studentId: string
│   ├── status: "SUBMITTED" | "UNDER_REVIEW" | "SHORTLISTED" | "ACCEPTED" | "REJECTED" | "WITHDRAWN"
│   ├── submittedAt: timestamp
│   ├── updatedAt: timestamp
│   ├── coverLetter: string
│   └── resumeUrl: string
│
└── /notifications/{notificationId}         [Notification Document - PROPOSED Path]
    ├── id: string
    ├── userId: string
    ├── title: string
    ├── message: string
    ├── isRead: boolean
    └── createdAt: timestamp
```

> [!NOTE]
> - `users/{studentId}/bookmarks/{internshipId}` is the only **ACCEPTED** Firestore collection path decision.
> - Collection paths such as `/users/{userId}`, `/studentProfiles/{studentId}` (vs `/users/{userId}/profile`), `/skills/{skillId}`, `/internships/{internshipId}`, `/applications/{applicationId}`, and `/notifications/{notificationId}` and exact field layouts are **PROPOSED** structural choices subject to final technical specification (`[PENDING]`).

---

## 6. Application Workflow & Transition Matrix `[ACCEPTED]`

### State Transition Diagram

```
[ SUBMITTED ] ───────► [ UNDER_REVIEW ] ───────► [ SHORTLISTED ] ───────► [ ACCEPTED ] (Terminal)
      │                       │                        │
      ├───────────────────────┼────────────────────────┴───────────────► [ REJECTED ] (Terminal)
      │                       │                        │
      └───────────────────────┴────────────────────────┴───────────────► [ WITHDRAWN ] (Terminal)
```

### State Transition Matrix

| Current State | Target State | Allowed? | Initiated By | Guard Condition / Business Rule |
| :--- | :--- | :--- | :--- | :--- |
| *None* | `SUBMITTED` | **YES** | Student | **One student may submit only one application per internship.** |
| `SUBMITTED` | `UNDER_REVIEW` | **YES** | Provider | Provider begins reviewing the application. |
| `SUBMITTED` | `WITHDRAWN` | **YES** | Student | Student withdraws before review starts. |
| `SUBMITTED` | `SHORTLISTED` | **NO** | - | Must enter `UNDER_REVIEW` first. |
| `SUBMITTED` | `ACCEPTED` | **NO** | - | **Prohibited**: Direct acceptance from `SUBMITTED` is strictly forbidden. |
| `SUBMITTED` | `REJECTED` | **NO** | - | **Prohibited**: Direct rejection from `SUBMITTED` is strictly forbidden. |
| `UNDER_REVIEW` | `SHORTLISTED` | **YES** | Provider | Candidate meets initial criteria. |
| `UNDER_REVIEW` | `REJECTED` | **YES** | Provider | Candidate does not meet requirements. |
| `UNDER_REVIEW` | `WITHDRAWN` | **YES** | Student | Student withdraws during review. |
| `UNDER_REVIEW` | `ACCEPTED` | **NO** | - | Must be shortlisted prior to acceptance. |
| `SHORTLISTED` | `ACCEPTED` | **YES** | Provider | Final offer extended. |
| `SHORTLISTED` | `REJECTED` | **YES** | Provider | Candidate not selected post-shortlisting. |
| `SHORTLISTED` | `WITHDRAWN` | **YES** | Student | Student withdraws while shortlisted. |
| `ACCEPTED` | *Any State* | **NO** | - | **Terminal State**: No further transitions permitted. |
| `REJECTED` | *Any State* | **NO** | - | **Terminal State**: No further transitions permitted. |
| `WITHDRAWN` | *Any State* | **NO** | - | **Terminal State**: The resubmission policy after withdrawal remains `[PENDING]`. No automatic replacement or resubmission should be implemented until decided. |

---

## 7. Matching Formula Specification `[ACCEPTED]`

### Mathematical Representation

$$\text{Match Score} = \left( \frac{\sum_{s \in S_{\text{matched}}} w(s)}{\sum_{r \in S_{\text{required}}} w(r)} \right) \times 100$$

Where:
- $S_{\text{required}}$ is the set of required skills specified for an internship.
- $w(r)$ is the equal weight assigned to required skill $r$.
- $S_{\text{matched}} = S_{\text{required}} \cap S_{\text{student}}$ is the intersection of required skills and skills selected in the student profile.
- $w(s)$ is the equal weight of each matched required skill.

### Matching Rules Summary
1. **Required Skills Only**: Only skills listed in `internship.requiredSkills` are evaluated.
2. **Boolean Match**: A skill is either present in student profile (`matched`) or absent (`unmatched`).
3. **Proficiency Exclusion**: `ProficiencyLevel` (BEGINNER, INTERMEDIATE, etc.) does **NOT** modify or scale the score.
4. **Non-Skill Exclusion**: GPA, location, university, degree program, and preferred skills do **NOT** contribute to the score.
5. **Equal Weights Rule**: The system assigns equal weights to all required skills. Provider-configurable custom weights are **not currently approved**. The exact numerical value, representation, and storage precision remain `[PENDING]`.
6. **Zero-Weight Rule**: **No match score is calculated when total required-skill weight is zero** ($\sum w(r) = 0$ or empty required skills list). The exact Kotlin return representation (e.g., returning `null`, a custom Result type, or `0.0`) is marked as `[PENDING]`.
7. **Pure Kotlin Domain Service**: Implemented in `MatchingEngine.kt` as pure domain logic with unit tests written by Jeevithan.

---

## 8. Security Principles `[ACCEPTED]`

> [!CAUTION]
> **Firestore Security Rules Limitations**:
> The security principles listed below represent conceptual authorization rules accepted by the project owner. They do **NOT** constitute deployable Firestore Security Rules syntax (`firestore.rules`).
> **Important Technical Restriction**: Cloud Firestore Security Rules evaluate access strictly at the **document level** (allow/deny read or write) and **cannot** perform field-level filtering or redaction within a returned document.

### Accepted Security Principles
1. **Role Self-Modification Prohibition**: Users cannot change their own role to `ADMIN`.
2. **Student Profile Ownership**: Students can edit only their own profiles (`request.auth.uid == studentId`).
3. **Bookmark Isolation**: Students can access only their own bookmarks (`users/{studentId}/bookmarks/{internshipId}`).
4. **Student Application Privacy**: Students can submit and view only their own applications.
5. **Provider Application Access**: Providers can review applications belonging to their own internships (`providerId`).
6. **Catalogue & Approval Security**: Only Admin can approve internships and manage the central skill catalogue.
7. **Authorization Boundary**: Match scores must never be used for authorization (they are strictly informational UI indicators).
8. **Applicant Data Minimization**: Providers receive only necessary applicant profile information for evaluation.

> [!NOTE]
> **Provider Privacy Architecture (`[PENDING]`)**: Because Firestore Rules cannot selectively redact fields within a document payload, the design for serving provider-visible applicant data while protecting private student information remains `[PENDING]`. Proposed architectures include:
> - **Proposal A**: Separate public student profile document (e.g., `/studentProfiles/{id}/public`) and private student document.
> - **Proposal B**: A controlled applicant-profile projection embedded directly into the application document or generated by a backend Cloud Function.

---

## 9. Team Ownership Matrix

| Task / Feature Domain | Primary Owner | Secondary / Integration Owner | Key Deliverables |
| :--- | :--- | :--- | :--- |
| **Authentication & Role Security** | Paviththiran | Jeevithan | Firebase Auth integration, user role routing (`STUDENT`, `PROVIDER`, `ADMIN`). |
| **Student Profile & Metadata** | Paviththiran | Jeevithan | `StudentProfile` CRUD, bio, projects, certificates, contact info. |
| **Search, Filters & Bookmarks** | Paviththiran | Jeevithan | Student internship search, filter UI, bookmark storage (`users/{studentId}/bookmarks`). |
| **Application Submission & Tracking** | Paviththiran | Jeevithan | Student application creation, tracking list view, withdrawal handling. |
| **Notifications & WorkManager** | Paviththiran | Jeevithan | `NotificationRepository` data access, background scheduling via `WorkManager`, background execution via `Worker`. |
| **Firestore Integration & Security** | Paviththiran | Jeevithan | Firestore repository implementations, security rules definition and testing. |
| **Skill Catalogue & Skill Selection** | Jeevithan | Paviththiran | Master `/skills` catalogue management, student skill selection component. |
| **Pure Kotlin Matching Engine** | Jeevithan | Paviththiran | Pure Kotlin `MatchingEngine`, match score calculations, unit test suite. |
| **Recommendations & Gap Analysis** | Jeevithan | Paviththiran | Recommended internships sorting, missing skill gap analysis UI. |
| **Provider Posting & Review** | Jeevithan | Paviththiran | Provider internship creation UI, provider applicant review & state transitions. |
| **Admin Approval & Dashboard** | Jeevithan | Paviththiran | Admin internship approval screen, catalogue management. |
| **Usability & Evaluation** | Jeevithan | Paviththiran | Matching engine usability testing, provider feedback evaluation. |
| **Shared Integration Tasks** | **Shared** | **Shared** | Domain contract maintenance, repository interface definitions, end-to-end integration testing. |

> [!IMPORTANT]
> **Consolidation Note**: This document consolidates agreed team decisions for Day 2, Task 4. It does not imply that Jeevithan has completed an individual sign-off review of this specific text.

---

## 10. Unresolved Technical Details `[PENDING]`

The following technical details are explicitly marked as **UNRESOLVED (`[PENDING]`)** and require formal technical specification prior to code implementation:

1. **Exact Timestamp Data Types**: Determination between Firestore `com.google.firebase.Timestamp`, `java.time.Instant`, or Unix epoch `Long` in domain models versus DTO mappers.
2. **Provider-Visible Student Privacy Architecture**: Specification of provider-visible student fields given Firestore's document-level security rules limitation. (Evaluating Proposal A: Separate public/private profile documents vs Proposal B: Applicant projection snapshot inside application document).
3. **Skill Proficiency Storage & Source of Truth**: Determining whether student skill proficiencies are embedded inside `StudentProfile` or queried dynamically from `SkillRepository`.
4. **Student Profile & User Collection Paths**: Choice between `/users/{userId}` path confirmation, `/studentProfiles/{studentId}` vs `/users/{userId}/profile`, and other Firestore collection path structures.
5. **Zero-Weight Match Score Return Type**: Exact Kotlin representation when total required skill weight is zero (e.g., `null`, `0.0`, or `Result.failure`).
6. **Equal Skill Weight Representation & Storage Precision**: The exact numerical default value, numerical representation, and storage precision for equal skill weights remain `[PENDING]`.
7. **Canonical Skill ID Generation**: Standardizing ID generation for catalogue skills (e.g., UUID vs slugified name like `kotlin-android`).
8. **Exact Firestore Security Rules File**: Authorship, testing, and deployment of syntax-level `firestore.rules` based on Section 8 principles.
9. **Provider Account Approval Details**: Administrative approval workflow or domain verification process before providers can post internships.
10. **Detailed Repository Method Signatures & Error Handling**: Precise Kotlin method signatures, parameter types, and result wrappers (`Result<T>` vs custom `Resource<T>`).
11. **Notification Persistence & Background Scheduling**: Architecture separating `NotificationRepository` data access, `WorkManager` scheduling, and `Worker` task execution.
12. **Resubmission Policy for Withdrawn Applications**: The resubmission policy after `WITHDRAWN` remains `[PENDING]`. No automatic replacement or resubmission should be implemented until this policy is decided. Do not claim that resubmission is either allowed or permanently prohibited.

---

## 11. Implementation Readiness Checklist

- [x] Consolidate Accepted Decision 1 (User vs StudentProfile separation).
- [x] Consolidate Accepted Decision 2 (Skill Catalogue and SkillProficiency).
- [x] Consolidate Accepted Decision 3 (Internship and RequiredSkill with Equal Weights).
- [x] Consolidate Accepted Decision 4 (Pure Kotlin Matching Formula & Zero-Weight Rule).
- [x] Consolidate Accepted Decision 5 (Application State Transitions & Uniqueness Rule).
- [x] Consolidate Accepted Decision 6 (Bookmark Firestore Path `users/{studentId}/bookmarks/{internshipId}`).
- [x] Consolidate Accepted Decision 7 (The 7 Repository Interfaces & Manual DI).
- [x] Consolidate Accepted Decision 8 (Firestore Security Principles & Document-level Limitations).
- [x] Document Firestore Collection Layout distinguishing ACCEPTED from PROPOSED paths.
- [x] Detail Team Ownership Matrix between Paviththiran and Jeevithan.
- [x] Detail Notification Responsibilities (`NotificationRepository` vs `WorkManager` vs `Worker`).
- [x] Document all 12 Unresolved Technical Details explicitly as `[PENDING]`.
- [ ] Resolve `[PENDING]` technical details (Task 5+ technical specification).
- [ ] Implement Domain Data Models in `com.internmatch.app.domain.model`.
- [ ] Implement Repository Interfaces in `com.internmatch.app.domain.repository`.
