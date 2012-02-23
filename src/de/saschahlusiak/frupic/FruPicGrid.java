package de.saschahlusiak.frupic;

 import java.net.UnknownHostException;

import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class FruPicGrid extends Activity implements OnItemClickListener, OnScrollListener {
	GridView grid;
	FruPicAdapter adapter;
	int base, count;	
	
	public final int FRUPICS = 20;
	public final int FRUPICS_STEP = 10;
	
	class RefreshTask extends AsyncTask<Void, Void, Frupic[]> {

		String error;
		
		@Override
		protected void onPreExecute() {
	        setProgressBarIndeterminateVisibility(true);
			super.onPreExecute();
		}
		@Override
		protected Frupic[] doInBackground(Void... arg0) {
			Frupic pics[];
			try {
				pics =	FrupicFactory.getThumbPics(base, count);
			} catch (UnknownHostException u) {
				pics = null;
				error = "Unknown host";
				cancel(false);
			} catch (Exception e) {
				pics = null;
				error = "Connection error";
				cancel(false);
			}
			
			return pics;
 
		}
		@Override
		protected void onCancelled() {
			Toast.makeText(FruPicGrid.this, error, Toast.LENGTH_LONG).show();
	        setProgressBarIndeterminateVisibility(false);
			super.onCancelled();
		}
		@Override
		protected void onPostExecute(Frupic result[]) {
	        adapter.setFrupics(result);
	        setProgressBarIndeterminateVisibility(false);
			super.onPostExecute(result);
		}
		
	}
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.grid);
    
        adapter = new FruPicGridAdapter(this);
        grid = (GridView) findViewById(R.id.gridView);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(this);
        grid.setOnScrollListener(this);
        
        base = 0;
        count = FRUPICS;
        
        new RefreshTask().execute();
    }


	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		Intent intent = new Intent(this, FruPicGallery.class);
		intent.putExtra("id", position);
		startActivity(intent);
	}


	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (firstVisibleItem + visibleItemCount > count - FRUPICS_STEP) {
			count += FRUPICS_STEP;
	        new RefreshTask().execute();
		}
	}


	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optionsmenu, menu);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.refresh) {
			adapter.setFrupics(null);
			adapter.clearCache();
			
			new RefreshTask().execute();
			return true;
		}
		
		if (item.getItemId() == R.id.gotowebsite) {
			Intent intent = new Intent(
					"android.intent.action.VIEW",
					Uri.parse("http://frupic.frubar.net"));
			startActivity(intent);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	
	
	
	
	
}