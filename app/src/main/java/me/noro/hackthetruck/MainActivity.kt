/*
 * Copyright (c) 2018. Daimler AG.
 */

package me.noro.hackthetruck

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.CountDownTimer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.support.constraint.ConstraintSet
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import me.noro.hackthetruck.repository.vehicle.IVehicleDataSubscriber
import me.noro.hackthetruck.repository.vehicle.VehicleDataRepository
import me.noro.hackthetruck.services.DataSimulationService
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.OnEngineInitListener
import com.here.android.mpa.common.ViewObject
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapFragment
import com.here.android.mpa.mapping.MapGesture
import com.here.android.mpa.mapping.MapMarker
import java.io.IOException
import java.util.*

//import java.io.InputStream


///
// Here we have our basic main activity. This is the entry point of connecting to the vehicle
///
class MainActivity : AppCompatActivity(), IVehicleDataSubscriber {


    private val WARNING_SPEED = 100
    private val WARNING_FUEL = 101
    private val WARNING_DISTANCE = 102
    private val MAX_FUEL_CONSUMPTION_WARNING = 44.9
    private val MAX_SPEED_WARNING = 94
    private val MAX_COUNTDOWN_TIME: Long = 10000
    private var isRecording = false
    private var countDownTimer: CountDownTimer? = null
    private var map: Map? = null
    private var mapFragment: MapFragment? = null
    private var speechService: SpeechRecognizer? = null
    private var droppedOverSpeed = false //just for demo so that we don't get 2 or more markers
    private var droppedFuelConsumption = false //just for demo so that we don't get 2 or more markers

    companion object {
        private const val TAG = "MAIN"
    }

    // Define the variable for our vehicle repository, which will handle the vehicle connection
    // This will be done, when the activity is created
    lateinit var vehicleDataRepository: VehicleDataRepository

    // Define the variable for our data simulation service, which will give us defined simulated
    // values from the vehicle FMS interface or CAN bus
    lateinit var dataSimulationService: DataSimulationService

    // Define a helper variable for tracking if this activity is in foreground. So we are able to
    // handle some things that are not useful while in background
    private var isInForeground: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askForPermession()
        // Here we inject everything defined within the injector like any depending services
        // or repositories
        Injector.inject(this)

        // After having all dependencies, we can now register this activity as a listener on
        // vehicle related messages
        vehicleDataRepository.register(this)

