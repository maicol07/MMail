package it.maicol07.mmail.ui.views.mailbox

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.*
import it.maicol07.mmail.data.Folder
import it.maicol07.mmail.data.Mail
import it.maicol07.mmail.data.RenderingOptions
import it.maicol07.mmail.resources.*
import it.maicol07.mmail.ui.components.MailAction
import it.maicol07.mmail.ui.components.SenderAvatar
import it.maicol07.mmail.ui.components.AttachmentChip
import it.maicol07.mmail.ui.components.MessageBody
import it.maicol07.mmail.ui.components.mailSharedBounds
import org.jetbrains.compose.resources.stringResource

@Composable
fun SharedTransitionScope.MessageDetail(
    mail: Mail, onAttachment: () -> Unit,
    visibilityScope: AnimatedVisibilityScope?,
    renderingDefaults: RenderingOptions = RenderingOptions(),
    modifier: Modifier = Modifier,
) {
    var images by rememberSaveable(mail.id, renderingDefaults.images) { mutableStateOf(renderingDefaults.images) }
    var javaScript by rememberSaveable(mail.id, renderingDefaults.javaScript) { mutableStateOf(renderingDefaults.javaScript) }
    var externalStyles by rememberSaveable(mail.id, renderingDefaults.externalStyles) { mutableStateOf(renderingDefaults.externalStyles) }
    var contentOptions by rememberSaveable(mail.id) { mutableStateOf(false) }
    var plainText by rememberSaveable(mail.id) { mutableStateOf(false) }
    val options = RenderingOptions(images, javaScript, externalStyles)
    Surface(modifier, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                mail.subject, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Medium,
                modifier = mailSharedBounds(mail.id to "subject", visibilityScope),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SenderAvatar(mail.sender, mail.id.hashCode(), mailSharedBounds(mail.id to "avatar", visibilityScope))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(mail.sender, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(mail.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(Res.string.to_recipient, mail.recipient), style = MaterialTheme.typography.bodySmall)
                }
                Text(mail.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val html = mail.htmlBody
            if (html != null) {
                Column {
                    TextButton(onClick = { contentOptions = !contentOptions }) { Text(stringResource(Res.string.content_options)) }
                    if (contentOptions) {
                        Text(stringResource(Res.string.message_content_scope), style = MaterialTheme.typography.bodySmall)
                        RenderingOptionsControls(options) { images = it.images; javaScript = it.javaScript; externalStyles = it.externalStyles }
                    }
                    TextButton(onClick = { plainText = !plainText }) {
                        Text(stringResource(if (plainText) Res.string.show_html else Res.string.show_plain_text))
                    }
                }
            }
            MessageBody(mail.body, html, plainText, options, Modifier.fillMaxWidth())
            if (mail.hasAttachments) {
                AttachmentChip(stringResource(Res.string.open_attachments), onAttachment, mailSharedBounds(mail.id to "attachment", visibilityScope))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun MessageToolbar(
    mail: Mail, onBack: () -> Unit, onStar: () -> Unit,
    onMove: (Folder) -> Unit, onReply: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MailAction(Icons.ArrowBackW500Rounded, stringResource(Res.string.back), onBack)
        if (mail.folder == Folder.Archive || mail.folder == Folder.Trash) {
            MailAction(Icons.InboxW500Rounded, stringResource(Res.string.restore), { onMove(Folder.Inbox) })
        } else {
            MailAction(Icons.ArchiveW500Rounded, stringResource(Res.string.archive_action), { onMove(Folder.Archive) })
        }
        if (mail.folder != Folder.Trash) {
            MailAction(Icons.DeleteW500Rounded, stringResource(Res.string.delete_action), { onMove(Folder.Trash) })
        }
        if (mail.folder != Folder.Sent && mail.folder != Folder.Trash) {
            MailAction(Icons.ReplyW500Rounded, stringResource(Res.string.reply), onReply)
        }
        Spacer(Modifier.weight(1f))
        MailAction(
            if (mail.starred) Icons.StarW500RoundedFill else Icons.StarW500Rounded,
            stringResource(if (mail.starred) Res.string.remove_star else Res.string.add_star), onStar,
        )
    }
}
