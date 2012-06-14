package de.saschahlusiak.frupic.detail;

import java.io.File;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DetailDialog extends ArrayAdapter<DetailItem> implements DialogInterface.OnClickListener{
	Context context;
	Frupic frupic;
	
	public DetailDialog(Context context, DetailItem[] objects, Frupic frupic) {
		super(context, R.layout.details_item, android.R.id.text1, objects);
		this.context = context;
		this.frupic = frupic;
	}

	public static AlertDialog create(Context context, Frupic frupic, FrupicFactory factory) {
		DetailDialog d;
		ContextThemeWrapper ctw = new ContextThemeWrapper( context, R.style.Theme_FruPic_Light_Dialog);		
		AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
		builder.setTitle("Frupic #" + frupic.getId());
		
		DetailItem items[] = new DetailItem[6];
		items[0] = new DetailItem(context.getString(R.string.details_posted_by));
		items[1] = new DetailItem(context.getString(R.string.details_tags));
		items[2] = new DetailItem(context.getString(R.string.details_date));
		items[3] = new DetailItem(context.getString(R.string.details_size));
		items[4] = new DetailItem(context.getString(R.string.details_filesize));
		items[5] = new DetailItem("URL");
		
		String tags = frupic.getTagsString();
		items[0].setValue(frupic.getUsername());
		items[1].setValue((tags != null) ? tags : "---");
		items[2].setValue(frupic.getDate());
		items[5].setValue(frupic.getFullUrl());

		File f = frupic.getCachedFile(factory, false);
		if (f.exists()) {
			Options options = new Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(FrupicFactory.getCacheFileName(factory,
					frupic, false), options);

			items[3].setValue(String.format(
					"%d x %d", options.outWidth, options.outHeight));
			items[4].setValue(f.length() / 1024
					+ " kb");
//			findViewById(R.id.cache_now).setVisibility(View.GONE);
		} else {
			items[3].setValue(context.getString(R.string.details_not_available));
			items[4].setValue(context.getString(R.string.details_not_available));
/*			findViewById(R.id.cache_now).setVisibility(View.VISIBLE);
			findViewById(R.id.cache_now).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							showDialog(DIALOG_PROGRESS);
							fetchTask = new FetchTask(frupic, factory);
							fetchTask.setActivity(DetailsActivity.this, progressUpdater);
							fetchTask.execute();
						}
					}); */
		}
	
		
		
		
		
	
		d = new DetailDialog(context, items, frupic);
		builder.setAdapter(d, null);
		builder.setIcon(null);
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		return builder.create();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		TextView t = (TextView) v.findViewById(android.R.id.text2);
//		t.setTextColor(android.R.color.widget_edittext_dark);
//		t.setTextAppearance(context, android.R.attr.textAppearanceLarge);
		
		t.setText(getItem(position).getValue());
		return v;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub
		
	}
}
