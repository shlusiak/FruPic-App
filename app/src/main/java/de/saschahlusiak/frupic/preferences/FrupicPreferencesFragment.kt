package de.saschahlusiak.frupic.preferences

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.about.AboutFragment

class FrupicPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        findPreference<Preference>("about")?.setOnPreferenceClickListener {
            AboutFragment().show(parentFragmentManager, null)
            true
        }
    }
}