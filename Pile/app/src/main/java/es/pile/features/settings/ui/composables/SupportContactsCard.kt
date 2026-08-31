package es.pile.features.settings.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pile.R

/** Instagram handle shown in the About & Support section. */
const val SUPPORT_INSTAGRAM = "@rahulsahux004"

/** GitHub handle shown in the About & Support section. */
const val SUPPORT_GITHUB = "codebyrahulsahu"

/** Support email shown in the About & Support section. */
const val SUPPORT_EMAIL = "kanhaiyalaljojawar@gmail.com"

const val SUPPORT_INSTAGRAM_URL = "https://www.instagram.com/rahulsahux004"
const val SUPPORT_GITHUB_URL = "https://github.com/codebyrahulsahu"

/**
 * Card that carries the support QR code plus every direct contact channel.
 * It is displayed straight on the settings page, no dialog needed.
 */
@Composable
fun SupportContactsCard(
    modifier: Modifier = Modifier,
    onOpenUrl: (String) -> Unit,
    onSendEmail: (String) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.support_qr),
                contentDescription = stringResource(R.string.support_qr_description),
                modifier = Modifier
                    .size(148.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Text(
                text = stringResource(R.string.support_qr_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )

            Spacer(Modifier.size(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SupportContactRow(
                icon = painterResource(R.drawable.ic_instagram),
                label = stringResource(R.string.instagram),
                value = SUPPORT_INSTAGRAM,
                onClick = { onOpenUrl(SUPPORT_INSTAGRAM_URL) }
            )

            SupportContactRow(
                icon = painterResource(R.drawable.ic_github),
                label = stringResource(R.string.github),
                value = SUPPORT_GITHUB,
                onClick = { onOpenUrl(SUPPORT_GITHUB_URL) }
            )

            SupportContactRow(
                icon = painterResource(R.drawable.mail_24px),
                label = stringResource(R.string.email),
                value = SUPPORT_EMAIL,
                onClick = { onSendEmail(SUPPORT_EMAIL) }
            )
        }
    }
}

@Composable
private fun SupportContactRow(
    icon: Painter,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AboutSupportSectionContent(
    modifier: Modifier = Modifier,
    onOpenUrl: (String) -> Unit,
    onSendEmail: (String) -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SupportContactsCard(
            onOpenUrl = onOpenUrl,
            onSendEmail = onSendEmail
        )
    }
}
