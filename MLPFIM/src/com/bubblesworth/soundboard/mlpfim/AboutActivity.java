/**
 * 
 */
package com.bubblesworth.soundboard.mlpfim;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

/**
 * @author paulh
 *
 */
public class AboutActivity extends Activity {

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		TextView tv = (TextView) findViewById(R.id.aboutText);
		String[] text = getResources().getStringArray(R.array.about_text);
		StringBuilder builder = new StringBuilder();
		for (String str: text) {
			builder.append(str);
			builder.append("<br/>");
		}
		tv.setText(Html.fromHtml(builder.toString()));
	}

}
