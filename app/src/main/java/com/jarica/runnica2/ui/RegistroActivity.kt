package com.jarica.runnica2.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.jarica.runnica2.R
import com.jarica.runnica2.databinding.ActivityRegistroBinding
import com.jarica.runnica2.ui.LoginActivity.Companion.nombreUsuario
import com.jarica.runnica2.ui.LoginActivity.Companion.providerSesion
import com.jarica.runnica2.ui.LoginActivity.Companion.usuarioEmail
import java.text.SimpleDateFormat
import java.util.*


class RegistroActivity : AppCompatActivity() {

    //Variable Firebase
    private lateinit var auth: FirebaseAuth

    //Variable de vinculación de vistas
    private lateinit var binding: ActivityRegistroBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        //Establezco funcioanlidades
        funcionalidades()
        comprobarCamposdeTexto()

    }


    // Metodo que crea un nuevo usuario y lo mete en la coleccion de FireBase usuarios
    private fun nuevoregistro() {


        if (binding.etEmail.text.isNotEmpty() || binding.etContrasea.text.isNotEmpty() || binding.etRepiteContrasea.text.isNotEmpty() || binding.etNombre.text.isNotEmpty()) {

            if (binding.etContrasea.text == binding.etRepiteContrasea.text) {

                if (binding.etContrasea.text.length > 5) {

                    if (binding.cbcondiciones.isChecked) {
                        auth.createUserWithEmailAndPassword(
                            binding.etEmail.text.toString(),
                            binding.etRepiteContrasea.text.toString()
                        )
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    registrarBBDD(binding.etEmail.text.toString(), nombreUsuario)
                                    usuarioEmail = binding.etEmail.text.toString()
                                    providerSesion = "email"
                                    nombreUsuario = nombreUsuario

                                    goHome()
                                } else {
                                    Toast.makeText(this, "naaada", Toast.LENGTH_SHORT).show()
                                }

                            }
                    } else {
                        Toast.makeText(this, R.string.terminosycondiciones, Toast.LENGTH_SHORT)
                            .show()
                    }

                } else {
                    Toast.makeText(this, R.string.contrasediferente, Toast.LENGTH_SHORT).show()
                }


            } else {
                Toast.makeText(this, R.string.contrase6caracteres, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, R.string.faltandatos, Toast.LENGTH_SHORT).show()
        }

    }

    //Metodo que inserta en la BBDD el usuario nuevo
    @SuppressLint("SimpleDateFormat")
    private fun registrarBBDD(email: String, user: String) {

        var fechaRegistro = SimpleDateFormat("dd/MM/yyyy").format(Date())
        var registroBBDD = FirebaseFirestore.getInstance()
        registroBBDD.collection("usuarios").document(email).set(
            hashMapOf(
                "apodo" to user,
                "fechaRegistro" to fechaRegistro,
                "emailUsuario" to email,
                "registro" to "email",
            )
        )
    }

    //Metodo que envio a la pagina principal
    private fun goHome() {
        val intento = Intent(this, MainActivity::class.java)
        startActivity(intento)
    }

    private fun funcionalidades() {

        //Anular o activar el boton de registrarse si no estan los termines y condiciones aceptados.
        binding.cbcondiciones.setOnClickListener {
            if (binding.cbcondiciones.isChecked) binding.btRegistrate.isEnabled = true
            if (!binding.cbcondiciones.isChecked) binding.btRegistrate.isEnabled = false
        }

        //Boton de registrarse
        binding.btRegistrate.setOnClickListener { nuevoregistro() }

        //Intent terminos y condiciones de uso
        binding.tvTerminosYCondiciones.setOnClickListener {
            //val intento = Intent(this, TerminosYCondiciones::class.java)
            //startActivity(intento)
        }

        //EditText Email
        binding.etEmail.addTextChangedListener {
            if (binding.etEmail.text.toString().isEmpty()) binding.etEmail.error = ""
        }

        //EditText Usuario
        binding.etNombre.addTextChangedListener {
            if (binding.etNombre.text.toString().isEmpty()) binding.etNombre.error = ""
        }

        //EditText contraseña
        binding.etContrasea.addTextChangedListener {
            if (binding.etContrasea.text.toString().isEmpty()) binding.etContrasea.error = ""
        }

        //EditText Nueva Contraseña
        binding.etRepiteContrasea.addTextChangedListener {
            if (binding.etRepiteContrasea.text.toString()
                    .isEmpty()
            ) binding.etRepiteContrasea.error = ""
        }

    }

    private fun comprobarCamposdeTexto() {
        if (binding.etEmail.text.toString().isEmpty()) binding.etEmail.error = ""
        if (binding.etNombre.text.toString().isEmpty()) binding.etNombre.error = ""
        if (binding.etContrasea.text.toString().isEmpty()) binding.etContrasea.error = ""
        if (binding.etRepiteContrasea.text.toString().isEmpty()) binding.etRepiteContrasea.error =
            ""
    }
}