package com.daniel.iot_security_app

import DatabaseHelper
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var editTextUser: EditText
    private lateinit var editTextPass: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. Inicializar el helper de la BD
        dbHelper = DatabaseHelper(this)

        // 2. Referenciar las Vistas (Views)
        // Nota: Asegúrate de que los IDs en activity_login.xml coincidan
        editTextUser = findViewById(R.id.editTextUser)
        editTextPass = findViewById(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)

        // 3. Configurar el listener del botón de Registro
        buttonRegister.setOnClickListener {
            handleRegister()
        }

        // 4. Configurar el listener del botón de Login
        buttonLogin.setOnClickListener {
            handleLogin()
        }
    }

    private fun handleRegister() {
        val username = editTextUser.text.toString().trim()
        val password = editTextPass.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            showToast("Usuario y contraseña no pueden estar vacíos")
            return
        }

        // Intentar registrar al usuario
        val success = dbHelper.registerUser(username, password)

        if (success) {
            showToast("Usuario '$username' registrado exitosamente")
            // Limpiar campos después del registro
            editTextUser.text.clear()
            editTextPass.text.clear()
        } else {
            showToast("Error al registrar: El usuario '$username' ya existe")
        }
    }

    private fun handleLogin() {
        val username = editTextUser.text.toString().trim()
        val password = editTextPass.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            showToast("Por favor, ingrese usuario y contraseña")
            return
        }

        // Validar con la base de datos
        val isValid = dbHelper.validateUser(username, password)

        if (isValid) {
            showToast("¡Bienvenido $username!")

            // --- ¡ÉXITO! ---
            // Aquí es donde navegaríamos a la siguiente pantalla (MainActivity)
            // (Lo haremos en el siguiente paso)

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Cierra la actividad de Login

        } else {
            showToast("Error: Usuario o contraseña incorrectos")
        }
    }

    // Función de utilidad para mostrar mensajes
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}