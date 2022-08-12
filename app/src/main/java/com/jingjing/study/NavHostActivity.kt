package com.jingjing.study

import android.media.MediaCodecList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.Utils
import com.jingjing.study.extension.logx


class NavHostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.init(application)
        setContentView(R.layout.activity_nav_host)

        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS) //REGULAR_CODECS参考api说明
        val codecs = list.codecInfos
        logx("Decoders: ")
        for (codec in codecs) {
            if (codec.isEncoder) continue
            logx(codec.name)
        }
        logx("Encoders: ")
        for (codec in codecs) {
            if (codec.isEncoder) logx(codec.name)
        }
    }
}