package com.hilight.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONArray
import org.json.JSONObject

/**
 * The answer to "why didn't my rule fire?".
 *
 * A per-conversation rule can only match on what the app actually puts in its notification, and apps
 * differ wildly: some set a `shortcutId` and a MessagingStyle sender, some set nothing but a title
 * with the server and channel packed into it. Nothing about that is visible from the outside, so
 * without this screen a rule that never fires is unexplainable — the user cannot tell a bug from an
 * app that simply gives HiLight no name to match on.
 *
 * **Privacy:** message text must never be shown here or leave the device through this screen. The
 * list holds whole notifications, so `MessageInfo.text` is deliberately absent from both the cards
 * and [peeksToJson]. This is the one screen whose whole purpose is to be pasted into a bug report,
 * which makes it the one screen where a private message would end up in a stranger's inbox. Only
 * names, ids and structural facts are surfaced; anything added here later must clear the same bar.
 */

/** Shown in the UI and repeated inside the export, so the guarantee travels with the data. */
private const val PRIVACY_NOTE =
    "Names, ids and structure only. Message text is never shown here and never exported."

/** What a field the app left empty reads as. */
private const val NONE = "none"

/**
 * The recent notifications HiLight has seen, as a dialog.
 *
 * A dialog rather than a destination because this is a diagnostic the user visits once, from the
 * notification access card, and it needs no navigation of its own.
 */
@Composable
fun NotificationInspectorDialog(store: Store, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val peeks by store.recentPeeks.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Notification inspector") },
        confirmButton = { TextButton(onClick = onDismiss) { ButtonLabel("Close") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelCard(tone = 3) {
                    Caption(PRIVACY_NOTE)
                    if (peeks.isNotEmpty()) {
                        Caption(
                            if (peeks.size == 1) "The last notification HiLight saw."
                            else "The last ${peeks.size} notifications HiLight saw, newest first."
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // built on the tap rather than on every recomposition: the list changes
                            // while the dialog is open, every time a notification arrives
                            Button(
                                onClick = { copyJson(ctx, peeksToJson(peeks)) },
                                modifier = Modifier.weight(1f),
                            ) { ButtonLabel("Copy as JSON") }
                            TextButton(
                                onClick = { shareJson(ctx, peeksToJson(peeks)) },
                                modifier = Modifier.weight(1f),
                            ) { ButtonLabel("Send") }
                        }
                    }
                }

                if (peeks.isEmpty()) {
                    PixelCard(tone = 3) {
                        SectionTitle("Nothing seen yet")
                        Caption(
                            "Notifications appear here as they arrive, so leave this open — or " +
                                "reopen it once one has landed."
                        )
                        Caption(
                            "Nothing is seen at all without notification access. The list is kept " +
                                "in memory only, so it empties whenever HiLight stops."
                        )
                    }
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        // no item key: the same notification is re-posted with the same key every
                        // time a chat updates, and duplicate keys are fatal to a lazy list
                        items(peeks) { peek -> PeekCard(peek) }
                    }
                }
            }
        },
    )
}

/** One notification, as the matcher sees it. */
@Composable
private fun PeekCard(peek: MessageInfo) {
    val name = peek.displayName
    PixelCard(tone = 3) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // the package is bounded so a long one wraps instead of squeezing the pill out of the row
            Text(
                peek.pkg,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(0.58f),
            )
            // The distinction that decides whether a rule is even writable, so it is the one thing
            // readable at a glance. A failed read gets its own state rather than being folded into
            // "no name": the two look identical in the fields below, and the advice for each is the
            // opposite of the other's.
            when {
                peek.readFailed -> LivePill("could not read", ok = false)
                name != null -> LivePill("name found", ok = true)
                else -> LivePill("no name", ok = false)
            }
        }
        // Stated before the fields rather than after them, because it changes how every line below
        // should be read: those are missing values, not values an app chose to leave out.
        if (peek.readFailed) {
            Caption(
                "This notification could not be read, so the fields below are missing rather than " +
                    "absent. That is a fault worth reporting with this output attached."
            )
        }
        Caption("Shortcut id: ${orNone(peek.shortcutId)}")
        Caption("Sender: ${orNone(peek.sender)}")
        Caption("Conversation: ${orNone(peek.conversationTitle)}")
        Caption("Title: ${orNone(peek.title)}")
        Caption("MessagingStyle: ${if (peek.isMessagingStyle) "detected" else "not detected"}")
        if (peek.isGroupSummary) {
            Caption("Group summary, which HiLight ignores so a chat cannot fire twice.")
        }
        // Reassurance only where it is true. A failed read has no name either, and telling the user
        // their app simply names nobody would send them away satisfied from a bug.
        if (name == null && !peek.readFailed) {
            Caption(
                "This app gave no name to match on, so no per-chat rule can be written from it. A " +
                    "rule for the whole app still works."
            )
        }
    }
}

/**
 * The list as JSON, for pasting into a bug report about a specific app.
 *
 * `MessageInfo.text` is absent by design — see the note at the top of this file — and the same
 * promise is written into the export, so whoever receives it can see what it does not contain. The
 * timestamps are here because they are what the re-post check reads, and a rule that fires once and
 * then never again is diagnosed from them.
 */
fun peeksToJson(peeks: List<MessageInfo>): String {
    val array = JSONArray()
    peeks.forEach { peek ->
        array.put(
            JSONObject().apply {
                put("pkg", peek.pkg)
                put("shortcutId", peek.shortcutId ?: JSONObject.NULL)
                put("sender", peek.sender ?: JSONObject.NULL)
                put("conversationTitle", peek.conversationTitle ?: JSONObject.NULL)
                put("title", peek.title ?: JSONObject.NULL)
                put("messagingStyle", peek.isMessagingStyle)
                put("groupSummary", peek.isGroupSummary)
                put("group", peek.isGroup)
                put("usableName", peek.displayName != null)
                // Without this a bug report of a failed read is indistinguishable from an app that
                // names nobody: both arrive as a package and a row of nulls. It is the one field
                // that says which of the two the reader is looking at.
                put("readFailed", peek.readFailed)
                put("messageStampMs", peek.messageStampMs)
                put("postTimeMs", peek.postTimeMs)
            }
        )
    }
    return JSONObject().apply {
        put("note", "HiLight notification inspector, newest first. $PRIVACY_NOTE")
        put("count", peeks.size)
        put("peeks", array)
    }.toString(2)
}

/** A field an app left empty is worth stating, since its absence is usually the whole answer. */
private fun orNone(value: String?): String = value?.takeIf { it.isNotBlank() } ?: NONE

private fun copyJson(ctx: Context, text: String) {
    ctx.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText("hilight-inspector", text))
    Toast.makeText(ctx, "Copied as JSON", Toast.LENGTH_SHORT).show()
}

private fun shareJson(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(send, "Send inspector output"))
}
