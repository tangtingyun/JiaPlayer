package com.jingjing.study.audio

import android.content.Context
import android.media.*
import android.os.Build
import com.blankj.utilcode.util.FileIOUtils
import com.jingjing.study.extension.logx
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.min


class AudioPlayerPcmHelper {
    private val minBufferSizeInBytes: Int
    private val audioTrack: AudioTrack
    private var mBytesWritten = 0

    @Volatile
    private var isPlaying = false

    init {
        // 获取音频数据缓冲段大小
        minBufferSizeInBytes = AudioTrack.getMinBufferSize(
            sampleRateInHz, outputChannelConfig, audioFormat
        )

        logx("AudioTrack buffer size: $minBufferSizeInBytes")

        val audioAttributes = AudioAttributes.Builder()
            .setLegacyStreamType(streamType)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setChannelMask(outputChannelConfig)
            .setEncoding(audioFormat)
            .setSampleRate(sampleRateInHz).build()
        // 初始化音频播放
        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            minBufferSizeInBytes,
            trackMode,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        mBytesWritten = 0
    }

    fun play(ctx: Context) {
        val inputStream = ctx.assets.open("44100_s16le_2.pcm")
        isPlaying = true
        Thread {
            try {
                val data = ByteArray(minBufferSizeInBytes * 2)
                while (inputStream.read(data) != -1 && isPlaying) {
                    logx("AudioTrack 从流中读取: ${data.size}  isPlaying  $isPlaying")

                    val bytesWritten: Int = writeToAudioTrack(audioTrack, data)
                    mBytesWritten += bytesWritten
                }

            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun writeToAudioTrack(audioTrack: AudioTrack, bytes: ByteArray): Int {
        if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
            logx("AudioTrack not playing, restarting : " + audioTrack.hashCode())
            audioTrack.play()
        }
        var count = 0
        while (count < bytes.size && isPlaying) {
            // Note that we don't take bufferCopy.mOffset into account because
            // it is guaranteed to be 0.
            val written = audioTrack.write(bytes, count, bytes.size)
            logx("AudioTrack 写入播放: $written   isPlaying  $isPlaying")
            if (written <= 0) {
                break
            }
            count += written
        }
        return count
    }

    fun free() {
        isPlaying = false
    }
}