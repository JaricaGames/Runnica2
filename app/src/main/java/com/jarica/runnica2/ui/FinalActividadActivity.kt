package com.jarica.runnica2.ui


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.google.firebase.firestore.FirebaseFirestore
import com.jarica.runnica2.R
import com.jarica.runnica2.ui.LoginActivity.Companion.deporteSeleccionado
import com.jarica.runnica2.ui.LoginActivity.Companion.usuarioEmail
import com.jarica.runnica2.Utilidades.redondeaNumeros
import com.jarica.runnica2.databinding.ActivityFinalActividadBinding

class FinalActividadActivity : AppCompatActivity() {

    //Variables para recibir y enviar datos
    private var distancia = ""
    private var ritmoMedio = ""
    private var tiempoTranscurrido = ""
    private var diaSemana = ""
    private var diaMes = 0
    private var mes = ""
    private var año = ""
    private var hora = ""
    private var mesNumero = 0
    private lateinit var idCarrera : String
    private lateinit var fechaActividad : String

    private var registroBBDD =  FirebaseFirestore.getInstance()

    private lateinit var binding: ActivityFinalActividadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =  ActivityFinalActividadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        funcionalidades()
        actualizarInterfaz()
    }

    private fun funcionalidades() {

        val bundle = intent.extras
        distancia = bundle?.getString("distancia").toString()
        ritmoMedio = bundle?.getString("ritmoMedio").toString()
        diaSemana = bundle?.getString("diaSemana").toString()
        diaMes = bundle?.getInt("diaMes")!!
        mes = bundle.getString("mes").toString()
        año = bundle.getString("año").toString()
        hora = bundle.getString("hora").toString()
        mesNumero = bundle.getInt("MesNumero")
        tiempoTranscurrido = bundle.getString("tiempotranscurrido").toString()

        var mesNumeroFormato = ""
        var diaMesFormato = ""
        diaMesFormato = String.format("%02d",diaMes)
        mesNumeroFormato = String.format("%02d",mesNumero)

        fechaActividad = año+mesNumeroFormato+diaMesFormato
        idCarrera = usuarioEmail+año+mesNumeroFormato+diaMesFormato+hora

        binding.btnGuardar.setOnClickListener {crearRegistroBBDD()}
        binding.appbarFinalActividad.actionbar[0].setOnClickListener { onBackPressed() }

    }

    private fun crearRegistroBBDD() {

        registroBBDD.collection("Actividades").document(idCarrera).set(
            hashMapOf(
                "usuario" to usuarioEmail,
                "deporte" to deporteSeleccionado,
                "distancia" to distancia,
                "ritmoMedio" to ritmoMedio,
                "diaSemana" to diaSemana,
                "diaMes" to diaMes,
                "mes" to mes,
                "hora" to hora,
                "tiempoTranscurrido" to tiempoTranscurrido,
                "fechaActividad" to fechaActividad

            ))

        var intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun actualizarInterfaz() {

        binding.tvDistanciaFA.text = redondeaNumeros(distancia,2)
        binding.tvRitmoFA.text = redondeaNumeros(ritmoMedio, 2)
        binding.tvTiempoFA.text = tiempoTranscurrido
        binding.tvfechaRegistro.text = "$diaMes de $mes de $año a las $hora"

        when (diaSemana){
            "MONDAY" -> binding.tvdiaRegistro.text = "LUNES"
            "TUESDAY" -> binding.tvdiaRegistro.text = "MARTES"
            "WEDNESDAY" -> binding.tvdiaRegistro.text = "MIERCOLES"
            "THURSDAY" -> binding.tvdiaRegistro.text = "JUEVES"
            "FRIDAY" -> binding.tvdiaRegistro.text = "VIERNES"
            "SATURDAY" -> binding.tvdiaRegistro.text = "SABADO"
            "SUNDAY" -> binding.tvdiaRegistro.text = "DOMINGO"
        }

        if(deporteSeleccionado == "Running") binding.iviconoActividad.setImageResource(R.drawable.ic_run)
        if(deporteSeleccionado == "Walk") binding.iviconoActividad.setImageResource(R.drawable.ic_walk)
        if(deporteSeleccionado == "Bike") binding.iviconoActividad.setImageResource(R.drawable.ic_bike)

    }

    //Metodo que al darle atras nos envia a la pantalla principal del movil
    override fun onBackPressed() {
        var intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

}