package com.jingjing.study.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.jingjing.study.extension.logx


class AudioPlayerHelper {

    fun play() {
        // 获取音频数据缓冲段大小
        val outAudioBufferSize = AudioTrack.getMinBufferSize(
            sampleRateInHz, outputChannelConfig, audioFormat
        )

        logx("AudioTrack buffer size: $outAudioBufferSize")

        val audioAttributes = AudioAttributes.Builder()
            .setLegacyStreamType(streamType)
            .build()
        val audioFormat = AudioFormat.Builder()
            .setChannelMask(outputChannelConfig)
            .setEncoding(audioFormat)
            .setSampleRate(sampleRateInHz).build()
        // 初始化音频播放
        var audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            outAudioBufferSize,
            trackMode,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }
}