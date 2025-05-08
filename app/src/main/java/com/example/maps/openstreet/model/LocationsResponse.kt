package com.example.maps.openstreet.model

import com.google.gson.annotations.SerializedName

data class LocationsResponse(
    @SerializedName("sucursales" ) var sucursales: List<SucursalesResponse> = arrayListOf()
)