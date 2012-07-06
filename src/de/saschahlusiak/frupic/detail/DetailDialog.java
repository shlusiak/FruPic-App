package de.saschahlusiak.frupic.detail;

import java.io.File;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.Toast;

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

		File f = frupic.getCachedFile(factory, false);
		if (f.exists()) {
			Options options = new Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(FrupicFactory.getCacheFileName(factory,
					frupic, false), options);

			items[3].setValue(String.format(
					"%d x %d (%d kb)", options.outWidth, options.outHeight, f.length() / 1024));
		} else {
			items[3].setValue(context.getString(R.string.details_not_available));
		}
	
		
	
		d = new DetailDialog(context, items, frupic);
		/* TODO: Specifying an OnClickListener here will dismiss the dialog on select. Do not want! */
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
		/* TODO: this is not called; see setAdapter(...) above */ 
		switch (which) {
		case 4:
			ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("simple text",frupic.getFullUrl());
			clipboard.setPrimaryClip(clip);
			Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
			break;
		}
	}
}
