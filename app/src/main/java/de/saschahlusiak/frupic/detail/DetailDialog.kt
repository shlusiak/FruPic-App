package de.saschahlusiak.frupic.detail

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.text.ClipboardManager
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.app.FrupicManager
import de.saschahlusiak.frupic.model.Frupic

class DetailItem(val title: String, val value: String = "") {
    override fun toString(): String = title
}

class DetailDialog(context: Context, objects: List<DetailItem>) : ArrayAdapter<DetailItem>(context, R.layout.details_item, android.R.id.text1, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getView(position, convertView, parent)
        val t = v.findViewById<View>(android.R.id.text2) as TextView
//		t.setTextColor(android.R.color.widget_edittext_dark);
//		t.setTextAppearance(context, android.R.attr.textAppearanceLarge);
        t.text = getItem(position)?.value

        v.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.text = getItem(position)?.value
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
        return v
    }

    companion object {
        @JvmStatic
        fun create(context: Context, manager: FrupicManager, frupic: Frupic): AlertDialog {
            val d: DetailDialog
            val ctw = ContextThemeWrapper(context, R.style.AppTheme_Dialog)
            val builder = AlertDialog.Builder(ctw)
            builder.setTitle("Frupic #" + frupic.id)
            val f = manager.getFile(frupic)
            val size = if (f.exists()) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(f.absolutePath, options)
                String.format("%d x %d (%d kb)", options.outWidth, options.outHeight, f.length() / 1024)
            } else {
                context.getString(R.string.details_not_available)
            }
            val items = listOf(
                DetailItem(context.getString(R.string.details_posted_by), frupic.username ?: ""),
                DetailItem(context.getString(R.string.details_tags), frupic.tagsString),
                DetailItem(context.getString(R.string.details_date), frupic.date ?: ""),
                DetailItem(context.getString(R.string.details_size), size),
                DetailItem("URL", frupic.fullUrl)
            )

            d = DetailDialog(context, items)
            /* Specifying an OnClickListener here will dismiss the dialog on select.
		    * Do not want! See hack in getView */
            builder.setAdapter(d, null)
            builder.setIcon(null)
            builder.setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }

            return builder.create()
        }
    }
}