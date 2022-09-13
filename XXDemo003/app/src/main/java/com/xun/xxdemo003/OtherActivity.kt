package com.xun.xxdemo003

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.xun.xxdemo003.anno.InjectIntent
import com.xun.xxdemo003.anno.InjectView
import org.w3c.dom.Text

class OtherActivity : AppCompatActivity() {
    @InjectView(R.id.mOtherNameTv)
    private lateinit var nameTv: TextView

    @InjectIntent("name")
    private lateinit var name: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other)
        InjectUtils.injectView(this)
        InjectUtils.injectIntent(this)

        nameTv.text = name
    }
}