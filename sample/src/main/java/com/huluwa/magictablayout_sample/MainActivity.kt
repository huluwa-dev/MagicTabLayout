package com.huluwa.magictablayout_sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.huluwa.lib.magictablayout.Title
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = arrayListOf(
            Title("title1", "fullTitle1", "subtitle1"),
            Title("title2", "fullTitle2", "subtitle2"),
            Title("title3", "fullTitle3", "subtitle3"),
            Title("title4", "fullTitle4", "subtitle4"),
            Title("title5", "fullTitle5", "subtitle5"),
            Title("title6", "fullTitle6", "subtitle6")
        )
        magicLayout.setTitles(list)
        magicLayout.onSelectChangeListener = {
            Log.d("MagicTabLayout", "$it is selected")
        }
    }
}
