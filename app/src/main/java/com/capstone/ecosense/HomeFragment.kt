package com.capstone.ecosense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var dateTextView: TextView
    private lateinit var searchDropdown: Spinner
    private lateinit var locationTextView: TextView
    private lateinit var ndviTextView: TextView
    private lateinit var carbonStockTextView: TextView

    // Data dummy untuk average NDVI dan average carbon stock
    private val averageNDVI = arrayOf(
        0.5, // Placeholder untuk item kosong
        0.75, // Jakarta
        0.65, // Bandung
        0.70, // Surabaya
        0.60, // Medan
        0.55, // Semarang
        0.80, // Yogyakarta
        0.90, // Bali
        0.85, // Makassar
        0.78, // Palembang
        0.72  // Batam
    )

    private val averageCarbonStock = arrayOf(
        0.0, // Placeholder untuk item kosong
        120.0, // Jakarta
        110.0, // Bandung
        130.0, // Surabaya
        115.0, // Medan
        100.0, // Semarang
        140.0, // Yogyakarta
        150.0, // Bali
        145.0, // Makassar
        125.0, // Palembang
        135.0  // Batam
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout untuk fragment ini
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Inisialisasi TextView dan Spinner
        dateTextView = view.findViewById(R.id.dateTextView)
        searchDropdown = view.findViewById(R.id.searchDropdown)
        locationTextView = view.findViewById(R.id.locationTextView) // Pastikan ID ini sesuai dengan layout Anda
        ndviTextView = view.findViewById(R.id.ndviTextView) // TextView untuk average NDVI
        carbonStockTextView = view.findViewById(R.id.carbonStockTextView) // TextView untuk average carbon stock

        // Set tanggal dinamis
        setDynamicDate()

        // Mengisi data dummy ke Spinner
        setUpSpinner()

        // Set default selection ke Jakarta
        searchDropdown.setSelection(1) // 1 adalah indeks untuk Jakarta
        updateDataForSelectedCity(1) // Memperbarui data untuk Jakarta

        return view
    }

    private fun setDynamicDate() {
        // Mendapatkan tanggal saat ini
        val currentDate = Calendar.getInstance().time

        // Format tanggal sesuai kebutuhan
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate)

        // Set teks pada TextView
        dateTextView.text = formattedDate
    }

    private fun setUpSpinner() {
        // Data dummy berupa nama kota dan provinsi dengan item kosong sebagai placeholder
        val cities = arrayOf(
            "", // Item kosong sebagai placeholder
            "Jakarta, DKI Jakarta",
            "Bandung, Jawa Barat",
            "Surabaya, Jawa Timur",
            "Medan, Sumatera Utara",
            "Semarang, Jawa Tengah",
            "Yogyakarta, DI Yogyakarta",
            "Bali, Bali",
            "Makassar, Sulawesi Selatan",
            "Palembang, Sumatera Selatan",
            "Batam, Kepulauan Riau"
        )

        // Membuat ArrayAdapter untuk Spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Mengatur adapter ke Spinner
        searchDropdown.adapter = adapter

        // Menambahkan listener untuk menangani pemilihan item
        searchDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView< *>, view: View, position: Int, id: Long) {
                // Mendapatkan kota yang dipilih
                val selectedCity = parent.getItemAtPosition(position).toString()
                // Mengupdate TextView lokasi dengan kota yang dipilih jika bukan item kosong
                if (selectedCity.isNotEmpty()) {
                    locationTextView.text = selectedCity
                    // Mengupdate average NDVI dan average carbon stock berdasarkan kota yang dipilih
                    updateDataForSelectedCity(position)
                } else {
                    locationTextView.text = "" // Kosongkan TextView jika item kosong dipilih
                    ndviTextView.text = "" // Kosongkan TextView average NDVI
                    carbonStockTextView.text = "" // Kosongkan TextView average carbon stock
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Tidak ada tindakan yang diperlukan
            }
        }
    }

    private fun updateDataForSelectedCity(position: Int) {
        ndviTextView.text = " ${averageNDVI[position]}"
        carbonStockTextView.text = " ${averageCarbonStock[position]} tons"
    }
}