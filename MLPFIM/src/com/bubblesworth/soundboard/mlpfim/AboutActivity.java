/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author paulh
 * 
 */
public class AboutActivity extends ListActivity {
	private static final String TAG = "AboutActivity";

	private static final String TEXT = "text";
	private static final String LINK = "link";

	List<Map<String, Object>> data;

	private class AboutViewBinder implements SimpleAdapter.ViewBinder {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.widget.SimpleAdapter.ViewBinder#setViewValue(android.view
		 * .View, java.lang.Object, java.lang.String)
		 */
		@Override
		public boolean setViewValue(View view, Object data,
				String textRepresentation) {
			if (!(view instanceof TextView)) {
				return false;
			}
			TextView tv = (TextView) view;
			if (data instanceof Spanned) {
				tv.setText((Spanned) data);
				return true;
			}
			if (data instanceof String) {
				if (((String) data).length() != 0) {
					tv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
				} else {
					tv.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
				}
				return true;
			}
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String[] text = getResources().getStringArray(R.array.about_text);
		String[] links = getResources().getStringArray(R.array.about_links);
		assert text.length == links.length;
		data = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < text.length; ++i) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put(TEXT, Html.fromHtml(text[i]));
			map.put(LINK, links[i]);
			data.add(map);
		}

		SimpleAdapter adapter = new SimpleAdapter(this, data,
				R.layout.about_list_item, new String[] { TEXT, LINK },
				new int[] { R.id.aboutText, R.id.aboutText });
		adapter.setViewBinder(new AboutViewBinder());
		setListAdapter(adapter);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Map<String, Object> map = data.get(position);
		String link = (String) map.get(LINK);
		if (link.length() == 0) {
			return;
		}
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(link));
		try {
			startActivity(i);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "No activity for " + link, e);
			Toast.makeText(this,
					getResources().getText(R.string.toast_link_failed),
					Toast.LENGTH_SHORT).show();
		}
	}

}
