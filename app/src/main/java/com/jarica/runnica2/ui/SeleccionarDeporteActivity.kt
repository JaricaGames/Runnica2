package com.jarica.runnica2.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import com.jarica.runnica2.R
import com.jarica.runnica2.ui.LoginActivity.Companion.deporteSeleccionado

class SeleccionarDeporteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selecionar_deporte)

        var imiviconoCheckCorrer = findViewById<ImageView>(R.id.iviconoCheckCorrer)
        var imiviconoCheckAndar = findViewById<ImageView>(R.id.iviconoCheckAndar)
        var imiviconoCheckBike = findViewById<ImageView>(R.id.iviconoCheckBicicleta)

        var lyCorrer: ConstraintLayout = findViewById(R.id.lySeleccionCorrer)
        var lyAndar: ConstraintLayout = findViewById(R.id.lySeleccionAndar)
        var lyBicicleta: ConstraintLayout = findViewById(R.id.lySeleccionBicicleta)

        var appBarSeleccionActividad: androidx.appcompat.widget.Toolbar = findViewById(R.id.actionbar)


        //Codigo boton felcha tras del actionBar
        var im = appBarSeleccionActividad.get(0)
        im.setOnClickListener {
            onBackPressed()
        }

        if (deporteSeleccionado == "Running") {

            imiviconoCheckCorrer.setVisibility(View.VISIBLE)
            imiviconoCheckAndar.setVisibility(View.INVISIBLE)
            imiviconoCheckBike.setVisibility(View.INVISIBLE)

        }
        if (deporteSeleccionado == "Walk") {
            imiviconoCheckCorrer.setVisibility(View.INVISIBLE)
            imiviconoCheckAndar.setVisibility(View.VISIBLE)
            imiviconoCheckBike.setVisibility(View.INVISIBLE)


        }

        if (deporteSeleccionado == "Bike") {
            imiviconoCheckCorrer.setVisibility(View.INVISIBLE)
            imiviconoCheckAndar.setVisibility(View.INVISIBLE)
            imiviconoCheckBike.setVisibility(View.VISIBLE)


        }

        lyCorrer.setOnClickListener {
            deporteSeleccionado = "Running"
            volverAtras()
        }

        lyAndar.setOnClickListener {
            deporteSeleccionado = "Walk"
            volverAtras()
        }


        lyBicicleta.setOnClickListener {
            deporteSeleccionado = "Bike"
            volverAtras()
        }

    }

    private fun volverAtras() {
        var intent = Intent(this, MainActivity::class.java)
        intent.putExtra("deporteSeleccionado", deporteSeleccionado)
        startActivity(intent)
    }
}