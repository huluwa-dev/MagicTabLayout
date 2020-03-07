package com.huluwa.lib.magictablayout

import android.content.res.Resources
import android.util.TypedValue


val Int.dp: Int
    get() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics).toInt()
    }

val Int.sp: Int
    get() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(), Resources.getSystem().displayMetrics).toInt()
    }