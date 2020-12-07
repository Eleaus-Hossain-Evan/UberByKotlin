package com.example.uberbykotlin

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.uberbykotlin.Common.currentUser
import com.example.uberbykotlin.model.DriverInfoModel
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.getInstance
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import io.reactivex.Completable
import kotlinx.android.synthetic.main.activity_splash_screen.*
import java.util.*
import java.util.concurrent.TimeUnit


class SplashScreenActivity : AppCompatActivity() {


    companion object {
        private val LOGIN_REQUEST_CODE = 7171
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference
    private lateinit var user: FirebaseUser

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
        if (firebaseAuth != null && listener != null) {
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

        providers = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )


        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            user = myFirebaseAuth.currentUser!!
            if (user != null) {
                checkUserFromFirebase()
            } else {
                showLoginLayout()
            }
        }
    }

    private fun checkUserFromFirebase() {
//        val rootRef = FirebaseDatabase.getInstance().reference
//        val userNameRef = rootRef.child(Common.DRIVER_INFO_REFERENCE)
//        val queries = userNameRef.orderByChild("uid").equalTo(user.uid)
        driverInfoRef
            .child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
//                    Toast.makeText(applicationContext, "User already registered!", Toast.LENGTH_SHORT).show()
                    Log.d("User-Registered", "user already registered" + user.uid)
                    val model = dataSnapshot.getValue(DriverInfoModel::class.java)
                    Log.i("checking", "-----------------showRegisterLayout: starting new activity")
                    goToHomeActivity(model)
                } else {
                    showRegisterLayout()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    applicationContext,
                    "-------DatabaseError: " + databaseError.message,
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        })
//        val eventListener: ValueEventListener = object : ValueEventListener
//        queries.addListenerForSingleValueEvent(eventListener)
    }

    private fun goToHomeActivity(model: DriverInfoModel?) {
        Common.currentUser = model
        startActivity(Intent(this,DriverHomeActivity::class.java))
        finish()
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this)
        lateinit var dialog: AlertDialog
        builder.setTitle("REGISTER ")
        builder.setMessage(R.string.registerText)

        val inflater = LayoutInflater.from(this)
        val itemView = inflater.inflate(R.layout.layout_register, null)
        val edtFirstName: TextInputEditText = itemView.findViewById(R.id.edtFirstName)
        val edtLastName: TextInputEditText = itemView.findViewById(R.id.edtLastName)
        val edtPhoneNumber: TextInputEditText = itemView.findViewById(R.id.edtPhoneNumber)
        val btnReg: MaterialButton = itemView.findViewById(R.id.btnRegister)

        //Set data
        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
            !TextUtils.isDigitsOnly(firebaseAuth.currentUser!!.phoneNumber)
        ) {
            edtPhoneNumber.setText(firebaseAuth.currentUser!!.phoneNumber)
        }
        //Event
        btnReg.setOnClickListener {
            if (TextUtils.isDigitsOnly(edtFirstName.text.toString())) {
                Toast.makeText(
                    applicationContext,
                    "Please enter First name",
                    Toast.LENGTH_SHORT
                ).show()

            } else if (TextUtils.isDigitsOnly(edtLastName.text.toString())) {
                Toast.makeText(applicationContext, "Please enter Last name", Toast.LENGTH_SHORT)
                    .show()

            } else if (TextUtils.isDigitsOnly(edtPhoneNumber.text.toString())) {
                Toast.makeText(
                    applicationContext,
                    "Please enter Phone number",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                dialog.dismiss()
                val model = DriverInfoModel()
                model.firstName = edtFirstName.text.toString().trim()
                model.lastName = edtLastName.text.toString().trim()
                model.phone = edtPhoneNumber.text.toString().trim()
                model.rating = 0.0

                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener {
                        Toast.makeText(applicationContext, "" + it.message, Toast.LENGTH_SHORT).show()
                        Log.d("__register", "" + it.message)
                        dialog.dismiss()
                        progressBar.visibility = View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(applicationContext, "Registered Successfully!", Toast.LENGTH_SHORT).show()
                        Log.d("__register", "" + it)
                        Log.i("checking", "-----------------showRegisterLayout: dialog screen dismissed")
                        dialog.dismiss()
                        Log.i("checking", "-----------------showRegisterLayout: starting new activity")
                        goToHomeActivity(model)

                        progressBar.visibility = View.GONE
                    }
            }
        }
        //Set view
        builder.setView(itemView)
        dialog= builder.create()
        dialog.show()
    }

    private fun showLoginLayout() {
        progressBar.visibility = View.GONE
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
                .build(), LOGIN_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                user = FirebaseAuth.getInstance().currentUser!!
                Log.i("FirebaseAuth-User", "user: $user")
                Log.i("FirebaseAuth-User", "uid: " + user.uid)
                Log.i("FirebaseAuth-User", "response: $response")
                Log.i("FirebaseAuth-User", "responseStr: " + response.toString().get(1))
                Log.i("FirebaseAuth-User", "responseStr: ")
            } else {
                Toast.makeText(this, "" + response!!.error!!.message, Toast.LENGTH_SHORT).show()
                Log.d("SignIn Error", "" + response.error!!.message)
            }
        }
    }
}