        // Finally just connect this application to the vehicle
        connectVehicle(applicationContext)
        initialize()
    }

    private fun initialize() {

        speechService = SpeechRecognizer.createSpeechRecognizer(applicationContext)
        speechService?.setRecognitionListener(DriverRecognitionListener())

        countDownTextView.text = "10"
        countDownTextView.visibility = View.GONE

        playConstraintLayout.visibility = View.GONE

        recordImageButton.setOnClickListener {
            isRecording = !isRecording

            if (isRecording) {
                startListening()

            }else {
                speechService?.cancel()

            }
        }

        companyBtn.setOnClickListener {
            channelConstraintLayout.visibility = View.GONE
            dropMakerOnMap()

        }

        privateBtn.setOnClickListener {
            channelConstraintLayout.visibility = View.GONE
            dropMakerOnMap()
        }

        publicBtn.setOnClickListener {
            channelConstraintLayout.visibility = View.GONE
            dropMakerOnMap()
        }

        dismissButton.setOnClickListener {
            togglePlayBar()
        }

        mapFragment = fragmentManager.findFragmentById(R.id.mapfragment) as? MapFragment
        mapFragment?.init(object : OnEngineInitListener {
            override fun onEngineInitializationCompleted(error: OnEngineInitListener.Error) {
               if (error == OnEngineInitListener.Error.NONE) {
                   map = mapFragment?.map
                   if (map != null) {
                       val geoCoordinate = GeoCoordinate(60.1867, 24.8277, 0.0)
                       map!!.setCenter(geoCoordinate, Map.Animation.NONE)
                       map!!.zoomLevel = (map!!.maxZoomLevel + map!!.minZoomLevel) / 2
                   } else {
                       System.out.println("ERROR: map is null");
                   }

                   if (mapFragment?.mapGesture != null) {
                       print("ok")
                   }
                   mapFragment?.mapGesture?.addOnGestureListener(object: MapGesture.OnGestureListener.OnGestureListenerAdapter() {
                       override fun onLongPressRelease() {
                       }

                       override fun onRotateEvent(p0: Float): Boolean {
                           return false
                       }

                       override fun onMultiFingerManipulationStart() {
                       }

                       override fun onMultiFingerManipulationEnd() {
                       }

                       override fun onPinchLocked() {
                       }

                       override fun onPinchZoomEvent(p0: Float, p1: PointF?): Boolean {
                           return false
                       }

                       override fun onTapEvent(p0: PointF?): Boolean {
                           return true
                       }

                       override fun onPanStart() {
                       }

                       override fun onDoubleTapEvent(p0: PointF?): Boolean {
                           return false
                       }

                       override fun onPanEnd() {
                       }

                       override fun onTiltEvent(p0: Float): Boolean {
                           return false
                       }

                       override fun onRotateLocked() {
                       }

                       override fun onLongPressEvent(p0: PointF?): Boolean {
                           return false
                       }

                       override fun onTwoFingerTapEvent(p0: PointF?): Boolean {
                           return false
                       }

                       override fun onMapObjectsSelected(objects: MutableList<ViewObject>): Boolean {
                           print("ok")
                           for (marker in objects) {
                               togglePlayBar()
                           }
                           return false
                       }


                   })

               }else  {

                   System.out.println("ERROR: Cannot initialize Map Fragment $error");
               }
            }

        })







    }

    private fun askForPermession() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            print("not grandted")
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.RECORD_AUDIO)) {

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        1)

            }
        } else {
           print("ok")
        }
    }


    private fun startListening(){

        countDownTextView.text = "${MAX_COUNTDOWN_TIME / 1000}"
        countDownTextView.visibility = View.VISIBLE
        micImageView.visibility = View.GONE

        val mSpeechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                applicationContext.packageName);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_RESULTS,true)
        speechService?.startListening(mSpeechRecognizerIntent)


    }

    fun countDownListener() {
        countDownTimer = object: CountDownTimer(MAX_COUNTDOWN_TIME, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countDownTextView.text = "${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                countDownTextView.visibility = View.GONE
                micImageView.visibility = View.VISIBLE
            }
        }

        countDownTimer?.start()
    }

    fun endCountDownListener() {
        countDownTimer?.cancel()
        countDownTextView.visibility = View.GONE
        micImageView.visibility = View.VISIBLE
    }


    fun dropMakerOnMap(bagOfWords: ArrayList<String>? = null){

        val icons = listOf<Int>(
            R.drawable.map_icon_joke,
            R.drawable.map_icon_park,
            R.drawable.map_icon_speed,
            R.drawable.map_icon_toilet,
                R.drawable.map_icon_slip)

        val random = icons.shuffled().take(1)[0]

        val myImage = com.here.android.mpa.common.Image()
        try {
            myImage.setImageResource(random)
        } catch (e: IOException) {
            finish()
        }

        val lat = 60.186 + ((Random().nextInt((9 + 1) - 1) +  1).toFloat() / 100)
        val lng = 24.827 +  ((Random().nextInt((9 + 1) - 1) +  1).toFloat() / 100)

        val myMapMarker = MapMarker(GeoCoordinate(lat, lng, 0.0),myImage )

        map?.addMapObject(myMapMarker);
    }

    fun dropWarningMakerOnMap(id: Int){
        var markerIcon = R.drawable.warning_speed_icon

       when(id) {
           WARNING_SPEED -> markerIcon = R.drawable.warning_speed_icon
           WARNING_FUEL -> markerIcon = R.drawable.warning_fuel_icon
       }

        val myImage = com.here.android.mpa.common.Image()
        try {
            myImage.setImageResource(markerIcon)
        } catch (e: IOException) {
            finish()
        }

        val lat = 60.186 + ((Random().nextInt((9 + 1) - 1) +  1).toFloat() / 100)
        val lng = 24.827 +  ((Random().nextInt((9 + 1) - 1) +  1).toFloat() / 100)

        val myMapMarker = MapMarker(GeoCoordinate(lat, lng, 0.0),myImage )

        map?.addMapObject(myMapMarker);
    }



    private fun togglePlayBar() {
        if (playConstraintLayout.visibility == View.VISIBLE) {
            sideConstraintLayout.visibility = View.VISIBLE
            playConstraintLayout.visibility = View.GONE
        } else {
            sideConstraintLayout.visibility = View.GONE
            playConstraintLayout.visibility = View.VISIBLE
        }
       /*
       val constraintSetExpanded = ConstraintSet()
       constraintSetExpanded.clone(playConstraintLayout)
       val constraintSetShink = ConstraintSet()
       constraintSetShink.clone(playConstraintLayout)
       constraintSetShink.setTranslationX(R.id.playConstraintLayout, -200.0f)
       TransitionManager.beginDelayedTransition(playConstraintLayout)
       val constraint = constraintSetShink
       constraint.applyTo(playConstraintLayout)
       */
   }

    private fun connectVehicle(context: Context) {
        try {
            if (vehicleDataRepository.initializeSdk(context)) {
                vehicleDataRepository.connectVehicle(context)
                Toast.makeText(context, "Connection to vehicle established", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't connect to the vehicle due to $e")
            Toast.makeText(context, "Connection to vehicle couldn't be established", Toast.LENGTH_LONG).show()
        }
    }

    override fun onVehicleSpeed(speed: Float) {
        Log.i(TAG, "Current vehicle speed: $speed km/h")

        // Only do something with incoming values, if the app is in foreground
        if (isInForeground) {
            // Show the new incoming value on the ui
            txt_info.text = speed.toInt().toString() + " km/h"
            if (speed.toInt() >= MAX_SPEED_WARNING) {
                if (!droppedOverSpeed) {
                    droppedOverSpeed = true
                    dropWarningMakerOnMap(WARNING_SPEED)
                }
            }
        }
    }

    override fun onFuelConsumptionHigh(amount: Float) {
        Log.i(TAG, "Current total vehicle distance: $amount")

        // Only do something with incoming values, if the app is in foreground
        if (isInForeground) {
            // Show the new incoming value on the ui
            fuel_text.text = amount.toFloat().toString() + " l/h"
            if (amount.toInt() >= MAX_FUEL_CONSUMPTION_WARNING) {
                if (!droppedFuelConsumption) {
                    droppedFuelConsumption = true
                    dropWarningMakerOnMap(WARNING_FUEL)
                }
            }
        }
    }

    override fun onTotalVehicleDistance(totalDistance: Long) {
        Log.i(TAG, "Current total vehicle distance: $totalDistance km")

        // Only do something with incoming values, if the app is in foreground
        if (isInForeground) {
            // Show the new incoming value on the ui
            // ...
        }
    }

    override fun onPause() {
        // This activity is not in foreground anymore
        isInForeground = false
        super.onPause()
    }

    override fun onDestroy() {
        // When the activity is going to be destroyed, we should also clear up everything
        // regarding the vehicle
        super.onDestroy()
        vehicleDataRepository.disconnectVehicle()
        vehicleDataRepository.deinitializeSdk()
        vehicleDataRepository.remove(this)
        speechService?.destroy()
        countDownTimer?.cancel()
    }

    override fun onResume() {
        // We're back on... so also tell that our variable
        isInForeground = true
        super.onResume()
    }

    inner class DriverRecognitionListener: RecognitionListener {

        override fun onReadyForSpeech(p0: Bundle?) {
            System.out.print("Error is")
        }

        override fun onRmsChanged(p0: Float) {
            System.out.print("Error is")
        }

        override fun onBufferReceived(p0: ByteArray?) {
            System.out.print("Error is")
        }

        override fun onPartialResults(p0: Bundle?) {
            System.out.print("Error is")
        }

        override fun onEvent(p0: Int, p1: Bundle?) {
            System.out.print("Error is")
        }

        override fun onBeginningOfSpeech() {
            countDownListener()
        }

        override fun onEndOfSpeech() {
            endCountDownListener()
        }

        override fun onError(error: Int) {
           System.out.print("Error is $error")
        }

        override fun onResults(bundle: Bundle?) {

            val results = bundle!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            channelConstraintLayout.visibility = View.VISIBLE

            Log.d("SPEECH", "word is $results")

        }

    }

}
