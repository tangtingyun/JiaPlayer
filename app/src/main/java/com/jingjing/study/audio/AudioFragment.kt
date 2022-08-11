package com.jingjing.study.audio

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.jingjing.study.R
import com.jingjing.study.databinding.FragmentAudioBinding
import com.permissionx.guolindev.PermissionX

class AudioFragment : Fragment() {
    private lateinit var fragmentAudioBinding: FragmentAudioBinding
    private val mAudioRecordHelper = AudioRecordHelper()
    private val mAsynAudioRecordHelper = AudioRecordAsynHelper()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentAudioBinding = FragmentAudioBinding.inflate(layoutInflater)
        return fragmentAudioBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentAudioBinding.btnStart.setOnClickListener {
            PermissionX.init(this)
                .permissions(Manifest.permission.RECORD_AUDIO)
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        mAsynAudioRecordHelper.recordAudio()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "These permissions are denied: $deniedList",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        fragmentAudioBinding.btnStop.setOnClickListener {
            mAsynAudioRecordHelper.free()
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = AudioFragment()

    }
}