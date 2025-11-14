Aplicación de Seguridad IoT

Este proyecto es una aplicación nativa de Android, desarrollada como parte de la asignatura "Aplicaciones Móviles para IoT" (TI3042). La aplicación sirve como un panel de control móvil para un prototipo de hardware de seguridad basado en Arduino, reemplazando la necesidad de una terminal serial genérica.

La aplicación permite a los usuarios autenticarse, escanear y conectarse a un dispositivo Bluetooth (como un módulo HC-05), monitorear el estado del sistema en tiempo real y enviar comandos para controlar actuadores (luces, alarmas). Además, todos los eventos relevantes se almacenan en una base de datos local para su posterior consulta.

Características Principales

1. Autenticación y Seguridad

Pantalla de Login: La aplicación se inicia con una pantalla de autenticación segura.

Registro de Usuarios: Permite a los nuevos usuarios registrarse.

Hashing de Contraseñas: Las contraseñas no se guardan en texto plano. Se utiliza un hash SHA-256 (a través de la clase PasswordHasher) antes de almacenar las credenciales en la base de datos local.

Base de Datos Local: Toda la información de usuarios y eventos se almacena en una base de datos SQLite (DatabaseHelper.kt).

2. Conectividad Bluetooth

Gestión de Permisos: Maneja la solicitud de permisos de Bluetooth (BLUETOOTH_SCAN, BLUETOOTH_CONNECT) y ubicación (ACCESS_FINE_LOCATION) para ser compatible con Android 11 (API 30) e inferiores, y Android 12 (API 31) y superiores.

Escaneo de Dispositivos: Escanea dispositivos Bluetooth Clásico y los muestra en una lista interactiva.

Gestión de Conexión: Se conecta al perfil SPP (Serial Port Profile) estándar, utilizando el UUID: 00001101-0000-1000-8000-00805F9B34FB, ideal para módulos como el HC-05.

Manejo de Tareas Asíncronas: Utiliza Corutinas de Kotlin (lifecycleScope) para manejar las operaciones de red (conexión, envío y recepción de datos) en hilos de fondo (Dispatchers.IO), evitando que la interfaz de usuario se congele.

3. Monitoreo y Control (Dashboard)

Recepción de Datos: Una vez conectado, la app entra en un bucle (startReadingData) para escuchar constantemente los mensajes enviados desde el Arduino (ej. "Movimiento detectado").

Control de Actuadores:

Armar/Desarmar Alarma: Un Switch (switchAlarm) permite armar (A) o desarmar (D) la alarma.

Prueba de Actuadores: Dos botones (buttonBuzzerTest, buttonLightTest) permiten activar momentáneamente la alarma (B) y la luz (L).

Estado en Tiempo Real: La UI refleja el estado actual de la conexión y los últimos mensajes recibidos.

4. Historial de Eventos

Registro Persistente: Cada acción importante (movimiento detectado, alarma armada/desarmada, pruebas manuales) se guarda en la base de datos SQLite con una marca de tiempo.

Visualización de Historial: Una pantalla dedicada (HistoryActivity) consulta la base de datos y muestra todos los eventos en orden cronológico inverso (el más nuevo primero), actualizándose cada vez que se abre la pantalla (onResume).

Stack Tecnológico

Lenguaje: Kotlin

IDE: Android Studio

Arquitectura de UI: Sistema de Vistas (Views) con XML

Componentes de UI: Material Design (ConstraintLayout, MaterialCardView, SwitchMaterial, Button)

Base de Datos: SQLite (usando SQLiteOpenHelper)

Asincronía: Corutinas de Kotlin (lifecycleScope, Dispatchers.IO, withContext)

Navegación: Intent entre Actividades (LoginActivity, MainActivity, HistoryActivity)

Hardware Requerido (Prototipo)

Arduino UNO (o compatible)

Módulo Bluetooth HC-05 (o similar con perfil SPP)

Sensor de Movimiento PIR

Sensor de Luz LDR

Buzzer y LED

Autor

Daniel Figueroa