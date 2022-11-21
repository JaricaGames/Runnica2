package com.jarica.runnica2.Servicio

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.jarica.runnica2.Utilidades.Constantes.ACTUALIZACION_CARRERA
import com.jarica.runnica2.Utilidades.Constantes.ID_CANAL
import com.jarica.runnica2.Utilidades.Constantes.ID_NOTIFICACION
import com.jarica.runnica2.Utilidades.Constantes.INTERVALO_ACTUALIZACION
import com.jarica.runnica2.Utilidades.Constantes.INTERVALO_MAS_RAPIDO
import com.jarica.runnica2.Utilidades.Constantes.OBJETO_CARRERA
import com.jarica.runnica2.Utilidades.Constantes.TIEMPO_EXTRA
import com.jarica.runnica2.Utilidades.Constantes.radioTierra
import com.jarica.runnica2.Dataclasses.Carrera
import com.jarica.runnica2.ui.MainActivity.Companion.centrarMapa
import com.jarica.runnica2.ui.MainActivity.Companion.listaPuntos
import com.jarica.runnica2.ui.MainActivity.Companion.mapa
import com.jarica.runnica2.R
import com.jarica.runnica2.Utilidades.Constantes.INTERVALO_ACTUALIZACION_GPS_EN_SEGUNDOS
import com.jarica.runnica2.ui.LoginActivity.Companion.deporteSeleccionado
import com.jarica.runnica2.ui.MainActivity.Companion.distanciaCompanion
import com.jarica.runnica2.ui.MainActivity.Companion.ritmoMedioCompanion
import com.jarica.runnica2.ui.MainActivity.Companion.tiempoCompanion
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*


class MyServicio : Service() {

    //VARIABLE TIMER
    private val timer = Timer()

    //VARIABLES CARERRA
    private var tiempoCarrera: Double = tiempoCompanion
    private var distanciaCarrera: Double = distanciaCompanion
    private var velocidadCarrera: Double = 0.0
    private var ritmoMedioCarrera: Double = ritmoMedioCompanion

    //VARIABLES GPS Y LOCALIZACION
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //VARIABLES MEDICIONES
    private var latitud: Double = 0.0
    private var longitud: Double = 0.0


    override fun onBind(intent: Intent): IBinder? = null // Metodo obligatorio y que no es necesario

