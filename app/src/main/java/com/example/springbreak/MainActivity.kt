package com.example.springbreak
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val REQ_CODE_SPEECH_INPUT = 100
    private val RECORD_AUDIO_PERMISSION_CODE = 101
    private lateinit var speaker_edit_text: EditText // Declaring EditText variable
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastUpdate: Long = 0
    private val SHAKE_THRESHOLD = 800 // Adjust this threshold according to your requirements
    private var selectedLanguage: String = "" // Variable to store the selected language
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializing the EditText after setContentView
        speaker_edit_text = findViewById(R.id.speaker_edit_text)

        val languageListView: ListView = findViewById(R.id.language_list_view)
        val languages = arrayOf("English", "Spanish", "French", "Chinese")

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, languages)
        languageListView.adapter = adapter

        languageListView.setOnItemClickListener { parent, view, position, id ->
            selectedLanguage = languages[position]
            promptSpeechInput(selectedLanguage)
        }

        // Initialize sensor manager and accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Register sensor listener
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister sensor listener to avoid battery drain
        sensorManager.unregisterListener(this)
    }

    private fun promptSpeechInput(language: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLanguageCode(language))
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(applicationContext, "Speech input not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLanguageCode(language: String): String {
        return when (language) {
            "English" -> "en-US"
            "Spanish" -> "es-ES"
            "French" -> "fr-FR"
            "Chinese" -> "zh-CN"
            else -> "en-US" // Default to English
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: "" // Get the first recognized text, or an empty string if none
            speaker_edit_text.setText(spokenText)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val language_list_view = findViewById<ListView>(R.id.language_list_view)
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdate > 100) {
                val diffTime = currentTime - lastUpdate
                lastUpdate = currentTime

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val speed = Math.sqrt((x.toDouble() * x.toDouble() + y.toDouble() * y.toDouble() + z.toDouble() * z.toDouble()).toDouble()) / diffTime * 10000

                if (speed > SHAKE_THRESHOLD) {
                    if (selectedLanguage.isNotEmpty()) {
                        openGoogleMaps(selectedLanguage)
                    } else {
                        Toast.makeText(this, "Please select a language from the list", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    private fun openGoogleMaps(language: String) {
        val coordinates = when (language) {
            "English" -> listOf(
                "51.5072° N, 0.1276° W",
                "27.6648° N, 81.5158° W",
                "1.3521° N, 103.8198° E"
            )
            "French" -> listOf(
                "48.8566° N, 2.3522° E",
                "18.7669° S, 46.8691° E",
                "50.8476° N, 4.3572° E"
            )
            "Chinese" -> listOf(
                "23.6978° N, 120.9605° E",
                "39.9042° N, 116.4074° E",
                "31.2304° N, 121.4737° E"
            )
            "Spanish" -> listOf(
                "40.4168° N, 3.7038° W",
                "22.9068° S, 43.1729° W",
                "21.1619° N, 86.8515° W"
            )
            else -> emptyList() // Default to empty list if language not found
        }

        val randomIndex = (0 until coordinates.size).random()
        val selectedCoordinates = coordinates[randomIndex]
        val resourceId = getResourceIdForLanguage(language)
        if (resourceId != 0) {
            mediaPlayer = MediaPlayer.create(this, resourceId)
            mediaPlayer.start()
        }
        val mapIntent = Intent(Intent.ACTION_VIEW)
        mapIntent.data = Uri.parse("geo:0,0?q=$selectedCoordinates")
        startActivity(mapIntent)

    }
    private fun getResourceIdForLanguage(language: String): Int {
        return when (language) {
            "English" -> R.raw.english_hello
            "Spanish" -> R.raw.spanish_hola
            "French" -> R.raw.french_bonjour
            "Chinese" -> R.raw.chinese_nihao
            else -> 0 // Default to English
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}