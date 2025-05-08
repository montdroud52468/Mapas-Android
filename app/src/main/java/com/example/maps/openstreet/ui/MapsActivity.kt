package com.example.maps.openstreet.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.maps.R
import com.example.maps.openstreet.ui.MapsFragment

class MapsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MapsFragment.newInstance())
                .commitNow()
        }
    }
}