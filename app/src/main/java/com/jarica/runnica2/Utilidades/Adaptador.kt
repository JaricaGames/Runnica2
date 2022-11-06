package com.jarica.runnica2.Utilidades

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jarica.runnica2.Dataclasses.Actividades
import com.jarica.runnica2.Dataclasses.Carrera
import com.jarica.runnica2.R
import java.util.*

class Adaptador(arrayListActividades2: ArrayList<Actividades>) :
    RecyclerView.Adapter<Adaptador.ViewHolder>() {


    private var arrayListActividades = arrayListActividades2
    private lateinit var context: Context


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        context = parent.context
        var v = LayoutInflater.from(context).inflate(R.layout.card_view_historial, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val actividad: Actividades = arrayListActividades[position]

        var deporte = ""
        when (actividad.deporte) {
            "Running" -> holder.tvDeporte.text = "Correr"
            "Bike" -> holder.tvDeporte.text = "Bicicleta"
            "Walk" -> holder.tvDeporte.text = "Andar"

        }

        when (actividad.deporte) {
            "Running" -> holder.imagenDeporte.setImageResource(R.drawable.ic_run_small)
            "Bike" -> holder.imagenDeporte.setImageResource(R.drawable.ic_bike_small)
            "Walk" -> holder.imagenDeporte.setImageResource(R.drawable.ic_walk_small)
        }

        holder.tvDuracion.text = actividad.tiempoTranscurrido.toString()
        var fecha = actividad.diaMes.toString() + " de " + actividad.mes.toString()
        holder.tvfechaDiaSemana.text = actividad.diaSemana
        holder.tvFechaActividad.text = fecha

        holder.tvDistancia.text = actividad.distancia.toString() + " km"

        holder.tvRitmoMedio.text = actividad.ritmoMedio.toString() + " min/km"


    }

    override fun getItemCount(): Int {
        return arrayListActividades.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var imagenDeporte: ImageView = itemView.findViewById(R.id.ivDeporteCV)
        var tvDeporte: TextView = itemView.findViewById(R.id.tvDeporteCV)
        var tvFechaActividad: TextView = itemView.findViewById(R.id.tvFechaCV)
        var tvfechaDiaSemana: TextView = itemView.findViewById(R.id.tvFechaDiaSemanaCV)
        var tvDuracion: TextView = itemView.findViewById(R.id.tvDuracionCV)
        var tvDistancia: TextView = itemView.findViewById(R.id.tvDistanciaCV)
        var tvRitmoMedio: TextView = itemView.findViewById(R.id.tvRitmoMedioCV)


    }

}