    override fun onCreate() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        crearCanaldeNotificacion()

    }


    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        mostrarNotificacion()
        tiempoCarrera = intent!!.getDoubleExtra(TIEMPO_EXTRA, tiempoCompanion)
        timer.scheduleAtFixedRate(TimeTask(tiempoCompanion), 0, 1000)

        return START_STICKY

    }

    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }

    //CALSE QUE EJECUTA UN HILO PARA LA CARRERA
    private inner class TimeTask(private var tiempoCarreraAux: Double) : TimerTask() {

        override fun run() {

            val intent = Intent(ACTUALIZACION_CARRERA)

            tiempoCarreraAux++
            tiempoCarrera = tiempoCarreraAux
            mostrarNotificacion()

            if (tiempoCarreraAux % INTERVALO_ACTUALIZACION_GPS_EN_SEGUNDOS == 0.0) {

                adminitrarLocalizacion()
            }


            var carrera =
                Carrera(tiempoCarrera, distanciaCarrera, velocidadCarrera, ritmoMedioCarrera)
            intent.putExtra(OBJETO_CARRERA, carrera)
            sendBroadcast(intent)

        }

    }

    @SuppressLint("MissingPermission")
    private fun adminitrarLocalizacion() {

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            solicitarNuevaLocalizacion()
        }
    }

    //Metodo que solicita una nueva localizacion
    @SuppressLint("MissingPermission")
    private fun solicitarNuevaLocalizacion() {

        var miSolicitudLocalizacion = com.google.android.gms.location.LocationRequest()
        miSolicitudLocalizacion.interval = INTERVALO_ACTUALIZACION
        miSolicitudLocalizacion.fastestInterval = INTERVALO_MAS_RAPIDO
        miSolicitudLocalizacion.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        fusedLocationProviderClient.requestLocationUpdates(
            miSolicitudLocalizacion,
            miLocalizacionCallBack,
            Looper.getMainLooper()
        )

    }

    private val miLocalizacionCallBack = object : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {

            val miUltimaLocalizacion: Location? = locationResult.lastLocation

            if (miUltimaLocalizacion != null) {
                registrarNuevaLocalizacion(miUltimaLocalizacion)
            }

            mostrarNotificacion()
        }
    }

    private fun registrarNuevaLocalizacion(location: Location) {

        centrarMapa(location.latitude, location.longitude)

        var new_latitude: Double = location.latitude
        var new_longitude: Double = location.longitude

        if (tiempoCarrera == 1.0) {
            latitud = location.latitude
            longitud = location.longitude
        }

        if (tiempoCarrera > 1.0) {
            new_latitude = location.latitude
            new_longitude = location.longitude

            //CALCULOS DE DATOS
            var distanciaIntervalo = calcularDistancia(new_latitude, new_longitude)
            calcularVelocidad(distanciaIntervalo)
            calcularRitmo()

            //Dicujado de la polilinea
            var nuevaPosicion = LatLng(new_latitude, new_longitude)
            (listaPuntos as ArrayList<LatLng>).add(nuevaPosicion)
            crearPolilinea(listaPuntos)
        }

        latitud = new_latitude
        longitud = new_longitude
    }

    private fun crearPolilinea(listaPuntos: Iterable<LatLng>) {
        val polilineaOpciones = PolylineOptions()
            .width(25f)
            .color(ContextCompat.getColor(this, R.color.azul))
            .addAll(listaPuntos)

        val polilinea = mapa.addPolyline(polilineaOpciones)
        polilinea.startCap = RoundCap()

    }

    ///////////// METODOS PAAR MOSTRAR NoTIFICACION DE SERVICIO EN PRIMER PLANO ////////////////

    private fun mostrarNotificacion() {

        val pendingIntent: PendingIntent =
            Intent(this, MyServicio::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        var iconId = 0
        if (deporteSeleccionado == "Running") iconId = R.drawable.ic_run
        if (deporteSeleccionado == "Walk") iconId = R.drawable.ic_walk
        if (deporteSeleccionado == "Bike") iconId = R.drawable.ic_bike


        val notification = Notification
            .Builder(this, ID_CANAL)
            .setContentText(
                getTimeStringFromDoblue(tiempoCarrera.toInt()) + "          ${
                    redondeaNumeros(
                        distanciaCarrera.toString(),
                        2
                    )
                } Km"
            )
            .setSmallIcon(iconId)    ///////////////////  CAMBIAR ICONO SEGUN DEPORTE ////////////////
            .setContentIntent(pendingIntent)
            .build()

        startForeground(ID_NOTIFICACION, notification)
    }

    private fun crearCanaldeNotificacion() {

        val canalServicio = NotificationChannel(
            ID_CANAL, "Mi canal Servicio",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(
            NotificationManager::class.java
        )

        manager.createNotificationChannel(canalServicio)

    }

    //////////  CALCULOS y METODOS AUXILIARES ///////////////

    //Metodo que calcula la distancia recorrida en un intervalo de timepo
    private fun calcularDistancia(n_lt: Double, n_lg: Double): Double {

        //Calculo de los deltas
        val dLat = Math.toRadians(n_lt - latitud)
        val dLng = Math.toRadians(n_lg - longitud)

        val sindLat = sin(dLat / 2)
        val sindLng = sin(dLng / 2)
        val va1 =
            sindLat.pow(2.0) + (sindLng.pow(2.0)
                    * cos(Math.toRadians(latitud)) * cos(
                Math.toRadians(n_lt)
            ))
        val va2 = 2 * atan2(sqrt(va1), sqrt(1 - va1))
        var n_distance = radioTierra * va2

        if (n_distance < 100) {
            distanciaCarrera += n_distance
            return n_distance
        } else {
            return n_distance
        }
    }

    private fun makeTimeString(hours: Int, minutes: Int, seconds: Int): String =
        String.format("%02d:%02d:%02d", hours, minutes, seconds)


    private fun getTimeStringFromDoblue(time: Int): String {

        val hours = time % 86400 / 3600
        val minutes = time % 86400 % 3600 / 60
        val seconds = time % 86400 % 3600 % 60

        return makeTimeString(hours, minutes, seconds)
    }

    //Metodo que redondea los datos calculados
    fun redondeaNumeros(data: String, decimals: Int): String {
        var d: String = data
        var p = d.indexOf(".", 0)

        if (p != null) {
            var limit: Int = p + decimals + 1
            if (d.length <= p + decimals + 1) limit = d.length //-1
            d = d.subSequence(0, limit).toString()
        }

        return d
    }


    //Metodo que calcula la velocidad en el intervalo
    fun calcularVelocidad(distanciaIntervalo: Double) {

        velocidadCarrera = ((distanciaIntervalo * 1000)) * 3.6 //Para pasar a KM/h

        if (velocidadCarrera < 0.25f) velocidadCarrera =
            0.00 // poor si apenass hay movimiento que no se vuelva loca la interfaz

    }

    //Metodo que calcula el ritmo de la carrera
    fun calcularRitmo() {
        ritmoMedioCarrera = tiempoCarrera / (distanciaCarrera * 60)
    }

}