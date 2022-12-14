package com.jarica.runnica2.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.jarica.runnica2.Utilidades.Constantes.ACTUALIZACION_CARRERA
import com.jarica.runnica2.Utilidades.Constantes.OBJETO_CARRERA
import com.jarica.runnica2.Utilidades.Constantes.TIEMPO_EXTRA
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jarica.runnica2.Dataclasses.Carrera
import com.jarica.runnica2.Dataclasses.Usuario
import com.jarica.runnica2.R
import com.jarica.runnica2.ui.LoginActivity.Companion.deporteSeleccionado
import com.jarica.runnica2.ui.LoginActivity.Companion.iconoUsuario
import com.jarica.runnica2.ui.LoginActivity.Companion.nombreUsuario
import com.jarica.runnica2.ui.LoginActivity.Companion.providerSesion
import com.jarica.runnica2.ui.LoginActivity.Companion.usuarioEmail
import com.jarica.runnica2.Utilidades.PermissionUtils.isPermissionGranted
import com.jarica.runnica2.Servicio.MyServicio
import com.jarica.runnica2.Utilidades.PermissionUtils
import com.jarica.runnica2.Utilidades.getTimeStringFromDoblue
import com.jarica.runnica2.Utilidades.redondeaNumeros
import com.jarica.runnica2.databinding.ActivityMainBinding
import com.jarica.runnica2.ui.MainActivity.Companion.distanciaCompanion
import com.jarica.runnica2.ui.MainActivity.Companion.ritmoMedioCompanion
import com.jarica.runnica2.ui.MainActivity.Companion.tiempoCompanion
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity(),
    OnMapReadyCallback,
    NavigationView.OnNavigationItemSelectedListener,
    ActivityCompat.OnRequestPermissionsResultCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener {

    //BINDING PARA INTERFAZ
    private lateinit var binding: ActivityMainBinding


    //VARIABLES NAVIGATIONDRAWER
    private lateinit var drawer: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle


    private lateinit var headerView: android.view.View

    //VARIABLES PARA LAS ANIMACIONES
    private val movimientoArriba: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.movimientoarriba
        )
    }
    private val movimientoAbajo: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.movimientoabajo
        )
    }

    //VARIABLE DE SHAREDPREFERENCES
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor


    //VARIABLES DE CARRERA
    private var tiempoEmpezado = false
    private var tiempoCarreraInterfaz = 0.0
    private var distanciaInterfaz = 0.0
    private var velocidadInterfaz = 0.0
    private var ritmoMedioInterfaz = 0.0
    private var carreraRealizada: Carrera? = null

    private var permissionDenied = false

    //Variables fecha
    private var mesActividad = ""
    private var diaSemanaActividad = ""
    private var diaMesActividad = 0
    private var anoActividad = ""
    private var fechaformateada = ""
    private var mesNumero = 0
    private lateinit var fecha: LocalDateTime
    private lateinit var segundosInicio: String
    private var mesesAno = arrayOf(
        "Enero",
        "Febrero",
        "Marzo",
        "Abril",
        "Mayo",
        "Junio",
        "Julio",
        "Agosto",
        "Septiembre",
        "Octubre",
        "Noviembre",
        "Diciembre"
    )

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        iniciarObjetos()

    }

    override fun onResume() {
        super.onResume()
        recargarusuario()
    }

    override fun onStart() {
        super.onStart()
        tiempoCompanion = 0.0
    }


    private fun iniciarObjetos() {

        listaPuntos = arrayListOf()
        listaPuntos as ArrayList<LatLng>

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.Googlemap) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.actionbar)
        setSupportActionBar(toolbar)

        drawer = binding.drawerLayout
        toggle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val navigationView: NavigationView = binding.navView
        navigationView.setNavigationItemSelectedListener(this)
        headerView =
            LayoutInflater.from(this).inflate(R.layout.nav_header_layout, navigationView, false)


        val intent = Intent(applicationContext, MyServicio::class.java)

        //BOTON PLAY

        intent.putExtra(TIEMPO_EXTRA, tiempoCarreraInterfaz)
        binding.fabPlay.setOnClickListener {


            tiempoEmpezado = true
            startForegroundService(intent)
            registerReceiver(
                actualizadorInterfaz,
                IntentFilter(ACTUALIZACION_CARRERA)
            )
            cambiarbotonPlayporCandadoCerrado()
            calcularFecha()

        }

        //BOTON PAUSA
        binding.fabPausa.setOnClickListener {
            tiempoEmpezado = false
            tiempoCompanion = tiempoCarreraInterfaz
            distanciaCompanion = distanciaInterfaz
            ritmoMedioCompanion = ritmoMedioInterfaz
            stopService(intent)
            mostrarBotonPlayEnPausa()
        }

        //BOTON STOP
        binding.fabStop.setOnClickListener {
            tiempoEmpezado = false
            stopService(intent)
            terminarCarreraYReiniciarPantalla()

        }

        binding.fabCandadoCerrado.setOnClickListener {

            binding.fabCandadoCerrado.visibility = View.INVISIBLE
            binding.fabCandadoAbierto.visibility = View.VISIBLE
            iniciarAnimacionAbrirBotones()

        }

        binding.fabCandadoAbierto.setOnClickListener {

            binding.fabCandadoCerrado.visibility = View.VISIBLE
            binding.fabCandadoAbierto.visibility = View.INVISIBLE
            iniciarAnimacionCerrarBotones()
        }

    }

    private fun mostrarBotonPlayEnPausa() {

        binding.fabPlay.visibility = View.VISIBLE
        binding.fabCandadoAbierto.visibility = View.INVISIBLE
        binding.fabCandadoCerrado.visibility = View.INVISIBLE
        iniciarAnimacionCerrarBotones()

    }


    private fun iniciarAnimacionCerrarBotones() {

        binding.fabStop.visibility = View.INVISIBLE
        binding.fabPausa.visibility = View.INVISIBLE
        binding.fabStop.startAnimation(movimientoAbajo)
        binding.fabPausa.startAnimation(movimientoAbajo)


    }

    private fun cambiarbotonPlayporCandadoCerrado() {

        binding.fabPlay.visibility = View.INVISIBLE
        binding.fabCandadoCerrado.visibility = View.VISIBLE

    }

    private fun iniciarAnimacionAbrirBotones() {

        binding.fabStop.visibility = View.VISIBLE
        binding.fabPausa.visibility = View.VISIBLE
        binding.fabStop.startAnimation(movimientoArriba)
        binding.fabPausa.startAnimation(movimientoArriba)


    }

    //Metodo que termina la carrera y reinicia la pantalla
    private fun terminarCarreraYReiniciarPantalla() {

        (listaPuntos as ArrayList<LatLng>).clear()


        val intent = Intent(this, FinalActividadActivity::class.java)
        intent.putExtra("tiempotranscurrido", binding.tvTiempo.text)
        intent.putExtra("distancia", binding.tvDistancia.text)
        intent.putExtra("ritmoMedio", binding.tvRitmo.text)
        intent.putExtra("diaSemana", diaSemanaActividad)
        intent.putExtra("diaMes", diaMesActividad)
        intent.putExtra("mes", mesActividad)
        intent.putExtra("a??o", anoActividad)
        intent.putExtra("hora", fechaformateada)
        intent.putExtra("MesNumero", mesNumero)
        intent.putExtra("segundosInicio", segundosInicio)
        startActivity(intent)

        tiempoCarreraInterfaz = 0.0
        tiempoEmpezado = false
        binding.tvDistancia.text = "0.00"
        binding.tvVelocidad.text = "0.00"
        binding.tvRitmo.text = "0.00"

    }

    private fun calcularFecha() {
        val formatoFechaInicioActividad = DateTimeFormatter.ofPattern("HH:mm:ss")
        fecha = LocalDateTime.now()

        fechaformateada = fecha.format(formatoFechaInicioActividad)
        diaMesActividad = fecha.dayOfMonth
        anoActividad = fecha.year.toString()
        mesActividad = mesesAno[fecha.monthValue - 1]
        diaSemanaActividad = fecha.dayOfWeek.toString()
        mesNumero = fecha.monthValue
        segundosInicio = fecha.second.toString()

    }

    //Metodo que al darle atras nos envia a la pantalla principal del movil
    override fun onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START)
        else {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
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

        var fusedLocationProviderClientAux = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClientAux.lastLocation
            .addOnSuccessListener { location: Location? ->
                //centrarMapa(location!!.latitude, location.longitude)
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
            Toast.makeText(this, "Por favor Acepta los permisos de localizacion", Toast.LENGTH_SHORT).show()
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

    //Metodo que deslogea al usuario y le devuelve a la pantalla de Login
    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        nombreUsuario = ""
        usuarioEmail = ""
        providerSesion = ""
        iconoUsuario = "".toUri()


        val intento = Intent(this, LoginActivity::class.java)
        startActivity(intento)
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
            Toast.makeText(this, "Por favor Acepta los permisos de localizacion", Toast.LENGTH_SHORT).show()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.seleccionar_deporte, menu)

        val menuitem = menu?.getItem(0)

        if (menuitem != null) {

            if (deporteSeleccionado == "Running") menuitem.setIcon(R.drawable.ic_run)
            if (deporteSeleccionado == "Walk") menuitem.setIcon(R.drawable.ic_walk)
            if (deporteSeleccionado == "Bike") menuitem.setIcon(R.drawable.ic_bike)

        }
        return super.onCreateOptionsMenu(menu)
    }

    fun clickDeporte(item: MenuItem) {
        if (!tiempoEmpezado) {
            var intent = Intent(this, SeleccionarDeporteActivity::class.java)
            startActivity(intent)
        }

    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        Toast.makeText(this, "Por favor Acepta los permisos de localizacion", Toast.LENGTH_SHORT).show()
    }


    companion object {


        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        lateinit var mapa: GoogleMap
        //lateinit var deporteSeleccionado: String

        lateinit var listaPuntos: Iterable<LatLng>

        //Metodo que centra el mapa en la posicion del movil
        fun centrarMapa(latInicial: Double, longInicial: Double) {

            val posicionMapa = LatLng(latInicial, longInicial)
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionMapa, 18f), 1000, null)

        }

        var tiempoCompanion = -1.0
        var distanciaCompanion = 0.0
        var ritmoMedioCompanion = 0.0

    }

    //Metodo que recarga el usuario tras reinicio de activity
    private fun recargarusuario() {

        var lecturaBBDD = FirebaseFirestore.getInstance()
        lecturaBBDD.collection("usuarios").document(usuarioEmail)
            .get()
            .addOnSuccessListener { document ->
                var usuario = document.toObject(Usuario::class.java)
                binding.navView.findViewById<TextView>(R.id.tvNombreUsuario).text = usuario?.apodo
                providerSesion = usuario?.registro.toString()

                if (usuario?.registro.toString() == "Google") {
                    Glide.with(this).load(iconoUsuario)
                        .into(binding.navView.findViewById(R.id.sivAvatar))
                }

                if (usuario?.registro.toString() == "email") {

                }
            }


    }

    private fun abrirHistorial() {
        var intent = Intent(this, HistorialActivity::class.java)
        startActivity(intent)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.nav_item_CerrarSesion -> logout()
            R.id.nav_item_Historial -> abrirHistorial()
            R.id.nav_item_Actividad -> onBackPressed()
        }

        return true

    }


}
