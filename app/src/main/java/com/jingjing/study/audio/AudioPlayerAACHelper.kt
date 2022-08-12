package com.jingjing.study.audio

import android.content.Context
import android.media.*
import android.os.Build
import com.blankj.utilcode.util.FileIOUtils
import com.jingjing.study.extension.logx
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.min


class AudioPlayerAACHelper {
    private val minBufferSizeInBytes: Int
    private val audioTrack: AudioTrack
    private var mBytesWritten = 0

    @Volatile
    private var isPlaying = false

    private lateinit var decoderAAC: MediaCodec

    init {
        // 获取音频数据缓冲段大小
        minBufferSizeInBytes = AudioTrack.getMinBufferSize(
            sampleRateInHz, outputChannelConfig, audioFormat
        )

        logx("AudioTrack AudioPlayerAACHelper buffer size: $minBufferSizeInBytes")

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

    private var fileEof = false

    fun playAAc(ctx: Context) {
        Thread {
            val inputStream = ctx.assets.open("Stranger.aac")

            val mediaExtractor = MediaExtractor()

            val openFd = ctx.assets.openFd("Stranger.aac")

            mediaExtractor.setDataSource(openFd.fileDescriptor, openFd.startOffset, openFd.length)
//            mediaExtractor.setDataSource("file:///android_asset/Stranger.aac")
            for (index in 0 until mediaExtractor.trackCount) {
                val trackFormat = mediaExtractor.getTrackFormat(index)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                logx("mime:   $mime")
            }


//            isPlaying = true
//            decoderAAC = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
//            val medFormat = MediaFormat.createAudioFormat(
//                MediaFormat.MIMETYPE_AUDIO_RAW, sampleRateInHz, 2
//            )
//            decoderAAC.setCallback(object : MediaCodec.Callback() {
//                override fun onInputBufferAvailable(codec: MediaCodec, inputBufferId: Int) {
//                    logx("输入 input 可用   ------------  $inputBufferId ")
//
//                    val codecInputBuffer: ByteBuffer = decoderAAC.getInputBuffer(inputBufferId)!!
//                    val capacity = codecInputBuffer.capacity()
//                    val finalCapacity = min(capacity, minBufferSizeInBytes)
//                    val buffer = ByteArray(finalCapacity)
//
//                    try {
//                        val readSize = inputStream.read(buffer)
//                        if (readSize != -1 && isPlaying) {
//                            codecInputBuffer.put(buffer, 0, readSize)
//                            decoderAAC.queueInputBuffer(
//                                inputBufferId,
//                                0,
//                                readSize,
//                                0,
//                                0
//                            )
//                        } else if (readSize == -1 && !fileEof) {
//                            fileEof = true
//                            try {
//                                inputStream.close()
//                            } catch (e: IOException) {
//                                e.printStackTrace()
//                            }
//                        }
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                    }
//                }
//
//                override fun onOutputBufferAvailable(
//                    codec: MediaCodec,
//                    outputBufferId: Int,
//                    info: MediaCodec.BufferInfo
//                ) {
//                    logx(" 输出output 可用  ------------  $outputBufferId  infoSize : ${info.size}  flag:  ${info.flags}  offsets:  ${info.offset}")
//
//                    val oneADTSFrameBytes = ByteArray(info.size)
//
//                    val outputBuffer = decoderAAC.getOutputBuffer(outputBufferId)!!
//
//                    outputBuffer.get(oneADTSFrameBytes, 0, info.size);
//
//                    val bytesWritten: Int = writeToAudioTrack(audioTrack, oneADTSFrameBytes)
//
//                    decoderAAC.releaseOutputBuffer(outputBufferId, false)
//                    if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
//                        decoderAAC.stop()
//                        decoderAAC.release()
//                        logx(" 释放解码器 ---  ")
//                    }
//                }
//
//                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
//                    logx(" onError   $e")
//                }
//
//                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
//                    logx(" onOutputFormatChanged   ${format.toString()}")
//                }
//
//            })
//            decoderAAC.configure(
//                medFormat,
//                null, null, MediaCodec.CONFIGURE_FLAG_ENCODE
//            )
//            decoderAAC.start()
        }.start()
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