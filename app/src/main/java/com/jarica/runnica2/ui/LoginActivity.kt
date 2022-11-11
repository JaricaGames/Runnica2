package com.jarica.runnica2.ui

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.jarica.runnica2.Dataclasses.Usuario
import com.jarica.runnica2.R
import com.jarica.runnica2.databinding.ActivityLoginBinding
import java.text.SimpleDateFormat
import java.util.*


class LoginActivity : AppCompatActivity() {

    //variable Firebase
    private lateinit var auth: FirebaseAuth

    //Variable de vinculación de vistas
    private lateinit var binding: ActivityLoginBinding

    //Variables Inicio Google
    private lateinit var googleSignInClient: GoogleSignInClient
    val RC_SIGN_IN = 1000

    companion object {
        lateinit var usuarioEmail: String
        lateinit var providerSesion: String
        lateinit var nombreUsuario: String
        lateinit var iconoUsuario: Uri
        lateinit var deporteSeleccionado: String //Running -- Walk ---Bike
    }

    //COMIENZO DEL ONCREATE
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        //establezco listeners & onClicks
        funcionalidades()

        // [START config_signin]
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) //Error no se puede quitar
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        // [END config_signin]


    }
    //FIN DEL ONCREATE


    public override fun onStart() {

        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {

            usuarioEmail = currentUser.email.toString()
            nombreUsuario = currentUser.displayName.toString()
            providerSesion = ""
            iconoUsuario = "".toUri()
            buscarUsuarioenBBDD(usuarioEmail)
            //println(providerSesion)

            if (providerSesion == "Google") {
                iconoUsuario = currentUser.photoUrl!!
                println("Estoy aqui")

            }

            buscarUsuarioenBBDD(usuarioEmail)
            goHome()

        }

        deporteSeleccionado = "Running"

    }

    private fun buscarUsuarioenBBDD(usuarioEmail: String) {

        var usuario: Usuario
        val BBDDFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
        BBDDFirestore.collection("usuarios").document(usuarioEmail)
            .get()
            .addOnSuccessListener { document ->
                if (document.data?.size != null) {
                    usuario = document.toObject<Usuario>()!!
                    providerSesion = usuario.registro.toString()
                    nombreUsuario = usuario.apodo.toString()

                    //println(providerSesion)
                } else {
                    println("pringao")
                }

            }
            .addOnFailureListener {
                providerSesion = "hola"
            }
    }

    //METODO DONDE IMPLANTO LAS FUNCIONALIDADES AL LAYOUT
    private fun funcionalidades() {

        binding.tvRegistro.setOnClickListener {
            val intento = Intent(this, RegistroActivity::class.java)
            startActivity(intento)
        }

        binding.btInicioSesion.setOnClickListener {
            providerSesion = "email"
            iniciarSesion()
        }

        binding.tvolvido.setOnClickListener {
            //   val intento = Intent(this, Olvido_Activity::class.java)
            // startActivity(intento)
        }

        binding.btGoogle.setOnClickListener {
            providerSesion = "Google"
            signIn()
        }
    }

    //METODO INICIO SESION CON USUARIO Y CONTRASEÑA
    private fun iniciarSesion() {

        if (binding.etContraseA.text.toString().isEmpty() || binding.etEmail.text.toString()
                .isEmpty()
        ) {
            Toast.makeText(
                baseContext, "Faltan datos por rellenar",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            val password = binding.etContraseA.text.toString()
            val email = binding.etEmail.text.toString()

            usuarioEmail = email
            nombreUsuario = ""
            providerSesion = "email"


            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        goHome()
                    } else {
                        Toast.makeText(
                            baseContext, "Usuario y/o contraseña incorrectos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

    }


    // [START onactivityresult - PARA INICIO DE SESION CON GOOGLE]
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(ContentValues.TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)

                usuarioEmail = account.email.toString()
                nombreUsuario = account.displayName.toString()
                iconoUsuario = account.photoUrl!!
                providerSesion = "Google"

            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(ContentValues.TAG, "Google sign in failed", e)
            }
        }
    }
    // [END onactivityresult]

    // [START auth_with_google]
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    registrarBBDD(usuarioEmail, nombreUsuario)
                    goHome()
                    Log.d(ContentValues.TAG, "signInWithCredential:success")

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(ContentValues.TAG, "signInWithCredential:failure", task.exception)
                }
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
                "registro" to "Google",
            )
        )
    }


    // [START signin]
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    // [END signin]


    private fun showError(provider: String) {
        Toast.makeText(this, "Error en la conexión con $provider", Toast.LENGTH_SHORT)
    }


    //Metodo que al darle atras nos envia a la pantalla principal del movil
    override fun onBackPressed() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    //Metodo que nos manda a la pagina principal
    private fun goHome() {
        val intento = Intent(this, MainActivity::class.java)
        startActivity(intento)
    }

}