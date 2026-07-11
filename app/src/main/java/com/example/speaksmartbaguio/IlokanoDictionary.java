package com.example.speaksmartbaguio;

import java.util.HashMap;
import java.util.Map;

public class IlokanoDictionary {

    private static final Map<String, String> dictionary = new HashMap<>();

    static {
        // Greetings
        dictionary.put("hello", "kamusta");
        dictionary.put("hi", "kamusta");
        dictionary.put("good morning", "naimbag nga bigat");
        dictionary.put("good afternoon", "naimbag nga malem");
        dictionary.put("good evening", "naimbag nga rabii");
        dictionary.put("good night", "naimbag nga rabii");
        dictionary.put("how are you?", "kumusta ka?");
        dictionary.put("i am fine", "mayat met");
        dictionary.put("see you later", "kitaen kan to");
        dictionary.put("welcome", "umayka");
        dictionary.put("goodbye", "goodbye");

        // Courtesy
        dictionary.put("please", "panga-asim");
        dictionary.put("thank you", "agyamanak");
        dictionary.put("thanks a lot", "agyamanak unay");
        dictionary.put("sorry", "pasensya");
        dictionary.put("excuse me", "pakawanen nak");
        dictionary.put("you're welcome", "awan problema dita");

        // Basic conversation
        dictionary.put("yes", "wen");
        dictionary.put("no", "saan");
        dictionary.put("maybe", "mabalin");
        dictionary.put("i love you", "ay-ayaten ka");
        dictionary.put("what is your name?", "ania ti nagan mo?");
        dictionary.put("my name is", "ti naganko ket");
        dictionary.put("where are you going?", "ayna ti papanam?");
        dictionary.put("how much?", "mano?");
        dictionary.put("i don't know", "saan ko ammo");
        dictionary.put("i understand", "maawatak");
        dictionary.put("i don't understand", "haan ko maawatan");
        dictionary.put("wait", "aguray");

        // Everyday actions
        dictionary.put("eat", "mangan");
        dictionary.put("let's eat", "manganen tayo");
        dictionary.put("drink", "mainom");
        dictionary.put("let's go", "umay tayo");
        dictionary.put("sleep", "maturog");
        dictionary.put("wake up", "agriing");
        dictionary.put("come here", "umayka ditoy");
        dictionary.put("go there", "mapanka idjay");
        dictionary.put("sit down", "agtugaw");
        dictionary.put("stand up", "tumakder");
        dictionary.put("run", "tumaray");
        dictionary.put("walk", "magna");
        dictionary.put("read", "agbasa");
        dictionary.put("write", "agsurat");
        dictionary.put("listen", "agdengeg");
        dictionary.put("speak", "agsao");
        dictionary.put("play", "agayayam");
        dictionary.put("buy", "gumatang");
        dictionary.put("sell", "lako");
        dictionary.put("open", "lukay");
        dictionary.put("close", "rikep");

        // Common objects
        dictionary.put("house", "balay");
        dictionary.put("water", "danum");
        dictionary.put("food", "taraon");
        dictionary.put("friend", "gayyem");
        dictionary.put("family", "pamilya");
        dictionary.put("school", "eskwela");
        dictionary.put("book", "libro");
        dictionary.put("chair", "silya");
        dictionary.put("table", "lamesaan");
        dictionary.put("car", "lugan");
        dictionary.put("bus", "bus");
        dictionary.put("road", "dalan");
        dictionary.put("dog", "aso");
        dictionary.put("cat", "pusa");
        dictionary.put("child", "ubing");
        dictionary.put("man", "lalaki");
        dictionary.put("woman", "babae");
        dictionary.put("phone", "telepono");
        dictionary.put("money", "kwarta");
        dictionary.put("clothes", "bado");
        dictionary.put("shoes", "sapatos");

        // Numbers
        dictionary.put("one", "maysa");
        dictionary.put("two", "dua");
        dictionary.put("three", "tallo");
        dictionary.put("four", "uppat");
        dictionary.put("five", "lima");
        dictionary.put("six", "innem");
        dictionary.put("seven", "pito");
        dictionary.put("eight", "walo");
        dictionary.put("nine", "siyam");
        dictionary.put("ten", "sangapulo");

        // Days & Time
        dictionary.put("today", "itatta nga aldaw");
        dictionary.put("tomorrow", "inton bigat");
        dictionary.put("yesterday", "idi kalman");
        dictionary.put("morning", "bigat");
        dictionary.put("afternoon", "malem");
        dictionary.put("evening", "rabii");
        dictionary.put("night", "rabii");
        dictionary.put("hour", "oras");
        dictionary.put("minute", "minuto");
        dictionary.put("second", "segundo");

        // Emotions
        dictionary.put("happy", "naragsak");
        dictionary.put("sad", "naliday");
        dictionary.put("angry", "unget");
        dictionary.put("tired", "banog");
        dictionary.put("hungry", "nabisin");
        dictionary.put("thirsty", "uwaw");
        dictionary.put("scared", "nabuteng");
        dictionary.put("excited", "na excite ak");
        dictionary.put("bored", "maburburyong");

        // Common questions
        dictionary.put("where?", "ayanna?");
        dictionary.put("when?", "kaano?");
        dictionary.put("who?", "sino?");
        dictionary.put("what?", "anya?");
        dictionary.put("why?", "apay?");
        dictionary.put("how?", "kasano?");

        // Emergency & safety
        dictionary.put("help", "tulong");
        dictionary.put("stop", "sardeng");
        dictionary.put("danger", "delikado");
        dictionary.put("call the police", "agtawag ka ti pulis");
        dictionary.put("i am lost", "naawanak");
        dictionary.put("i need a doctor", "kasapulan ko ti doktor");
        dictionary.put("fire", "apoy");
        dictionary.put("earthquake", "gined");
        dictionary.put("flood", "baha");

        // Miscellaneous phrases
        dictionary.put("good luck", "good luck");
        dictionary.put("be careful", "ag anad ka");
        dictionary.put("i am hungry", "mabisbisinak");
        dictionary.put("i am thirsty", "makaininomak");
        dictionary.put("i am tired", "nabanogak");
        dictionary.put("i am sick", "agpanpanatengak");
        dictionary.put("i am allergic", "haan nga mabalin dayta kanyak");
        dictionary.put("let's go home", "agawid tayo");
        dictionary.put("let's play", "agayayam ta");
        dictionary.put("thank you very much", "agyamanak unay");
        dictionary.put("you're welcome", "welcome");
    }

    public static String translate(String englishText) {
        if (englishText == null) return null;
        String key = englishText.trim().toLowerCase();
        return dictionary.get(key);
    }

    public static Map<String, String> getAllEntries() {
        return dictionary;
    }
}
