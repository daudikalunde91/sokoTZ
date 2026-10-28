# SokoTZ — Testing & Verification Guide

This document details the automated testing procedures and manual verification workflows for SokoTZ.

---

## 1. Automated Testing

### A. Firestore Security Rules Tests
SokoTZ enforces role-based access control, business ownership verification, listing moderation protections, and anti-abuse view counting via Cloud Firestore security rules.

**To run the test suite:**
```bash
# Ensure Firebase emulators are running (Firestore on 127.0.0.1:8085, Auth on 127.0.0.1:9099):
# setsid nohup firebase emulators:start --only firestore,auth --project demo-no-project > /tmp/firebase-emulators.log 2>&1 < /dev/null &

node --test firestore.test.js
```

**Test Coverage (15/15 Passing):**
1. `Unauthenticated user: cannot read users, but can read businesses and listings` (Public Guest Discovery)
2. `Authenticated user: Alice can create and read her own user document`
3. `Authenticated user: Alice cannot write Bob's user document` (User Isolation)
4. `Authenticated user: Alice can create business and listing with her ownerId`
5. `Authenticated user: Bob cannot update or delete Alice's listing` (Listing Ownership Protection)
6. `Security: User cannot self-assign or escalate role to ADMIN` (Privilege Escalation Prevention)
7. `Security: Business owner cannot self-verify business` (Authoritative Verification Protection)
8. `Security: Listing cannot be created under another user's business or self-featured` (Relationship & Placement Integrity)
9. `Security (Criteria 2): Missing business document explicitly denies listing and campaign creation`
10. `Security (Criteria 3): Listing moderation - owner cannot bypass review to PUBLISHED`
11. `Security (Criteria 3): Viewer can only increment listing viewsCount +1 without altering other fields`
12. `Security (Criteria 4): Advertising engagement counters cannot alter sensitive business fields`
13. `Security (Criteria 5): Payment transactions cannot be verified by client and are immutable`
14. `Security (Criteria 5): Unvalidated notification creation for arbitrary users is denied`
15. `Security (Criteria 8): Admin operations are authorized for genuine admins only`

### B. Android JVM & Robolectric Unit Tests
```bash
gradle :app:testDebugUnitTest
```
**Coverage (10/10 Passing):**
- `ExampleRobolectricTest`: Database ID configuration, Google Sign-In web client ID generation, Product vs Service model distinction, Gemini multi-turn format, app name string.
- `MarketplaceRepositoryRuleTest`: Business registration, listing under owned business, nonexistent business rejection, unauthenticated rejection.
- `ExampleUnitTest`: Basic logic sanity.

---

## 2. Compilation & Build Verification

The application compiles with zero warnings or errors using modern Jetpack Compose and Material 3:
```bash
gradle assembleDebug
```

---

## 3. Manual Verification Checklist

### Full-Screen Dark & Light Mode
- [x] **Discover Screen**: Banner, category cards, filter chips, and listing cards adapt dynamically to Light and Dark mode.
- [x] **Search Screen**: Search bar, category filters, and result cards respond to system theme changes.
- [x] **Assistant Screen**: Chat background, query suggestions, user/AI message bubbles, and input field update completely without dark bands.
- [x] **Business Hub**: Seller status card, statistics panels, action buttons, and listing cards adapt seamlessly.
- [x] **Detail Screens (Listing & Business)**: Hero cards, badge rows, description cards, and floating contact bars fully respect the active theme.
- [x] **Account Screen**: Profile cards, theme picker dialog, and menu tiles render clearly in both modes.
- [x] **Sign In & Register Screen**: Authentication card, tab row, input fields, and terms dialog are fully theme-adaptive.

### Authentication & Account Security
- [x] **Standard Email/Password Sign-In**: Uses `FirebaseAuth.signInWithEmailAndPassword` with localized error messages (invalid credentials, user not found, network failure).
- [x] **Standard Registration**: Uses `FirebaseAuth.createUserWithEmailAndPassword` with full validation (email syntax, min 6-character password, matching password confirmation, terms acceptance).
- [x] **Password Reset**: Integrated via `FirebaseAuth.sendPasswordResetEmail` in a dedicated modal dialog.
- [x] **Google Sign-In**: Powered by Android Jetpack `CredentialManager` and `GetSignInWithGoogleOption`.
- [x] **Guest Browsing**: Unauthenticated users can freely explore verified businesses and active marketplace listings.
