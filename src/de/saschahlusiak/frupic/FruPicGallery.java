package de.saschahlusiak.frupic;

import java.io.IOException;

import de.saschahlusiak.frupic.model.FrupicFactory;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.AdapterView.OnItemSelectedListener;

public class FruPicGallery extends Activity implements OnItemSelectedListener {
	Gallery gallery;
	FruPicAdapter adapter;
	int base;
	public final int FRUPICS = 7;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);
    
        adapter = new FruPicGalleryAdapter(this);
        gallery = (Gallery) findViewById(R.id.gallery);
        gallery.setAdapter(adapter);
        
        base = getIntent().getIntExtra("id", 0);
        try {
			adapter.setFrupics(FrupicFactory.getFrupics(base, FRUPICS));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        gallery.setOnItemSelectedListener(this);
        gallery.setCallbackDuringFling(true);
    }

	@Override
	public void onItemSelected(AdapterView<?> adapterview, View view, int position,	long id) {
		if (position == FRUPICS - 1) {			
			base += FRUPICS / 2;
	        try {
				adapter.setFrupics(FrupicFactory.getFrupics(base, FRUPICS));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        gallery.setSelection(position - FRUPICS / 2);
		}
		if (position == 0 && base > 0) {
			base -= FRUPICS / 2;
	        try {
				adapter.setFrupics(FrupicFactory.getFrupics(base, FRUPICS));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        gallery.setSelection(position + FRUPICS / 2);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		
	}
}