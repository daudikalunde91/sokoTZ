const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users, but can read businesses and listings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertSucceeds(unauthDb.collection("businesses").get());
  await assertSucceeds(unauthDb.collection("listings").get());
});

test("Authenticated user: Alice can create and read her own user document", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Kalunde",
      email: "alice@sokotz.co.tz",
      role: "CUSTOMER",
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: Alice cannot write Bob's user document", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Fake",
      email: "bob@sokotz.co.tz",
      role: "CUSTOMER",
    })
  );
});

test("Authenticated user: Alice can create business and listing with her ownerId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("businesses").doc("biz_alice_1").set({
      ownerId: ALICE_UID,
      name: "Alice Kariakoo Store",
      categorySlug: "electronics",
      city: "Dar es Salaam",
      phoneNumber: "+255712345678",
      whatsappNumber: "+255712345678",
      isVerified: false,
    })
  );

  await assertSucceeds(
    aliceDb.collection("listings").doc("list_alice_1").set({
      businessId: "biz_alice_1",
      ownerId: ALICE_UID,
      businessName: "Alice Kariakoo Store",
      titleSw: "Simu ya Mkononi Samsung",
      titleEn: "Samsung Smartphone",
      priceTzs: 450000,
      categorySlug: "electronics",
      type: "PRODUCT",
      city: "Dar es Salaam",
      phoneNumber: "+255712345678",
    })
  );
});

test("Authenticated user: Bob cannot update or delete Alice's listing", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("listings").doc("list_alice_1").set({
      businessId: "biz_alice_1",
      ownerId: ALICE_UID,
      businessName: "Alice Store",
      titleSw: "Bidhaa ya Alice",
      priceTzs: 50000,
      categorySlug: "fashion",
      type: "PRODUCT",
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("listings").doc("list_alice_1").update({
      priceTzs: 1000,
    })
  );
  await assertFails(bobDb.collection("listings").doc("list_alice_1").delete());
});

test("Security: User cannot self-assign or escalate role to ADMIN", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Attempt to create user with ADMIN role should fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Hacker",
      email: "alice@sokotz.co.tz",
      role: "ADMIN",
    })
  );

  // Normal creation as CUSTOMER succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Legitimate",
      email: "alice@sokotz.co.tz",
      role: "CUSTOMER",
    })
  );

  // Attempt to update role to ADMIN should fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
    })
  );
});

test("Security: Business owner cannot self-verify business", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Creation with isVerified: true should fail
  await assertFails(
    aliceDb.collection("businesses").doc("biz_alice_fake_ver").set({
      ownerId: ALICE_UID,
      name: "Alice Store",
      categorySlug: "electronics",
      city: "Dar es Salaam",
      phoneNumber: "+255712345678",
      isVerified: true,
    })
  );

  // Creation with isVerified: false succeeds
  await assertSucceeds(
    aliceDb.collection("businesses").doc("biz_alice_unver").set({
      ownerId: ALICE_UID,
      name: "Alice Store",
      categorySlug: "electronics",
      city: "Dar es Salaam",
      phoneNumber: "+255712345678",
      isVerified: false,
    })
  );

  // Updating isVerified to true should fail
  await assertFails(
    aliceDb.collection("businesses").doc("biz_alice_unver").update({
      isVerified: true,
    })
  );
});

