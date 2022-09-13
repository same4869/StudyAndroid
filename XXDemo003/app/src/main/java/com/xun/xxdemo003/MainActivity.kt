package com.xun.xxdemo003

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.xun.xxdemo003.anno.InjectView

class MainActivity : AppCompatActivity() {

    @InjectView(R.id.otherBtn)
    private lateinit var otherBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        InjectUtils.injectView(this)

        otherBtn.setOnClickListener {
            startActivity(Intent(MainActivity@ this, OtherActivity::class.java).apply {
                putExtra("name","lilei")
            })
        }
    }
}