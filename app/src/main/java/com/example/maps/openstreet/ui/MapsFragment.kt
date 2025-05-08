package com.example.maps.openstreet.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.maps.R
import com.example.maps.databinding.FragmentMapsBinding
import com.example.maps.openstreet.main.CustomInfoWindow
import com.example.maps.openstreet.model.SucursalesResponse
import com.example.maps.openstreet.utils.CheckServicesGH
import com.example.maps.openstreet.utils.Location
import com.example.maps.openstreet.utils.Location.Companion.LOCATION_PERMISSION_REQUEST_CODE
import com.example.maps.openstreet.vm.MapsOpenViewViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class MapsFragment : Fragment() {

    private lateinit var binding: FragmentMapsBinding
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: MapsOpenViewViewModel

    companion object {
        fun newInstance() = MapsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (CheckServicesGH().hasGooglePlayServices(requireContext())) {
            Log.e("ServiciosGoogle", "Este dispositivo tiene servicios de Google.")
        } else {
            Log.e("ServiciosGoogle", "Este dispositivo NO tiene servicios de Google.")
        }

        viewModel = ViewModelProvider(this)[MapsOpenViewViewModel::class.java]

        mapView = binding.map
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            obtenerUbicacionActual()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionActual()
            } else {
                Log.e("PermisoUbicacion", "Permiso denegado.")
            }
        }
    }


    private fun obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        Location(requireContext()).obtenerUbicacion { latitud, longitud ->
            val currentPoint = GeoPoint(latitud, longitud)
            val mapController = mapView.controller
            mapController.setZoom(16.0)
            mapController.setCenter(currentPoint)

            val marker = Marker(mapView)
            marker.position = currentPoint
            marker.title = "Ubicación actual"
            val icon = resizeDrawable(requireContext(), R.drawable.baseline_home_24, 36f, 36f)
            marker.icon = BitmapDrawable(resources, icon)
            marker.snippet = "Lat: $latitud, Lng: $longitud"
            mapView.overlays.add(marker)

            viewModel.cargarSucursales(latitud, longitud)

            viewModel.sucursales.observe(viewLifecycleOwner) { response ->
                response?.let {
                    agregarMarcadoresSucursales(latitud, longitud, it.sucursales)
                }
            }

            mapView.invalidate()
        }
    }

    private fun agregarMarcadoresSucursales(
        lat: Double,
        lng: Double,
        sucursales: List<SucursalesResponse>
    ) {
        val ubicacionesCercanas = sucursales.sortedBy {
            it.latitud?.let { it1 -> it.longitud?.let { it2 ->
                calcularDistanciaKm(lat, lng, it1, it2)
            } }
        }.take(100)
        for (sucursal in ubicacionesCercanas) {
            val marker = Marker(mapView)
            marker.position = GeoPoint(sucursal.latitud!!, sucursal.longitud!!)
            marker.title = sucursal.titulo

            marker.snippet = buildString {
                append(sucursal.subtitulo)
                if (sucursal.cardSize!! >= 1) {
                    append("\nCuenta con tarjetas: Si\nCantidad de tarjetas: ${sucursal.cardSize}\nHorario: 7:00 AM - 11:00 PM")
                } else {
                    append("\nCuenta con tarjetas: No\nCantidad de tarjetas: ${sucursal.cardSize}\nHorario: 7:00 AM - 11:00 PM")
                }
            }

            val iconBitmap = if (sucursal.cardSize!! >= 1) {
                resizeDrawable(requireContext(), R.drawable.farmacias_logo, 36f, 36f)
            } else {
                resizeDrawable(requireContext(), R.drawable.farmacias_logo_gray, 36f, 36f)
            }
            marker.icon = BitmapDrawable(resources, iconBitmap)

            marker.infoWindow = CustomInfoWindow(mapView, requireContext())

            mapView.overlays.add(marker)

            marker.setOnMarkerClickListener { _, _ ->
                marker.showInfoWindow()

                val targetZoomLevel = 19.0
                val currentZoom = mapView.zoomLevelDouble
                val zoomDifference = targetZoomLevel - currentZoom
                val zoomSteps = 150
                val zoomStep = zoomDifference / zoomSteps

                val handler = Handler(Looper.getMainLooper())

                mapView.controller.animateTo(marker.position)

                handler.postDelayed({
                    for (i in 1..zoomSteps) {
                        val zoomRunnable = Runnable {
                            val newZoom = currentZoom + (zoomStep * i)
                            mapView.controller.setZoom(newZoom)
                        }
                        handler.postDelayed(zoomRunnable, (i * 12).toLong())
                    }
                }, 10)

                true
            }
        }

        mapView.invalidate()
    }

    private fun calcularDistanciaKm(
        lat1: Double, lng1: Double, lat2: Double, lng2: Double
    ): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun resizeDrawable(
        context: Context, drawableResId: Int, widthDp: Float, heightDp: Float
    ): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableResId)!!
        val metrics = context.resources.displayMetrics
        val widthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp, metrics).toInt()
        val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, metrics).toInt()
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
