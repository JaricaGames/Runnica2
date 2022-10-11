package com.jarica.runnica2

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.jarica.runnica2.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import com.jarica.runnica2.PermissionUtils.isPermissionGranted
import com.jarica.runnica2.databinding.ActivityMainBinding
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    ActivityCompat.OnRequestPermissionsResultCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener {

    private lateinit var binding: ActivityMainBinding
    private var tiempoEmpezado = false
    private var tiempoCarrera = 0.0

    private var permissionDenied = false

   // private lateinit var mapa: GoogleMap
    override fun onCreate(savedInstanceState: Bundle?) {

       super.onCreate(savedInstanceState)
       binding = ActivityMainBinding.inflate(layoutInflater)
       setContentView(binding.root)

       val mapFragment =
           supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
       mapFragment?.getMapAsync(this)


       binding.floatingActionButton.setOnClickListener {
           val intent = Intent(applicationContext, MyServicio::class.java)
           startForegroundService(intent)
           registerReceiver(updateTime, IntentFilter(MyServicio.TIMER_UPDATE))
       }




       tvTiempo = binding.tvTiempo
       tvDistancia = binding.tvDistancia
       tvRitmoMedio = binding.tvRitmoMedio
       tvVelocidad = binding.tvVelocidad

       listaPuntos = arrayListOf()
       (listaPuntos as ArrayList<LatLng>).clear()
    }

     private val updateTime : BroadcastReceiver = object : BroadcastReceiver() {
       override fun onReceive(context: Context, intent: Intent) {

           tiempoCarrera = intent.getDoubleExtra(MyServicio.TIMER_EXTRA, 0.0)
           binding.tvTiempo.text = getTimeStringFromDoblue(tiempoCarrera)

       }
   }

    override fun onMapReady(googleMap: GoogleMap) {
        mapa = googleMap
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        enableMyLocation()
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {

        // [START maps_check_location_permission]
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mapa.isMyLocationEnabled = true
            return
        }

        // 2. If if a permission rationale dialog should be shown
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            PermissionUtils.RationaleDialog.newInstance(
                LOCATION_PERMISSION_REQUEST_CODE, true
            ).show(supportFragmentManager, "dialog")
            return
        }

        // 3. Otherwise, request permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
        // [END maps_check_location_permission]
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT)
            .show()
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location:\n$location", Toast.LENGTH_LONG)
            .show()
    }



    // [START maps_check_location_permission_result]
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
            return
        }

        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Permission was denied. Display an error message
            // [START_EXCLUDE]
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true
            // [END_EXCLUDE]
        }
    }



    // [END maps_check_location_permission_result]
    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        newInstance(true).show(supportFragmentManager, "dialog")
    }

    companion object {

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        var contadorTiempo = 0
        var contadorTiempo2 = 0
        var velocidad = 0.0
        var ritmoMedio = 0.0
        var distancia = 0.0

        lateinit var mapa:GoogleMap

        lateinit var tvTiempo:TextView
        lateinit var tvDistancia: TextView
        lateinit var tvRitmoMedio: TextView
        lateinit var tvVelocidad: TextView

        lateinit var listaPuntos : Iterable<LatLng>




        //Metodo que centra el mapa en la posicion del movil
        fun centrarMapa(latInicial: Double, longInicial: Double) {

            val posicionMapa = LatLng(latInicial, longInicial)
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionMapa, 17f), 1000, null)

        }

    }

    private fun makeTimeString(hours: Int, minutes: Int, seconds: Int): String = String.format("%02d:%02d:%02d", hours, minutes, seconds)


    private fun getTimeStringFromDoblue(time: Double): String {

        val resultadoEntero = time.roundToInt()
        val hours = resultadoEntero % 86400 / 3600
        val minutes = resultadoEntero % 86400 % 3600 / 60
        val seconds = resultadoEntero % 86400 % 3600 %60

        return makeTimeString ( hours, minutes, seconds)
    }
}
