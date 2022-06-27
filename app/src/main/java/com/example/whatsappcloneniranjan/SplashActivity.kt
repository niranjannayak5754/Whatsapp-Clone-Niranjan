package com.example.whatsappcloneniranjan

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val mAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(mAuth.currentUser == null){
            startActivity(Intent(this,LogInActivity::class.java))
        }else{
            startActivity(Intent(this,MainActivity::class.java))
        }
        finish()
    }
}