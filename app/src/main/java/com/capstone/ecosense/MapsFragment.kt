package com.capstone.ecosense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)

        // Mendapatkan SupportMapFragment dan meminta notifikasi ketika peta siap digunakan
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Menambahkan marker di lokasi tertentu (misalnya, Jakarta)
        val jakarta = LatLng(-6.2088, 106.8456)
        mMap.addMarker(MarkerOptions().position(jakarta).title("Marker in Jakarta"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(jakarta, 12f))  // Zoom level 12

        // Mengaktifkan kontrol zoom
        mMap.uiSettings.isZoomControlsEnabled = true
    }
}