test("Security: Listing cannot be created under another user's business or self-featured", async () => {
  // Bob owns biz_bob_1
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("businesses").doc("biz_bob_1").set({
      ownerId: BOB_UID,
      name: "Bob Hardware",
      categorySlug: "hardware",
      city: "Mwanza",
      phoneNumber: "+255788888888",
      isVerified: false,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Alice tries to create a listing under Bob's business -> must fail
  await assertFails(
    aliceDb.collection("listings").doc("list_fake_bob").set({
      businessId: "biz_bob_1",
      ownerId: ALICE_UID,
      businessName: "Bob Hardware",
      titleSw: "Msumeno Bandia",
      priceTzs: 15000,
      categorySlug: "hardware",
      type: "PRODUCT",
    })
  );

  // Alice owns biz_alice_own
  await assertSucceeds(
    aliceDb.collection("businesses").doc("biz_alice_own").set({
      ownerId: ALICE_UID,
      name: "Alice Boutique",
      categorySlug: "fashion",
      city: "Arusha",
      phoneNumber: "+255712345678",
      isVerified: false,
    })
  );

  // Alice tries to self-feature her listing -> must fail
  await assertFails(
    aliceDb.collection("listings").doc("list_alice_featured").set({
      businessId: "biz_alice_own",
      ownerId: ALICE_UID,
      businessName: "Alice Boutique",
      titleSw: "Vazi Bora",
      priceTzs: 25000,
      categorySlug: "fashion",
      type: "PRODUCT",
      isFeatured: true,
    })
  );
});

test("Security (Criteria 2): Missing business document explicitly denies listing and campaign creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  
  // Missing business reference for listing
  await assertFails(
    aliceDb.collection("listings").doc("list_missing_biz").set({
      businessId: "biz_nonexistent_999",
      ownerId: ALICE_UID,
      businessName: "Ghost Shop",
      titleSw: "Bidhaa Isiyo na Duka",
      priceTzs: 10000,
      categorySlug: "general",
      type: "PRODUCT",
    })
  );

  // Missing business reference for ad campaign
  await assertFails(
    aliceDb.collection("ad_campaigns").doc("camp_missing_biz").set({
      sellerId: ALICE_UID,
      businessId: "biz_nonexistent_999",
      listingId: "list_123",
      packageId: "pkg_basic",
      packagePriceTzs: 3000,
      paymentStatus: "UNPAID",
      status: "DRAFT",
    })
  );
});

test("Security (Criteria 3): Listing moderation - owner cannot bypass review to PUBLISHED", async () => {
  // Setup Alice's business and a PENDING listing
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("businesses").doc("biz_alice_mod").set({
      ownerId: ALICE_UID,
      name: "Alice Mod Store",
      categorySlug: "general",
      city: "Dodoma",
      phoneNumber: "+255712000000",
      isVerified: false,
    });
    await context.firestore().collection("listings").doc("list_alice_pending").set({
      businessId: "biz_alice_mod",
      ownerId: ALICE_UID,
      businessName: "Alice Mod Store",
      titleSw: "Pending Item",
      priceTzs: 20000,
      categorySlug: "general",
      type: "PRODUCT",
      status: "PENDING",
      viewsCount: 0,
      isFeatured: false,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Alice tries to directly self-publish her pending listing -> must fail
  await assertFails(
    aliceDb.collection("listings").doc("list_alice_pending").update({
      status: "PUBLISHED",
    })
  );

  // Alice tries to forge views count -> must fail
  await assertFails(
    aliceDb.collection("listings").doc("list_alice_pending").update({
      viewsCount: 5000,
    })
  );
});

test("Security (Criteria 3): Viewer can only increment listing viewsCount +1 without altering other fields", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("listings").doc("list_pub_1").set({
      businessId: "biz_pub_1",
      ownerId: ALICE_UID,
      businessName: "Alice Store",
      titleSw: "Kitenge Safi",
      priceTzs: 35000,
      categorySlug: "fashion",
      type: "PRODUCT",
      status: "PUBLISHED",
      viewsCount: 10,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid viewer increment (+1 only, same other fields) succeeds
  await assertSucceeds(
    bobDb.collection("listings").doc("list_pub_1").update({
      viewsCount: 11,
    })
  );

  // Bob tries to alter the title while updating views -> fails
  await assertFails(
    bobDb.collection("listings").doc("list_pub_1").update({
      viewsCount: 12,
      titleSw: "Hacked Title",
    })
  );

  // Bob tries to jump views count by more than 1 -> fails
  await assertFails(
    bobDb.collection("listings").doc("list_pub_1").update({
      viewsCount: 50,
    })
  );
});

test("Security (Criteria 4): Advertising engagement counters cannot alter sensitive business fields", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("ad_campaigns").doc("camp_live_1").set({
      sellerId: ALICE_UID,
      businessId: "biz_alice_camp",
      listingId: "list_alice_1",
      packageId: "pkg_standard",
      packagePriceTzs: 5000,
      paymentStatus: "PAID",
      status: "ACTIVE",
      impressionsCount: 10,
      clicksCount: 2,
      sharesCount: 1,
      savesCount: 0,
      likesCount: 0,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Bob legitimately increments impressionsCount
  await assertSucceeds(
    bobDb.collection("ad_campaigns").doc("camp_live_1").update({
      impressionsCount: 11,
    })
  );

  // Bob tries to tamper with paymentStatus while updating counters -> MUST FAIL
  await assertFails(
    bobDb.collection("ad_campaigns").doc("camp_live_1").update({
      impressionsCount: 12,
      paymentStatus: "REFUNDED",
    })
  );

  // Bob tries to tamper with packagePriceTzs or sellerId -> MUST FAIL
  await assertFails(
    bobDb.collection("ad_campaigns").doc("camp_live_1").update({
      impressionsCount: 12,
      sellerId: BOB_UID,
    })
  );

  // Bob tries to decrement counters -> MUST FAIL
  await assertFails(
    bobDb.collection("ad_campaigns").doc("camp_live_1").update({
      impressionsCount: 5,
    })
  );
});

test("Security (Criteria 5): Payment transactions cannot be verified by client and are immutable", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Alice can submit a payment with status 'SUBMITTED' or 'PENDING'
  await assertSucceeds(
    aliceDb.collection("payment_transactions").doc("txn_alice_1").set({
      sellerId: ALICE_UID,
      campaignId: "camp_alice_1",
      provider: "MPESA",
      reference: "TZ_MPE_12345678",
      amountTzs: 5000,
      status: "SUBMITTED",
    })
  );

  // Alice cannot forge a 'VERIFIED' status upon creation
  await assertFails(
    aliceDb.collection("payment_transactions").doc("txn_alice_forged").set({
      sellerId: ALICE_UID,
      campaignId: "camp_alice_1",
      provider: "MPESA",
      reference: "TZ_MPE_88888888",
      amountTzs: 5000,
      status: "VERIFIED",
    })
  );

  // Alice cannot update or delete her payment transaction
  await assertFails(
    aliceDb.collection("payment_transactions").doc("txn_alice_1").update({
      status: "VERIFIED",
    })
  );
  await assertFails(
    aliceDb.collection("payment_transactions").doc("txn_alice_1").delete()
  );
});

test("Security (Criteria 5): Unvalidated notification creation for arbitrary users is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Alice tries to inject a forged payment verified notification to Bob
  await assertFails(
    aliceDb.collection("ad_notifications").doc("notif_forged_bob").set({
      recipientUserId: BOB_UID,
      titleSw: "Malipo Yamekamilika",
      type: "PAYMENT_CONFIRMED",
    })
  );

  // Alice recording valid self-notification for PAYMENT_SUBMITTED succeeds
  await assertSucceeds(
    aliceDb.collection("ad_notifications").doc("notif_alice_valid").set({
      recipientUserId: ALICE_UID,
      titleSw: "Malipo Yamewasilishwa",
      type: "PAYMENT_SUBMITTED",
    })
  );
});

test("Security (Criteria 8): Admin operations are authorized for genuine admins only", async () => {
  const ADMIN_UID = "admin_super_1";

  // Seed genuine admin user doc
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      displayName: "Chief Administrator",
      email: "admin@sokotz.co.tz",
      role: "ADMIN",
    });
    await context.firestore().collection("payment_transactions").doc("txn_to_verify").set({
      sellerId: ALICE_UID,
      campaignId: "camp_awaiting_admin",
      provider: "TIGO_PESA",
      reference: "TZ_TIG_99999999",
      amountTzs: 10000,
      status: "SUBMITTED",
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Admin can update transaction to VERIFIED
  await assertSucceeds(
    adminDb.collection("payment_transactions").doc("txn_to_verify").update({
      status: "VERIFIED",
    })
  );

  // Non-admin Alice cannot update transaction
  await assertFails(
    aliceDb.collection("payment_transactions").doc("txn_to_verify").update({
      status: "REJECTED",
    })
  );
});


