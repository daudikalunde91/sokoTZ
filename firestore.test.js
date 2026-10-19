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

test("Unauthenticated user: cannot read users, businesses, or listings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("businesses").get());
  await assertFails(unauthDb.collection("listings").get());
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
