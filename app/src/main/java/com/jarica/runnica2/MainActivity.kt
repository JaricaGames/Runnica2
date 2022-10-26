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
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.jarica.runnica2.Constantes.ACTUALIZACION_CARRERA
import com.jarica.runnica2.Constantes.OBJETO_CARRERA
import com.jarica.runnica2.Constantes.TIEMPO_EXTRA
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.navigation.NavigationView
import com.jarica.runnica2.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import com.jarica.runnica2.PermissionUtils.isPermissionGranted
import com.jarica.runnica2.databinding.ActivityMainBinding
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    ActivityCompat.OnRequestPermissionsResultCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener, NavigationView.OnNavigationItemSelectedListener

{

    //BINDING PARA INTERFAZ
    private lateinit var binding: ActivityMainBinding

    //VARIABLES NAVIGATIONDRAWER
    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle


    //VARIABLES DE CARRERA
    private var tiempoEmpezado = false
    private var tiempoCarreraInterfaz = 0.0
    private var distanciaInterfaz = 0.0
    private var velocidadInterfaz = 0.0
    private var ritmoMedioInterfaz = 0.0
    private var carreraRealizada: Carrera? = null


    private var permissionDenied = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.Googlemap) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.actionbar)
        setSupportActionBar(toolbar)

        drawer = binding.drawerLayout
        toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val navigationView: NavigationView = binding.navView
        navigationView.setNavigationItemSelectedListener(this)





        iniciarObjetos()

        listaPuntos = arrayListOf()
        listaPuntos as ArrayList<LatLng>
    }


    private fun iniciarObjetos() {


        val intent = Intent(applicationContext, MyServicio::class.java)

        //BOTON PLAY

        intent.putExtra(TIEMPO_EXTRA, tiempoCarreraInterfaz)
        binding.floatingActionButton.setOnClickListener {
            if (tiempoCarreraInterfaz == 0.0) {
                tiempoEmpezado = true
                startForegroundService(intent)
                registerReceiver(
                    actualizadorInterfaz,
                    IntentFilter(ACTUALIZACION_CARRERA)
                )
            }


        }

        //BOTON PAUSA
        binding.fabPausa.setOnClickListener {
            tiempoEmpezado = false
            MyServicio.tiempoCompanion = tiempoCarreraInterfaz
            MyServicio.distanciaCompanion = distanciaInterfaz
            MyServicio.ritmoMedioCompanion = ritmoMedioInterfaz
            stopService(intent)
        }


        //BOTON STOP
        binding.fabStop.setOnClickListener {
            tiempoEmpezado = false
            stopService(intent)

        }

    }


    //OBJETO BROADCAST_RECEIVER
    private val actualizadorInterfaz: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            carreraRealizada = intent.getParcelableExtra(OBJETO_CARRERA)

            //RECEPCION DE PARAMETROS
            tiempoCarreraInterfaz = carreraRealizada!!.tiempoCarrera
            distanciaInterfaz = carreraRealizada!!.distanciaCarrera
            velocidadInterfaz = carreraRealizada!!.velocidadCarrera
            ritmoMedioInterfaz = carreraRealizada!!.ritmoMedioCarrera

            //MUESTRA DE PARAMETROS EN LA INTERFAZ
            binding.tvTiempo.text = getTimeStringFromDoblue(tiempoCarreraInterfaz)
            binding.tvDistancia.text = redondeaNumeros(distanciaInterfaz.toString(), 2)
            binding.tvVelocidad.text = redondeaNumeros(velocidadInterfaz.toString(), 2)
            binding.tvRitmo.text = redondeaNumeros(ritmoMedioInterfaz.toString(), 2)

        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        mapa = googleMap
        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        mapa.mapType = GoogleMap.MAP_TYPE_HYBRID


        enableMyLocation()
        var FusedLocationProviderClientAux = LocationServices.getFusedLocationProviderClient(this)
        FusedLocationProviderClientAux.lastLocation
            .addOnSuccessListener { location: Location? ->

                centrarMapa(location!!.latitude, location.longitude)
            }

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
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true
        }
    }

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

    private fun makeTimeString(hours: Int, minutes: Int, seconds: Int): String =
        String.format("%02d:%02d:%02d", hours, minutes, seconds)


    private fun getTimeStringFromDoblue(time: Double): String {

        val resultadoEntero = time.roundToInt()
        val hours = resultadoEntero % 86400 / 3600
        val minutes = resultadoEntero % 86400 % 3600 / 60
        val seconds = resultadoEntero % 86400 % 3600 % 60

        return makeTimeString(hours, minutes, seconds)
    }

    //Metodo que redondea los datos calculados
    fun redondeaNumeros(dato: String, decimales: Int): String {
        var d: String = dato
        var p = d.indexOf(".", 0)

        if (p != null) {
            var limite: Int = p + decimales + 1
            if (d.length <= p + decimales + 1) limite = d.length //-1
            d = d.subSequence(0, limite).toString()
        }

        return d
    }

    companion object {


        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        lateinit var mapa: GoogleMap

        lateinit var listaPuntos: Iterable<LatLng>

        //Metodo que centra el mapa en la posicion del movil
        fun centrarMapa(latInicial: Double, longInicial: Double) {

            val posicionMapa = LatLng(latInicial, longInicial)
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionMapa, 18f), 1000, null)

        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        TODO("Not yet implemented")
    }


}
