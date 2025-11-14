import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "SecurityApp.db"

        // Tabla Usuarios
        private const val TABLE_USERS = "Usuarios"
        private const val COL_USER_ID = "id"
        private const val COL_USER_NAME = "username"
        private const val COL_USER_PASSWORD_HASH = "password_hash" // Importante: Hash, no texto plano [cite: 151]

        // Tabla Eventos
        private const val TABLE_EVENTS = "Eventos"
        private const val COL_EVENT_ID = "id"
        private const val COL_EVENT_TYPE = "tipo_evento" // Ej. "Movimiento Detectado"
        private const val COL_EVENT_TIMESTAMP = "timestamp" // Hora y fecha
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Crear tabla Usuarios
        val CREATE_USERS_TABLE = (
                "CREATE TABLE $TABLE_USERS (" +
                        "$COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "$COL_USER_NAME TEXT UNIQUE," +
                        "$COL_USER_PASSWORD_HASH TEXT)"
                )
        db?.execSQL(CREATE_USERS_TABLE)

        // Crear tabla Eventos
        val CREATE_EVENTS_TABLE = (
                "CREATE TABLE $TABLE_EVENTS (" +
                        "$COL_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "$COL_EVENT_TYPE TEXT," +
                        "$COL_EVENT_TIMESTAMP TEXT)" // Guardamos como TEXT (ISO 8601) para simplicidad
                )
        db?.execSQL(CREATE_EVENTS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        onCreate(db)
    }

    // (Aquí añadiremos funciones para registrar usuarios, validar login, insertar eventos, etc.)
    /**
     * Registra un nuevo usuario en la base de datos
     * Retorna 'true' si fue exitoso, 'false' si el usuario ya existe o hubo un error.
     */
    fun registerUser(username: String, password: String): Boolean {
        val db = this.writableDatabase

        // 1. Hashear la contraseña
        val hashedPassword = PasswordHasher.hashPassword(password)

        val values = android.content.ContentValues()
        values.put(COL_USER_NAME, username)
        values.put(COL_USER_PASSWORD_HASH, hashedPassword)

        // 2. Insertar el usuario.
        // El 'insert' retorna el ID de la fila, o -1 si hubo un error (ej. usuario duplicado)
        val result = db.insert(TABLE_USERS, null, values)
        db.close()

        return result != -1L
    }

    /**
     * Valida las credenciales del usuario.
     * Retorna 'true' si el usuario y la contraseña coinciden, 'false' en caso contrario.
     */
    fun validateUser(username: String, password: String): Boolean {
        val db = this.readableDatabase

        // 1. Hashear la contraseña ingresada para compararla
        val hashedPassword = PasswordHasher.hashPassword(password)

        // 2. Buscar en la BD un usuario que coincida con el nombre Y el hash de la contraseña
        val query = "SELECT * FROM $TABLE_USERS WHERE $COL_USER_NAME = ? AND $COL_USER_PASSWORD_HASH = ?"
        val cursor = db.rawQuery(query, arrayOf(username, hashedPassword))

        val userExists = cursor.count > 0

        cursor.close()
        db.close()

        return userExists
    }

    /**
     * Inserta un nuevo evento de seguridad en la base de datos.
     */
    fun addEvent(eventType: String): Boolean {
        val db = this.writableDatabase

        // 1. Obtener la fecha y hora actual
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())

        val values = android.content.ContentValues()
        values.put(COL_EVENT_TYPE, eventType)
        values.put(COL_EVENT_TIMESTAMP, timestamp)

        // 2. Insertar el evento
        val result = db.insert(TABLE_EVENTS, null, values)
        db.close()

        return result != -1L
    }
    /**
     * Retorna una lista de todos los eventos, del más nuevo al más viejo.
     */
    fun getAllEvents(): List<String> {
        val eventList = mutableListOf<String>()
        val db = this.readableDatabase

        // Consultar la BD, ordenando por ID de forma descendente (los más nuevos primero)
        val query = "SELECT * FROM $TABLE_EVENTS ORDER BY $COL_EVENT_ID DESC"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val eventTypeIndex = cursor.getColumnIndex(COL_EVENT_TYPE)
                val timestampIndex = cursor.getColumnIndex(COL_EVENT_TIMESTAMP)

                if (eventTypeIndex != -1 && timestampIndex != -1) {
                    val eventType = cursor.getString(eventTypeIndex)
                    val timestamp = cursor.getString(timestampIndex)

                    // Formateamos el string para la lista
                    eventList.add("[$timestamp] - $eventType")
                }
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return eventList
    }
}