package tech.thdev.mediacodecexample.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import tech.thdev.mediacodecexample.R


class VideoDecodeActivity : AppCompatActivity(), SurfaceHolder.Callback {

    companion object {

        fun newIntent(context: Context): Intent =
            Intent(context, VideoDecodeActivity::class.java)
    }

    private var videoDecode: VideoDecodeThread? = null

    private val videoPath by lazy {
        resources.openRawResourceFd(R.raw.sample_video)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SurfaceView(this).apply {
            holder.addCallback(this@VideoDecodeActivity)
            setContentView(this)

            videoDecode = VideoDecodeThread()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (videoDecode?.init(holder.surface, videoPath) == true) {
            videoDecode?.start()
        } else {
            videoDecode = null
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        videoDecode?.close()
    }

    override fun onPause() {
        super.onPause()
        videoDecode?.close()
    }
}