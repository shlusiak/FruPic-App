package de.saschahlusiak.frupic.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.about.AboutFragment

class FrupicPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        findPreference<Preference>("donate_bitcoin")?.setOnPreferenceClickListener {
            onDonateClick()
            true
            
        }
        findPreference<Preference>("about")?.setOnPreferenceClickListener {
            AboutFragment().show(parentFragmentManager, null)
            true
        }
    }

    private fun onDonateClick() {
        val wallet = "bc1qdgm2zvlc6qzqh8qs44wv8l622tfrhvkjqn0fkl"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("bitcoin:$wallet"))
            startActivity(intent)
        }
        catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.blockchain.com/btc/address/$wallet"))
            startActivity(intent)
        }
    }
}