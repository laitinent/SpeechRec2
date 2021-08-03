package com.example.speechrec2


import android.Manifest
import android.R.string
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings.Global.getString
import android.speech.RecognizerIntent.*
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Layout
import android.widget.LinearLayout
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
//import androidx.activity.compose.registerForActivityResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.speechrec2.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.moshi.Moshi
import java.lang.reflect.Type
//import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

/**
 * "header" - see MyAdapter, it is using 2 layouts, another for header. That is, header is not part of list anymore
 */

class MainActivity : AppCompatActivity(/*R.layout.activity_main*/), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    //private val strFirst="Ostoslista"
    private var myDataset = arrayListOf<ShoppingListItem>()
    private lateinit var moshi: Moshi
    private lateinit var binding: ActivityMainBinding


    private val type: Type = object : TypeToken<ArrayList<ShoppingListItem>>() {}.type // for sharedprefs

    // grant permissions results handler
    private val getContent =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Handle the returned result (=true/false)
        }

    // handle language recognition result
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { (resultCode, data) -> //: ActivityResult//result: ActivityResult ->
            if (/*result.*/resultCode == Activity.RESULT_OK) {
                val resultdata = /*result.*/data?.getStringArrayListExtra(EXTRA_RESULTS)
                val wordOrig = resultdata?.get(0) ?: "NOT FOUND"
                val word = wordOrig.toLowerCase(Locale.getDefault())
                binding.txtSpeechInput.text = word //?: "NOT FOUND"

                if (word.startsWith("poista")) {
                    // removeAt
                    val words = word.split(" ")
                    if (words.size > 1) {
                        // words[0] = "poista"
                        val itemToRemove = ShoppingListItem(words[1])   // collected defaults false
                        val checkedItemToRemove = ShoppingListItem(words[1], true)

                        //if (!myDataset.remove(itemToRemove)) { myDataset.remove(checkedItemToRemove) }
                        if (myDataset.remove(itemToRemove) || myDataset.remove(checkedItemToRemove))
                            viewAdapter.notifyDataSetChanged()
                    }
                } else {
                    // Add
                    val item = ShoppingListItem(word)   // collected defaults false
                    val checkedItem = ShoppingListItem(word, true)
                    // not on list, even checked version
                    if (item !in myDataset && checkedItem !in myDataset) {
                        myDataset.add(item)
                        viewAdapter.notifyItemInserted(myDataset.size) // was - 1
                        //viewAdapter.notifyDataSetChanged()
                    } else {
                        // already listed
                        // item not collected?
                        var index = myDataset.indexOf(item)
                        // item already collected?
                        if (index == -1) {
                            index = myDataset.indexOf(checkedItem)
                        }
                        // reverse collected status
                        myDataset[index].collected = checkedItem !in myDataset
                        viewAdapter.notifyItemChanged(index)
                    }

                    //val words = word.split(" ")
                    var savoWords = ""
                    //if(words.size>1) {
                    // translate each word
                    word.split(" ").forEach { w ->
                        savoWords += (SavoConverter.toSavo(w) + " ")
                    }
                    // output whole sentence
                    speakOut(savoWords)
                    // }
                }
            }
        }
/* see SavoConverter.kt
    fun isVowel(ch: Char): Boolean {
        return when (ch) {
            'a', 'e', 'i', 'o', 'u', 'y', 'å', 'ä', 'ö' -> true
            else -> false
        }
    }

    private val vowels=arrayOf('a', 'e', 'i', 'o', 'u', 'y', 'ä', 'ö')

    private fun toSavo(wordToConvert: String) : String
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
*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        //val view = binding.root
        //setContentView(view)//R.layout.activity_main)
        setContent {
            MaterialTheme {
                Greeting(allItems = myDataset, tts)
            }
        }

        moshi = Moshi.Builder()
            //.add(KotlinJsonAdapterFactory()
            .build()
        //val listMyData: Type = Types.newParameterizedType( MutableList::class.java, ShoppingListItem::class.java  )
        //var adapter: JsonAdapter<List<MyData?>?>? = moshi.adapter<Any?>(listMyData)
        //val jsonAdapter: JsonAdapter<ArrayList<ShoppingListItem>> = moshi.adapter<Any>(BlackjackHand::class.java)
        //if(savedInstanceState != null) {
        try {
            // list was saved as JSON string
            //val type = object : TypeToken<ArrayList<ShoppingListItem>>() {}.type
            myDataset.addAll(
                /*val data =*/ Gson().fromJson<ArrayList<ShoppingListItem>>(
                    savedInstanceState?.getString("lista"), type
                )
            )

            //if (
            //myDataset.addAll(data)    // using header
            /*){
                myDataset.removeAt(0)
                viewAdapter.notifyItemRemoved(0)
            }*/
        } catch (ex: Exception) {
            print(ex.message)
        }
        //}

        // recyclerView stuff
        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(myDataset)

        binding.recyclerView.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        tts = TextToSpeech(this, this)

        // speech & text-to-speech
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            binding.txtSpeechInput.text = getString(R.string.txt_no_support)
        }

        getContent.launch(Manifest.permission.RECORD_AUDIO)


