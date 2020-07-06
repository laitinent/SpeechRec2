package com.example.speechrec2


import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.PersistableBundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

/**
 * "header" - see MyAdapter, it is using 2 layouts, another for header. That is, header is not part of list anymore
 */

class MainActivity : AppCompatActivity() , TextToSpeech.OnInitListener{

    private lateinit var tts: TextToSpeech
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    //private val strFirst="Ostoslista"
    private var myDataset = arrayListOf<ShoppingListItem>()

    // grant permissions results handler
    private val getContent = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        // Handle the returned result (=true/false)

    }

    // handle language recognition result
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode  == Activity.RESULT_OK) {
            val resultdata = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val word = resultdata?.get(0) ?:"NOT FOUND"
            txtSpeechInput.text =  word //?: "NOT FOUND"

            if(word.toLowerCase().startsWith("poista"))
            {
                // removeAt
                val words = word.split(" ")
                if(words.size>1) {
                    val itemToRemove = ShoppingListItem(words[1])   // collected defaults false
                    val checkedItemToRemove = ShoppingListItem(words[1], true)
                    if (!myDataset.remove(itemToRemove)) {
                        myDataset.remove(checkedItemToRemove)
                    }
                    viewAdapter.notifyDataSetChanged()
                }
            }
            else {
                // Add
                val item = ShoppingListItem(word)   // collected defaults false
                val checkedItem = ShoppingListItem(word, true)
                // not on list, even checked version
                if (!myDataset.contains(item) && !myDataset.contains(checkedItem)) {
                    myDataset.add(item)
                    viewAdapter.notifyItemInserted(myDataset.size - 1)
                } else {
                    // already listed
                    var index = myDataset.indexOf(item)
                    if (index == -1) {
                        index = myDataset.indexOf(checkedItem)
                    }
                    myDataset.get(index).collected = !myDataset.contains(checkedItem)
                    viewAdapter.notifyItemChanged(index)
                }

                val words = word.split(" ")
                var savoWords = ""
                //if(words.size>1) {
                // translate each word
                    for(w in words) {
                        savoWords+= (toSavo(w) + " ")

                    }
                // output whole sentence
                speakOut(savoWords)
               // }
            }
        }
    }

    private val vowels=arrayOf('a','e','i','o','u','y','ä','ö')
    public fun toSavo(wordToConvert :String) : String
    {
        var w = wordToConvert
        if(w.endsWith("io")){
            w = w.replace("io","ijo")
        }

        if(w.endsWith("i") && w[w.length-2] != 'k' && w[w.length-2] != 'r'){
            w = w.substring(0, w.length-1) + "j"
        }

        // halpaa->halpoo
        if(w.endsWith("a") && w[w.length-2] != 'i' && vowels.contains(w[w.length-2])){
            w = w.substring(0, w.length-2) + "oo"
        }


        for(s in arrayOf("lh","lj","lk","lp","lv","nh")) {
            w = convertInMiddle(w, s);
        }

        // diana -> tiana TODO: ends
        if(w.startsWith("d", true)){
            w = "t"+ w.substring(1, w.length-1)
        }

        // kristus -> ristus
        if(!vowels.contains(w[0]) && !vowels.contains(w[1])){
            w = if(w[1]=='h') {
                w[0]+ w.substring(2, w.length)
            }
            else
            {
                w.substring(1, w.length)
            }
        }

        // käden -> käen
        val index = w.indexOf("d")
        if(index>0 && index < w.length-1){
            val i1 = w[index-1]
            w = if(w[index-1]=='u' || w[index+1]=='u') {
                w.replace("d", "v")
            } else {
                w.replace("d", "")
            }
        }
        
        w= w.replace("b", "p")

        w= convertInStart(w, "aa","ua")
        if(w != wordToConvert)return w

        w= convertInStart(w, "ai","ae")
        if(w != wordToConvert)return w

        w= convertInStart(w, "au","aa")
        if(w != wordToConvert)return w

        w= convertInStart(w, "ee","ie")
        if(w != wordToConvert)return w

        w= convertInStart(w, "ei","ee")
        if(w != wordToConvert)return w

        w= convertInStart(w, "eu","eo")
        if(w != wordToConvert)return w


        w= convertInStart(w, "oi","oe")
        if(w != wordToConvert)return w

        w= convertInStart(w, "ou","oo")
        if(w != wordToConvert)return w

        w= convertInStart(w, "ui","ue")
        if(w != wordToConvert)return w

        w= convertInStart(w, "yi","ye")
        if(w != wordToConvert)return w

        w= convertInStart(w, "äi","äe")
        if(w != wordToConvert)return w

        w= convertInStart(w, "äy","ää")
        if(w != wordToConvert)return w

        w= convertInStart(w, "ää","iä")
        if(w != wordToConvert)return w

        w= convertInStart(w, "öy","öö")
        if(w != wordToConvert)return w



        return w
    }

    /**
     * Process 2 letters in start of word (1st or 2nd)
     * @param wordToConvert - string to process
     * @param s1 - template for modifying (2 chars)
     * @param s2 - replace string (2 chars)
     * @return - modified string, if match, original otrherwise
     */
    private fun convertInStart(wordToConvert: String, s1: String, s2:String) :String {
        var w=wordToConvert
        if (wordToConvert.length > 2 && (wordToConvert.substring(
                0,
                2
            ) == s1 || wordToConvert.substring(1, 3) == s1)
        ) {
            w = wordToConvert.replace(s1, s2)
        }
        return w
    }

    /**
     * process 2 letters starting from 2nd or 3rd
     * @param wordToConvert - string to process
     * @param s1 - template for modifying (2 chars)
     * @return - modified string, if match, original otrherwise
     */
    private fun convertInMiddle(wordToConvert: String, s1: String) :String {
        var w=wordToConvert // to mutable
        if (s1.length >1 && wordToConvert.length > 3) {

            // double preceding vowel : kalja -> kalaja
            if (wordToConvert.substring(1, 3) == s1 && vowels.contains(wordToConvert[0])) {
                w = wordToConvert.replace(s1, s1[0].toString() + wordToConvert[0] + s1[1])
            }

            if (wordToConvert.substring(2, 4) == s1 && vowels.contains(wordToConvert[1])) {
                w = wordToConvert.replace(s1, s1[0].toString() + wordToConvert[1] + s1[1])
            }
        }
        return w
    }
