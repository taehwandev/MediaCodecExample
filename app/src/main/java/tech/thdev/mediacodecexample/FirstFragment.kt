package tech.thdev.mediacodecexample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tech.thdev.mediacodecexample.audio.AACAudioDecodeActivity
import tech.thdev.mediacodecexample.databinding.FragmentFirstBinding
import tech.thdev.mediacodecexample.video.VideoDecodeActivity

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonVideoExample.setOnClickListener {
            startActivity(VideoDecodeActivity.newIntent(requireContext()))
        }

        binding.buttonAudioExample.setOnClickListener {
            startActivity(AACAudioDecodeActivity.newIntent(requireContext()))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}