package it.maicol07.mmail.ui.views.mailbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SendW500Rounded
import it.maicol07.mmail.data.isValidRecipient
import it.maicol07.mmail.resources.*
import it.maicol07.mmail.ui.components.MailAction
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Composer(
    recipient: String, subject: String, body: String,
    onRecipient: (String) -> Unit, onSubject: (String) -> Unit, onBody: (String) -> Unit,
    onClose: () -> Unit, onSend: () -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().fillMaxHeight(0.9f).imePadding(),
            shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.new_message), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    MailAction(Icons.CloseW500Rounded, stringResource(Res.string.close), onClose)
                }
                Text(
                    stringResource(Res.string.draft_notice), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = recipient, onValueChange = onRecipient, label = { Text(stringResource(Res.string.recipient)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = recipient.isNotBlank() && !isValidRecipient(recipient),
                        supportingText = {
                            if (recipient.isNotBlank() && !isValidRecipient(recipient)) Text(stringResource(Res.string.invalid_recipient))
                        },
                    )
                    OutlinedTextField(
                        value = subject, onValueChange = onSubject, label = { Text(stringResource(Res.string.subject)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                    )
                    OutlinedTextField(
                        value = body, onValueChange = onBody, placeholder = { Text(stringResource(Res.string.body)) },
                        modifier = Modifier.fillMaxWidth(), minLines = 7,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(Res.string.send_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = onSend, enabled = isValidRecipient(recipient) && subject.isNotBlank() && body.isNotBlank(),
                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
                ) {
                    Icon(Icons.SendW500Rounded, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.send_demo))
                }
            }
        }
    }
}
