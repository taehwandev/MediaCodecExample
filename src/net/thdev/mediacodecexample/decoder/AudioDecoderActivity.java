package net.thdev.mediacodecexample.decoder;

import net.thdev.mediacodec.R;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class AudioDecoderActivity extends Activity {
	
	protected static AudioDecoderThread mAudioDecoder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_decoder);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		mAudioDecoder.stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/temp.aac";

		public PlaceholderFragment() {
			mAudioDecoder = new AudioDecoderThread();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_audio_decoder, container, false);
			
			final Button btn = (Button) rootView.findViewById(R.id.play);
			btn.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mAudioDecoder.startPlay(SAMPLE);
				}
			});
			return rootView;
		}
	}

}
