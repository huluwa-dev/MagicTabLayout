package com.huluwa.magictablayout_sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
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
        )
        magicLayout.setTitles(list)
        magicLayout.onSelectChangeListener = {
            viewPager.currentItem = it
        }

        viewPager.adapter = FragmentPagerAdapter(
            this, listOf(
                { SampleFragment.newInstance("1") },
                { SampleFragment.newInstance("2") },
                { SampleFragment.newInstance("3") },
                { SampleFragment.newInstance("4") },
                { SampleFragment.newInstance("5") },
            )
        )
        magicIndicator.count = 5

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                magicIndicator.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                magicLayout.select(position)
            }
        })
    }
}
