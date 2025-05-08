package com.example.maps.openstreet.main

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import com.example.maps.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class CustomInfoWindow(mapView: MapView, private val context: Context) : InfoWindow(R.layout.bubble, mapView) {
    override fun onOpen(item: Any?) {
        val marker = item as Marker
        InfoWindow.closeAllInfoWindowsOn(mapView)
        val titleView = mView.findViewById<TextView>(R.id.info_title)
        val snippetView = mView.findViewById<TextView>(R.id.info_details)

        titleView.text = marker.title
        snippetView.text = marker.snippet

        mView.setOnClickListener {
            val geoUri = "geo:${marker.position.latitude},${marker.position.longitude}?q=${marker.position.latitude},${marker.position.longitude}(${marker.title})"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(geoUri))
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No se encontró una app de mapas instalada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onClose() {
    }
}
