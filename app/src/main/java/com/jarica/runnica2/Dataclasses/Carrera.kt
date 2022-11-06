package com.jarica.runnica2.Dataclasses

import android.os.Parcel
import android.os.Parcelable

data class Carrera(

    var tiempoCarrera: Double,
    var distanciaCarrera: Double,
    var velocidadCarrera: Double,
    var ritmoMedioCarrera: Double,

) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(tiempoCarrera)
        parcel.writeDouble(distanciaCarrera)
        parcel.writeDouble(velocidadCarrera)
        parcel.writeDouble(ritmoMedioCarrera)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Carrera> {
        override fun createFromParcel(parcel: Parcel): Carrera {
            return Carrera(parcel)
        }

        override fun newArray(size: Int): Array<Carrera?> {
            return arrayOfNulls(size)
        }
    }
}



