package com.jingjing.study.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaRecorder


const val inputChannelConfig = AudioFormat.CHANNEL_IN_STEREO
const val outputChannelConfig = AudioFormat.CHANNEL_OUT_STEREO

const val audioFormat = AudioFormat.ENCODING_PCM_16BIT
const val bitRate = 96000
const val sampleRateInHz = 44100

const val audioSource = MediaRecorder.AudioSource.MIC


const val streamType = AudioManager.STREAM_VOICE_CALL

const val trackMode = AudioTrack.MODE_STREAM