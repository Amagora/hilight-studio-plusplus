package com.hilight.studio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Dedicated Info Screen:
 * Version & Build Info, AI Disclosures, About & MIT License, End-to-End Self Test,
 * Notification Inspector trigger, Learned Conversations cache,
 * "Original Source" (original dev repo), and "This Build's Source" (user's fork).
 */
@Composable
fun InfoScreen(store: Store) {
    val ctx = LocalContext.current
    val conversations by store.conversations.collectAsStateWithLifecycle()

    var showAiDisclosure by remember { mutableStateOf(false) }
    var showLicense by remember { mutableStateOf(false) }
    var inspecting by remember { mutableStateOf(false) }
    var forgetting by remember { mutableStateOf(false) }

    // 1. Build Version & AI Disclosure Card
    PixelCard(tone = 0) {
        SectionTitle(
            stringResource(R.string.setup_ai_disclosure_title),
            trailing = {
                LivePill(
                    stringResource(
                        R.string.setup_updates_installed,
                        BuildConfig.VERSION_NAME,
                    ),
                    ok = true,
                )
            },
        )
        Caption(stringResource(R.string.setup_ai_disclosure_body))
        FilledTonalButton(
            onClick = { showAiDisclosure = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            ButtonLabel(stringResource(R.string.setup_ai_disclosure_button))
        }
    }

    // 2. About & Open Source License Card
    PixelCard {
        SectionTitle(stringResource(R.string.setup_license_title))
        Caption(stringResource(R.string.setup_license_body))
        FilledTonalButton(
            onClick = { showLicense = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            ButtonLabel(stringResource(R.string.setup_license_button))
        }
    }


    // 4. Diagnostics & Learned Conversations Card
    PixelCard {
        SectionTitle(stringResource(R.string.setup_notif_title))
        Caption(stringResource(R.string.setup_inspector_body))

        FilledTonalButton(
            onClick = { inspecting = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            ButtonLabel(stringResource(R.string.setup_inspector_button))
        }

        if (conversations.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { forgetting = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                ButtonLabel(stringResource(R.string.setup_forget_chats_button))
            }
        }
    }

    // 5. Original Source Card (Dhananjay Bhosale)
    PixelCard(tone = 2) {
        SectionTitle(stringResource(R.string.info_original_source_title))
        Caption(stringResource(R.string.info_original_source_body))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = { openUrl(ctx, "https://github.com/DhananjayBhosale/hilight-studio") },
                modifier = Modifier.weight(1f),
            ) {
                ButtonLabel(stringResource(R.string.info_original_source_button))
            }
            FilledTonalButton(
                onClick = { openUrl(ctx, "https://github.com/DhananjayBhosale") },
                modifier = Modifier.weight(1f),
            ) {
                ButtonLabel("Profile")
            }
        }
    }

    // 6. This Build's Source Card (Amagora Fork)
    PixelCard {
        SectionTitle(stringResource(R.string.info_this_build_source_title))
        Caption(stringResource(R.string.info_this_build_source_body))
        FilledTonalButton(
            onClick = { openUrl(ctx, "https://github.com/Amagora/hilight-studio") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            ButtonLabel(stringResource(R.string.info_this_build_source_button))
        }
    }

    // Dialog: AI Assistance Disclosures
    if (showAiDisclosure) {
        AiDisclosureDialog(onDismiss = { showAiDisclosure = false })
    }

    // Dialog: MIT License & Notice
    if (showLicense) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.setup_license_dialog_title)) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "MIT License\n\nCopyright (c) 2024-2026 Dhananjay Bhosale & Contributors\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicense = false }) {
                    ButtonLabel(stringResource(R.string.common_close))
                }
            },
        )
    }

    // Dialog: Notification Inspector
    if (inspecting) {
        NotificationInspectorDialog(store = store, onDismiss = { inspecting = false })
    }

    // Dialog: Forget Learned Chats
    if (forgetting) {
        AlertDialog(
            onDismissRequest = { forgetting = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.setup_forget_chats_title)) },
            text = { Text(stringResource(R.string.setup_forget_chats_body, conversations.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        store.forgetConversations()
                        forgetting = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    ButtonLabel(stringResource(R.string.setup_forget_chats_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { forgetting = false }) {
                    ButtonLabel(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

private fun openUrl(ctx: Context, url: String) {
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(ctx, "Could not open URL: $url", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun AiDisclosureDialog(
    onDismiss: () -> Unit,
    confirmButtonText: String = stringResource(R.string.setup_ai_disclosure_dialog_understand),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.setup_ai_disclosure_dialog_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "This fork (HiLight Studio PlusPlus) was enhanced, redesigned, and maintained with the assistance of Advanced AI code generation and pair-programming tools.\n\nAll modifications build upon Dhananjay Bhosale's original architecture to deliver expanded patterns, rich per-LED and multi-color controls, battery guards, and streamlined tools.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                ButtonLabel(confirmButtonText)
            }
        },
    )
}
