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
            Title("title1", "fTitle1", "subtitle1"),
            Title("title2", "fTitle2", "subtitle2"),
            Title("title3", "fTitle3", "subtitle3"),
            Title("title4", "fTitle4", "subtitle4"),
            Title("title5", "fTitle5", "subtitle5"),
            Title("title6", "fTitle6", "subtitle6")
        )
        magicLayout.setTitles(list)
        magicLayout.onSelectChangeListener = {
            Log.d("MagicTabLayout", "$it is selected")
        }
    }
}
