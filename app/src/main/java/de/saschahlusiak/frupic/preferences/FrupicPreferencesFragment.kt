package de.saschahlusiak.frupic.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import de.saschahlusiak.frupic.R

class FrupicPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }
}