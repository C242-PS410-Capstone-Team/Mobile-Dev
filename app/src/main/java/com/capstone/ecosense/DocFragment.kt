package com.capstone.ecosense

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DocFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doc, container, false)

        val cautionInfo = view.findViewById<TextView>(R.id.caution_info)

        cautionInfo.setOnClickListener {
            val url = "https://wahyu-ramadhan.medium.com/normalized-difference-vegetation-index-b619f1855a9"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }

        return view
    }
}
