package com.example.newweathers

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.newweathers.R.layout.weatherview
import com.google.android.gms.location.*
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import org.w3c.dom.Text
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.android.synthetic.main.activity_main.view.*

class MainActivity : AppCompatActivity() {

    lateinit var arFragment: ArFragment
    //lateinit var arFragment2: ArFragment
    private var testRenderable: ModelRenderable? = null
    private lateinit var danceAnimator: ModelAnimator
    private lateinit var modelUri: Uri
    /*lateinit var base: Node
    lateinit var weather: Node
*/

    val API: String = "7c9b423faa6dc538821ef3799ee498c0"
    val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var buttonstate = 1
        val btn = findViewById<Button>(R.id.color_button)
        val layout = findViewById<LinearLayout>(R.id.theTop)


        btn.setOnClickListener {
            if (buttonstate % 2 == 0){
                layout.setBackgroundColor(getColor(R.color.colorPrimaryDark))

                Toast.makeText(this@MainActivity, "Color Changed.", Toast.LENGTH_SHORT).show()
            }
            else {
                layout.setBackgroundColor(getColor(R.color.colorAccent))

            }
            buttonstate++
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        getLastLocation()

        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        //arFragment2 = supportFragmentManager.findFragmentById(R.id.sceneform_fragment2) as ArFragment
        modelUri = Uri.parse("sun.sfb")
        val renderableFuture = ModelRenderable.builder()
            .setSource(arFragment.context, modelUri)
            .build()
        renderableFuture.thenAccept {
                it -> testRenderable = it
            val danceData = testRenderable!!.getAnimationData(0)
            danceAnimator = ModelAnimator(danceData, testRenderable)
        }




        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
            if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                return@setOnTapArPlaneListener
            }
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            var nodepass = Node()
            nodepass.setParent(anchorNode)
          /*  val weather: Node = Node()
            val base: Node = Node()
            val pose = Pose.makeTranslation(55.0f, 55.0f, 55.0f)
            val localPosition = Vector3(pose.tx(), pose.ty(), pose.tz())
            val weather1 = weather.localPosition*//*
            weather.setParent(base)*/
            placeObject(arFragment, anchor)
            placeObject2(arFragment, anchor, nodepass,v = 6.1F, v1 = 5F, v2 = 0F)

        }

    }



    @SuppressLint("MissingPermission")
    fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result


                    if (location == null) {
                        requestNewLocationData()
                    } else {
                      weatherTask().execute(location)
                        findViewById<TextView>(R.id.latTextView)?.text = location.latitude.toString()
                        findViewById<TextView>(R.id.lonTextView)?.text = location.longitude.toString()
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            var mLastLocation: Location = locationResult.lastLocation
            findViewById<TextView>(R.id.latTextView)?.text = mLastLocation.latitude.toString()
            findViewById<TextView>(R.id.lonTextView)?.text = mLastLocation.longitude.toString()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }



    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

    inner class weatherTask() : AsyncTask<Location, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            /* Showing the ProgressBar, Making the main design GONE */
