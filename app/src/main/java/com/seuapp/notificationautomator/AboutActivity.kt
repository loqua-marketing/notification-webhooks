package com.seuapp.notificationautomator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        
        val tvDeepSeek = findViewById<TextView>(R.id.tvDeepSeek)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnDeepSeek = findViewById<Button>(R.id.btnDeepSeek)
        
        tvDeepSeek.text = "DeepSeek Rulez! 🤖"
        
        btnBack.setOnClickListener {
            finish()
        }
        
        btnDeepSeek.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://deepseek.com"))
            startActivity(intent)
        }
    }
}
