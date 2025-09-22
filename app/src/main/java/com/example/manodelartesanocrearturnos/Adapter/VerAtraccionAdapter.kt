package com.example.manodelartesanocrearturnos.Adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.manodelartesanocrearturnos.Model.AtraccionModel
import com.example.manodelartesanocrearturnos.R
import com.google.firebase.database.FirebaseDatabase

class VerAtraccionAdapter (
    private var atraccionLista: List<AtraccionModel>,
    private val onItemClick: (AtraccionModel) -> Unit
) : RecyclerView.Adapter<VerAtraccionAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.ver_atraccion_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val Latraccion = atraccionLista[position]

        holder.nombreAtraccion.text = Latraccion.Nombre

        holder.ContenidoAtraccion.setOnClickListener {
            onItemClick(Latraccion)
        }
    }

    override fun getItemCount(): Int {
        return atraccionLista.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreAtraccion: TextView = itemView.findViewById(R.id.nombreAtraccion)
        val ContenidoAtraccion: ConstraintLayout = itemView.findViewById(R.id.ContenidoAtraccion)
    }
}