package com.luuu.switchbutton

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_swith.setOnSelectedChangeListener(object : SwitchButtonView.OnSelectedChangeListener {
            override fun onSelectedChange(state: Int) {
                Log.e("test", "Now Selected : " + state)
            }
        })
    }
}
