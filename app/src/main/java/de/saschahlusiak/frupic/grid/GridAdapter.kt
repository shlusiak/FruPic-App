package de.saschahlusiak.frupic.grid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.model.cloudfront
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GridAdapter internal constructor(private val listener: OnItemClickListener) : RecyclerView.Adapter<GridAdapter.ViewHolder>() {
    private var items = emptyList<Frupic>()
    private val picasso = Picasso.get()

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

            picasso
                .load(frupic.thumbUrl.cloudfront)
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
        return items.size
    }

    fun getItem(position: Int): Frupic? {
        return if (position in items.indices)
            items[position]
        else
            null
    }

    fun setItems(newItems: List<Frupic>) {
        // both cursors are open, so we can calculate a diff on both of them
        val oldItems = this.items

        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = oldItems.size
            override fun getNewListSize() = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition].id == newItems[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition] == newItems[newItemPosition]
            }
        }

        val result = DiffUtil.calculateDiff(diffCallback)

        this.items = newItems
        result.dispatchUpdatesTo(this)
    }
}