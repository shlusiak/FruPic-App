package de.saschahlusiak.frupic.upload

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.utils.asFlow
import de.saschahlusiak.frupic.utils.sizeString

@Composable
fun UploadScreen(
    viewModel: UploadActivityViewModel,
    onDismiss: () -> Unit,
    onUpload: (username: String, tags: String, resized: Boolean) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        val state = rememberScrollState()
        var username by rememberSaveable { mutableStateOf(viewModel.username) }
        var tags by rememberSaveable { mutableStateOf("") }
        var resize by rememberSaveable { mutableStateOf(true) }
        var canResize by rememberSaveable { mutableStateOf(true) }

        val originals by viewModel.originals.asFlow().collectAsStateWithLifecycle(null)
        val resized by viewModel.resized.asFlow().collectAsStateWithLifecycle(null)

        val selected by remember { derivedStateOf { if (resize) resized else originals } }
        val okEnabled by remember { derivedStateOf { selected != null } }

        LaunchedEffect(resized, originals) {
            val resized = resized ?: return@LaunchedEffect
            val originals = originals ?: return@LaunchedEffect
            if (resized.sumOf { it.size } >= originals.sumOf { it.size } * 0.9) {
                canResize = false
                resize = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state)
                .padding(vertical = 16.dp),
            verticalArrangement = spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.upload_title),
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .height(200.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected == null) {
                    CircularProgressIndicator()
                }

                Spacer(modifier = Modifier.width(16.dp))

                selected?.forEach { image ->
                    ImagePreview(image, Modifier.padding(horizontal = 4.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))
            }

            Row(
                horizontalArrangement = spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(stringResource(R.string.upload_posted_by_label)) }
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(stringResource(R.string.upload_tags_hint)) }
                )
            }

            Row(
                modifier = Modifier
                    .clickable(enabled = canResize, onClick = { resize = !resize })
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.image_resize_label),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
//                Checkbox(resize, { resize = it })
                Switch(resize, { resize = it })
            }

            Row(
                horizontalArrangement = spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                val size = selected?.sumOf { it.size }
                val count = selected?.count()
                Column {
                    if (count != null) {
                        val s = pluralStringResource(R.plurals.files, count, count)
                        Text(
                            s,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Start
                        )
                    }
                    if (size != null) {
                        Text(
                            size.sizeString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (resize || !canResize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    horizontalArrangement = spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {
                    TextButton(onDismiss, Modifier.weight(1f)) {
                        Text(stringResource(R.string.cancel))
                    }

                    Button(
                        onClick = { onUpload(username, tags, resize) },
                        enabled = okEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.upload))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePreview(image: PreparedImage, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        AsyncImage(
            image.path, "",
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit
        )

        AnimatedContent(image) { image ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${image.width}x${image.height}",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary
                )

                Text(
                    image.size.sizeString(),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
