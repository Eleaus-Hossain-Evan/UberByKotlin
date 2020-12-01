package com.example.uberbykotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import io.reactivex.Completable
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Completable.timer(3, TimeUnit.SECONDS)
            .subscribe({
                Toast.makeText(this@SplashScreenActivity, "Splash Screen", Toast.LENGTH_SHORT).show()
            })
    }
}