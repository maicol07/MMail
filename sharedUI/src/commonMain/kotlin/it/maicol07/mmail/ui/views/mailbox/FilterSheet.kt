package it.maicol07.mmail.ui.views.mailbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.maicol07.mmail.data.MailFilter
import it.maicol07.mmail.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch

val MailFilter.title: StringResource
    get() = when (this) {
        MailFilter.All -> Res.string.all
        MailFilter.Unread -> Res.string.unread
        MailFilter.Attachments -> Res.string.attachments
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilterSheet(filter: MailFilter, onFilter: (MailFilter) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(Res.string.filters), style = MaterialTheme.typography.headlineSmallEmphasized)
            Text(stringResource(Res.string.filter_description), style = MaterialTheme.typography.bodyMedium)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MailFilter.entries.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (filter == item) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().selectable(filter == item, role = Role.RadioButton, onClick = { onFilter(item) })
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            RadioButton(selected = filter == item, onClick = null)
                            Text(stringResource(item.title), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            Button(
                onClick = { scope.launch { sheetState.hide(); onDismiss() } },
                shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.show_messages))
            }
        }
    }
}
