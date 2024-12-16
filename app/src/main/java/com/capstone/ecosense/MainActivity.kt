package com.capstone.ecosense

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Menampilkan HomeFragment secara default saat pertama kali dibuka
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())  // Menampilkan HomeFragment secara default
        }

        // Inisialisasi BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set listener untuk BottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.maps -> {
                    replaceFragment(MapsFragment())
                    true
                }
                R.id.documentation -> {
                    replaceFragment(DocFragment())
                    true
                }
                R.id.settings -> {
                    replaceFragment(SettingFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Fungsi untuk mengganti fragment
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)  // Menambahkan ke back stack jika diperlukan
        transaction.commit()
    }
}