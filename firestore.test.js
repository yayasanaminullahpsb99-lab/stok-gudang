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

test("Unauthenticated user: cannot read inventory items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("inventory_items").get()
  );
});

test("Authenticated user: cannot read another user's inventory item", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("inventory_items")
      .doc("item_bob_1")
      .set({
        userId: BOB_UID,
        sku: "SKU-BOB-01",
        barcode: "8991234567890",
        name: "Pallet Forklift Sensor",
        category: "Elektronik",
        zone: "Zona A - Fast Moving",
        aisle: "A-01-R2",
        quantity: 25,
        minStock: 10,
        maxStock: 100,
        unit: "Pcs",
        unitCost: 150000,
        unitPrice: 220000,
        supplierName: "PT Tekno Logistik",
        batchNumber: "BATCH-2026-01",
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("inventory_items")
      .doc("item_bob_1")
      .get()
  );
});

test("Authenticated user: can query their own inventory items", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("inventory_items")
      .doc("item_alice_1")
      .set({
        userId: ALICE_UID,
        sku: "SKU-ALC-01",
        barcode: "8991234567891",
        name: "Scanner Laser Industri",
        category: "Elektronik",
        zone: "Zona A - Fast Moving",
        aisle: "A-02-R1",
        quantity: 40,
        minStock: 10,
        maxStock: 100,
        unit: "Unit",
        unitCost: 850000,
        unitPrice: 1250000,
        supplierName: "PT Indo Elektronika",
        batchNumber: "BATCH-2026-02",
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("inventory_items")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Authenticated user: cannot spoof userId on inventory item creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("inventory_items")
      .doc("spoof_item")
      .set({
        userId: BOB_UID,
        sku: "SKU-SPOOF",
        barcode: "899000",
        name: "Spoof Item",
        category: "Umum",
        zone: "Zona A",
        aisle: "A-01",
        quantity: 10,
        minStock: 5,
        maxStock: 50,
        unit: "Pcs",
        unitCost: 1000,
        unitPrice: 2000,
        supplierName: "Vendor",
        batchNumber: "B1",
      })
  );
});
