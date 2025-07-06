package de.saschahlusiak.frupic.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.saschahlusiak.frupic.BuildConfig
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.utils.AppTheme

@Composable
fun AboutScreen(
    onLink: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val marketUrl = "https://play.google.com/store/apps/details?id=de.saschahlusiak.frupic"
    val githubLink = "https://github.com/shlusiak/FruPic-App"
    val state = rememberScrollState()

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier
                .verticalScroll(state)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.frupic),
                modifier = Modifier.size(80.dp),
                contentDescription = null
            )

            Text(
                "v" + BuildConfig.VERSION_NAME,
                textAlign = TextAlign.Left,
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )


            Text(
                stringResource(id = R.string.copyright),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )

            val email = "mail@saschahlusiak.de"
            TextButton(onClick = { onLink("mailto:$email") }) {
                Text(
                    text = email,
                    textAlign = TextAlign.Center
                )
            }

            TextButton(onClick = { onLink(marketUrl) }) {
                Text(
                    marketUrl,
                    textAlign = TextAlign.Center
                )
            }

            TextButton(
                onClick = { onLink(githubLink) },
            ) {
                Text(
                    text = githubLink,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .width(intrinsicSize = IntrinsicSize.Min)
            ) {
                Button(onClick = onDismiss) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        }
    }
}

@Composable
@Preview
fun Preview() {
    AppTheme {
        AboutScreen({}, {})
    }
}