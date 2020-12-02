package tech.thdev.mediacodecexample.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import tech.thdev.mediacodecexample.R
import tech.thdev.mediacodecexample.databinding.AudioActivityBinding


class AACAudioDecodeActivity : AppCompatActivity() {

    companion object {

        fun newIntent(context: Context): Intent =
            Intent(context, AACAudioDecodeActivity::class.java)
    }

    private val audioDecoder: AACAudioDecoderThread by lazy {
        AACAudioDecoderThread()
    }

    private val audioPathAAC by lazy {
        resources.openRawResourceFd(R.raw.bensound_littleidea_aac)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = AudioActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playAac.setOnClickListener {
            audioDecoder.startPlay(audioPathAAC)
        }
    }

    override fun onPause() {
        super.onPause()
        audioDecoder.stop()
    }
}