/*      replaced my new androidx.activity:activity-ktx
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf( Manifest.permission.RECORD_AUDIO ), 1  )
            // See the documentation for ActivityCompat#requestPermissions for more details.
            return
        }*/

        // Buttons
        // Listen and add
        binding.btnSpeak.setOnClickListener { promptSpeechInput() }
        // undo add
        binding.btnDelete.setOnClickListener {
            if (myDataset.isNotEmpty()) {  // leave header at [0] (when using header, not used
                myDataset.removeLast() //removeAt(myDataset.size - 1)  // last
                binding.recyclerView.adapter?.notifyItemRemoved(myDataset.size) // note. size, not size-1
            }
            // TODO: should this undo strike through (now repeated same item toggles)
            // !! TESTING !!
            //val s = toSavo("voi")
            //print (s)
        }

        // reset
        binding.btnDelete.setOnLongClickListener {
            if (myDataset.isNotEmpty()) {
                val builder = AlertDialog.Builder(this)

                with(builder)
                {
                    setTitle("Uusi lista")
                    setMessage("Haluatko tyhjentää listan?")
                    setPositiveButton(
                        string.ok,
                        DialogInterface.OnClickListener(function = positiveButtonClick)
                    )
                    setNegativeButton(string.cancel, negativeButtonClick)
                    //setNeutralButton("Maybe", neutralButtonClick)
                    show()
                }
            }
            true
        }
    }


    public override fun onDestroy() {
        // Shutdown TTS
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    private fun speakOut(text: String) {
        //val text = editText!!.text.toString()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "") // was QUEUE_FLUSH
    }

    private val positiveButtonClick = { _: DialogInterface, _: Int ->
        //if(myDataset.size>1) {  // leave header at [0]
        //myDataset.subList(1, myDataset.size).clear();  // last
        myDataset.clear()  // last (using header)
        binding.recyclerView.adapter?.notifyDataSetChanged() // note. size, not size-1
        saveSharedPrefs()
        //}
        makeText(applicationContext, string.ok, LENGTH_SHORT).show()
    }

    private val negativeButtonClick = { _: DialogInterface, _: Int ->
        makeText(applicationContext, string.cancel, LENGTH_SHORT).show()
    }

    /* private val neutralButtonClick = { _: DialogInterface, _: Int ->
        Toast.makeText(applicationContext, "Maybe", Toast.LENGTH_SHORT).show() } */

    /**
     * Showing google speech input dialog
     */
    private fun promptSpeechInput() {

        Intent(ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                EXTRA_LANGUAGE_MODEL,
                LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(EXTRA_PROMPT, "Sano lisättävä tuote\n(voit poistaa \"poista ...\") ")
        }.run {
            try {
                startForResult.launch(this)
                //startActivityForResult(this, REQ_CODE_SPEECH_INPUT)
            } catch (a: ActivityNotFoundException) {
                makeText(
                    applicationContext, getString(R.string.txtNotSupported),
                    LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putString("lista", Gson().toJson(myDataset))
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onPause() {
        saveSharedPrefs()
        super.onPause()
    }

    private fun saveSharedPrefs() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        with(editor)
        {
            putString("SavedLista", Gson().toJson(myDataset))
            apply()
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        try {
            //val type = object : TypeToken<ArrayList<ShoppingListItem>>() {}.type
            val data = Gson().fromJson<ArrayList<ShoppingListItem>>(
                sharedPref?.getString("SavedLista", ""), type
            )
            // restore only if reset
            if (myDataset.isEmpty()) {   // was <=1 before header
                //if (
                myDataset.addAll(data)
                /*&& myDataset.size > 1) {
        // Remove duplicates
        val index = myDataset.lastIndexOf(ShoppingListItem(strFirst))
        if (index > 0) {
            myDataset.removeAt(index)
            viewAdapter.notifyItemRemoved(index)
        }
    }*/
            }
        } catch (ex: Exception) {
            print(ex.message)
        }
    }

    override fun onInit(p0: Int) {
        //TODO("Not yet implemented")
    }

    /* unused, because replaced by new androidx.activity:activity-ktx

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

            when (requestCode and 0xFFFF) {
                1 -> true
                REQ_CODE_SPEECH_INPUT -> {
                    if (resultCode == RESULT_OK) {
                        val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        txtSpeechInput.text = result?.get(0) ?: "NOT FOUND"
                    }
                }
                else -> false
            }
            super.onActivityResult(requestCode, resultCode, data)
        }*/
    operator fun ActivityResult.component1() = resultCode
    operator fun ActivityResult.component2() = data
}

@Composable
fun Greeting(allItems: ArrayList<ShoppingListItem>, tts:TextToSpeech?) {
    Column {
        Row { //(modifier=Modifier.fillMaxHeight())
            LazyColumn {
                items(allItems)
                {
                    Text("Item is ${it.title}")
                }
            }
        }
        //Row{ Spacer(modifier = Modifier.fillMaxHeight()) }
        Row {
            //Text(text = "Hello $name!")
            Button(onClick = {
                if (allItems.isNotEmpty()) {  // leave header at [0] (when using header, not used
                    allItems.removeLast() //removeAt(myDataset.size - 1)  // last
                }
            }, modifier = Modifier.padding(6.dp)) {
                Text(text = "Add")
            }
            Button(onClick = {
            /*TODO*/

            })
            {
                Text(text = "Remove")
            }
            //https://android-review.googlesource.com/c/platform/frameworks/support/+/1569321/7/activity/activity-compose/samples/src/main/java/androidx/activity/compose/samples/RegisterForActivityResultSample.kt
            //val result = remember { mutableStateOf<Bitmap?>(null) }
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    //result.value = it
                }

            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text(text = "Permission")
            }

            /*
            result.value?.let { image ->
               // Image(image.asImageBitmap(), null, modifier = Modifier.fillMaxWidth())
            }*/

            // and another
            val result2 = remember { mutableStateOf<ActivityResult?>(null) }
            val launcher2 = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                //result.value = it
            }

            Intent(ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    EXTRA_LANGUAGE_MODEL,
                    LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(EXTRA_PROMPT, "Sano lisättävä tuote\n(voit poistaa \"poista ...\") ")
            }.run {
                //    startForResult.launch(this)
                Button(onClick = { launcher2.launch(this) }) {
                    Text(text = "Add2")
                }
            }

            result2.value?.let {
                if (it.resultCode == Activity.RESULT_OK) {
                    val resultdata = it.data?.getStringArrayListExtra(EXTRA_RESULTS)
                    val wordOrig = resultdata?.get(0) ?: "NOT FOUND"
                    val word = wordOrig.toLowerCase(Locale.getDefault())
                    //binding.txtSpeechInput.text = word //?: "NOT FOUND"

                    if (word.startsWith("poista")) {
                        // removeAt
                        val words = word.split(" ")
                        if (words.size > 1) {
                            // words[0] = "poista"
                            val itemToRemove = ShoppingListItem(words[1])   // collected defaults false
                            val checkedItemToRemove = ShoppingListItem(words[1], true)

                            //if (!myDataset.remove(itemToRemove)) { myDataset.remove(checkedItemToRemove) }
                            if (allItems.remove(itemToRemove) || allItems.remove(checkedItemToRemove)) {
                                //viewAdapter.notifyDataSetChanged()
                            }
                        }
                    } else {
                        // Add
                        val item = ShoppingListItem(word)   // collected defaults false
                        val checkedItem = ShoppingListItem(word, true)
                        // not on list, even checked version
                        if (item !in allItems && checkedItem !in allItems) {
                            allItems.add(item)
                            //viewAdapter.notifyItemInserted(allItems.size) // was - 1
                        } else {
                            // already listed
                            // item not collected?
                            var index = allItems.indexOf(item)
                            // item already collected?
                            if (index == -1) {
                                index = allItems.indexOf(checkedItem)
                            }
                            // reverse collected status
                            allItems[index].collected = checkedItem !in allItems
                            //viewAdapter.notifyItemChanged(index)
                        }

                        //val words = word.split(" ")
                        var savoWords = ""
                        //if(words.size>1) {
                        // translate each word
                        word.split(" ").forEach { w ->
                            savoWords += (SavoConverter.toSavo(w) + " ")
                        }
                        // output whole sentence
                        // TODO: uncomment:  speakOut(savoWords)
                        tts?.speak(savoWords, TextToSpeech.QUEUE_FLUSH, null, "") // was QUEUE_FLUSH
                        // }
                    }
                }
            }

        }

    }
}

@Preview
@Composable
fun ComposablePreview() {
    Greeting(arrayListOf(ShoppingListItem("Maitoa")),null)
}



