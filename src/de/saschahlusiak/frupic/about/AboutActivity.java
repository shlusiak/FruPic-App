package de.saschahlusiak.frupic.about;

import de.saschahlusiak.frupic.R;
import android.app.Activity;
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
		LayoutParams params = getWindow().getAttributes(); 
        params.width = LayoutParams.FILL_PARENT; 
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params); 
		
		((Button)findViewById(R.id.ok)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();		
			}
		});
	}
}
