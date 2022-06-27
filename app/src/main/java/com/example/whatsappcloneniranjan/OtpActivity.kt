package com.example.whatsappcloneniranjan

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.whatsappcloneniranjan.databinding.ActivityOtpBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

const val PHONE_NUMBER = "phoneNumber"

class OtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpBinding

    private lateinit var mAuth: FirebaseAuth

    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var phoneNumber: String? = null
    private var mVerificationId: String? = null
    private var mResendToken: PhoneAuthProvider.ForceResendingToken? = null
    private lateinit var progressDialog: ProgressDialog
    private var mCounterDown: CountDownTimer? = null
    private var timeLeft: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        initView()
        startVerify()
    }

    private fun startVerify() {
        startPhoneNumberVerification(phoneNumber!!)
        showTimer(60000)
        progressDialog = createProgressDialog("Sending a verification code", false)
        progressDialog.show()
    }

    private fun initView() {
        phoneNumber = intent.getStringExtra(PHONE_NUMBER)
        binding.verifyTv.text = getString(R.string.verify_number, phoneNumber)
        setSpannableString()

        // init click listener
        binding.verificationBtn.setOnClickListener {
            // try to enter the code by yourself to handle the case
            // if user enter another sim card used in another phone ...
            var code = binding.sentcodeEt.text.toString()
            if (code.isNotEmpty() && !mVerificationId.isNullOrEmpty()) {

                progressDialog = createProgressDialog("Please wait...", false)
                progressDialog.show()
                val credential =
                    PhoneAuthProvider.getCredential(mVerificationId!!, code.toString())
                signInWithPhoneAuthCredential(credential)
            }
        }
        binding.resendBtn.setOnClickListener{
            if (mResendToken != null) {
                resendVerificationCode(phoneNumber.toString(), mResendToken)
                showTimer(60000)
                progressDialog = createProgressDialog("Sending a verification code", false)
                progressDialog.show()
            } else {
                Toast.makeText(
                    this,
                    "Sorry, You Can't request new code now, Please wait ...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // init fire base verify Phone number callback
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                if (::progressDialog.isInitialized) {
                    progressDialog.dismiss()
                }

                val smsMessageSent = credential.smsCode
                if (!smsMessageSent.isNullOrBlank())
                    binding.sentcodeEt.setText(smsMessageSent)

                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }

                // Show a message and update the UI
                notifyUserAndRetry("Your Phone Number might be wrong or connection error.Retry again!")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                progressDialog.dismiss()
                binding.counterTv.isVisible = false
                // Save verification ID and resending token so we can use them later
                Log.e("onCodeSent==", "onCodeSent:$verificationId")
                mVerificationId = verificationId
                mResendToken = token
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    progressDialog.dismiss()
                    Toast.makeText(this, "Log In Success", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, SignUpActivity::class.java)
                    intent.putExtra(PHONE_NUMBER, phoneNumber)
                    startActivity(intent)
                    finish()
                }else {
                    notifyUserAndRetry("Phone Number Verification Failed. Try Again !!")
                }
            }
    }

    private fun notifyUserAndRetry(message: String) {
        MaterialAlertDialogBuilder(this).apply {
            setMessage(message)
            setPositiveButton("Ok") { _, _ ->
                showLogInActivity()
            }

            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            setCancelable(false)
            create()
            show()
        }
    }

    private fun setSpannableString() {
        val span = SpannableString(getString(R.string.waiting_text, phoneNumber))

        val clickSpan: ClickableSpan = object : ClickableSpan() {

            override fun updateDrawState(ds: TextPaint) {
                ds.color = ds.linkColor // you can use custom color
                ds.isUnderlineText = false // this remove the underline
            }

            override fun onClick(widget: View) {
               showLogInActivity()
            }

        }
        span.setSpan(clickSpan, span.length - 13, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.waitingTv.movementMethod = LinkMovementMethod.getInstance()
        binding.waitingTv.text = span
    }

    override fun onBackPressed() {

    }

    private fun showLogInActivity() {
        startActivity(
            Intent(this, LogInActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    private fun showTimer(milliesInFuture: Long) {
        binding.resendBtn.isEnabled = false
        mCounterDown = object : CountDownTimer(milliesInFuture, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                binding.counterTv.isVisible = true
                binding.counterTv.text = "Seconds remaining: " + millisUntilFinished / 1000

                //here you can have your logic to set text to edittext
            }

            override fun onFinish() {
                binding.resendBtn.isEnabled = true
                binding.counterTv.isVisible = false
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mCounterDown != null) {
            mCounterDown!!.cancel()
        }
    }


    // This method will send a code to a given phone number as an SMS
    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendVerificationCode(
        phoneNumber: String,
        mResendToken: PhoneAuthProvider.ForceResendingToken?
    ) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber, // Phone number to verify
            60, // Timeout duration
            TimeUnit.SECONDS, // Unit of timeout
            this, // Activity (for callback binding)
            callbacks, // OnVerificationStateChangedCallbacks
            mResendToken
        ) // ForceResendingToken from callbacks
    }
}

fun Context.createProgressDialog(message: String, isCancelable: Boolean): ProgressDialog {
    return ProgressDialog(this).apply {
        setCancelable(isCancelable)
        setCanceledOnTouchOutside(false)
        setMessage(message)
    }
}