/*
    private fun isDoubleVowel(w:String):Boolean
    {
        if(w.indexOf())
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //if(savedInstanceState != null) {
            try {
                val type = object : TypeToken<ArrayList<ShoppingListItem>>() {}.type
                //myDataset.addAll(
                val data =
                    Gson().fromJson<ArrayList<ShoppingListItem>>(
                    savedInstanceState?.getString("lista"), type
                )

                //if (
                    myDataset.addAll(data)    // using header
                /*){
                    myDataset.removeAt(0)
                    viewAdapter.notifyItemRemoved(0)
                }*/
            } catch (ex: Exception) {
                print(ex.message)
            }
        //}
        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(myDataset)

        recyclerView.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            txtSpeechInput.text = "No voice recognition support on your device!"
        }

        getContent.launch(Manifest.permission.RECORD_AUDIO)

        tts = TextToSpeech(this, this)

/*      replaced my new androidx.activity:activity-ktx
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf( Manifest.permission.RECORD_AUDIO ), 1  )
            // See the documentation for ActivityCompat#requestPermissions for more details.
            return
        }*/

        // Listen and add
        btnSpeak.setOnClickListener {
           promptSpeechInput()
        }
        // undo add
        btnDelete.setOnClickListener {
            if(!myDataset.isEmpty()) {  // leave header at [0] (when using header, not used
                myDataset.removeAt(myDataset.size-1)  // last
                recyclerView.adapter?.notifyItemRemoved(myDataset.size) // note. size, not size-1
            }
            // TODO: should this undo strike through (now repeated same item toggles)
            
        }

        // reset
        btnDelete.setOnLongClickListener {
            if(!myDataset.isEmpty()) {
                val builder = AlertDialog.Builder(this)

                with(builder)
                {
                    setTitle("Uusi lista")
                    setMessage("Haluatko tyhjentää listan?")
                    setPositiveButton(
                        android.R.string.yes,
                        DialogInterface.OnClickListener(function = positiveButtonClick)
                    )
                    setNegativeButton(android.R.string.no, negativeButtonClick)
                    //setNeutralButton("Maybe", neutralButtonClick)
                    show()
                }
            }
            true
        }
    }


    public override fun onDestroy() {
        // Shutdown TTS
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    private fun speakOut(text: String) {
        //val text = editText!!.text.toString()
        tts!!.speak(text, TextToSpeech.QUEUE_ADD, null,"") // was QUEUE_FLUSH
    }

    private val positiveButtonClick = { _: DialogInterface, _: Int ->
        //if(myDataset.size>1) {  // leave header at [0]
            //myDataset.subList(1, myDataset.size).clear();  // last
            myDataset.clear();  // last (using header)
            recyclerView.adapter?.notifyDataSetChanged() // note. size, not size-1
            saveSharedPrefs()
        //}
        Toast.makeText(applicationContext,
            android.R.string.yes, Toast.LENGTH_SHORT).show()
    }
    private val negativeButtonClick = { _: DialogInterface, _: Int ->
        Toast.makeText(applicationContext,
            android.R.string.no, Toast.LENGTH_SHORT).show()
    }
    private val neutralButtonClick = { _: DialogInterface, _: Int ->
        Toast.makeText(applicationContext,
            "Maybe", Toast.LENGTH_SHORT).show()
    }

    /**
     * Showing google speech input dialog
    */
    private fun promptSpeechInput() {

        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sano lisättävä tuote\n(voit poistaa \"poista ...\") ")
        }.run {
            try {
                startForResult.launch(this)
                //startActivityForResult(this, REQ_CODE_SPEECH_INPUT)
            } catch (a: ActivityNotFoundException) {
                Toast.makeText(
                    applicationContext,"Sorry! Your device doesn't support speech input",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putString("lista",Gson().toJson(myDataset))
        super.onSaveInstanceState(outState, outPersistentState)

    }

    override fun onPause() {
        saveSharedPrefs()
        super.onPause()
    }

    private fun saveSharedPrefs() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("SavedLista", Gson().toJson(myDataset))
        editor.commit()
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        try {
            val type = object : TypeToken<ArrayList<ShoppingListItem>>() {}.type
            val data = Gson().fromJson<ArrayList<ShoppingListItem>>(
                sharedPref?.getString("SavedLista",""),
                type
            )
            // restore only if reset
            if(myDataset.isEmpty()) {   // was <=1 before header

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
        }
        catch(ex: Exception){
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

}
