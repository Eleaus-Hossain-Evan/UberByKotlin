package com.example.uberbykotlin

import android.app.Activity
import android.content.Intent
import android.icu.lang.UCharacter.isDigit
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AlertDialogLayout
import com.example.uberbykotlin.model.DriverInfoModel
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.reactivex.Completable
import kotlinx.android.synthetic.main.activity_splash_screen.*
import java.lang.Character.isDigit
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {


    companion object{
        private val LOGIN_REQUEST_CODE = 7171
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS)
            .subscribe({
                firebaseAuth.addAuthStateListener(listener)
            })
    }

    override fun onStop() {
        if (firebaseAuth != null && listener != null){
            firebaseAuth.removeAuthStateListener(listener)
        }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        init()
    }

    private fun init() {
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE)

        this.providers = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        listener = FirebaseAuth.AuthStateListener {
            val user = it.currentUser
            if (user != null){
                checkUserFromFirebase()
            }
            else {
                showLoginLayout()
            }
        }
    }

    private fun checkUserFromFirebase() {
        driverInfoRef
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(applicationContext, "User already registered!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    showRegisterLayput()
                }
            })
    }

    private fun showRegisterLayput() {
        val builder = AlertDialog.Builder(applicationContext,R.style.DialogTheme)
        val itemView = LayoutInflater.from(applicationContext).inflate(R.layout.layout_register, null)

        val edtFirstName = itemView.findViewById<View>(R.id.edtFirstName) as TextInputEditText
        val edtLastName = itemView.findViewById<View>(R.id.edtLastName) as TextInputEditText
        val edtPhone = itemView.findViewById<View>(R.id.edtPhoneNumber) as TextInputEditText

        val btnContinue = itemView.findViewById<View>(R.id.btnRegister)

        //Set data
        if(FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
                !TextUtils.isDigitsOnly(firebaseAuth.currentUser!!.phoneNumber)) {
            edtPhone.setText(firebaseAuth.currentUser!!.phoneNumber)
        }

        //Set view
        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        //Event
        btnContinue.setOnClickListener{
            if(TextUtils.isDigitsOnly(edtFirstName.text.toString())){
                Toast.makeText(applicationContext, "Please enter First name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if(TextUtils.isDigitsOnly(edtLastName.text.toString())){
                Toast.makeText(applicationContext, "Please enter Last name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if(TextUtils.isDigitsOnly(edtPhone.text.toString())){
                Toast.makeText(applicationContext, "Please enter Phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                val model = DriverInfoModel()
                model.firstName = edtFirstName.text.toString().trim()
                model.lastName = edtLastName.text.toString().trim()
                model.phone = edtPhone.text.toString().trim()
                model.rating = 0.0

                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener {
                        Toast.makeText(applicationContext, ""+it.message, Toast.LENGTH_SHORT).show()
                        Log.d("__register",""+it.message)
                        dialog.dismiss()
                        progressBar.visibility = View.GONE
                    }
                    .addOnSuccessListener{
                        Toast.makeText(applicationContext, "Registered Successfully!", Toast.LENGTH_SHORT).show()
                        Log.d("__register",""+it)
                        dialog.dismiss()
                        progressBar.visibility = View.GONE
                    }
            }
        }

    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btnPhoneSignIn)
            .setGoogleButtonId(R.id.btnGoogleSignIn)
            .build()
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
        , LOGIN_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE){
            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK){
                val user = FirebaseAuth.getInstance().currentUser
            }else{
                Toast.makeText(this, ""+response!!.error!!.message, Toast.LENGTH_SHORT).show()
                Log.d("SignIn Error",""+ response.error!!.message)
            }
        }
    }
}