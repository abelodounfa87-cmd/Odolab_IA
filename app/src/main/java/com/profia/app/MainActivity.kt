package com.profia.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfTheme { ProfApp() }
        }
    }
}

@Composable
fun ProfApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("profia", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString("key", "") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("model", "gemini-2.5-flash") ?: "gemini-2.5-flash") }
    var subject by remember { mutableStateOf("") }
    val uris = remember { mutableStateListOf<Uri>() }
    var status by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { picked -> uris.addAll(picked) }

    if (showSettings) {
        SettingsScreen(
            initialKey = apiKey,
            initialModel = model,
            onSave = { k, m ->
                apiKey = k
                model = m
                prefs.edit().putString("key", k).putString("model", m).apply()
            },
            onBack = { showSettings = false }
        )
        return
    }

    HomeScreen(
        hasKey = apiKey.isNotBlank(),
        uris = uris,
        subject = subject,
        onSubject = { subject = it },
        busy = busy,
        status = status,
        summary = summary,
        onSettings = { showSettings = true },
        onPick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onSummarize = {
            busy = true
            summary = ""
            scope.launch {
                try {
                    summary = Summarizer.run(
                        ctx, uris.toList(), apiKey.trim(), model.trim(), subject.trim()
                    ) { status = it }
                    status = "Résumé terminé"
                } catch (e: Exception) {
                    status = "Erreur : ${e.message}"
                }
                busy = false
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hasKey: Boolean,
    uris: MutableList<Uri>,
    subject: String,
    onSubject: (String) -> Unit,
    busy: Boolean,
    status: String,
    summary: String,
    onSettings: () -> Unit,
    onPick: () -> Unit,
    onSummarize: () -> Unit
) {
    val ctx = LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("ODOLAB AI", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Paramètres")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!hasKey) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Clé API manquante",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Ajoute ta clé Gemini pour lancer un résumé.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        TextButton(onClick = onSettings) { Text("Ajouter") }
                    }
                }
            }

            Section("Cours") {
                OutlinedTextField(
                    value = subject,
                    onValueChange = onSubject,
                    label = { Text("Matière (optionnel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (uris.isEmpty()) "Aucune page ajoutée"
                    else "${uris.size} page(s), dans l'ordre du cours",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                uris.chunked(3).forEachIndexed { r, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEachIndexed { c, uri ->
                            val idx = r * 3 + c
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.75f)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Thumb(uri, Modifier.fillMaxSize())
                                Text(
                                    "${idx + 1}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .background(Color(0x99000000), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(Color(0x99000000), CircleShape)
                                        .clickable(enabled = !busy) { uris.removeAt(idx) }
                                ) {
                                    Icon(
                                        Icons.Filled.Close, "Retirer la page",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp).align(Alignment.Center)
                                    )
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(enabled = !busy, onClick = onPick) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Ajouter des photos")
                    }
                    OutlinedButton(enabled = !busy && uris.isNotEmpty(), onClick = { uris.clear() }) {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Vider")
                    }
                }
            }

            Button(
                enabled = !busy && uris.isNotEmpty() && hasKey,
                onClick = onSummarize,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (busy) "Travail en cours…" else "Résumer le cours", fontSize = 16.sp) }

            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (status.isNotBlank()) {
                Text(status, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (summary.isNotBlank()) {
                Section("Résumé") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val cm = ctx.getSystemService(ClipboardManager::class.java)
                            cm.setPrimaryClip(ClipData.newPlainText("résumé", summary))
                            Toast.makeText(ctx, "Copié", Toast.LENGTH_SHORT).show()
                        }) { Text("Copier") }
                        OutlinedButton(onClick = {
                            try {
                                val name = exportPdf(ctx, summary)
                                Toast.makeText(ctx, "PDF enregistré dans Téléchargements : $name", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Erreur PDF : ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }) { Text("PDF") }
                        OutlinedButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, summary)
                            }
                            ctx.startActivity(Intent.createChooser(send, "Partager le résumé"))
                        }) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Partager")
                        }
                    }
                    parseMd(summary).forEach { l ->
                        when {
                            l.text.isBlank() -> Spacer(Modifier.height(2.dp))
                            l.level == 1 -> Text(l.text, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                            l.level == 2 -> Text(l.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                            l.level == 3 -> Text(l.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 6.dp))
                            l.text.startsWith("• ") -> Text(l.text, lineHeight = 22.sp, modifier = Modifier.padding(start = 8.dp))
                            else -> Text(l.text, lineHeight = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun Thumb(uri: Uri, modifier: Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var s = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / s > 400) s *= 2
                ctx.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
                }?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    val b = bmp
    if (b != null) {
        Image(b, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialKey: String,
    initialModel: String,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf(initialKey) }
    var model by remember { mutableStateOf(initialModel) }
    var visible by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Paramètres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Section("Clé API Gemini") {
                Text(
                    "Elle reste uniquement sur ce téléphone. Gratuite sur aistudio.google.com.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim(); info = "" },
                    label = { Text("Clé API") },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { visible = !visible }) { Text(if (visible) "Masquer" else "Afficher") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                    try { ctx.startActivity(i) } catch (e: Exception) {
                        Toast.makeText(ctx, "Aucun navigateur trouvé", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Obtenir une clé gratuite") }
            }

            Section("Modèle") {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it.trim() },
                    label = { Text("Nom du modèle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = key.isNotBlank() && model.isNotBlank(),
                onClick = {
                    onSave(key, model)
                    ok = true
                    info = "Paramètres enregistrés"
                }
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Enregistrer", fontSize = 16.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = key.isNotBlank() && model.isNotBlank() && !testing,
                    onClick = {
                        testing = true
                        ok = false
                        info = "Test en cours…"
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    GeminiClient.generate(
                                        key, model, "Réponds par un seul mot.",
                                        listOf(GeminiClient.textPart("Dis OK."))
                                    )
                                }
                                ok = true
                                info = "Clé valide"
                            } catch (e: Exception) {
                                ok = false
                                info = "Échec : ${e.message}"
                            }
                            testing = false
                        }
                    }
                ) { Text(if (testing) "Test…" else "Tester la clé") }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = initialKey.isNotBlank() || key.isNotBlank(),
                    onClick = {
                        key = ""
                        onSave("", model)
                        ok = false
                        info = "Clé supprimée"
                    }
                ) { Text("Supprimer") }
            }

            if (info.isNotBlank()) {
                Text(
                    info,
                    fontSize = 13.sp,
                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
