package de.saschahlusiak.frupic.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import de.saschahlusiak.frupic.BuildConfig
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.databinding.AboutFragmentBinding

class AboutFragment : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) = super.onCreateDialog(savedInstanceState).apply {
        setTitle(R.string.about_frupic)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.about_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = AboutFragmentBinding.bind(view)

        with(binding) {
            version.text = BuildConfig.VERSION_NAME

            ok.setOnClickListener { dismiss() }
        }
    }
}