package com.example.maps.openstreet.model

import com.google.gson.annotations.SerializedName

data class SucursalesResponse(
    @SerializedName("latitud"   ) var latitud   : Double? = null,
    @SerializedName("longitud"  ) var longitud  : Double? = null,
    @SerializedName("titulo"    ) var titulo    : String? = null,
    @SerializedName("subtitulo" ) var subtitulo : String? = null,
    @SerializedName("cardSize " ) var cardSize  : Int? = null
)
