package it.maicol07.mmail

import androidx.compose.animation.*
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.*
import it.maicol07.mmail.data.*
import it.maicol07.mmail.resources.*
import it.maicol07.mmail.ui.components.MailAction
import it.maicol07.mmail.ui.theme.AppTheme
import it.maicol07.mmail.ui.views.mailbox.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App() {
    // shortcut: demo state is session-only; introduce persistence with real mail providers.
    var mailbox by remember { mutableStateOf(MailboxState()) }
    var folder by remember { mutableStateOf(Folder.Inbox) }
    var filter by remember { mutableStateOf(MailFilter.All) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var composing by rememberSaveable { mutableStateOf(false) }
    var showingFilters by rememberSaveable { mutableStateOf(false) }
    var recipient by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listScrollState = rememberLazyListState()
    LaunchedEffect(folder, filter, query) { listScrollState.scrollToItem(0) }

    AppTheme(isSystemInDarkTheme()) {
        val archived = stringResource(Res.string.archived)
        val trashed = stringResource(Res.string.trashed)
        val restored = stringResource(Res.string.restored)
        val undo = stringResource(Res.string.undo)
        val attachmentNotice = stringResource(Res.string.attachment_notice)
        val sentNotice = stringResource(Res.string.message_sent)
        val draftResumed = stringResource(Res.string.draft_resumed)
        val messages = mailbox.messagesIn(folder, query, filter)
        val selected = mailbox.messages.find { it.id == selectedId }
        val selectFolder: (Folder) -> Unit = {
            folder = it
            filter = MailFilter.All
            selectedId = null
            scope.launch { drawerState.close() }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val permanentNavigation = maxWidth >= 1200.dp
            val twoPanes = maxWidth >= 840.dp
            PlatformBackHandler(!composing && !showingFilters && selectedId != null && !twoPanes) { selectedId = null }
            ModalNavigationDrawer(
                drawerState = drawerState, gesturesEnabled = !permanentNavigation,
                drawerContent = {
                    ModalDrawerSheet {
                        FolderNavigation(mailbox, folder, selectFolder, {
                            scope.launch { drawerState.close() }
                            composing = true
                        }, Modifier.width(280.dp).fillMaxHeight())
                    }
                },
            ) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surface,
                    snackbarHost = { SnackbarHost(snackbar) },
                    floatingActionButton = {
                        if (!permanentNavigation && (selected == null || twoPanes)) {
                            ExtendedFloatingActionButton(
                                onClick = { composing = true },
                                icon = { Icon(Icons.EditW500Rounded, null) },
                                text = { Text(stringResource(Res.string.compose)) },
                            )
                        }
                    },
                ) { padding ->
                    Row(Modifier.fillMaxSize().padding(padding)) {
                        if (permanentNavigation) {
                            FolderNavigation(mailbox, folder, selectFolder, { composing = true }, Modifier.width(264.dp).fillMaxHeight())
                        }
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (!permanentNavigation) {
                                    MailAction(Icons.MenuW500Rounded, stringResource(Res.string.menu), { scope.launch { drawerState.open() } })
                                }
                                OutlinedTextField(
                                    value = query, onValueChange = { query = it },
                                    modifier = Modifier.weight(1f), shape = CircleShape, singleLine = true,
                                    placeholder = { Text(stringResource(Res.string.search)) },
                                    leadingIcon = { Icon(Icons.SearchW500Rounded, null) },
                                    trailingIcon = {
                                        if (query.isNotEmpty()) MailAction(Icons.CloseW500Rounded, stringResource(Res.string.clear_search), { query = "" })
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                )
                                BadgedBox(badge = { if (filter != MailFilter.All) Badge() }) {
                                    MailAction(
                                        Icons.FilterListW500Rounded,
                                        if (filter == MailFilter.All) stringResource(Res.string.filters)
                                        else stringResource(Res.string.filters_active, stringResource(filter.title)),
                                        { showingFilters = true },
                                    )
                                }
                            }
                            SharedTransitionLayout(Modifier.weight(1f).padding(end = if (twoPanes) 16.dp else 0.dp, bottom = 12.dp)) {
                                val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                                val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                                val transition = updateTransition(selectedId, label = "Mail navigation")
                                // Mark read after opening so unread filtering does not remove the shared source mid-transition.
                                LaunchedEffect(transition.currentState) {
                                    transition.currentState?.let { mailbox = mailbox.markRead(it) }
                                }
                                val listContent: @Composable (AnimatedVisibilityScope?, Modifier) -> Unit = { visibilityScope, modifier ->
                                    MessageList(
                                        messages, folder, filter, query, if (twoPanes) selectedId else null,
                                        onOpen = { mail -> selectedId = mail.id },
                                        onStar = { mailbox = mailbox.toggleStar(it) },
                                        onAttachment = { scope.launch { snackbar.showSnackbar(attachmentNotice) } },
                                        scrollState = listScrollState, visibilityScope = visibilityScope, modifier = modifier,
                                    )
                                }
                                Row(Modifier.fillMaxSize()) {
                                    if (twoPanes) listContent(null, Modifier.width(380.dp).fillMaxHeight())
                                    transition.AnimatedContent(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        contentKey = { it },
                                        transitionSpec = {
                                            (fadeIn(fadeSpec) + slideInHorizontally(spatialSpec) {
                                                if (targetState == null) -it / 12 else it / 12
                                            } togetherWith fadeOut(fadeSpec)).using(null)
                                        },
                                    ) { messageId ->
                                        val mail = mailbox.messages.find { it.id == messageId }
                                        if (!twoPanes && mail == null) {
                                            listContent(this, Modifier.fillMaxSize())
                                        } else if (mail != null) {
                                            MessageDetail(
                                                mail, !twoPanes, onBack = { selectedId = null },
                                                onStar = { mailbox = mailbox.toggleStar(mail.id) },
                                                onMove = { destination ->
                                                    val previousFolder = mail.folder
                                                    mailbox = mailbox.move(mail.id, destination)
                                                    selectedId = null
                                                    scope.launch {
                                                        val result = snackbar.showSnackbar(
                                                            when (destination) {
                                                                Folder.Archive -> archived
                                                                Folder.Trash -> trashed
                                                                else -> restored
                                                            }, actionLabel = undo,
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) mailbox = mailbox.move(mail.id, previousFolder)
                                                    }
                                                },
                                                onReply = {
                                                    if (recipient.isBlank() && subject.isBlank() && body.isBlank()) {
                                                        recipient = mail.address
                                                        subject = if (mail.subject.startsWith("Re: ")) mail.subject else "Re: ${mail.subject}"
                                                    } else scope.launch { snackbar.showSnackbar(draftResumed) }
                                                    composing = true
                                                },
                                                onAttachment = { scope.launch { snackbar.showSnackbar(attachmentNotice) } },
                                                visibilityScope = if (twoPanes) null else this,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            Surface(
                                                modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(28.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            ) {
                                                MailEmptyState(
                                                    Icons.MailW500Rounded, stringResource(Res.string.select_message),
                                                    stringResource(Res.string.select_message_hint), Modifier.fillMaxSize(),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showingFilters) FilterSheet(
            filter = filter,
            onFilter = { filter = it; selectedId = null },
            onDismiss = { showingFilters = false },
        )
        if (composing) Composer(
            recipient, subject, body,
            onRecipient = { recipient = it }, onSubject = { subject = it }, onBody = { body = it },
            onClose = { composing = false },
            onSend = {
                mailbox = mailbox.send(recipient, subject, body)
                composing = false
                recipient = ""
                subject = ""
                body = ""
                scope.launch { snackbar.showSnackbar(sentNotice) }
            },
        )
    }
}
