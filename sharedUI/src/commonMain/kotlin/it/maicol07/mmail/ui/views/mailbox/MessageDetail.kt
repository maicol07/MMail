package it.maicol07.mmail.ui.views.mailbox

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.*
import it.maicol07.mmail.data.Folder
import it.maicol07.mmail.data.Mail
import it.maicol07.mmail.resources.*
import it.maicol07.mmail.ui.components.MailAction
import it.maicol07.mmail.ui.components.SenderAvatar
import it.maicol07.mmail.ui.components.AttachmentChip
import it.maicol07.mmail.ui.components.mailSharedBounds
import org.jetbrains.compose.resources.stringResource

@Composable
fun SharedTransitionScope.MessageDetail(
    mail: Mail, showBack: Boolean, onBack: () -> Unit, onStar: () -> Unit,
    onMove: (Folder) -> Unit, onReply: () -> Unit, onAttachment: () -> Unit,
    visibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (showBack) MailAction(Icons.ArrowBackW500Rounded, stringResource(Res.string.back), onBack)
                if (mail.folder == Folder.Archive || mail.folder == Folder.Trash) {
                    MailAction(Icons.InboxW500Rounded, stringResource(Res.string.restore), { onMove(Folder.Inbox) })
                } else {
                    MailAction(Icons.ArchiveW500Rounded, stringResource(Res.string.archive_action), { onMove(Folder.Archive) })
                }
                if (mail.folder != Folder.Trash) {
                    MailAction(Icons.DeleteW500Rounded, stringResource(Res.string.delete_action), { onMove(Folder.Trash) })
                }
                Spacer(Modifier.weight(1f))
                MailAction(
                    if (mail.starred) Icons.StarW500RoundedFill else Icons.StarW500Rounded,
                    stringResource(if (mail.starred) Res.string.remove_star else Res.string.add_star), onStar,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
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
                Text(mail.body, style = MaterialTheme.typography.bodyLarge)
                if (mail.hasAttachments) {
                    AttachmentChip(stringResource(Res.string.open_attachments), onAttachment, mailSharedBounds(mail.id to "attachment", visibilityScope))
                }
                if (mail.folder != Folder.Sent && mail.folder != Folder.Trash) {
                    FilledTonalButton(onClick = onReply, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.ReplyW500Rounded, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.reply))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
