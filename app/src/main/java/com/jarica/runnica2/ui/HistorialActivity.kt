package com.jarica.runnica2.ui

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jarica.runnica2.Dataclasses.Actividades
import com.jarica.runnica2.R
import com.jarica.runnica2.ui.LoginActivity.Companion.usuarioEmail
import com.jarica.runnica2.Utilidades.Adaptador
import com.jarica.runnica2.databinding.ActivityHistorialBinding
import java.util.ArrayList

class HistorialActivity : AppCompatActivity() {

    //private lateinit var recyclerView: RecyclerView
    private lateinit var arrayListActividades: ArrayList<Actividades>
    private lateinit var miAdapter: Adaptador

    private lateinit var binding: ActivityHistorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvHistorial.layoutManager = LinearLayoutManager(this)
        binding.rvHistorial.setHasFixedSize(true)

        arrayListActividades = arrayListOf()
        miAdapter = Adaptador(arrayListActividades)
        binding.rvHistorial.adapter = miAdapter
        binding.appbarHistorialActividad.actionbar[0].setOnClickListener { onBackPressed() }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun cargarDatosBBDD(campo: String, order: Query.Direction) {

        var collection = "Actividades"

        arrayListActividades.clear()
        var conectorBBDD = FirebaseFirestore.getInstance()
        conectorBBDD.collection(collection).orderBy(campo, order)
            .whereEqualTo("usuario", usuarioEmail)
            .get()
            .addOnSuccessListener { documento ->
                for (actividad in documento){
                    arrayListActividades.add(actividad.toObject(Actividades::class.java))
                }
                miAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error:", exception)
            }
    }

    override fun onResume() {
        super.onResume()
        cargarDatosBBDD("fechaActividad", Query.Direction.DESCENDING)
    }

    //Metodo que al darle atras nos envia a la pantalla principal del movil
    override fun onBackPressed() {
        var intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

}