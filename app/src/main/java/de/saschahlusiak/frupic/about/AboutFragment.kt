package de.saschahlusiak.frupic.about

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.utils.AppTheme

class AboutFragment : DialogFragment() {
//    override fun getTheme() = R.style.AppTheme_Dialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        ComposeView(inflater.context)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view as ComposeView

        view.setContent {
            AppTheme {
                AboutScreen(
                    onLink = { startActivity(Intent(Intent.ACTION_VIEW, it.toUri())) },
                    onDismiss = { dismiss() }
                )
            }
        }
    }
}