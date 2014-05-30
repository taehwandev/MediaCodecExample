package net.thdev.mediacodecexample;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class MainActivity extends ListActivity {
	private final String EXTRAPATH = "net.thdev.Path";
	private final String CATEGORYNAME = "net.thdev.mediacodecexample.SAMPLE_CODE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		String path = intent.getStringExtra(EXTRAPATH);
		
		if (path == null) {
			path = "";
		}
		
		setListAdapter(new SimpleAdapter(this, getData(path), android.R.layout.simple_list_item_1, new String[] { "title" }, new int[] { android.R.id.text1 }));
		getListView().setTextFilterEnabled(true);
	}
	
	/**
	 * Android ApiDemos Example code
	 * @param prefix
	 * @return
	 */
	protected List<Map<String, Object>> getData(String prefix) {
		List<Map<String, Object>> myData = new ArrayList<Map<String, Object>>();
		
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(CATEGORYNAME);
		
		PackageManager pm = getPackageManager();
		List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);
		
		if (list == null) return myData;
		
		String[] prefixPath;
		String prefixWithSlash = prefix;
		
		if (prefix.equals("")) {
			prefixPath = null;
			
		} else {
			prefixPath = prefix.split("/");
			prefixWithSlash = prefix + "/";
		}
		
		int len = list.size();
		
		Map<String, Boolean> entries = new HashMap<String, Boolean>();
		
		for (int i = 0; i < len; i++) {
			ResolveInfo info = list.get(i);
			CharSequence labelSeq = info.loadLabel(pm);
			String label = labelSeq != null ? labelSeq.toString() : info.activityInfo.name;
			
			if(prefixWithSlash.length() == 0 || label.startsWith(prefixWithSlash)) {
				String[] labelPath = label.split("/");
				String nextLabel = prefixPath == null ? labelPath[0] : labelPath[prefixPath.length];
				
				if((prefixPath != null ? prefixPath.length : 0) == labelPath.length - 1) {
					addItem(myData, nextLabel, activityIntent(info.activityInfo.applicationInfo.packageName, info.activityInfo.name));
					
				} else {
					if (entries.get(nextLabel) == null) {
						addItem(myData, nextLabel, browseIntent(prefix.equals("") ?  nextLabel : prefix + "/" + nextLabel));
						entries.put(nextLabel, true);
					}
				}
			}
		}
		Collections.sort(myData, sDisplayNameComparator);
		
		return myData;
	}
	
	private final static Comparator<Map<String, Object>> sDisplayNameComparator = new Comparator<Map<String, Object>>() {
		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
			return collator.compare(lhs.get("title"), rhs.get("title"));
		}
	};
	
	protected Intent activityIntent(String pkg, String componentName) {
		Intent result = new Intent();
		result.setClassName(pkg, componentName);
		return result;
	}
	
	protected Intent browseIntent(String path) {
		Intent result = new Intent();
		result.setClass(this, MainActivity.class);
		result.putExtra(EXTRAPATH, path);
		return result;
	}
	
	protected void addItem(List<Map<String, Object>> data, String name, Intent intent) {
		Map<String, Object> temp = new HashMap<String, Object>();
		temp.put("title", name);
		temp.put("intent", intent);
		data.add(temp);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Map<String, Object> map = (Map<String, Object>)l.getItemAtPosition(position);
		
		Intent intent = (Intent) map.get("intent");
		startActivity(intent);
	}
	
}
