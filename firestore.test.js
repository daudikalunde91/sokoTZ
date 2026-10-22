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

