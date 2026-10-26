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
import androidx.compose.ui.window.Dialog
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
fun App(provider: MailProvider) {
    var mailbox by remember { mutableStateOf(MailboxState()) }
    var account by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<MailFailure?>(null) }
    var folder by remember { mutableStateOf(Folder.Inbox) }
    var filter by remember { mutableStateOf(MailFilter.All) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var replyTo by rememberSaveable { mutableStateOf<String?>(null) }
    var composing by rememberSaveable { mutableStateOf(false) }
    var showingFilters by rememberSaveable { mutableStateOf(false) }
    var recipient by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    suspend fun refreshMailbox() {
        var loaded = provider.loadMailbox()
        val selected = loaded.messages.find { it.id == selectedId }
        if (selected != null) {
            val full = provider.loadMessage(selected)
            loaded = loaded.copy(messages = loaded.messages.map { if (it.id == full.id) full else it })
        } else selectedId = null
        mailbox = loaded
    }
    val perform: (suspend () -> Unit) -> Unit = { operation ->
        if (!busy) {
            busy = true
            failure = null
            scope.launch {
                try {
                    operation()
                } catch (exception: MailProviderException) {
                    failure = exception.failure
                } finally {
                    busy = false
                }
            }
        }
    }
    LaunchedEffect(provider) {
        perform {
            account = provider.restoreAccount()
            if (account != null) refreshMailbox()
        }
    }
    val listScrollState = rememberLazyListState()
    LaunchedEffect(folder, filter, query) { listScrollState.scrollToItem(0) }

    AppTheme(isSystemInDarkTheme()) {
        if (busy) Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(if (account == null) Res.string.working else Res.string.loading_mail))
                }
            }
        }
        if (account == null) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(24.dp))
                Text(stringResource(if (provider.configured) Res.string.connect_outlook else Res.string.outlook_configuration_required))
                failure?.let { Text(stringResource(it.title), color = MaterialTheme.colorScheme.error) }
                Button(onClick = { perform { account = provider.signIn(); refreshMailbox() } }, enabled = provider.configured && !busy) {
                    Text(stringResource(Res.string.sign_in_outlook))
                }
            }
            return@AppTheme
        }
        val archived = stringResource(Res.string.archived)
        val trashed = stringResource(Res.string.trashed)
        val restored = stringResource(Res.string.restored)
        val undo = stringResource(Res.string.undo)
        val sentNotice = stringResource(Res.string.message_sent)
        val draftResumed = stringResource(Res.string.draft_resumed)
        val messages = mailbox.messagesIn(folder, query, filter)
        val selected = mailbox.messages.find { it.id == selectedId }
        val toggleStar: (String) -> Unit = { id ->
            mailbox.messages.find { it.id == id }?.let { mail ->
                perform { provider.setStarred(id, !mail.starred); mailbox = mailbox.toggleStar(id) }
            }
        }
        val signOut: () -> Unit = {
            perform {
                provider.signOut()
                snackbar.currentSnackbarData?.dismiss()
                mailbox = MailboxState()
                account = null
                selectedId = null
                composing = false
                recipient = ""
                subject = ""
                body = ""
                replyTo = null
                query = ""
            }
        }
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
                        }, account.orEmpty(), signOut, { perform { refreshMailbox() } }, Modifier.width(280.dp).fillMaxHeight())
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
                            FolderNavigation(mailbox, folder, selectFolder, { composing = true }, account.orEmpty(), signOut,
                                { perform { refreshMailbox() } }, Modifier.width(264.dp).fillMaxHeight())
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
                            failure?.let { error ->
                                Text(stringResource(error.title), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                                if (error == MailFailure.Authentication) TextButton(onClick = {
                                    perform { account = provider.signIn(); refreshMailbox() }
                                }) { Text(stringResource(Res.string.sign_in_outlook)) }
                            }
                            SharedTransitionLayout(Modifier.weight(1f).padding(end = if (twoPanes) 16.dp else 0.dp, bottom = 12.dp)) {
                                val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                                val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                                val transition = updateTransition(selectedId, label = "Mail navigation")
                                // Mark read after opening so unread filtering does not remove the shared source mid-transition.
                                LaunchedEffect(transition.currentState) {
                                    mailbox.messages.find { it.id == transition.currentState && it.unread }?.let { mail ->
                                        perform { provider.markRead(mail.id); mailbox = mailbox.markRead(mail.id) }
                                    }
                                }
                                val listContent: @Composable (AnimatedVisibilityScope?, Modifier) -> Unit = { visibilityScope, modifier ->
                                    MessageList(
                                        messages, folder, filter, query, if (twoPanes) selectedId else null,
                                        onOpen = { mail -> perform {
                                            val full = provider.loadMessage(mail)
                                            mailbox = mailbox.copy(messages = mailbox.messages.map { if (it.id == full.id) full else it })
                                            selectedId = full.id
                                        } },
                                        onStar = toggleStar,
                                        onAttachment = { mail -> perform { provider.openAttachments(mail) } },
                                        hasMore = mailbox.hasMore(folder),
                                        onLoadMore = { perform { mailbox = provider.loadMore(mailbox, folder) } },
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
                                                onStar = { toggleStar(mail.id) },
                                                onMove = { destination ->
                                                    val previousFolder = mail.folder
                                                    perform {
                                                        provider.move(mail.id, destination)
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
                                                            if (result == SnackbarResult.ActionPerformed) perform {
                                                                provider.move(mail.id, previousFolder)
                                                                mailbox = mailbox.move(mail.id, previousFolder)
                                                            }
                                                        }
                                                    }
                                                },
                                                onReply = {
                                                    if (recipient.isBlank() && subject.isBlank() && body.isBlank()) {
                                                        recipient = mail.address
                                                        subject = if (mail.subject.startsWith("Re: ")) mail.subject else "Re: ${mail.subject}"
                                                        replyTo = mail.id
                                                    } else scope.launch { snackbar.showSnackbar(draftResumed) }
                                                    composing = true
                                                },
                                                onAttachment = { perform { provider.openAttachments(mail) } },
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
            error = failure?.let { stringResource(it.title) },
            enabled = !busy,
            onRecipient = { recipient = it }, onSubject = { subject = it }, onBody = { body = it },
            onClose = { if (!busy) composing = false },
            onSend = {
                perform {
                    provider.send(recipient, subject, body, replyTo)
                    composing = false
                    recipient = ""
                    subject = ""
                    body = ""
                    replyTo = null
                    scope.launch { snackbar.showSnackbar(sentNotice) }
                    refreshMailbox()
                }
            },
        )
    }
}

private val MailFailure.title: org.jetbrains.compose.resources.StringResource
    get() = when (this) {
        MailFailure.Authentication -> Res.string.error_authentication
        MailFailure.Network -> Res.string.error_network
        MailFailure.Throttled -> Res.string.error_throttled
        MailFailure.Rejected -> Res.string.error_rejected
        MailFailure.InvalidResponse -> Res.string.error_response
        MailFailure.SendUncertain -> Res.string.error_send_uncertain
    }
