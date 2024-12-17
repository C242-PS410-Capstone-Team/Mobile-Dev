package com.capstone.ecosense

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class FullscreenImageActivity : AppCompatActivity() {
    companion object {
        fun startWithUrl(context: Context, imageUrl: String) {
            val intent = Intent(context, FullscreenImageActivity::class.java)
            intent.putExtra("image_url", imageUrl)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)

        val imageView = findViewById<ImageView>(R.id.fullscreenImageView)
        val imageUrl = intent.getStringExtra("image_url")

        Glide.with(this).load(imageUrl).into(imageView)

        imageView.setOnClickListener {
            finish()
        }
    }
}
