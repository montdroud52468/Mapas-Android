package com.example.maps.openstreet.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maps.openstreet.model.LocationsResponse
import com.example.maps.openstreet.model.SucursalesResponse
import kotlin.math.*
import kotlin.random.Random

class MapsOpenViewViewModel : ViewModel() {

    private val _sucursales = MutableLiveData<LocationsResponse>()
    val sucursales: LiveData<LocationsResponse> = _sucursales

    fun cargarSucursales(baseLat: Double, baseLng: Double) {
        val listaSucursales = List(150) {
            val ubicacion = generarUbicacionAleatoria(baseLat, baseLng, 1.0, 20.0)
            SucursalesResponse(
                latitud = ubicacion.first,
                longitud = ubicacion.second,
                titulo = "Farmacias del Ahorro Sucursal #${it + 1}",
                subtitulo = "Sucursal con horario 24 hrs",
                cardSize = Random.nextInt(0,10)
            )
        }
        _sucursales.value = LocationsResponse(listaSucursales)
    }

    private fun generarUbicacionAleatoria(
        lat: Double, lng: Double, minDistKm: Double, maxDistKm: Double
    ): Pair<Double, Double> {
        val earthRadius = 6371.0
        val distanciaKm = Random.nextDouble(minDistKm, maxDistKm)
        val angulo = Random.nextDouble(0.0, 360.0)

        val distanciaAngular = distanciaKm / earthRadius
        val anguloRad = Math.toRadians(angulo)
        val latRad = Math.toRadians(lat)
        val lngRad = Math.toRadians(lng)

        val nuevaLat = asin(
            sin(latRad) * cos(distanciaAngular) +
                    cos(latRad) * sin(distanciaAngular) * cos(anguloRad)
        )

        val nuevaLng = lngRad + atan2(
            sin(anguloRad) * sin(distanciaAngular) * cos(latRad),
            cos(distanciaAngular) - sin(latRad) * sin(nuevaLat)
        )

        return Pair(Math.toDegrees(nuevaLat), Math.toDegrees(nuevaLng))
    }
}
