# Changelog

All notable repairs, security enhancements, and UI bug fixes implemented in SokoTZ.

## [1.1.0] - Security Hardening & Full-Screen Theme Repair

### 1. Full-Screen Dark & Light Mode Theme Repair
- **Root Cause Fix**: Removed hardcoded dark hex backgrounds (`SokoBlueBlack`, `SokoCardSurface`, `SokoMidnightBlue`) across all Compose screens and components.
- **Dynamic Theming System**: Introduced `SokoColors` singleton with reactive composable getters backed by `LocalIsDarkTheme.current`:
  - `background`: `#070B18` in Dark mode, `#F8FAFC` in Light mode.
  - `surface`: `#0F1A38` in Dark mode, `#FFFFFF` in Light mode.
  - `cardSurface`: `#0F1A38` in Dark mode, `#FFFFFF` in Light mode.
  - `cardBorder`: `#1A2D62` in Dark mode, `#E2E8F0` in Light mode.
  - `textPrimary`: `#FFFFFF` in Dark mode, `#0F172A` in Light mode.
  - `textMuted`: `#8FA0C7` in Dark mode, `#64748B` in Light mode.
  - `inputBackground`: `#0A132C` in Dark mode, `#F1F5F9` in Light mode.
- **Screens Updated**:
  - `SokoTzApp.kt`: Root Scaffold and Bottom Navigation Bar.
  - `DiscoverScreen.kt`: Root container, hero card, categories, region pills, listing cards.
  - `SearchScreen.kt`: Search bar, filter pills, chips, result cards.
  - `AssistantScreen.kt`: Root container, suggestion chips, message bubbles, input container.
  - `BusinessDetailScreen.kt`: Hero, business hours card, listing catalog.
  - `ListingDetailScreen.kt`: Hero image card, price card, contact button row.
  - `BusinessHubScreen.kt`: Seller gate, metric cards, listing management items.
  - `AccountScreen.kt`: Profile banner, theme dialog, settings options.
  - `SignInScreen.kt`: Auth tab row, card container, input fields, terms dialog.
  - `CommonComponents.kt`: Shared cards, headers, trust badges.

### 2. Firebase Authentication Architecture
- **Standard Email & Password Login**: Switched from routing into Google Sign-In to `FirebaseAuth.signInWithEmailAndPassword()`.
- **Standard Email & Password Registration**: Implemented genuine `FirebaseAuth.createUserWithEmailAndPassword()` with Firebase profile display name update.
- **Password Confirmation**: Added a `registerConfirmPassword` field to the registration form with validation against mismatch.
- **Password Reset**: Integrated `FirebaseAuth.sendPasswordResetEmail()` via a dedicated modal dialog in `SignInScreen.kt`.
- **Backward Compatibility**: Preserved method overloads in `AuthManager.kt` so existing callers continue to work without breaking changes.
- **Error Handling**: Implemented bilingual, descriptive error feedback for common Firebase error states (weak password, user not found, wrong password, email already in use, network error).

### 3. Cloud Firestore Security Hardening (`firestore.rules`)
- **Role Escalation Protection**: Denied client attempts to set or change `role == 'ADMIN'` in `/users/{userId}`.
- **Business Verification Integrity**: Enforced that business owners cannot self-verify (`isVerified == true`) on creation or update; verification status is reserved for administrators or trusted backends.
- **Rating Manipulation Prevention**: Blocked arbitrary client modification of `rating` and `reviewCount` on businesses.
- **Listing & Business Relationship**: Mandated that every newly created listing must reference an existing business owned by the authenticated user (`isBusinessOwner(incoming().businessId)`).
- **Featured Placement Protection**: Blocked client attempts to self-assign `isFeatured: true`.
- **View Counter Abuse Prevention**: Enforced unique views subcollection `/listings/{id}/viewers/{uid}` and restricted listing update writes to atomic increments of `viewsCount`.

### 4. Verification & Testing
- Added automated unit tests in `firestore.test.js` covering privilege escalation, business verification, and listing relationship rules (8/8 tests passing).
- Verified full compilation with `compile_applet`.
