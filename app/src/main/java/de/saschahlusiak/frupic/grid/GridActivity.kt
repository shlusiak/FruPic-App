package de.saschahlusiak.frupic.grid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GridActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, GridFragment())
                .commit()
        }
    }
}