package it.maicol07.mmail.ui.views.mailbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.maicol07.mmail.data.RenderingOptions
import it.maicol07.mmail.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RenderingOptionsControls(options: RenderingOptions, onChange: (RenderingOptions) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RenderingSwitch(Res.string.load_images, Res.string.load_images_description, options.images) { onChange(options.copy(images = it)) }
        RenderingSwitch(Res.string.allow_javascript, Res.string.allow_javascript_description, options.javaScript) { onChange(options.copy(javaScript = it)) }
        RenderingSwitch(Res.string.external_styles, Res.string.external_styles_description, options.externalStyles) { onChange(options.copy(externalStyles = it)) }
    }
}

@Composable
private fun RenderingSwitch(title: StringResource, description: StringResource, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RenderingSettingsSheet(options: RenderingOptions, onChange: (RenderingOptions) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(Res.string.settings), style = MaterialTheme.typography.headlineSmallEmphasized)
            Text(stringResource(Res.string.rendering_defaults), style = MaterialTheme.typography.bodyMedium)
            RenderingOptionsControls(options, onChange)
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(Res.string.close)) }
        }
    }
}
