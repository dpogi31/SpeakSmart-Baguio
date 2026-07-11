// importFirestore.js
const admin = require("firebase-admin");
const fs = require("fs");

// Path to your service account key JSON
const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Load your JSON file
const data = JSON.parse(fs.readFileSync("ilokano_words.json", "utf8"));

data.forEach(async (item) => {
  try {
    await db.collection("dictionary").add(item); // Change "dictionary" to your collection name
    console.log(`Added: ${item.englishTranslation}`);
  } catch (err) {
    console.error(err);
  }
});
