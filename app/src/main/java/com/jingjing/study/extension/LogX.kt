package com.jingjing.study.extension

import android.util.Log

private const val TAG = "Jia-Country"

fun Any.logx(msg: String) {
    Log.w(TAG, msg)
}