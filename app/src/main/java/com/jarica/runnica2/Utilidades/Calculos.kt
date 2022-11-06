package com.jarica.runnica2.Utilidades

import kotlin.math.roundToInt

fun getTimeStringFromDoblue(time: Double): String {

    val resultadoEntero = time.roundToInt()
    val hours = resultadoEntero % 86400 / 3600
    val minutes = resultadoEntero % 86400 % 3600 / 60
    val seconds = resultadoEntero % 86400 % 3600 % 60

    return makeTimeString(hours, minutes, seconds)
}

fun makeTimeString(hours: Int, minutes: Int, seconds: Int): String =
    String.format("%02d:%02d:%02d", hours, minutes, seconds)


//Metodo que redondea los datos calculados
fun redondeaNumeros(dato: String, decimales: Int): String {
    var d: String = dato
    var p = d.indexOf(".", 0)

    if (p != null) {
        var limite: Int = p + decimales + 1
        if (d.length <= p + decimales + 1) limite = d.length //-1
        d = d.subSequence(0, limite).toString()
    }

    return d
}