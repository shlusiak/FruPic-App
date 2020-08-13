package de.saschahlusiak.frupic.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import de.saschahlusiak.frupic.BuildConfig
import de.saschahlusiak.frupic.R
import kotlinx.android.synthetic.main.about_activity.*

class AboutFragment : AppCompatDialogFragment() {
    override fun getTheme() = R.style.AppTheme_Dialog

    override fun onCreateDialog(savedInstanceState: Bundle?) = super.onCreateDialog(savedInstanceState).apply {
        setTitle(R.string.about_frupic)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.about_activity, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        version.text = BuildConfig.VERSION_NAME
        ok.setOnClickListener { dismiss() }
    }
}