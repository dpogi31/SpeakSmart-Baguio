// importTranslations.js
const admin = require("firebase-admin");
const fs = require("fs");

// Path to your Firebase service account key
const serviceAccount = require("./serviceAccountKey.json"); // <-- replace with your key file

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Load JSON file
const data = JSON.parse(fs.readFileSync("translations.json", "utf8"));

// Collection name in Firestore
const collectionName = "translations";

async function importTranslations() {
  for (const item of data) {
    try {
      const docRef = db.collection(collectionName).doc(); // auto-generate document ID
      await docRef.set(item); // assumes each object in JSON has correct fields
      console.log(`Added: ${JSON.stringify(item)}`);
    } catch (error) {
      console.error(`Error adding item:`, error);
    }
  }
  console.log("Import completed!");
}

importTranslations();
