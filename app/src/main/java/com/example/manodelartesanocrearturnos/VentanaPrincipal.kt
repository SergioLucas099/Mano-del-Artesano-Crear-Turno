package com.example.manodelartesanocrearturnos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.manodelartesanocrearturnos.Adapter.VerAtraccionAdapter
import com.example.manodelartesanocrearturnos.Model.AtraccionModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.core.widget.doOnTextChanged
import com.google.firebase.database.DatabaseReference

class VentanaPrincipal : AppCompatActivity() {

    private lateinit var listaAtracciones : ArrayList<AtraccionModel>

    var contador = 1
    private lateinit var databaseReference : DatabaseReference
    private var tiempoAcumuladoSegundos: Int = 0
    var tiempoAcumulado = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ventana_principal)

        val RevVerAtraccion = findViewById<RecyclerView>(R.id.RevVerAtraccion)
        val AtraccionSeleccionada = findViewById<TextView>(R.id.AtraccionSeleccionada)
        val disminuir = findViewById<ImageView>(R.id.disminuir)
        val ContadorPersonas = findViewById<EditText>(R.id.ContadorPersonas)
        val agregar = findViewById<ImageView>(R.id.agregar)
        val txtTiempo = findViewById<TextView>(R.id.txtTiempo)
        val edtxNumero = findViewById<EditText>(R.id.edtxNumero)
        val btnSubirInfo = findViewById<Button>(R.id.btnSubirInfo)

        var valorId = ""
        var valorNombre = ""
        var valorTurno = ""

        // Obtener tiempo acumulado de Firebase
        databaseReference = FirebaseDatabase.getInstance()
            .getReference("TiempoAcumulado")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val texto = snapshot.getValue(String::class.java)
                if (texto != null) {
                    // convertir mm:ss a segundos
                    tiempoAcumuladoSegundos = convertirATiempoSegundos(texto)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Error: ${error.message}")
            }
        })

        // Configuracion Ver Atracciones
        RevVerAtraccion.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        listaAtracciones = arrayListOf<AtraccionModel>()
        RevVerAtraccion.visibility = View.GONE
        FirebaseDatabase.getInstance().reference.child("Atracciones")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot){
                    listaAtracciones.clear()
                    if (snapshot.exists()){
                        for (Snap in snapshot.children){
                            val data = Snap.getValue(AtraccionModel::class.java)
                            listaAtracciones.add(data!!)
                        }
                    }

                    val adapter = VerAtraccionAdapter(listaAtracciones) { textoSeleccionado ->
                        AtraccionSeleccionada.setText(textoSeleccionado.Nombre)
                        valorId = textoSeleccionado.Id.toString()
                        //valorNombre = textoSeleccionado.Nombre.toString()
                        valorNombre = "Mano Del Artesano"
                        valorTurno = textoSeleccionado.Turno.toString()
                    }
                    RevVerAtraccion.adapter = adapter
                    RevVerAtraccion.visibility = View.VISIBLE
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        //Configurar Botones Aumentar Disminuir Personas

        disminuir.setOnClickListener {
            val valorActual = ContadorPersonas.text.toString().toIntOrNull() ?: 1
            contador = valorActual

            if (contador > 1) {
                contador--
            } else {
                contador = 1
            }

            ContadorPersonas.setText(contador.toString())
            ContadorPersonas.setSelection(ContadorPersonas.text.length)
            txtTiempo.text = "Tiempo: ${mostrarTiempo(contador)}"
        }

        agregar.setOnClickListener {
            val valorActual = ContadorPersonas.text.toString().toIntOrNull() ?: 1
            contador = valorActual + 1

            ContadorPersonas.setText(contador.toString())
            ContadorPersonas.setSelection(ContadorPersonas.text.length)
            txtTiempo.text = "Tiempo: ${mostrarTiempo(contador)}"
        }

        // Escuchar cambios directos del usuario en el EditText
        ContadorPersonas.doOnTextChanged { text, _, _, _ ->
            val valorActual = text?.toString()?.toIntOrNull()

            if (text.isNullOrEmpty()) {
                // Si el usuario borra todo, dejamos vacío
                contador = 0
                txtTiempo.text = "Tiempo: 00:00 seg"
            } else if (valorActual != null && valorActual > 0) {
                // Si escribe un número válido mayor a 0
                contador = valorActual
                txtTiempo.text = "Tiempo: ${mostrarTiempo(contador)}"
            } else {
                // Si escribe 0 o algo inválido, solo actualizamos el texto de tiempo
                contador = 0
                txtTiempo.text = "Tiempo: 00:00 seg"
            }
        }

        // Crear Turno
        btnSubirInfo.setOnClickListener {
            val BDTurnosAcumulados = FirebaseDatabase.getInstance().getReference("TurnosAcumulados")
            val numeroTelefonico = edtxNumero.text?.toString()?.trim() ?: ""
            val IdTurnoE = BDTurnosAcumulados.push().key.toString()
            val nombreAtraccion = AtraccionSeleccionada.text.toString()


            val atraccionRef = FirebaseDatabase.getInstance()
                .getReference("Atracciones")
                .child("Mano Del Artesano")

            if (numeroTelefonico.isEmpty()){
                atraccionRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {

                        val ultimoTurno = snapshot.child("Turno").getValue(Int::class.java) ?: 0
                        val nuevoTurno = ultimoTurno + 1
                        val turnoFormateado = String.format("%04d", nuevoTurno) // ejemplo: 0001, 0002
                        var tiempoPreview = ""

                        // acumular tiempo

                        if (tiempoAcumuladoSegundos == 0){
                            tiempoPreview = "0"
                        }else{
                            tiempoPreview = formatearTiempo(tiempoAcumuladoSegundos + contador * 20)
                        }

                        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmar_info, null)
                        dialogView.findViewById<TextView>(R.id.txtDatosAtraccionTurno).text = "Mano Del Artesano"
                        dialogView.findViewById<TextView>(R.id.txtDatosNumeroTurno).text = turnoFormateado
                        dialogView.findViewById<TextView>(R.id.txtDatosPersonasTurno).text = ContadorPersonas.text
                        dialogView.findViewById<TextView>(R.id.txtDatosTelefonoTurno).text = "No Registrado"
                        dialogView.findViewById<TextView>(R.id.txtDatosTiempoTurno).text = "${actualizarTiempo(contador)} min"
                        dialogView.findViewById<TextView>(R.id.txtTiempoEsperaTurno).text = "$tiempoPreview min"

                        val dialog = AlertDialog.Builder(this)
                            .setView(dialogView)
                            .setCancelable(false)
                            .setNegativeButton("Cancelar") { d, _ ->
                                d.dismiss()
                            }.setPositiveButton("Aceptar"){ d, _ ->

                                tiempoAcumuladoSegundos += contador * 20
                                val tiempoFinal = formatearTiempo(tiempoAcumuladoSegundos)

                                val map: MutableMap<String, Any> = HashMap()
                                map["Id"] = IdTurnoE
                                map["Atraccion"] = "Mano Del Artesano"
                                map["TurnoAsignado"] = turnoFormateado
                                map["NumeroPersonas"] = ContadorPersonas.text.toString()
                                map["NumeroTelefonico"] = "No Registrado"
                                map["Tiempo"] = actualizarTiempo(contador)
                                map["TiempoEspera"] = tiempoPreview
                                map["Estado"] = "En Espera"

                                databaseReference.setValue(tiempoFinal)

                                BDTurnosAcumulados.child(IdTurnoE).setValue(map).addOnSuccessListener {
                                    AtraccionSeleccionada.setText("Mano Del Artesano")
                                    edtxNumero.setText("")
                                    ContadorPersonas.setText("")
                                    txtTiempo.text = "Tiempo: 00:20 seg"
                                    contador = 1
                                    Toast.makeText(this, "Turno $turnoFormateado para $valorNombre creado con exito", Toast.LENGTH_SHORT).show()

                                    // Quitar el foco del EditText
                                    edtxNumero.clearFocus()

                                    // Ocultar el teclado
                                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.hideSoftInputFromWindow(edtxNumero.windowToken, 0)

                                }.addOnFailureListener {
                                    Toast.makeText(this, "Error al crear el turno", Toast.LENGTH_SHORT).show()
                                }

                                // Guardar nuevo turno en Firebase
                                atraccionRef.child("Turno").setValue(nuevoTurno)
                            }.create()

                        dialog.show()

                    } else {
                        // Si la atracción no tiene campo UltimoTurno aún, se crea con 1
                        atraccionRef.child("Turno").setValue(1)
                    }
                }
            }else{
                atraccionRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {

                        val ultimoTurno = snapshot.child("Turno").getValue(Int::class.java) ?: 0
                        val nuevoTurno = ultimoTurno + 1
                        val turnoFormateado = String.format("%04d", nuevoTurno) // ejemplo: 0001, 0002
                        var tiempoPreview = ""

                        // acumular tiempo

                        if (tiempoAcumuladoSegundos == 0){
                            tiempoPreview = "0"
                        }else{
                            tiempoPreview = formatearTiempo(tiempoAcumuladoSegundos + contador * 20)
                        }

                        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmar_info, null)
                        dialogView.findViewById<TextView>(R.id.txtDatosAtraccionTurno).text = "Mano Del Artesano"
                        dialogView.findViewById<TextView>(R.id.txtDatosNumeroTurno).text = turnoFormateado
                        dialogView.findViewById<TextView>(R.id.txtDatosPersonasTurno).text = ContadorPersonas.text
                        dialogView.findViewById<TextView>(R.id.txtDatosTelefonoTurno).text = numeroTelefonico
                        dialogView.findViewById<TextView>(R.id.txtDatosTiempoTurno).text = "${actualizarTiempo(contador)} min"
                        dialogView.findViewById<TextView>(R.id.txtTiempoEsperaTurno).text = "$tiempoPreview min"

                        val dialog = AlertDialog.Builder(this)
                            .setView(dialogView)
                            .setCancelable(false)
                            .setNegativeButton("Cancelar") { d, _ ->
                                d.dismiss()
                            }.setPositiveButton("Aceptar"){ d, _ ->

                                tiempoAcumuladoSegundos += contador * 20
                                val tiempoFinal = formatearTiempo(tiempoAcumuladoSegundos)

                                val map: MutableMap<String, Any> = HashMap()
                                map["Id"] = IdTurnoE
                                map["Atraccion"] = "Mano Del Artesano"
                                map["TurnoAsignado"] = turnoFormateado
                                map["NumeroPersonas"] = ContadorPersonas.text.toString()
                                map["NumeroTelefonico"] = numeroTelefonico
                                map["Tiempo"] = actualizarTiempo(contador)
                                map["TiempoEspera"] = tiempoPreview
                                map["Estado"] = "En Espera"

                                databaseReference.setValue(tiempoFinal)

                                BDTurnosAcumulados.child(IdTurnoE).setValue(map).addOnSuccessListener {
                                    AtraccionSeleccionada.setText("Mano Del Artesano")
                                    edtxNumero.setText("")
                                    ContadorPersonas.setText("")
                                    txtTiempo.text = "Tiempo: 00:20 seg"
                                    contador = 1
                                    Toast.makeText(this, "Turno $turnoFormateado para $valorNombre creado con exito", Toast.LENGTH_SHORT).show()
                                    // 📲 Enviar WhatsApp según el tiempo
                                    // ---------------------------
                                    val valorNombre = nombreAtraccion

                                    val mensajeSintiempoEspera = "⚠️ Aviso de turno ⚠️\n\nDirígete de inmediato a la Atracción: *$valorNombre*\nEl turno *$turnoFormateado* es el siguiente en pasar\n\nGracias por visitar el Pueblito de Barro, disfruta de tu atracción"
                                    val mensajemenor10min = "⚠️ Aviso de turno ⚠️\n\nDirígete de inmediato a la Atracción: *$valorNombre*\nEl turno *$turnoFormateado* está próximo a ser llamado en *$tiempoFinal min* ⏳\n\nPodrás consultar más a detalle el estado de tu turno en el sitio web:\n*https://sergiolucas099.github.io/Mano_Artesano_Web.github.io/*"
                                    val mensajeentre11a30min = "⚠️ Aviso de turno ⚠️\n\nTu turno es el *'$turnoFormateado'*, puedes hacer un recorrido corto por el parque.\nSeras llamado en *$tiempoFinal min* ⏳, pero por favor mantente cerca de '$valorNombre', ya que podrías ser llamado en cualquier momento.\n\nPodrás consultar más a detalle el estado de tu turno en el sitio web:\n*https://sergiolucas099.github.io/Mano_Artesano_Web.github.io/*"
                                    val mensajemayor30min = "⚠️ Aviso de turno ⚠️\n\nTu turno es el *'$turnoFormateado'*, te invitamos a que conozcas todo lo que Pueblito de Barro tiene para ti mientras llega tu turno para '$valorNombre'\nSeras llamado en *$tiempoFinal min* ⏳.\n\nPodrás consultar más a detalle el estado de tu turno en el sitio web:\n*https://sergiolucas099.github.io/Mano_Artesano_Web.github.io/*"

                                    val mensajeEnviar = when {
                                        // Rango 1: exactamente 0
                                        tiempoAcumuladoSegundos == 0 -> mensajeSintiempoEspera

                                        // Rango 2: menor a 10 min, excluyendo cero
                                        tiempoAcumuladoSegundos in 1..599 -> mensajemenor10min

                                        // Rango 3: entre 11 y 30 min (660s = 11:00)
                                        tiempoAcumuladoSegundos in 660..1800 -> mensajeentre11a30min

                                        // Rango 4: mayor a 30 min
                                        else -> mensajemayor30min
                                    }


                                    val numeroTelefonicoConPrefijo = if (numeroTelefonico.startsWith("+57")) {
                                        numeroTelefonico
                                    } else {
                                        "+57$numeroTelefonico"
                                    }

                                    try {
                                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$numeroTelefonicoConPrefijo&text=${Uri.encode(mensajeEnviar)}")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        intent.setPackage("com.whatsapp") // abre directamente WhatsApp
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(this, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
                                    }
                                    // Quitar el foco del EditText
                                    edtxNumero.clearFocus()

                                    // Ocultar el teclado
                                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.hideSoftInputFromWindow(edtxNumero.windowToken, 0)

                                }.addOnFailureListener {
                                    Toast.makeText(this, "Error al crear el turno", Toast.LENGTH_SHORT).show()
                                }

                                // Guardar nuevo turno en Firebase
                                atraccionRef.child("Turno").setValue(nuevoTurno)
                            }.create()

                        dialog.show()

                    } else {
                        // Si la atracción no tiene campo UltimoTurno aún, se crea con 1
                        atraccionRef.child("Turno").setValue(1)
                    }
                }
            }
        }

    }

    fun actualizarTiempo(contador: Int): String {
        val tiempoTotal = contador * 20 // segundos totales
        val minutos = tiempoTotal / 60
        val segundos = tiempoTotal % 60
        return String.format("%02d:%02d", minutos, segundos) // devuelve en formato mm:ss
    }

    fun mostrarTiempo(contador: Int): String {
        val tiempoTotal = contador * 20
        return if (tiempoTotal < 60) {
            "${actualizarTiempo(contador)} seg"
        } else {
            "${actualizarTiempo(contador)} min"
        }
    }

    // convierte mm:ss a segundos
    private fun convertirATiempoSegundos(tiempo: String): Int {
        val partes = tiempo.split(":")
        return if (partes.size == 2) {
            val minutos = partes[0].toIntOrNull() ?: 0
            val segundos = partes[1].toIntOrNull() ?: 0
            (minutos * 60) + segundos
        } else {
            0
        }
    }

    // convierte segundos a formato mm:ss
    private fun formatearTiempo(segundosTotales: Int): String {
        val minutos = segundosTotales / 60
        val segundos = segundosTotales % 60
        return String.format("%02d:%02d", minutos, segundos)
    }
}