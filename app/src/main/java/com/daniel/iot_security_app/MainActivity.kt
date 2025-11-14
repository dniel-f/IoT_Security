package com.daniel.iot_security_app

import DatabaseHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest // Importante
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.bluetooth.BluetoothDevice

import android.widget.ArrayAdapter
import android.widget.ListView

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class MainActivity : AppCompatActivity() { // <-- 1. Cambiamos la herencia


    // --- Variables para la lista de dispositivos ---
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val discoveredDevicesList = ArrayList<BluetoothDevice>() // Lista para guardar los dispositivos
    private val discoveredDevicesNames = ArrayList<String>()
    private var deviceFoundCount = 0


    // --- Variables de Conexión ---
    private var bluetoothSocket: BluetoothSocket? = null

    // UUID Estándar para Serial Port Profile (SPP)
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private lateinit var switchAlarm: com.google.android.material.switchmaterial.SwitchMaterial

    //base de datos
    private lateinit var dbHelper: DatabaseHelper

    //botones
    private lateinit var buttonBuzzerTest: Button
    private lateinit var buttonLightTest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        // 1. Referenciar las Vistas
        buttonConnect = findViewById(R.id.buttonConnect)
        textViewSystemStatus = findViewById(R.id.textViewSystemStatus)
        textViewConnectionStatus = findViewById(R.id.textViewConnectionStatus)
        // val switchAlarm = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchAlarm)
        // val buttonHistory = findViewById<Button>(R.id.buttonHistory)
        switchAlarm = findViewById(R.id.switchAlarm)

        buttonBuzzerTest = findViewById(R.id.buttonBuzzerTest)
        buttonLightTest = findViewById(R.id.buttonLightTest)

        //
        val buttonHistory = findViewById<Button>(R.id.buttonHistory)
        buttonHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        // 2. Inicializar el Manager de Bluetooth
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // 3. Configurar el botón de Conectar
        buttonConnect.setOnClickListener {
            checkPermissionsAndSetupBluetooth()
        }

        // 4. Configurar la ListView y su adaptador
        listViewDevices = findViewById(R.id.listViewDevices)
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, discoveredDevicesNames)
        listViewDevices.adapter = deviceListAdapter

        // 5. Configurar el clic de la lista
        listViewDevices.setOnItemClickListener { parent, view, position, id ->
            // El usuario hizo clic en un dispositivo
            val selectedDevice = discoveredDevicesList[position] // Obtenemos el dispositivo real
            connectToDevice(selectedDevice) // Próximo paso: conectar!
        }
        // 6. Configurar el listener del Switch de Alarma
        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            // Este código se ejecuta CADA VEZ que el switch cambia

            if (isChecked) {
                // El switch fue ACTIVADO
                val eventMessage = "Alarma ARMADA (Manual)"
                sendData("A")

                // Guarda el evento en la BD
                dbHelper.addEvent(eventMessage)
            } else {
                // El switch fue DESACTIVADO
                val eventMessage = "Alarma DESARMADA (Manual)"
                sendData("D")

                //Guarda el evento en la BD
                dbHelper.addEvent(eventMessage)
            }
        }
        // Listener para el botón de Alarma
        buttonBuzzerTest.setOnClickListener {
            val eventMessage = "Test de Alarma Activado (Manual)"
            sendData("B") // 'B' para Buzzer

            //Guarda el evento en la BD
            dbHelper.addEvent(eventMessage)
        }

        // Listener para el botón de Luz
        buttonLightTest.setOnClickListener {
            val eventMessage = "Test de Luz Activado (Manual)"
            sendData("L") // 'L' para Luz

            //Guarda el evento en la BD
            dbHelper.addEvent(eventMessage)
        }
    }

    private lateinit var listViewDevices: ListView

    /**
     * Paso 1: Revisa si tenemos los permisos necesarios.
     */
    private fun checkPermissionsAndSetupBluetooth() {

        // 1. Preparamos la lista de permisos necesarios
        val requiredPermissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Para Android 12 (API 31) y superior
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)

            // --- 💡 EL CAMBIO CLAVE ESTÁ AQUÍ 💡 ---
            // Añadimos el permiso de ubicación también para API 31+
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        } else {
            // Para Android 11 (API 30) e inferior
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // 2. Revisamos si ya tenemos los permisos de esa lista
        var allPermissionsGranted = true
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false
                break
            }
        }

        // 3. Actuamos
        if (allPermissionsGranted) {
            // Ya tenemos todo, vamos al Paso 2
            setupBluetooth()
        } else {
            // Pedimos los permisos que falten
            requestBluetoothPermissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }
    /**
     * Paso 2: Revisa si el Bluetooth está encendido.
     */
    private fun setupBluetooth() {
        if (bluetoothAdapter == null) {
            textViewConnectionStatus.text = "Error: Dispositivo no compatible con Bluetooth."
            return
        }

        // Revisa los permisos de conexión ANTES de usarlos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Si llegamos aquí sin permisos, es un error (aunque no debería pasar por el checkPermissions)
            textViewConnectionStatus.text = "Error: Faltan permisos de conexión."
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            // El Bluetooth está apagado, pedir al usuario que lo encienda
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            // Usamos el NUEVO launcher para manejar el resultado
            enableBluetoothLauncher.launch(enableBtIntent)

            textViewConnectionStatus.text = "Por favor, enciende el Bluetooth."

        } else {
            // ¡Bluetooth ya está encendido!
            isBluetoothEnabled = true
            textViewConnectionStatus.text = "Bluetooth activado."

            // Empezamos a escanear directamente
            scanDevices() // <-- Este es el próximo paso
        }
    }
    // --- Variables de Bluetooth ---
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var isBluetoothEnabled = false

    // --- Variables de UI (Vistas) ---
    private lateinit var buttonConnect: Button
    private lateinit var textViewSystemStatus: TextView
    private lateinit var textViewConnectionStatus: TextView
    // (Añadiremos el switch y el historial más tarde)

    // --- Manejador de permisos ---
    // Este es el nuevo método para manejar los resultados de permisos
    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        // Revisamos cada permiso individualmente
        val permissionsGranted = permissions.all { it.value }

        if (permissionsGranted) {
            // Todos los permisos concedidos, continuamos
            setupBluetooth()
        } else {
            // Informamos al usuario si algo fue denegado
            var deniedPermission = ""
            permissions.forEach {
                if (!it.value) {
                    deniedPermission = it.key // Guardamos el nombre del permiso denegado
                }
            }
            textViewConnectionStatus.text = "Error: Permiso '$deniedPermission' fue denegado."
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // El usuario activó el Bluetooth
            isBluetoothEnabled = true
            textViewConnectionStatus.text = "Bluetooth activado."
            // ¡AHORA SÍ! Empezamos a escanear
            scanDevices() // <-- Este es el próximo paso
        } else {
            // El usuario canceló la activación
            textViewConnectionStatus.text = "Error: Bluetooth no fue activado."
        }


    }

    /**
     * "Antena" que escucha los eventos de descubrimiento de Bluetooth.
     */
    private val discoveryReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            val action: String = intent.action ?: ""

            when(action) {

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    // (Tu código está bien)
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // (Tu código está bien)
                }

                BluetoothDevice.ACTION_FOUND -> {
                    // --- Vamos a ser más "ruidosos" ---

                    // Incrementamos un contador
                    deviceFoundCount++ // (Añade 'private var deviceFoundCount = 0' arriba en tu clase)

                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                    if (device != null) {
                        // (Tu chequeo de permiso para API 31+ está bien)
                        // ...

                        val deviceName = device.name ?: "--- SIN NOMBRE ---"
                        val deviceAddress = device.address
                        val deviceInfo = "$deviceName ($deviceAddress)"

                        // Evitamos duplicados
                        if (!discoveredDevicesList.contains(device)) {
                            discoveredDevicesList.add(device)
                            discoveredDevicesNames.add(deviceInfo)
                            deviceListAdapter.notifyDataSetChanged()
                        }

                    } else {
                        // Avisamos si el dispositivo es nulo
                        textViewSystemStatus.append("\nEncontrado: ¡Dispositivo NULO!")
                    }

                    // Mostramos el contador
                    textViewConnectionStatus.text = "Encontrados: $deviceFoundCount"
                }
            }
        }
    }
    /**
     * Paso 3: Escanea dispositivos Bluetooth cercanos
     */
    private fun scanDevices() {
        // (Tus chequeos de permisos Siguen aquí - ¡Perfecto!)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                textViewConnectionStatus.text = "Error: Faltan permisos de escaneo."
                return
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                textViewConnectionStatus.text = "Error: Faltan permisos de conexión."
                return
            }
        }

        // 1. Limpiamos el texto
        textViewSystemStatus.text = "Iniciando escaneo..."
        discoveredDevicesList.clear()
        deviceFoundCount = 0
        discoveredDevicesNames.clear()
        deviceListAdapter.notifyDataSetChanged() // Avisa a la UI que la lista está vacía

        // 2. Registramos nuestra "antena" para MÁS EVENTOS
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        //
        // Le decimos que también escuche cuándo empieza y termina el escaneo
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

        registerReceiver(discoveryReceiver, filter)

        // 3. ¡Comenzamos el descubrimiento!
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        //
        // Comprobamos si el escaneo pudo iniciarse
        val discoveryStarted = bluetoothAdapter.startDiscovery()

        if (discoveryStarted) {
            // No cambiamos el texto aquí, esperamos al ACTION_DISCOVERY_STARTED
            //textViewSystemStatus.text = "Buscando dispositivos..." // (Línea antigua)
        } else {
            // ¡EL ESCANEO FALLÓ!
            textViewSystemStatus.text = "Error: El escaneo no pudo iniciarse."
            textViewConnectionStatus.text = "Intenta reiniciar el Bluetooth de tu teléfono."
        }
    }


    /**
     * Paso 4: Se seleccionó un dispositivo, intentar conectar (versión REAL)
     */
    private fun connectToDevice(device: BluetoothDevice) {

        textViewSystemStatus.text = "Conectando a: ${device.name}..."
        textViewConnectionStatus.text = "Conectando..."

        // 1. Detenemos el escaneo
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) { return }
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        // 2. (El bloque 'Handler' de simulación se ha eliminado)

        // 3. Lanzamos una Corutina para hacer la conexión en segundo plano
        lifecycleScope.launch(Dispatchers.IO) { // Hilo de fondo (IO)

            var socket: BluetoothSocket? = null
            try {
                // 4. Chequeo de permiso (otra vez, por seguridad)
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    throw SecurityException("Permiso BLUETOOTH_CONNECT denegado")
                }

                // 5. Creamos el socket
                socket = device.createRfcommSocketToServiceRecord(MY_UUID)

                // 6. ¡Conectamos! Esta es la llamada bloqueante
                socket?.connect()

                // 7. ¡ÉXITO!
                bluetoothSocket = socket // Guardamos el socket globalmente

                // 8. Actualizamos la UI (volviendo al hilo principal)
                withContext(Dispatchers.Main) {
                    textViewSystemStatus.text = "¡Conectado a ${device.name}!"
                    textViewConnectionStatus.text = "Conexión establecida"
                    // Habilitamos los controles
                    switchAlarm.isEnabled = true
                    buttonBuzzerTest.isEnabled = true
                    buttonLightTest.isEnabled = true
                }

                // Ahora que estamos conectados, empezamos a leer datos del Arduino
                startReadingData(bluetoothSocket)

            } catch (e: Exception) {
                // 10. Si algo falla...
                socket?.close()
                withContext(Dispatchers.Main) {
                    textViewSystemStatus.text = "Error de conexión"
                    // (El error "read failed" que veías antes ahora será un error real)
                    textViewConnectionStatus.text = "Fallo al conectar: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Apagamos la "antena" para evitar fugas de memoria
        try {
            unregisterReceiver(discoveryReceiver)
        } catch (e: IllegalArgumentException) {
            // (Ignoramos el error si no estaba registrada)
        }
    }

    /**
     * Paso 5: Envía datos (un String) al dispositivo conectado.
     */
    private fun sendData(data: String) {
        if (bluetoothSocket == null) {
            // No estamos conectados, no podemos enviar nada
            Toast.makeText(this, "Error: No hay conexión", Toast.LENGTH_SHORT).show()

            // (Opcional: deshabilitar el switch si se pierde la conexión)
            // switchAlarm.isChecked = false
            // switchAlarm.isEnabled = false
            return
        }

        lifecycleScope.launch(Dispatchers.IO) { // Hilo de fondo para E/S
            try {
                // Obtenemos el 'stream' de salida y enviamos los bytes
                bluetoothSocket?.outputStream?.write(data.toByteArray())

                // (Opcional: Avisar en la UI que se envió)
                // withContext(Dispatchers.Main) {
                //    textViewSystemStatus.append("\nEnviado: $data")
                // }

            } catch (e: IOException) {
                // Error de envío (ej. se desconectó el HC-05)
                withContext(Dispatchers.Main) {
                    textViewSystemStatus.text = "Error de envío: ${e.message}"
                    // (Aquí deberíamos cerrar la conexión y deshabilitar controles)
                }
            }
        }
    }
    /**
     * Paso 6: Inicia un bucle para leer datos del socket conectado.
     */
    private fun startReadingData(socket: BluetoothSocket?) {
        if (socket == null) return

        lifecycleScope.launch(Dispatchers.IO) { // Hilo de fondo
            val inputStream = socket.inputStream
            val buffer = ByteArray(1024) // Espacio para guardar los bytes leídos
            var numBytes: Int

            while (true) { // Bucle infinito para seguir escuchando
                try {
                    // .read() es una llamada bloqueante. Espera hasta recibir datos.
                    numBytes = inputStream.read(buffer)

                    // Convertimos los bytes leídos a un String
                    val receivedMessage = String(buffer, 0, numBytes)

                    // ¡Recibimos datos! Ahora los procesamos
                    handleReceivedData(receivedMessage)

                } catch (e: IOException) {
                    // Error (ej. el HC-05 se desconectó)
                    withContext(Dispatchers.Main) {
                        textViewSystemStatus.text = "Error: Conexión perdida."
                        textViewConnectionStatus.text = "Desconectado"
                        switchAlarm.isEnabled = false
                        switchAlarm.isChecked = false
                        buttonBuzzerTest.isEnabled = false
                        buttonLightTest.isEnabled = false
                    }
                    break // Salir del bucle
                }
            }
        }
    }

    /**
     * Procesa los datos recibidos y actualiza la UI/Base de Datos.
     */
    private fun handleReceivedData(message: String) {
        // 'message' podría contener "Movimiento detectado" o "Noche detectada"

        // 1. Actualizar la UI (volviendo al Hilo Principal)
        lifecycleScope.launch(Dispatchers.Main) {
            textViewSystemStatus.text = "Recibido: $message"
        }

        // 2. Guardar en la Base de Datos (esto puede ser en el hilo actual)
        // Solo guardamos si es un evento de movimiento
        if (message.contains("Movimiento")) {
            dbHelper.addEvent(message)
        }
    }
}