//           findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
    //  findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
            //findViewById<TextView>(R.id.errorText).visibility = View.GONE
        }

        override fun doInBackground(vararg params: Location?): String? {
          Log.d("BGTASK", "alive?")
            try{
              val response = URL("https://api.openweathermap.org/data/2.5/weather?lat=${params[0]?.latitude}&lon=${params[0]?.longitude}&units=metric&appid=$API").readText(
                Charsets.UTF_8
              )
              Log.d("BGTASK", "with data? $response")
              return response
            }catch (e: Exception){
              print("error: response set to null")
                return ""
            }
        }

        override fun onPostExecute(result: String?) {

            super.onPostExecute(result)

            try {
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

                val updatedAt:Long = jsonObj.getLong("dt")
                val updatedAtText = "Updated at: "+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(updatedAt*1000))
                val temp = main.getString("temp")+"°C"
                val tempMin = "Min Temp: " + main.getString("temp_min")+"°C"
                val tempMax = "Max Temp: " + main.getString("temp_max")+"°C"
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")

                val sunrise:Long = sys.getLong("sunrise")
                val sunset:Long = sys.getLong("sunset")
                val windSpeed = wind.getString("speed")
                val weatherDescription = weather.getString("description")

                //val address = jsonObj.getString("name")+", "+sys.getString("country")
                //setContentView(R.layout.weatherview)
                /* Populating extracted data into our views */
                //findViewById<TextView>(R.id.address).text = address
                val address = jsonObj.getString("name")+", "+sys.getString("country")

                findViewById<TextView>(R.id.address)?.text = address
                findViewById<TextView>(R.id.updated_at)?.text =  updatedAtText
                findViewById<TextView>(R.id.status)?.text = weatherDescription.capitalize()
                findViewById<TextView>(R.id.temp)?.text = temp
                findViewById<TextView>(R.id.temp_min)?.text = tempMin
                findViewById<TextView>(R.id.temp_max)?.text = tempMax
                findViewById<TextView>(R.id.sunrise)?.text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise*1000))
                findViewById<TextView>(R.id.sunset)?.text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset*1000))
                findViewById<TextView>(R.id.wind)?.text = windSpeed
                findViewById<TextView>(R.id.pressure)?.text = pressure
                findViewById<TextView>(R.id.humidity)?.text = humidity
                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE


                //only now
                //tapListener()


            } catch (e: Exception) {
            }


        }


        /*fun conditionalWeather(result: String?): Int {

            try {
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                if (main.)


        }*/
    }

    fun tapListener(){
        Log.d("TAP", "really?")
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
            if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                return@setOnTapArPlaneListener
            }
            val anchor = hitResult.createAnchor()
            placeObject(arFragment, anchor)
        }
    }


    private fun placeObject(fragment: ArFragment, anchor: Anchor) {

        ViewRenderable.builder()
            .setView(arFragment.context, weatherview)
            .build()
            .thenAccept {
                it.isShadowCaster = false
                it.isShadowReceiver = false
                /*val base: Node
                val weather: Node*/


                /*it.view.findViewById<Button>(R.id.btn).setOnClickListener {
                }*/
                addControlsToScene(fragment, anchor, it)

            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
    }

    private fun placeObject2(fragment: ArFragment, anchor: Anchor, node: Node, v: Float, v1: Float, v2: Float) {

        ModelRenderable.builder()
            .setSource(fragment.context, modelUri)
            .build()
            .thenAccept {
                addNodetoScene(fragment, anchor, it )

                it.isShadowCaster = false
                it.isShadowReceiver = false

                addControlsToScene(fragment, anchor, it)


            }
            .exceptionally {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(it.message).setTitle("Error")
                val dialog = builder.create()
                dialog.show()
                return@exceptionally null
            }
        /*val weather = Node()
        val base = node
        //val pose = Pose.makeTranslation(55.0f, 55.0f, 55.0f)
        //val localPosition = Vector3(pose.tx(), pose.ty(), pose.tz())
        val localPosition = Vector3(v, v1, v2)
        weather.localPosition = localPosition
        weather.setParent(base)*/

    }

    private fun addNodetoScene(fragment: ArFragment, anchor: Anchor, renderable: Renderable?) {
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(fragment.transformationSystem)
        node.renderable = renderable
        node.setParent(anchorNode)
        fragment.arSceneView.scene.addChild(anchorNode)
        node.select()
    }



    private fun getScreenCenter(): android.graphics.Point {
        val vw = findViewById<View>(android.R.id.content)
        return android.graphics.Point(vw.width / 2, vw.height / 2)
    }


    private fun addControlsToScene(fragment: ArFragment, anchor: Anchor, renderable: Renderable) {
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(fragment.transformationSystem)
        node.renderable = renderable
        node.setParent(anchorNode)
        fragment.arSceneView.scene.addChild(anchorNode)
        node.scaleController.maxScale = 0.2f
        node.scaleController.minScale = 0.1f
        node.rotationController.isEnabled = true
    }


}





    /*override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View? {
        var myInflatedView = inflater.inflate(R.layout.weatherview, container,false);

    // Set the Text to try this out
    //var txt = myInflatedView.findViewById<View>(R.id.temp).te*/






