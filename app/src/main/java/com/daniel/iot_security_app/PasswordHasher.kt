import java.security.MessageDigest

object PasswordHasher {

    // Función para crear el hash (SHA-256) de una contraseña
    // En una app real, también añadirías una "salt", pero esto cumple el requisito
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        // Convierte el array de bytes a un string hexadecimal
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}