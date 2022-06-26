package com.example.whatsappcloneniranjan

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.example.whatsappcloneniranjan.databinding.ActivityLogInBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LogInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogInBinding

    private lateinit var phoneNumber: String
    private lateinit var countryCode: String
    private lateinit var alertDialogBuilder: MaterialAlertDialogBuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

       binding.phoneNumberEt.addTextChangedListener {
           binding.nextBtn.isEnabled = !(it.isNullOrEmpty())
        }

        //Add Hint Request - Assignment
        binding.nextBtn.setOnClickListener {
            checkNumber()
        }
    }

    private fun checkNumber() {
        countryCode = binding.ccp.selectedCountryCodeWithPlus
        phoneNumber = countryCode + binding.phoneNumberEt.text.toString()

        if (validatePhoneNumber(binding.phoneNumberEt.text.toString())) {
            notifyUserBeforeVerify(
                "We will be verifying the phone number:$phoneNumber\n" +
                        "Is this OK, or would you like to edit the number?"
            )
        } else {
            Toast.makeText(this, "Please enter a valid number to continue!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validatePhoneNumber(phone: String): Boolean {
        if (phone.isEmpty()) {
            return false
        }
        return true
    }

    private fun notifyUserBeforeVerify(message: String) {
        alertDialogBuilder = MaterialAlertDialogBuilder(this).apply {
            setMessage(message)
            setPositiveButton("Ok") { _, _ ->
                showLoginActivity()
            }

            setNegativeButton("Edit") { dialog, _ ->
                dialog.dismiss()
            }

            setCancelable(false)
            create()
            show()
        }
    }

    private fun showLoginActivity() {
        startActivity(
            Intent(this, OtpActivity::class.java).putExtra(PHONE_NUMBER, phoneNumber)
        )
        finish()
    }
}