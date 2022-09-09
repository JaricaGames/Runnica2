package com.jarica.runnica2

import android.annotation.SuppressLint
import android.app.*
import android.content.ContentValues.TAG
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.jarica.runnica2.Constantes.FASTEST_INTERVAL
import com.jarica.runnica2.Constantes.ID_CANAL
import com.jarica.runnica2.Constantes.ID_NOTIFICACION
import com.jarica.runnica2.Constantes.UPDATE_INTERVAL
import com.jarica.runnica2.Constantes.radioTierra
import com.jarica.runnica2.MainActivity.Companion.centrarMapa
import com.jarica.runnica2.MainActivity.Companion.contador
import com.jarica.runnica2.MainActivity.Companion.distancia
import com.jarica.runnica2.MainActivity.Companion.ritmoMedio
import com.jarica.runnica2.MainActivity.Companion.tvDistancia
import com.jarica.runnica2.MainActivity.Companion.tvRitmoMedio
import com.jarica.runnica2.MainActivity.Companion.tvTiempo
import com.jarica.runnica2.MainActivity.Companion.tvVelocidad
import com.jarica.runnica2.MainActivity.Companion.velocidad


class MyServicio() : Service() {


    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest
    private lateinit var locationCallback: LocationCallback

    //Variables de mediciones
    private var latitud:Double = 0.0
    private var longitud:Double = 0.0


    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        crearCanaldeNotificacion()
        createLocationRequest()
        createLocationCallBack()


    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d(TAG, "Servivcio Iniciado")
        mostrarNotificacion()

            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location : Location? ->

                    if (location != null) {

                        startLocationUpdates()
                    }
                }

        return START_NOT_STICKY

    }


    private fun createLocationCallBack() {

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {

                val miUltimaLocalizacion : Location? = locationResult.lastLocation

                    contador++
                    var tiempo:String = getTimeStringFromDoblue(contador)
                    tvTiempo.text = tiempo
                    tvDistancia.text = roundNumber(distancia.toString(), 2)
                    tvRitmoMedio.text = roundNumber(ritmoMedio.toString(),2)
                    tvVelocidad.text = roundNumber(velocidad.toString(),2)


                    if (miUltimaLocalizacion != null) {
                        registerNewLocation(miUltimaLocalizacion)
                    }

                mostrarNotificacion()
                println(contador)
            }
        }
    }

    private fun registerNewLocation(location: Location) {

        centrarMapa(location.latitude, location.longitude)

        var new_latitude: Double = location.latitude
        var new_longitude: Double = location.longitude

        if(contador == 1){
            latitud = location.latitude
            longitud = location.longitude
        }

        if (contador > 1){
            new_latitude = location.latitude
            new_longitude = location.longitude
        }

        var distanciaIntervalo = calcularDistancia(new_latitude, new_longitude)
        calcularVelocidad(distanciaIntervalo)
        calcularRitmo()

        latitud = new_latitude
        longitud = new_longitude
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {

        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

    fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = UPDATE_INTERVAL
            fastestInterval = FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }




    //Metodo que calcula la distancia recorrida en un intervalo de timepo
    private fun calcularDistancia(n_lt: Double, n_lg: Double): Double{

        //Calculo de los deltas
        val dLat = Math.toRadians(n_lt - latitud)
        val dLng = Math.toRadians(n_lg - longitud)

        val sindLat = Math.sin(dLat / 2)
        val sindLng = Math.sin(dLng / 2)
        val va1 =
            Math.pow(sindLat, 2.0) + (Math.pow(sindLng, 2.0)
                    * Math.cos(Math.toRadians(latitud)) * Math.cos(
                Math.toRadians( n_lt  )
            ))
        val va2 = 2 * Math.atan2(Math.sqrt(va1), Math.sqrt(1 - va1))
        var n_distance =  radioTierra * va2

        distancia += n_distance
        return n_distance
    }


    private fun mostrarNotificacion() {

        val pendingIntent: PendingIntent =
            Intent(this, MyServicio::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }

        val notification = Notification
            .Builder(this, ID_CANAL)
            .setContentText(getTimeStringFromDoblue(contador))
            .setSmallIcon(R.drawable.ic_bike)
            .setContentIntent(pendingIntent)
            .build()

// Notification ID cannot be 0.
        startForeground(ID_NOTIFICACION, notification)
    }

    private fun crearCanaldeNotificacion() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            val canalServicio = NotificationChannel(
                ID_CANAL, "Mi canal Servicio",
                NotificationManager.IMPORTANCE_LOW)

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(canalServicio)



        }
    }

    private fun makeTimeString(hours: Int, minutes: Int, seconds: Int): String = String.format("%02d:%02d:%02d", hours, minutes, seconds)


    private fun getTimeStringFromDoblue(time: Int): String {

        val hours = time % 86400 / 3600
        val minutes = time % 86400 % 3600 / 60
        val seconds = time % 86400 % 3600 %60

        return makeTimeString ( hours, minutes, seconds)
    }

    //Metodo que redondea los datos calculados
    private fun roundNumber(data: String, decimals: Int) : String{
        var d : String = data
        var p= d.indexOf(".", 0)

        if (p != null){
            var limit: Int = p+decimals +1
            if (d.length <= p+decimals+1) limit = d.length //-1
            d = d.subSequence(0, limit).toString()
        }

        return d
    }



    //Metodo que calcula la velocidad
    private fun calcularVelocidad(distanciaIntervalo: Double) {

        velocidad = ((distanciaIntervalo * 1000)) * 3.6 //Para pasar a KM/h
        if(velocidad<0.25f) velocidad = 0.00
    }

    //Metodo que calcula el ritmo de la carrera
    private fun calcularRitmo() {
        ritmoMedio = contador/ (distancia*60)
    }

}