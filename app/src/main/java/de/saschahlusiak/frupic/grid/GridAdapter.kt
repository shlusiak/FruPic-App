package de.saschahlusiak.frupic.grid

import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.model.Frupic

class GridAdapter internal constructor(private val listener: OnItemClickListener) : RecyclerView.Adapter<GridAdapter.ViewHolder>() {
    private var cursor: Cursor? = null

    interface OnItemClickListener {
        fun onFrupicClick(position: Int, frupic: Frupic)
        fun onFrupicLongClick(view: View, position: Int, frupic: Frupic)
    }

    inner class ViewHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {
        var frupic: Frupic? = null
        private var image: ImageView
        private var imageLabel: ImageView

        init {
            image = containerView.findViewById(R.id.image)
            imageLabel = containerView.findViewById(R.id.imageLabel)

            itemView.setOnClickListener {
                val frupic = frupic ?: return@setOnClickListener
                listener.onFrupicClick(adapterPosition, frupic)
            }

            itemView.setOnLongClickListener {
                val frupic = frupic ?: return@setOnLongClickListener false
                listener.onFrupicLongClick(it, adapterPosition, frupic)
                true
            }
        }

        fun bindFrupic(frupic: Frupic) {
            when {
                frupic.hasFlag(Frupic.FLAG_FAV) -> {
                    imageLabel.visibility = View.VISIBLE
                    imageLabel.setImageResource(R.drawable.star_label)
                }
                frupic.hasFlag(Frupic.FLAG_NEW) -> {
                    imageLabel.visibility = View.VISIBLE
                    imageLabel.setImageResource(R.drawable.new_label)
                }
                else -> imageLabel.visibility = View.INVISIBLE
            }
            if (this.frupic == frupic) return
            this.frupic = frupic

            Picasso.get()
                .load(frupic.thumbUrl)
                .placeholder(R.drawable.frupic)
                .into(image)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.grid_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val frupic = getItem(position) ?: return
        holder.bindFrupic(frupic)
    }

    override fun getItemCount(): Int {
        return cursor?.count ?: 0
    }

    fun getItem(position: Int): Frupic? {
        val cursor = cursor ?: return null
        cursor.moveToPosition(position)
        return Frupic(cursor)
    }

    fun setCursor(cursor: Cursor) {
        this.cursor = cursor
        notifyDataSetChanged()
    }
}