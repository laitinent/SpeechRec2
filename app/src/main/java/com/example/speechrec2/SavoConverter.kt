package com.example.speechrec2

import java.util.*

class SavoConverter {
    companion object {
        private fun isVowel(ch: Char): Boolean {
            return when (ch) {
                'a', 'e', 'i', 'o', 'u', 'y', 'å', 'ä', 'ö' -> true
                else -> false
            }
        }

        private val vowels=arrayOf('a', 'e', 'i', 'o', 'u', 'y', 'ä', 'ö')

        /**
         * Convert single word to Savo dialect
         * @param wordToConvert input
         * @return converted word
         */
        fun toSavo(wordToConvert: String) : String
        {
            //TODO: all rules from config file
            //TODO: second syllable au -> aa etc.
            // TODO: olla

            var w = wordToConvert.toLowerCase(Locale.getDefault())

            // ENDINGS
            if(w.length > 3 && w.endsWith("io")){
                w = w.replace("io", "ijo")
            }


            // hämmentää -> hämmentee, hätää-> hättee
            if(w.endsWith("tää")) {
                w = if (w.endsWith("ntää")) {
                    w.replace("ntää", "ntee")
                } else {
                    w.replace("tää", "ttee")
                }
            }

            // odottaa -> odottoo (-> oottoo)
            if(w.endsWith("taa")) {
                w = if (w.endsWith("ntaa")) {
                    w.replace("ntaa", "ntoo")   // probably same as below, here simply copied from above
                } else {
                    w.replace("taa", "too")
                }
            }

            // olen -> oon
            if(w.startsWith("ole", true) && !w.startsWith("olento")){
                w = w.replace("ole", "oo")
                return w
            }

            if(w == "ei") { return "ee" }
            if(w.substring(1, 3)== "oi") { return w[0]+"o e" }

            //TODO: triple vowel: kauas -> kauvas

            val index3 = indexOfTripleVowel(w)
            if(index3 > -1 && !w.contains("aaa"))   // vaa'an
            {
                w=if(w[index3 + 1]=='i') {
                    //w.substring(0, index3) + "j"+ w.substring(index3, w.length)
                    w.take(index3) + "j"+ w.substring(index3, w.length)
                }
                else
                {
                    w.substring(0, index3) + "v"+ w.substring(index3, w.length)
                }
            }

            //hakaa -> hakkaa (-> hakkoo)
            if(w.length>4) {
                val index4 = indexOfVCVV(w)
                if (index4 > -1) {
                    // double the consonant (w[index4+1])
                    w = w.substring(0, index4 + 1) + w[index4 + 1] + w.substring(index4 + 1, w.length)
                }
            }

            // to force pronounciation, add j as last
            if(w.length>3 && w.endsWith("i") && w[w.length - 2] != 'k' && w[w.length - 2] != 'r'){
                //w = w.substring(0, w.length - 1) + "j"
                w = w.take(w.length - 1) + "j"
            }

            // pyöreä -> pyöree
            if(w.endsWith("ea") || w.endsWith("eä") ){
                //w = w.substring(0, w.length - 2) + "ee"
                w = w.take(w.length - 2) + "ee"
            }
            else {
                // halpaa->halpoo (actually -> halapoo)
                if (w.endsWith("a") && w[w.length - 2] != 'i' && vowels.contains(w[w.length - 2])) {
                    //w = w.substring(0, w.length - 2) + "oo"
                    w = w.take(w.length - 2) + "oo"
                }
            }

            /* onko sama kuin edellä
            if(w.endsWith("ea") ){
                //w = w.substring(0, w.length - 2) + "ee"
                w = w.take( w.length - 2) + "ee"
            } */

            // kalkkuna -> kalakkuna
            for(s in arrayOf("lh", "lj", "lk", "lm", "lp", "lv", "nh")) {
                w = convertInMiddle(w, s)
            }

            // diana -> tiana TODO: ends
            if(w.startsWith("d", true)){
                w = "t"+ w.substring(1, w.length - 1)
            }

            // ff-> hv

            // no double consonant on start: kristus -> ristus
            //if(!vowels.contains(w[0]) && !vowels.contains(w[1])){
            if(!isVowel(w[0]) && !isVowel(w[1])){
                w = if(w[1]=='h') {
                    w[0]+ w.substring(2, w.length)  // shekki -> sekki
                }
                else  {  w.substring(1, w.length)  }
            }

            // D and B replaced always
            // käden -> käen
            val index = w.indexOf("d")
            if(index in 1 until w.length){
                //if(index>0 && index < w.length-1){
                //val i1 = w[index - 1]
                w = if(/* w[index-1]=='u' || */ w[index + 1]=='u') {
                    w.replace("d", "v")
                } else {
                    w.replace("d", "")
                }
            }

            w= w.replace("b", "p")


            //var s3=""    // define as var for later use

            val middleMap: HashMap<String, String> = hashMapOf(
                "ai" to "ae",
                "au" to "aa",
                "ea" to "ee",
                "ei" to "ee"
            )

            /* kalaisa -> kalaesa
            s3="ai"
            w= convertInMiddle2(w, "ae",s3)

            // kolaus -> kollaas
            s3="au"
            w= convertInMiddle2(w, "aa",s3)

            // kolea -> kollee
            s3="ea"
            w= convertInMiddle2(w, "ee",s3)

            s3="ei"
            w= convertInMiddle2(w, "ee",s3)  */

            middleMap.forEach { (from, to) ->
                w= convertInMiddle2(w, from, to)
            }

            // TODO: more middle conversions
            // --------------------------------


            // 1ST SYLLABLE VOWELS

            val startMap: HashMap<String, String> = hashMapOf(
                "aa" to "ua", "ai" to "ae", "au" to "aa",
                "ee" to "ie", "ei" to "ee", "eu" to "eo",
                "oi" to "oe ", "ou" to "oo",
                "ui" to "ue", "yi" to "ye",
                "äi" to "äe", "äy" to "ää", "ää" to "iä",
                "öy" to "öö",
            )

            /*
            var s2="ua"
            w= convertInStart(w, "aa",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="ae"
            w= convertInStart(w, "ai",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="aa"
            w= convertInStart(w, "au",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="ie"
            w= convertInStart(w, "ee",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="ee"
            w= convertInStart(w, "ei",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="eo"
            w= convertInStart(w, "eu",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="oe"
            w= convertInStart(w, "oi",s2) // TODO: does it work ("voi"->"voi" mutta "voit"-> "voet")
            if(w != wordToConvert) return w //if(w.endsWith(s2)) w+"h" else w

            s2="oo"
            w= convertInStart(w, "ou",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="ue"
            w= convertInStart(w, "ui",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="ye"
            w= convertInStart(w, "yi",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="äe"
            w= convertInStart(w, "äi",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="ää"
            w= convertInStart(w, "äy",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="iä"
            w= convertInStart(w, "ää",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w

            s2="öö"
            w= convertInStart(w, "öy",s2)
            if(w != wordToConvert) return if(w.endsWith(s2)) w+"h" else w
    */
            startMap.forEach { (from, to) ->
                w= convertInStart(w, from, to)
                if(w != wordToConvert)
                    return if(w.endsWith(to)) w+"h" else w //(!(from == "oi" && w.endsWith("oe"))) w+"h" else w
            }

            return w
        }

        /**
         * Process 2 letters in start of word (1st or 2nd)
         * @param word - string to process
         * @param s1 - template for modifying (2 chars)
         * @param s2 - replace string (2 chars)
         * @return - modified string, if match, original otherwise
         */
        private fun convertInStart(word: String, s1: String, s2: String) :String {
            var w=word
            if(isSubMatch(word, 1, s1) || isSubMatch(word, 2, s1))
            //if ((word.length > 1 && word.substring( 0, 2 ) == s1) || (word.length > 2 && word.substring(1, 3) == s1))
            {
                w = word.replace(s1, s2)
            }
            return w
        }

        /**
         * process 2 letters starting from 2nd or 3rd
         * @param wordToConvert - string to process
         * @param s1 - template for modifying (2 chars)
         * @return - modified string, if match, original otherwise
         */
        private fun convertInMiddle(wordToConvert: String, s1: String) :String {
            var w=wordToConvert // to mutable
            if (s1.length >1 && wordToConvert.length > 3) {
                (0..1)
                    // double preceding vowel : kalja -> kalaja
                    .forEach { i ->
                        //if (wordToConvert.substring(1, 3) == s1 && vowels.contains(wordToConvert[0])) {
                        if (wordToConvert.substring(i+1, i+3) == s1 && vowels.contains(wordToConvert[i])) {
                            //w = wordToConvert.replace(s1, s1[0].toString() + wordToConvert[0] + s1[1])
                            w = wordToConvert.replace(s1, s1[0].toString() + wordToConvert[i] + s1[1])
                        }
/*
                    if (wordToConvert.substring(2, 4) == s1 && vowels.contains(wordToConvert[1])) {
                        w = wordToConvert.replace(s1, s1[0].toString() + wordToConvert[1] + s1[1])
                    }
                    */
                    }
            }
            return w
        }

        /**
         * process 2 letters starting from 1nd, 3rd or 4th
         * @param word - string to process
         * @param s1 - template for modifying (2 chars)
         * @param s2 - replace string (2 chars)
         * @return - modified string, if match, original otrherwise
         */
        private fun convertInMiddle2(word: String, s1: String, s2: String) :String {
            var w=word

            var matches =false
            (3..5).forEach { i -> if(isSubMatch(word, i, s1)) matches=true  }

            if(matches){ w = word.replace(s1, s2) }

            /*
            if (isSubMatch(word, 3, s1) ||
                isSubMatch(word, 4, s1) ||
                isSubMatch(word, 5, s1))
                //|| (wordToConvert.length > 5 && wordToConvert.substring(4,6) == s1))
            {
                w = word.replace(s1, s2)
            }*/
            return w
        }

        /**
         * does middle 2 chars match
         * @param wordToConvert - word to search
         * @param len - index to search
         * @param s1 - search template
         * @returns true if match found
         */
        private fun isSubMatch(wordToConvert: String, len: Int, s1: String) =
            (len in 1 until wordToConvert.length && wordToConvert.substring(len - 1, len + 1) == s1)
        //(len>0 && wordToConvert.length > len && wordToConvert.substring(len-1, len+1) == s1)

        /**
         * search triple consecutive vowels in text
         * @param w - word to search
         * @returns index of first occurrence
         */
        private fun indexOfTripleVowel(w: String):Int
        {
            for (index in 0 .. w.length-3) {
                if( w[index] in vowels  && w[index + 1] in vowels && w[index + 2] in vowels) {
                    //if (vowels.contains(w[index]) && vowels.contains(w[index + 1]) && vowels.contains(w[index + 2])) {
                    return index
                }
            }
            return -1
        }

        /**
         * search (none or consonent+) vowel+consonent+2xvowel  in text
         * @param w - word to search
         * @returns index of first occurrence
         */
        private fun indexOfVCVV(w: String):Int
        {
            for (index in 0 .. w.length-4) {
                if (index == 0 || !vowels.contains(w[index - 1])) {
                    if (vowels.contains(w[index]) && !vowels.contains(w[index + 1]) && vowels.contains(w[index + 2]) && vowels.contains(
                            w[index + 3]
                        )
                    ) { return index }
                }
            }
            return -1
        }
    }
}