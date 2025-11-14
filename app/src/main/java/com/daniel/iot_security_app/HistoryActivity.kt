package com.daniel.iot_security_app

import DatabaseHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView

class HistoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var listViewHistory: ListView
    private lateinit var historyListAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // 1. Inicializar DB Helper y ListView (esto sí se queda en onCreate)
        dbHelper = DatabaseHelper(this)
        listViewHistory = findViewById(R.id.listViewHistory)
    }

    /**
     * onResume() se llama CADA VEZ que esta actividad
     * vuelve a estar en primer plano.
     */
    override fun onResume() {
        super.onResume()
        // 2. Cargamos el historial aquí.
        loadHistory()
    }

    private fun loadHistory() {
        // 1. Obtener eventos de la BD (obtiene la lista más reciente)
        val eventListFromDB = dbHelper.getAllEvents()

        // 2. Convertimos la lista inmutable a una MUTABLE
        val eventListForAdapter = eventListFromDB.toMutableList()

        // 3. Opcional: Mostrar un texto si la lista está vacía
        if (eventListForAdapter.isEmpty()) {
            eventListForAdapter.add("No hay eventos registrados.")
        }

        // 4. Configurar el adaptador con la lista mutable
        historyListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, eventListForAdapter)
        listViewHistory.adapter = historyListAdapter
    }
}