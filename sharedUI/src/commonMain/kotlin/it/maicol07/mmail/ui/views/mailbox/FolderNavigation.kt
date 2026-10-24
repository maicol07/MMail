package it.maicol07.mmail.ui.views.mailbox

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
import it.maicol07.mmail.data.MailboxState
import it.maicol07.mmail.data.MailFilter
import it.maicol07.mmail.resources.*
import it.maicol07.mmail.ui.components.SenderAvatar
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val Folder.title: StringResource
    get() = when (this) {
        Folder.Inbox -> Res.string.inbox
        Folder.Starred -> Res.string.starred
        Folder.Sent -> Res.string.sent
        Folder.Archive -> Res.string.archive
        Folder.Trash -> Res.string.trash
    }

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun FolderNavigation(
    mailbox: MailboxState,
    selected: Folder,
    onSelect: (Folder) -> Unit,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(Icons.MailW500Rounded, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onCompose,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shapes = ButtonDefaults.shapes(shape = RoundedCornerShape(20.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(Icons.EditW500Rounded, null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(Res.string.compose), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(Res.string.personal_mail), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp),
        )
        Folder.entries.forEach { folder ->
            val count = mailbox.messagesIn(folder, "", MailFilter.All).size
            NavigationDrawerItem(
                label = { Text(stringResource(folder.title)) },
                selected = selected == folder,
                onClick = { onSelect(folder) },
                icon = {
                    Icon(when (folder) {
                        Folder.Inbox -> Icons.InboxW500Rounded
                        Folder.Starred -> Icons.StarW500Rounded
                        Folder.Sent -> Icons.SendW500Rounded
                        Folder.Archive -> Icons.ArchiveW500Rounded
                        Folder.Trash -> Icons.DeleteW500Rounded
                    }, null)
                },
                badge = { if (count > 0) Text(count.toString(), style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(40.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.demo_badge), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(Res.string.demo_note), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(Res.string.demo_explanation), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SenderAvatar("Maicol", 0)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Maicol", style = MaterialTheme.typography.titleSmall)
                Text(stringResource(Res.string.demo_account), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
