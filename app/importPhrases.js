// importPhrases.js
const admin = require("firebase-admin");
const fs = require("fs");

// Path to your Firebase service account key
const serviceAccount = require("./serviceAccountKey.json"); // <-- replace with your key file

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Load JSON file
const data = JSON.parse(fs.readFileSync("phrasebook.json", "utf8"));

// Collection name
const collectionName = "phrasebook"; // Firestore collection

async function importPhrases() {
  for (const phrase of data) {
    try {
      const docRef = db.collection(collectionName).doc();
      await docRef.set({
        englishTranslation: phrase.englishTranslation,
        ilokanoWord: phrase.ilokanoWord,
        partOfSpeech: phrase.partOfSpeech
      });
      console.log(`Added: ${phrase.englishTranslation}`);
    } catch (error) {
      console.error(`Error adding ${phrase.englishTranslation}:`, error);
    }
  }
  console.log("Import completed!");
}

importPhrases();
