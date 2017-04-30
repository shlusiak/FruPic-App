package de.saschahlusiak.frupic.grid;

import de.saschahlusiak.frupic.R;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class NavigationListAdapter extends ArrayAdapter<String> {
	
	public static final int CATEGORY_MAX = 2;

	public NavigationListAdapter(Context context) {
		super(context, R.layout.drawer_list_item, context.getResources().getStringArray(R.array.grid_dropdown_list));
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}
	
	@Override
	public boolean isEnabled(int position) {
		if (position == 3)
			return false;
		return true;
	}
	
	
	public int getIcon(int position) {
		switch (position) {
		case 0:	return R.drawable.navigation_all;
		case 1: return R.drawable.ic_action_unread;
		case 2: return R.drawable.ic_action_important;
		
		case 4: return R.drawable.ic_action_upload;
		case 5: return R.drawable.ic_action_web_site;
		case 6: return R.drawable.ic_action_settings;
		default:
			return 0;
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view = (TextView)super.getView(position, convertView, parent);
		view.setCompoundDrawablesWithIntrinsicBounds(getIcon(position), 0, 0, 0);
		if (position > CATEGORY_MAX)
			view.setTextColor(Color.LTGRAY);
		else
			view.setTextColor(Color.WHITE);
		return view;
	}

}
