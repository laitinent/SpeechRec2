package com.example.speechrec2


import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var myDataset = arrayListOf(ShoppingListItem("Ostoslista"))

    // grant permissions results handler
    private val getContent = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        // Handle the returned result (=true/false)

    }

    // handle language recognition result
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode  == Activity.RESULT_OK) {
            val resultdata = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            txtSpeechInput.text = resultdata?.get(0) ?: "NOT FOUND"
            val item = ShoppingListItem( resultdata?.get(0).toString())   // collected defaults false
            val checkedItem = ShoppingListItem( resultdata?.get(0).toString(),true)
            // not on list
            if(!myDataset.contains(item)) {
                myDataset.add(item)
                viewAdapter.notifyItemInserted(myDataset.size - 1)
            }
            else {
                // already listed
                val index = myDataset.indexOf(item)
                myDataset.get(index).collected = !myDataset.contains(checkedItem)
                viewAdapter.notifyItemChanged(index)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

/*      replaced my new androidx.activity:activity-ktx
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf( Manifest.permission.RECORD_AUDIO ), 1  )
            // See the documentation for ActivityCompat#requestPermissions for more details.
            return
        }*/

        btnSpeak.setOnClickListener {
           promptSpeechInput()
        }
        btnDelete.setOnClickListener {
            if(myDataset.isNotEmpty()) {
                myDataset.removeAt(myDataset.size-1)  // last
                viewAdapter.notifyItemRemoved(myDataset.size) // note. size, not size-1
            }
        }
    }

    /**
     * Showing google speech input dialog
    */
    private fun promptSpeechInput() {

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sano lisättävä tuote")
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
