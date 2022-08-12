package com.jingjing.study.audio

import android.annotation.SuppressLint
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.jingjing.study.extension.logx
import java.io.File
import java.nio.ByteBuffer

/*
*
* 参考资料： https://www.jianshu.com/p/ce88092fabfa
*
* */
@SuppressLint("MissingPermission")
class AudioRecordAsynHelper {

    private lateinit var mAudioRecordMic: AudioRecord

    private var inAudioBufferSize = 0

    @Volatile
    private var isRecording = false

    private lateinit var pcmFile: File
    private lateinit var wavFile: File
    private lateinit var aacFile: File
    private lateinit var encoderAAC: MediaCodec

    private var mPresentationTime: Long = 0
    private var mTotalBytes: Long = 0

    val pcmToWavUtil = PcmToWavUtil(sampleRateInHz, inputChannelConfig, audioFormat)

    private val handlerThread = HandlerThread("aac-encoder")
    private lateinit var backHandler: Handler

    private var hasWriteEnd = false

    init {
        handlerThread.start()
        backHandler = Handler(handlerThread.looper)
        backHandler.post {
            inAudioBufferSize =
                AudioRecord.getMinBufferSize(sampleRateInHz, inputChannelConfig, audioFormat)
            logx("AudioRecordAsynHelper buffer size: $inAudioBufferSize")
            mAudioRecordMic = AudioRecord(
                audioSource,
                sampleRateInHz,
                inputChannelConfig,
                audioFormat,
                inAudioBufferSize
            )
            val pcmPath = PathUtils.join(PathUtils.getExternalAppFilesPath(), "jia.pcm")
            val wavPath = PathUtils.join(PathUtils.getExternalAppFilesPath(), "jia.wav")
            val aacPath = PathUtils.join(PathUtils.getExternalAppFilesPath(), "jia.aac")

            FileUtils.createFileByDeleteOldFile(pcmPath)
            FileUtils.createFileByDeleteOldFile(wavPath)
            FileUtils.createFileByDeleteOldFile(aacPath)

            pcmFile = File(pcmPath)
            wavFile = File(wavPath)
            aacFile = File(aacPath)

            encoderAAC = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            val medFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, 2
            )
            medFormat.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            medFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                medFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, audioFormat)
            }
            encoderAAC.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, inputBufferId: Int) {
                    logx("输入 input 可用   ------------  $inputBufferId   isRecording  $isRecording")

                    val codecInputBuffer: ByteBuffer = encoderAAC.getInputBuffer(inputBufferId)!!
                    val capacity = codecInputBuffer.capacity()
                    val buffer = ByteArray(capacity)
                    if (isRecording) {
                        val readBytes: Int = mAudioRecordMic.read(buffer, 0, buffer.size)
                        logx("录制的数量  $readBytes")
                        if (readBytes > 0) {
                            FileIOUtils.writeFileFromBytesByStream(pcmFile, buffer, true)
                            codecInputBuffer.put(buffer, 0, readBytes)
                            encoderAAC.queueInputBuffer(
                                inputBufferId,
                                0,
                                readBytes,
                                mPresentationTime,
                                0
                            )
                            mTotalBytes += readBytes.toLong()
                            mPresentationTime = 1000000L * (mTotalBytes / 2) / sampleRateInHz
                        }
                    } else if (!hasWriteEnd) {
                        logx(" 释放音频采集----  ")
                        mAudioRecordMic.stop()
                        mAudioRecordMic.release()

                        hasWriteEnd = true
                        encoderAAC.queueInputBuffer(
                            inputBufferId, 0, 0, mPresentationTime,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        logx("开始写入end标识 : ")
                        pcmToWavUtil.pcmToWav(pcmFile, wavFile)
                    }


                }

                override fun onOutputBufferAvailable(
                    codec: MediaCodec,
                    outputBufferId: Int,
                    info: MediaCodec.BufferInfo
                ) {
                    logx(" 输出output 可用  ------------  $outputBufferId  infoSize : ${info.size}  flag:  ${info.flags}  offsets:  ${info.offset}")

                    val oneADTSFrameBytes = ByteArray(7 + info.size)
                    addADTStoPacket(oneADTSFrameBytes, oneADTSFrameBytes.size)

                    val outputBuffer = encoderAAC.getOutputBuffer(outputBufferId)!!

                    outputBuffer.get(oneADTSFrameBytes, 7, info.size);

                    FileIOUtils.writeFileFromBytesByStream(aacFile, oneADTSFrameBytes, true)
                    encoderAAC.releaseOutputBuffer(outputBufferId, false)
                    if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                        encoderAAC.stop()
                        encoderAAC.release()
                        logx(" 释放编码器 ---  ")
                    }
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                    logx(" onError   $e")
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                    logx(" onOutputFormatChanged   ${format.toString()}")
                }

            })
            encoderAAC.configure(
                medFormat,
                null, null, MediaCodec.CONFIGURE_FLAG_ENCODE
            )
        }
    }


    fun recordAudio() {
        isRecording = true
        hasWriteEnd = false
        mAudioRecordMic.startRecording();
        encoderAAC.start()
    }


    fun free() {
        isRecording = false
    }


    private fun addADTStoPacket(packet: ByteArray, packetLen: Int) {
        val profile = 2 //AAC LC
        val freqIdx = 4 //44.1KHz
        val chanCfg = 1 //CPE
        // fill in ADTS data
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
    }
}