package it.maicol07.mmail.ui.views.mailbox

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.*
import it.maicol07.mmail.data.*
import it.maicol07.mmail.resources.*
import it.maicol07.mmail.ui.components.MailAction
import it.maicol07.mmail.ui.components.SenderAvatar
import it.maicol07.mmail.ui.components.AttachmentChip
import it.maicol07.mmail.ui.components.mailSharedBounds
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SharedTransitionScope.MessageList(
    messages: List<Mail>, folder: Folder, filter: MailFilter, query: String, selectedId: Int?,
    onOpen: (Mail) -> Unit, onStar: (Int) -> Unit, onAttachment: () -> Unit,
    scrollState: LazyListState, visibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
) {
    val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    Column(modifier) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(folder.title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(Res.string.message_count, messages.size) + " · " +
                    stringResource(Res.string.unread_count, messages.count { it.unread }),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (messages.isEmpty()) {
            MailEmptyState(
                Icons.SearchW500Rounded,
                stringResource(if (query.isBlank() && filter == MailFilter.All) Res.string.no_messages else Res.string.no_results),
                stringResource(if (query.isBlank() && filter == MailFilter.All) Res.string.empty_folder else Res.string.try_search),
                Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                state = scrollState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                itemsIndexed(messages, key = { _, mail -> mail.id }) { index, mail ->
                    val shape = RoundedCornerShape(
                        topStart = if (index == 0) 24.dp else 6.dp,
                        topEnd = if (index == 0) 24.dp else 6.dp,
                        bottomStart = if (index == messages.lastIndex) 24.dp else 6.dp,
                        bottomEnd = if (index == messages.lastIndex) 24.dp else 6.dp,
                    )
                    val selected = selectedId == mail.id
                    val containerColor by animateColorAsState(
                        targetValue = when {
                            selected -> MaterialTheme.colorScheme.secondaryContainer
                            mail.unread -> MaterialTheme.colorScheme.surfaceContainerLowest
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    )
                    Surface(
                        shape = shape,
                        color = containerColor,
                        modifier = Modifier.fillMaxWidth().animateItem(
                            fadeInSpec = fadeSpec, placementSpec = placementSpec, fadeOutSpec = fadeSpec,
                        ).semantics { this.selected = selected },
                    ) {
                        Row(
                            Modifier.clickable(onClick = { onOpen(mail) }).padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SenderAvatar(mail.sender, mail.id, mailSharedBounds(mail.id to "avatar", visibilityScope))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        mail.sender, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (mail.unread) FontWeight.Bold else FontWeight.Medium,
                                    )
                                    if (mail.unread) Box(Modifier.padding(horizontal = 6.dp).size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                    Text(mail.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    mail.subject, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = mailSharedBounds(mail.id to "subject", visibilityScope),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (mail.unread) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                Text(
                                    mail.body.replace("\n", " "), maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                mail.attachment?.let { attachment ->
                                    AttachmentChip(attachment, onAttachment, mailSharedBounds(mail.id to "attachment", visibilityScope))
                                }
                            }
                            MailAction(
                                if (mail.starred) Icons.StarW500RoundedFill else Icons.StarW500Rounded,
                                stringResource(if (mail.starred) Res.string.remove_star else Res.string.add_star),
                                onClick = { onStar(mail.id) },
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun MailEmptyState(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = MaterialShapes.Cookie9Sided.toShape(), color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(icon, null, modifier = Modifier.padding(28.dp).size(48.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
