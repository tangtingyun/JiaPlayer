package com.jingjing.study.audio

import android.annotation.SuppressLint
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
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
class AudioRecordHelper() {

    private var mAudioRecordMic: AudioRecord

    private var inAudioBufferSize = 0

    @Volatile
    private var isRecording = false

    private val pcmFile: File
    private val wavFile: File
    private val aacFile: File
    private val encoderAAC: MediaCodec
    val pcmToWavUtil = PcmToWavUtil(sampleRateInHz, inputChannelConfig, audioFormat)


    init {
        inAudioBufferSize =
            AudioRecord.getMinBufferSize(sampleRateInHz, inputChannelConfig, audioFormat)
        logx("AudioRecord buffer size: $inAudioBufferSize")
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
        encoderAAC.configure(
            medFormat,
            null, null, MediaCodec.CONFIGURE_FLAG_ENCODE
        )
    }


    fun recordAudio() {
        isRecording = true
        mAudioRecordMic.startRecording();
        encoderAAC.start()
        Thread {
            while (isRecording) {
                val rawData = ByteArray(inAudioBufferSize)
                val read = mAudioRecordMic.read(rawData, 0, inAudioBufferSize)!!
                logx("返回码:   $read")
                if (read > 0) {
                    FileIOUtils.writeFileFromBytesByStream(pcmFile, rawData, true)
                    mediaEncode2(rawData, read)
                }
            }
            logx("停止录音")
            endStream()
            mAudioRecordMic.stop()
            mAudioRecordMic.release()
            pcmToWavUtil.pcmToWav(pcmFile, wavFile)
        }.start()
    }

    private var mPresentationTime: Long = 0
    private var mTotalBytes: Long = 0
    private val TIMEOUT = 500L

    private fun mediaEncode2(buffer: ByteArray, read: Int) {
        logx("已经读取的长度:   $read")
        if (read > 0) {
            val bufferIndex: Int = encoderAAC.dequeueInputBuffer(TIMEOUT)
            logx("送入编码的index:  $bufferIndex")
            if (bufferIndex > 0) {
                val buff: ByteBuffer = encoderAAC.getInputBuffer(bufferIndex)!!
                buff.clear()
                buff.put(buffer)
                logx("对比取值 readBytes: $read   ---    bufferSize:  ${buff.capacity()}")
                encoderAAC.queueInputBuffer(bufferIndex, 0, read, mPresentationTime, 0)
                mPresentationTime = 1000000L * (read / 2) / sampleRateInHz
                writeOutput()
            }
        }

    }

    private fun mediaEncode(buffer: ByteArray, read: Int) {
        var readBytes = read
        var offset = 0
        logx("已经读取的长度:   $readBytes")
        while (readBytes > 0) {
            logx("已经读取的长度 2 :   $readBytes")
            var totalBytesRead = 0
            val bufferIndex: Int = encoderAAC.dequeueInputBuffer(TIMEOUT)
            logx("送入编码的index:  $bufferIndex")
            if (bufferIndex < 0) {
                writeOutput()
                return
            }
            val buff: ByteBuffer = encoderAAC.getInputBuffer(bufferIndex)!!
            buff.clear()
            val bufferSize = buff.capacity()
            logx("对比取值 readBytes: $readBytes   ---    bufferSize:  $bufferSize")
            val bytesToRead = if (readBytes > bufferSize) bufferSize else readBytes
            totalBytesRead += bytesToRead
            readBytes -= bytesToRead
            buff.put(buffer, offset, bytesToRead)
            offset += bytesToRead

            logx("bufferIndex: $bufferIndex    bytesToRead:  $bytesToRead   presentationTime:  $mPresentationTime    readBytes: $readBytes")
            encoderAAC.queueInputBuffer(bufferIndex, 0, bytesToRead, mPresentationTime, 0)
            mTotalBytes += totalBytesRead.toLong()
            mPresentationTime = 1000000L * (mTotalBytes / 2) / sampleRateInHz

            writeOutput()
        }
    }

    private fun writeOutput() {
        var bufferInfo = MediaCodec.BufferInfo()
        var bufferIndex: Int = encoderAAC.dequeueOutputBuffer(bufferInfo, TIMEOUT)
        logx("输出编码的index:   $bufferIndex")
        logx("------ 开始循环取--------------")
        while (bufferIndex > 0) {
            logx(
                "输出编码的信息:   ${bufferInfo.flags}  -- ${bufferInfo.offset}" +
                        " -- ${bufferInfo.size} -- ${bufferInfo.presentationTimeUs}" +
                        "  --  ${bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0}"
            )
            val buff: ByteBuffer = encoderAAC.getOutputBuffer(bufferIndex)!!
            if (bufferInfo.size != 0) {

                val chunkAudio = ByteArray(bufferInfo.size + 7) // 7 is ADTS size
                addADTStoPacket(chunkAudio, chunkAudio.size)
                buff.get(chunkAudio, 7, bufferInfo.size)

                logx("写入aac的长度:   ${chunkAudio.size}  ${chunkAudio.toList().joinToString()}")
                FileIOUtils.writeFileFromBytesByStream(aacFile, chunkAudio, true)
            }
            encoderAAC.releaseOutputBuffer(bufferIndex, false)
            bufferIndex = encoderAAC.dequeueOutputBuffer(bufferInfo, TIMEOUT)
            logx("输出编码的index 2 :   $bufferIndex")
        }
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

    private fun endStream() {
        val bufferIndex: Int =
            encoderAAC.dequeueInputBuffer(TIMEOUT)
        encoderAAC.queueInputBuffer(
            bufferIndex, 0, 0, mPresentationTime,
            MediaCodec.BUFFER_FLAG_END_OF_STREAM
        )
        logx("开始写入end标识 :   $bufferIndex")
        writeOutput()
    }

    fun free() {
        isRecording = false
    }

}