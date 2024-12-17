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
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.Glide

class HomeFragment : Fragment() {

    // UI Components
    private lateinit var dateTextView: TextView
    private lateinit var searchDropdown: Spinner
    private lateinit var locationTextView: TextView
    private lateinit var ndviTextView: TextView
    private lateinit var carbonStockTextView: TextView
    private lateinit var cityEditText: EditText
    private lateinit var tiffButton: Button
    private lateinit var csvButton: Button
    private lateinit var submitButton: Button
    private lateinit var ndviImageView: ImageView
    private lateinit var carbonImageView: ImageView

    // Firebase Instances
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    // Data Lists
    private val citiesList = mutableListOf<String>()
    private val averageNDVI = mutableListOf<Double>()
    private val averageCarbonStock = mutableListOf<Double>()
    private val urlFinalPngList = mutableListOf<String>()
    private val urlNdviPngList = mutableListOf<String>()

    // URIs for Selected Files
    private var tiffUri: Uri? = null
    private var csvUri: Uri? = null

    // Request Codes for File Picker
    private val TIFF_REQUEST_CODE = 1
    private val CSV_REQUEST_CODE = 2
    private val FILE_REQUEST_CODE = 3

    // Progress Dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize UI components
        dateTextView = view.findViewById(R.id.dateTextView)
        searchDropdown = view.findViewById(R.id.searchDropdown)
        locationTextView = view.findViewById(R.id.locationTextView)
        ndviTextView = view.findViewById(R.id.ndviTextView)
        carbonStockTextView = view.findViewById(R.id.carbonStockTextView)
        cityEditText = view.findViewById(R.id.cityEditText)
        tiffButton = view.findViewById(R.id.tiffButton)
        csvButton = view.findViewById(R.id.csvButton)
        submitButton = view.findViewById(R.id.submitButton)
        ndviImageView = view.findViewById(R.id.ndviImageView)
        carbonImageView = view.findViewById(R.id.carbonImageView)

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Uploading files, please wait...")
            setCancelable(false)
        }

        // Set the current date
        setDynamicDate()

        // Fetch city data from Firestore
        fetchCitiesData()

        // Set up button click listeners
        tiffButton.setOnClickListener {
            openFilePicker("image/tiff")
        }

        csvButton.setOnClickListener {
            openFilePicker("text/csv")
        }

        submitButton.setOnClickListener {
            submitAllData() // New Function to Handle All Steps
        }

        return view
    }

    /**
     * Sets the current date in the dateTextView
     */
    private fun setDynamicDate() {
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate)
        dateTextView.text = formattedDate
    }

    /**
     * Fetches cities data from Firestore and populates the spinner
     */
    private fun fetchCitiesData() {
        firestore.collection("data").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val cityName = document.id
                    val ndvi = document.getDouble("average_ndvi") ?: 0.0
                    val carbonStock = document.getDouble("average_carbon_stock") ?: 0.0
                    val urlFinalPng = document.getString("url_final_png") ?: ""
                    val urlNdviPng = document.getString("url_ndvi_png") ?: ""

                    citiesList.add(cityName)
                    averageNDVI.add(ndvi)
                    averageCarbonStock.add(carbonStock)
                    urlFinalPngList.add(urlFinalPng)
                    urlNdviPngList.add(urlNdviPng)
                }
                setUpSpinner()
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                Toast.makeText(requireContext(), "Failed to fetch cities data", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Sets up the spinner with fetched city data
     */
    private fun setUpSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, citiesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        searchDropdown.adapter = adapter

        searchDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedCity = citiesList[position]
                val ndvi = averageNDVI[position]
                val carbonStock = averageCarbonStock[position]
                val urlFinalPng = urlFinalPngList[position]
                val urlNdviPng = urlNdviPngList[position]

                locationTextView.text = selectedCity
                ndviTextView.text = String.format(" %.2f", ndvi)
                carbonStockTextView.text = String.format(" %.2f", carbonStock)

                // Load images using Glide
                Glide.with(requireContext())
                    .load(urlFinalPng)
                    .into(ndviImageView)

                Glide.with(requireContext())
                    .load(urlNdviPng)
                    .into(carbonImageView)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                locationTextView.text = ""
                ndviTextView.text = ""
                carbonStockTextView.text = ""
            }
        }
    }

    /**
      Opens the file picker based on the MIME type
     */
    private fun openFilePicker(mimeType: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(
            Intent.createChooser(intent, "Select File"),
            if (mimeType == "image/tiff") TIFF_REQUEST_CODE else CSV_REQUEST_CODE
        )
    }

    /**
     * Handles the result from file picker
     */
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

    /**
     * New Function: Orchestrates the entire submission process
     * 1. Uploads files to Firebase Storage
     * 2. Posts to /process endpoint
     * 3. Posts to /convert_to_png endpoint
     */
    private fun submitAllData() {
        val cityName = cityEditText.text.toString().trim()
        if (cityName.isEmpty() || tiffUri == null || csvUri == null) {
            Toast.makeText(requireContext(), "Please fill all fields and upload files", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()

        // Determine file names
        val tiffFileName = if (tiffUri?.toString()?.endsWith(".tif") == true) {
            "$cityName.tif"
        } else {
            "$cityName.tiff"
        }
        val csvFileName = "$cityName.csv"

        val tiffRef = storage.reference.child("RawFiles/$tiffFileName")
        val csvRef = storage.reference.child("RawFiles/$csvFileName")

        // Step 1: Upload TIFF File
        uploadFileWithProgress(tiffRef, tiffUri!!, "TIFF") { tiffUploaded ->
            if (tiffUploaded) {
                // Step 2: Upload CSV File
                uploadFileWithProgress(csvRef, csvUri!!, "CSV") { csvUploaded ->
                    if (csvUploaded) {
                        // Step 3: POST to /process
                        postToProcessApi(cityName, tiffFileName, csvFileName) { processSuccess ->
                            if (processSuccess) {
                                // Step 4: POST to /convert_to_png
                                postToConvertToPngApi(cityName) { convertSuccess ->
                                    if (convertSuccess) {
                                        progressDialog.dismiss()
                                        Toast.makeText(requireContext(), "Data submitted and converted to PNG successfully", Toast.LENGTH_LONG).show()
                                        resetInputs()
                                    } else {
                                        progressDialog.dismiss()
                                        Toast.makeText(requireContext(), "Failed to convert to PNG", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                progressDialog.dismiss()
                                Toast.makeText(requireContext(), "Failed to process data", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(), "Failed to upload CSV file", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Failed to upload TIFF file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Uploads TIFF and CSV files to Firebase Storage with callbacks
     */
    private fun uploadFilesToStorage(cityName: String, tiffFileName: String, csvFileName: String, callback: (Boolean) -> Unit) {
        val tiffRef = storage.reference.child("RawFiles/$tiffFileName")
        val csvRef = storage.reference.child("RawFiles/$csvFileName")

        // Upload TIFF File
        uploadFileWithProgress(tiffRef, tiffUri!!, "TIFF") { tiffUploaded ->
            if (tiffUploaded) {
                // Upload CSV File
                uploadFileWithProgress(csvRef, csvUri!!, "CSV") { csvUploaded ->
                    if (csvUploaded) {
                        callback(true)
                    } else {
                        callback(false)
                    }
                }
            } else {
                callback(false)
            }
        }
    }

    /**
     * Uploads a single file to Firebase Storage with progress updates
     * @param ref StorageReference where the file will be uploaded
     * @param fileUri Uri of the file to be uploaded
     * @param fileType Type of the file (e.g., "TIFF", "CSV")
     * @param callback Callback indicating success or failure
     */
    private fun uploadFileWithProgress(ref: StorageReference, fileUri: Uri, fileType: String, callback: (Boolean) -> Unit) {
        val uploadTask = ref.putFile(fileUri)

        uploadTask.addOnProgressListener { taskSnapshot ->
            // Calculate upload progress
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
            progressDialog.setMessage("Uploading $fileType file: ${progress.toInt()}%")
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "$fileType file uploaded successfully", Toast.LENGTH_SHORT).show()
            callback(true)
        }.addOnFailureListener { exception ->
            progressDialog.dismiss()
            Toast.makeText(requireContext(), "Error uploading $fileType: ${exception.message}", Toast.LENGTH_SHORT).show()
            callback(false)
        }
    }

    /**
     * Posts data to the /process API endpoint
     * @param cityName Name of the city
     * @param tiffFileName Name of the uploaded TIFF file
     * @param csvFileName Name of the uploaded CSV file
     * @param callback Callback indicating success or failure
     */
    private fun postToProcessApi(cityName: String, tiffFileName: String, csvFileName: String, callback: (Boolean) -> Unit) {
        // OkHttpClient dengan timeout diperpanjang
        val client = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // Timeout koneksi
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)    // Timeout membaca respons
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)   // Timeout menulis permintaan
            .build()

        val jsonObject = JSONObject().apply {
            put("tif_filename", tiffFileName)
            put("csv_filename", csvFileName)
            put("city_name", cityName)
        }

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
                    callback(false)
                    Log.e("POST_PROCESS_API", "Failure: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                requireActivity().runOnUiThread {
                    if (response.isSuccessful) {
                        callback(true)
                        Toast.makeText(requireContext(), "Data processed successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        callback(false)
                        Toast.makeText(requireContext(), "Process API Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        Log.e("POST_PROCESS_API", "Error: ${response.message}")
                    }
                }
            }
        })
    }
    /**
     * Posts data to the /convert_to_png API endpoint
     * @param cityName Name of the city
     * @param callback Callback indicating success or failure
     */
    private fun postToConvertToPngApi(cityName: String, callback: (Boolean) -> Unit) {
        val client = OkHttpClient()
        val jsonObject = JSONObject().apply {
            put("city_name", cityName)
        }

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            jsonObject.toString()
        )

        val request = Request.Builder()
            .url("https://capstone-c242-ps410.et.r.appspot.com/convert_to_png")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    callback(false)
                    Log.e("POST_CONVERT_API", "Failure: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                requireActivity().runOnUiThread {
                    if (response.isSuccessful) {
                        callback(true)
                        Toast.makeText(requireContext(), "Converted to PNG successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        callback(false)
                        Toast.makeText(requireContext(), "Convert to PNG API Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        Log.e("POST_CONVERT_API", "Error: ${response.message}")
                    }
                }
            }
        })
    }

    /**
     * Resets the input fields and buttons after successful submission
     */
    private fun resetInputs() {
        cityEditText.text.clear()
        tiffUri = null
        csvUri = null
        tiffButton.text = "Upload TIFF"
        csvButton.text = "Upload CSV"
    }
}
