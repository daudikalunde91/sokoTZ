# Changelog

All notable repairs, security enhancements, and UI bug fixes implemented in SokoTZ.

## [1.2.0] - Security Hardening, Database Consistency & Advertising Rules

### 1. Consistent Firestore Database Configuration (Criteria 1)
- Verified and aligned database selection across all repositories and helpers:
  - `firebase_applet_config.xml`: Authoritative database ID `ai-studio-android-sokotz-d5a575ab-0477-4160-b139-0caa7e2d8a9a`.
  - `firebase.json`: Fixed to declare only the single active database ID.
  - `FirebaseInitHelper.kt`: Centralized `getDatabaseId()` and `getFirestore(context)` provider.
  - `AdCampaignRepository.kt`, `FirestoreMarketplaceRepository.kt`, and `AuthManager.kt`: Updated to use `FirebaseInitHelper.getFirestore(...)` uniformly.

### 2. Business Ownership & Existence Validation (Criteria 2)
- Hardened `isBusinessOwner(bizId)` in `firestore.rules`:
  - Missing business documents explicitly return `false`.
  - Caller must match `ownerId == request.auth.uid` on the referenced business document.
  - Denies unauthorized creation of listings or ad campaigns with missing or foreign business IDs.

### 3. Listing Moderation & Immutability (Criteria 3)
- Locked down `listings/{listingId}` updates in `firestore.rules`:
  - `ownerId` and `businessId` are immutable.
  - Listing owners cannot bypass review to `PUBLISHED` or alter `isFeatured`.
  - Public viewers can only increment `viewsCount` +1 with zero changes to other fields.

### 4. Advertising Engagement Counter Security (Criteria 4)
- Restricted `ad_campaigns/{campaignId}` updates:
  - Engagement metrics restricted via `diff(resource.data).affectedKeys().hasOnly(...)`.
  - Counters cannot be decremented or set to negative values.
  - Engagement updates cannot alter sensitive business fields (`paymentStatus`, `sellerId`, `status`, `packagePriceTzs`).

### 5. Secure Payment Records & Notifications (Criteria 5)
- Secured `payment_transactions/{txnId}`:
  - Sellers can only submit with status `SUBMITTED` or `PENDING`.
  - Clients cannot mark transactions as `VERIFIED`.
  - Transaction deletions are permanently blocked (`allow delete: if false`).
  - Only authorized administrators can transition transactions to `VERIFIED`.
- Secured `ad_notifications/{notificationId}`:
  - Blocked unauthenticated/arbitrary notification spoofing between users.

### 6. Explicit Authentication Methods (Criteria 6)
- Refactored `AuthManager.kt`:
  - Added explicit `signInWithEmailPassword` and `registerWithEmailPassword`.
  - Removed misleading legacy overloads that silently redirected into Google Sign-In.
  - Updated `SignInScreen.kt` to call the explicit methods.
  - Preserved Google Sign-In via Android Credential Manager as a distinct option.

### 7. Seed Data Separation from Production (Criteria 7)
- Gated sample seed data in `FirestoreMarketplaceRepository.kt` behind `BuildConfig.DEBUG`.
- In production/release builds, initial state starts clean and only live Firestore documents are displayed.

### 8. Admin Authorization & Moderation Lifecycle (Criteria 8 & 9)
- Enforced that role cannot be escalated to `ADMIN` by client.
- Aligned `AdCampaignRepository.kt` payment submission (`PENDING_VERIFICATION` / `AWAITING_REVIEW`) and admin approval (`PAID` / `ACTIVE` / `VERIFIED`).

### 9. Test Suite & Verification (Criteria 10)
- Expanded `firestore.test.js` to 15 comprehensive security tests, covering all regression scenarios (15/15 passing).
- Updated Robolectric test suite in `MarketplaceRepositoryRuleTest.kt`.

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
