package de.saschahlusiak.frupic.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.model.Frupic

data class DetailItem(val title: String, val value: String = "") {
    override fun toString(): String = title
}

private class DetailDialogAdapter(context: Context, objects: List<DetailItem>) : ArrayAdapter<DetailItem>(context, R.layout.details_item, android.R.id.text1, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getView(position, convertView, parent)
        val t = v.findViewById<View>(android.R.id.text2) as TextView
        t.text = getItem(position)?.value

        v.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val value = getItem(position)?.value ?: return@setOnClickListener
            clipboard.setPrimaryClip(ClipData.newPlainText(null, value))
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
        return v
    }
}

fun createDetailDialog(context: Context, storage: FrupicStorage, frupic: Frupic): AlertDialog {
    val ctw = ContextThemeWrapper(context, R.style.AppTheme_Dialog)
    val builder = MaterialAlertDialogBuilder(ctw)
    builder.setTitle("Frupic #" + frupic.id)
    val f = storage.getFile(frupic)
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
        DetailItem(context.getString(R.string.details_date), frupic.dateString),
        DetailItem(context.getString(R.string.details_size), size),
        DetailItem("URL", frupic.fullUrl)
    )

    val adapter = DetailDialogAdapter(context, items)
    /* Specifying an OnClickListener here will dismiss the dialog on select.
    * Do not want! See hack in getView */
    builder.setAdapter(adapter, null)
    builder.setIcon(null)
    builder.setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }

    return builder.create()
}
