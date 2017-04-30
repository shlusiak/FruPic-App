package de.saschahlusiak.frupic.about;

import de.saschahlusiak.frupic.R;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;

public class AboutActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.about_activity);
		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) != Configuration.SCREENLAYOUT_SIZE_XLARGE)
		{
			LayoutParams params = getWindow().getAttributes();
			params.width = LayoutParams.MATCH_PARENT;
			getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
		}
		
		((Button)findViewById(R.id.ok)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();		
			}
		});
	}
}
