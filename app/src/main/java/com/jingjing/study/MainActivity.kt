package com.jingjing.study

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.Secure
import androidx.appcompat.app.AppCompatActivity
import com.jingjing.study.databinding.ActivityMainBinding
import com.jingjing.study.extension.logx
import java.nio.charset.StandardCharsets
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()

        binding.sampleText.setOnClickListener {
            startActivity(Intent(this, NavHostActivity::class.java))
        }
    }

    /**
     * A native method that is implemented by the 'study' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'study' library on application startup.
        init {
            System.loadLibrary("study")
        }
    }
}