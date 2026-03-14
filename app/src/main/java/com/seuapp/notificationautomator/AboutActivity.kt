package com.seuapp.notificationautomator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    private val DONATION_URL = "https://www.paypal.com/donate/?hosted_button_id=B2L7ZDLNJY35Q"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.title = "Sobre"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val btnDonate = findViewById<Button>(R.id.btnDonate)
        btnDonate.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Não foi possível abrir o link de donativo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}