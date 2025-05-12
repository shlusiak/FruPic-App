package de.saschahlusiak.frupic.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import de.saschahlusiak.frupic.BuildConfig
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.databinding.AboutActivityBinding

class AboutFragment : AppCompatDialogFragment() {
    override fun getTheme() = R.style.AppTheme_Dialog

    override fun onCreateDialog(savedInstanceState: Bundle?) = super.onCreateDialog(savedInstanceState).apply {
        setTitle(R.string.about_frupic)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.about_activity, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = AboutActivityBinding.bind(view)

        with(binding) {
            version.text = BuildConfig.VERSION_NAME

            ok.setOnClickListener { dismiss() }
            bitcoinLink.setOnClickListener { onDonateBitcoinClick() }
            litecoinLink.setOnClickListener { onDonateLitecoinClick() }
        }
    }

    private fun onDonateBitcoinClick() {
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


    private fun onDonateLitecoinClick() {
        val wallet = "Lh3YTC7Tv4edEe48kHMbyhgE6BNH22bqBt"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("litecoin:$wallet"))
            startActivity(intent)
        }
        catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.blockchain.com/ltc/address/$wallet"))
            startActivity(intent)
        }
    }
}