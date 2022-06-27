package com.example.whatsappcloneniranjan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.whatsappcloneniranjan.databinding.ActivitySignUpBinding
import com.example.whatsappcloneniranjan.models.User
import com.github.drjacky.imagepicker.ImagePicker
import com.github.drjacky.imagepicker.constant.ImageProvider
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    private val storage by lazy {
        FirebaseStorage.getInstance()
    }
    private val mAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val database by lazy {
        FirebaseFirestore.getInstance()
    }
    private lateinit var downloadUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.userImgView.setOnClickListener {
            pickImageFromGallery()  // image cropper library used automatically checks for storage and camera permissions. no need to add it manually
        }

        binding.nextBtn.setOnClickListener{
            binding.nextBtn.setOnClickListener {
                val name = binding.nameEt.text.toString()
                if (!::downloadUrl.isInitialized) {
                    Toast.makeText(this, "Photo cannot be empty", Toast.LENGTH_SHORT).show()
                } else if (name.isEmpty()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    val user = User(name, downloadUrl, downloadUrl/*Needs to thumbnail url*/, mAuth.uid!!)
                    database.collection("users").document(mAuth.uid!!).set(user).addOnSuccessListener {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }.addOnFailureListener {
                        binding.nextBtn.isEnabled = true
                    }
                }
            }

        }
    }

    override fun onBackPressed() {

    }

//    private fun checkPermissionForImage() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
//                && (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
//            ) {
//                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
//                val permissionWrite = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//
//                requestPermissions(
//                    permission,
//                    1001
//                ) // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
//                requestPermissions(
//                    permissionWrite,
//                    1002
//                ) // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_WRITE LIKE 1002
//            } else {
//                pickImageFromGallery()
//            }
//        }
//    }

    private fun pickImageFromGallery() {
        ImagePicker.with(this)
            .provider(ImageProvider.BOTH) //Or bothCameraGallery
            .crop()
            .maxResultSize(1080, 1080)
            .createIntentFromDialog {
                resultLauncher.launch(it)
            }
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data!!
                // Use the uri to load the image
                binding.userImgView.setImageURI(uri)
                startUpload(uri)
            }
        }

    private fun startUpload(filePath: Uri) {
        binding.nextBtn.isEnabled = false
        val ref = storage.reference.child("uploads/" + mAuth.uid.toString())
        val uploadTask = ref.putFile(filePath)
        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                downloadUrl = task.result.toString()
                binding.nextBtn.isEnabled = true
            } else {
                binding.nextBtn.isEnabled = true
                // Handle failures
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Image uploading failed. Try Again!!", Toast.LENGTH_SHORT).show()
        }
    }
}