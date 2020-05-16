package de.saschahlusiak.frupic.detail;

import java.io.File;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.cache.FileCacheUtils;
import de.saschahlusiak.frupic.model.Frupic;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.text.ClipboardManager;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class DetailDialog extends ArrayAdapter<DetailItem> {
	Context context;
	Frupic frupic;
	
	public DetailDialog(Context context, DetailItem[] objects, Frupic frupic) {
		super(context, R.layout.details_item, android.R.id.text1, objects);
		this.context = context;
		this.frupic = frupic;
	}

	public static AlertDialog create(Context context, Frupic frupic) {
		FileCacheUtils fileCache = new FileCacheUtils(context);
		
		DetailDialog d;
		ContextThemeWrapper ctw = new ContextThemeWrapper( context, R.style.AppTheme_Dialog);
		AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
		builder.setTitle("Frupic #" + frupic.id);
		
		DetailItem items[] = new DetailItem[5];
		items[0] = new DetailItem(context.getString(R.string.details_posted_by));
		items[1] = new DetailItem(context.getString(R.string.details_tags));
		items[2] = new DetailItem(context.getString(R.string.details_date));
		items[3] = new DetailItem(context.getString(R.string.details_size));
		items[4] = new DetailItem("URL");
		
		String tags = frupic.getTagsString();
		items[0].setValue(frupic.getUsername());
		items[1].setValue((tags != null) ? tags : "---");
		items[2].setValue(frupic.getDate());
		items[4].setValue(frupic.getFullUrl());

		File f = fileCache.getFile(frupic);
		if (f.exists()) {
			Options options = new Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(fileCache.getFileName(frupic), options);

			items[3].setValue(String.format(
					"%d x %d (%d kb)", options.outWidth, options.outHeight, f.length() / 1024));
		} else {
			items[3].setValue(context.getString(R.string.details_not_available));
		}
	
		
	
		d = new DetailDialog(context, items, frupic);
		/* Specifying an OnClickListener here will dismiss the dialog on select. 
		 * Do not want! See hack in getView */
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
	public View getView(final int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		TextView t = (TextView) v.findViewById(android.R.id.text2);
//		t.setTextColor(android.R.color.widget_edittext_dark);
//		t.setTextAppearance(context, android.R.attr.textAppearanceLarge);
		
		t.setText(getItem(position).getValue());
		
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(getItem(position).getValue());				
				Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();				
			}
		});
		return v;
	}
}
