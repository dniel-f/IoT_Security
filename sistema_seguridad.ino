/*
 * Codigo para el prototipo de Seguridad IoT (Unidad 1 y 2)
 * * Este código es compatible con la aplicación Android desarrollada:
 * - Lee los sensores LDR (luz) y PIR (movimiento).
 * - Envía alertas a la app por Bluetooth.
 * - Recibe comandos de la app para:
 * 'A' -> Armar alarma
 * 'D' -> Desarmar alarma
 * 'B' -> Probar Buzzer (Alarma)
 * 'L' -> Probar Luz (LED)
 */

// Incluir la librería para crear un puerto serial por software
#include <SoftwareSerial.h>

// --- Pines ---

// Bluetooth (HC-05/HC-06): RX en 10, TX en 11
SoftwareSerial BT(10, 11); 

// Sensores
const int LDR_PIN = A0;   // Sensor de luz
const int PIR_PIN = 2;    // Sensor de movimiento

// Actuadores
const int LED_PIN = 9;    // LED (pin PWM para regular brillo)
const int BUZZER_PIN = 8; // Buzzer

// --- Umbrales y Lógica ---
const int LDR_UMBRAL = 600;   // Umbral de oscuridad (ajusta según tu LDR)
const int LUZ_TENUE = 60;     // Brillo de la luz de noche (0-255)
const int LUZ_FUERTE = 255;   // Brillo de la luz al detectar movimiento (0-255)

// --- Estados Globales ---
bool esNoche = false;           // ¿Es de noche?
bool movimientoActivo = false;  // ¿Hay movimiento ahora?
bool alarmaArmada = false;      // ¿La app armó la alarma? (Controlado por el Switch)

void setup() {
  // Configurar pines de los sensores y actuadores
  pinMode(PIR_PIN, INPUT);
  pinMode(LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  // Iniciar comunicación Serial (para monitor USB)
  Serial.begin(9600);
  
  // Iniciar comunicación Bluetooth
  BT.begin(9600);

  delay(2000);
  Serial.println("Sistema de seguridad iniciado. Esperando conexión BT...");
  BT.println("Sistema de seguridad iniciado...");
}

void loop() {
  
  // 1. Leer los sensores
  int luz = analogRead(LDR_PIN);
  int pir = digitalRead(PIR_PIN);

  // 2. Lógica del Sensor de Luz (LDR)
  
  // Detectar si se hizo de noche
  if (luz < LDR_UMBRAL && !esNoche) {
    esNoche = true;
    analogWrite(LED_PIN, LUZ_TENUE); // Encender luz tenue
    Serial.println("Noche detectada - Luz tenue ON");
    BT.println("Noche detectada"); // Mensaje corto para la app
  }

  // Detectar si amaneció
  if (luz >= LDR_UMBRAL && esNoche) {
    esNoche = false;
    analogWrite(LED_PIN, 0); // Apagar luz
    Serial.println("Amanecer detectado - Luz OFF");
    BT.println("Amanecer detectado");
  }

  // 3. Lógica del Sensor de Movimiento (PIR)
  // Esta lógica solo se ejecuta si es de noche
  if (esNoche) {
    
    // Si se detecta NUEVO movimiento
    if (pir == HIGH && !movimientoActivo) {
      movimientoActivo = true;
      analogWrite(LED_PIN, LUZ_FUERTE); // Luz fuerte
      
      Serial.println("Movimiento detectado - Luz fuerte ON");
      BT.println("Movimiento detectado"); // Mensaje que la app guardará

      // Si la alarma está armada, sonar el buzzer
      if (alarmaArmada) {
        digitalWrite(BUZZER_PIN, HIGH);
        delay(600); // Beep corto
        digitalWrite(BUZZER_PIN, LOW);
      }
    }

    // Si el movimiento se detiene
    if (pir == LOW && movimientoActivo) {
      movimientoActivo = false;
      analogWrite(LED_PIN, LUZ_TENUE); // Volver a luz tenue
      Serial.println("Movimiento finalizado - Luz tenue ON");
      // No enviamos este mensaje a la app para no saturar el historial
    }
  }

  // 4. Lógica de Comandos de la App (Bluetooth)
  if (BT.available()) {
    char dato = BT.read(); // Leer el comando (un caracter)

    switch(dato) {
      case 'A': // (A)rmar Alarma (Switch ON)
        alarmaArmada = true;
        Serial.println("Comando: Alarma ARMADA");
        break;
        
      case 'D': // (D)esarmar Alarma (Switch OFF)
        alarmaArmada = false;
        Serial.println("Comando: Alarma DESARMADA");
        break;

      case 'B': // (B)uzzer Test
        Serial.println("Comando: Test Buzzer");
        digitalWrite(BUZZER_PIN, HIGH);
        delay(600); // Test momentáneo
        digitalWrite(BUZZER_PIN, LOW);
        break;

      case 'L': // (L)uz Test
        Serial.println("Comando: Test Luz");
        analogWrite(LED_PIN, LUZ_FUERTE);
        delay(600); // Test momentáneo
        // Volver al estado que corresponde
        if (esNoche) {
          analogWrite(LED_PIN, LUZ_TENUE);
        } else {
          analogWrite(LED_PIN, 0);
        }
        break;
    }
  }

  delay(200); // Pequeña pausa para estabilizar el bucle
}