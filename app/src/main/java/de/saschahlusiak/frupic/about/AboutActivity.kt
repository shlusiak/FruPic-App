package de.saschahlusiak.frupic.about

import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import de.saschahlusiak.frupic.R
import kotlinx.android.synthetic.main.about_activity.*

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_activity)

        if (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK != Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            val params = window.attributes
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            window.attributes = params
        }

        ok.setOnClickListener { finish() }
    }
}