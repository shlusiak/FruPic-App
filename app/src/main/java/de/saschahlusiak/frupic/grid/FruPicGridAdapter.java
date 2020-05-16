package de.saschahlusiak.frupic.grid;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.model.Frupic;


public class FruPicGridAdapter extends RecyclerView.Adapter<FruPicGridAdapter.ViewHolder> {
	private static final String tag = FruPicGridAdapter.class.getSimpleName();

	private OnItemClickListener activity;
	private Cursor cursor;

	public interface OnItemClickListener
	{
		void onItemClick(int position, long id);
	}

	class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		Frupic frupic;
		ImageView image, imageLabel;

		ViewHolder(View convertView) {
			super(convertView);

			itemView.setOnClickListener(this);

			image = convertView.findViewById(R.id.image);
			imageLabel = convertView.findViewById(R.id.imageLabel);
		}
		
		void setFrupic(Frupic frupic) {
			if (frupic.hasFlag(Frupic.FLAG_FAV)) {
				imageLabel.setVisibility(View.VISIBLE);
				imageLabel.setImageResource(R.drawable.star_label);
			} else if (frupic.hasFlag(Frupic.FLAG_NEW)) {
				imageLabel.setVisibility(View.VISIBLE);
				imageLabel.setImageResource(R.drawable.new_label);
			} else
				imageLabel.setVisibility(View.INVISIBLE);

			if ((this.frupic != null) && (this.frupic.id == frupic.id))
				return;

			Picasso.get().load(frupic.getThumbUrl())
				.placeholder(R.drawable.frupic)
				.into(image);

			this.frupic = frupic;
		}
		
		@Override
		public void onClick(View v) {
			activity.onItemClick(getAdapterPosition(), frupic.id);
		}
	}

	FruPicGridAdapter(FruPicGridFragment activity) {
		this.activity = activity;
	}

	@NotNull
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		Frupic frupic = new Frupic(getItem(position));
		holder.setFrupic(frupic);
	}

	@Override
	public int getItemCount() {
		return cursor == null ? 0 : cursor.getCount();
	}

	public Cursor getItem(int position) {
		if (cursor == null)
			return null;
		cursor.moveToPosition(position);
		return cursor;
	}

	public void setCursor(Cursor cursor) {
		if (this.cursor != null )
			this.cursor.close();
		this.cursor = cursor;

		notifyDataSetChanged();
	}
}
