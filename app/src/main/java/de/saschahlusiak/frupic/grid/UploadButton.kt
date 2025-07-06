package de.saschahlusiak.frupic.grid

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.saschahlusiak.frupic.utils.AppTheme

@Composable
fun UploadButton(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = IconButtonDefaults.filledIconButtonColors()

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(150.dp, 115.dp),
        shape = RoundedCornerShape(topStartPercent = 100, topEndPercent = 25),
        color = colors.containerColor,
    ) {
        Box(
            contentAlignment = Alignment.Companion.Center,
            modifier = Modifier.Companion.padding(
                bottom = contentPadding.calculateBottomPadding(),
                start = 32.dp
            ),
        ) {
            Icon(
                Icons.Filled.Add,
                "",
                modifier = Modifier.Companion.size(42.dp)
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview() {
    AppTheme {
        UploadButton(PaddingValues()) {}
    }
}