package com.capstone.ecosense

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var dateTextView: TextView
    private lateinit var searchDropdown: Spinner
    private lateinit var locationTextView: TextView
    private lateinit var ndviTextView: TextView
    private lateinit var carbonStockTextView: TextView
    private lateinit var cityEditText: EditText
    private lateinit var tiffButton: Button
    private lateinit var csvButton: Button
    private lateinit var submitButton: Button

    private val firestore: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    private val citiesList = mutableListOf<String>()
    private val averageNDVI = mutableListOf<Double>()
    private val averageCarbonStock = mutableListOf<Double>()

    private var tiffUri: Uri? = null
    private var csvUri: Uri? = null

    private val TIFF_REQUEST_CODE = 1
    private val CSV_REQUEST_CODE = 2

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        dateTextView = view.findViewById(R.id.dateTextView)
        searchDropdown = view.findViewById(R.id.searchDropdown)
        locationTextView = view.findViewById(R.id.locationTextView)
        ndviTextView = view.findViewById(R.id.ndviTextView)
        carbonStockTextView = view.findViewById(R.id.carbonStockTextView)
        cityEditText = view.findViewById(R.id.cityEditText)
        tiffButton = view.findViewById(R.id.tiffButton)
        csvButton = view.findViewById(R.id.csvButton)
        submitButton = view.findViewById(R.id.submitButton)

        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Uploading files, please wait...")
            setCancelable(false)
        }

        setDynamicDate()
        fetchCitiesData()

        tiffButton.setOnClickListener {
            openFilePicker("image/tiff")
        }

        csvButton.setOnClickListener {
            openFilePicker("text/csv")
        }

        submitButton.setOnClickListener {
            uploadFilesToStorage()
        }

        return view
    }

    private fun setDynamicDate() {
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate)
        dateTextView.text = formattedDate
    }

    private fun fetchCitiesData() {
        firestore.collection("data").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val cityName = document.id
                    val ndvi = document.getDouble("average_ndvi") ?: 0.0
                    val carbonStock = document.getDouble("average_carbon_stock") ?: 0.0

                    citiesList.add(cityName)
                    averageNDVI.add(ndvi)
                    averageCarbonStock.add(carbonStock)
                }
                setUpSpinner()
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }

    private fun setUpSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, citiesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        searchDropdown.adapter = adapter

        searchDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedCity = citiesList[position]
                val ndvi = averageNDVI[position]
                val carbonStock = averageCarbonStock[position]

                locationTextView.text = selectedCity
                ndviTextView.text = String.format(" %.2f", ndvi)
                carbonStockTextView.text = String.format(" %.2f", carbonStock)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                locationTextView.text = ""
                ndviTextView.text = ""
                carbonStockTextView.text = ""
            }
        }
    }

    private fun openFilePicker(mimeType: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = mimeType
        startActivityForResult(intent, if (mimeType == "image/tiff") TIFF_REQUEST_CODE else CSV_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            when (requestCode) {
                TIFF_REQUEST_CODE -> {
                    tiffUri = uri
                    tiffButton.text = "TIFF File Selected"
                }
                CSV_REQUEST_CODE -> {
                    csvUri = uri
                    csvButton.text = "CSV File Selected"
                }
            }
        }
    }

    private fun uploadFilesToStorage() {
        val cityName = cityEditText.text.toString().trim()
        if (cityName.isEmpty() || tiffUri == null || csvUri == null) {
            Toast.makeText(requireContext(), "Please fill all fields and upload files", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()

        val tiffRef = storage.reference.child("RawFiles/$cityName.tiff")
        val csvRef = storage.reference.child("RawFiles/$cityName.csv")

        var tiffUploaded = false
        var csvUploaded = false

        tiffUri?.let { uri ->
            tiffRef.putFile(uri)
                .addOnSuccessListener {
                    tiffUploaded = true
                    checkAndPostToApi(cityName, tiffRef.name, csvRef.name, tiffUploaded, csvUploaded)
                }
                .addOnFailureListener { exception ->
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Error uploading TIFF: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        csvUri?.let { uri ->
            csvRef.putFile(uri)
                .addOnSuccessListener {
                    csvUploaded = true
                    checkAndPostToApi(cityName, tiffRef.name, csvRef.name, tiffUploaded, csvUploaded)
                }
                .addOnFailureListener { exception ->
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Error uploading CSV: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkAndPostToApi(cityName: String, tiffFileName: String, csvFileName: String, tiffUploaded: Boolean, csvUploaded: Boolean) {
        if (tiffUploaded && csvUploaded) {
            progressDialog.dismiss()

            val client = OkHttpClient()
            val jsonObject = JSONObject()
            jsonObject.put("tif_filename", tiffFileName)
            jsonObject.put("csv_filename", csvFileName)
            jsonObject.put("city_name", cityName)

            val requestBody = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                jsonObject.toString()
            )

            val request = Request.Builder()
                .url("https://capstone-c242-ps410.et.r.appspot.com/process")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    requireActivity().runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Data submitted successfully", Toast.LENGTH_SHORT).show()
                            resetInputs()
                        } else {
                            Toast.makeText(requireContext(), "API Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }

    private fun resetInputs() {
        cityEditText.text.clear()
        tiffUri = null
        csvUri = null
        tiffButton.text = "Upload TIFF"
        csvButton.text = "Upload CSV"
    }
}
