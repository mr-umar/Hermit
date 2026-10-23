package xyz.umarmohammad.hermit.ui.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.umarmohammad.hermit.R
import xyz.umarmohammad.hermit.data.model.InstalledAppInfo
import xyz.umarmohammad.hermit.ui.components.EmptyStateView
import xyz.umarmohammad.hermit.ui.components.HermitTopBar
import xyz.umarmohammad.hermit.ui.components.TactileButton

@Composable
fun AppSelectionScreen(
    initiallySelectedPackages: List<String>,
    title: String? = null,
    maxSelection: Int? = null,
    onSaveSelection: (List<String>) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val selectedPackages = remember { mutableStateListOf<String>().apply { addAll(initiallySelectedPackages) } }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load installed apps asynchronously off main thread
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager

            // Exclude Input Method Editors (Keyboards: Gboard, Samsung Keyboard, SwiftKey, etc.)
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            val imePackages = imm?.enabledInputMethodList?.map { it.packageName }?.toSet() ?: emptySet()

            // Exclude all Home Launcher activities
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val homePackages = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)
                .map { it.activityInfo.packageName }
                .toSet()

            // Exclude critical system packages
            val systemExempt = setOf(
                context.packageName,
                "xyz.umarmohammad.hermit",
                "android",
                "com.android.systemui",
                "com.android.settings",
                "com.android.phone",
                "com.google.android.dialer",
                "com.android.dialer",
                "com.samsung.android.dialer",
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.google.android.permissioncontroller",
                "com.android.permissioncontroller"
            )

            val allExcluded = imePackages + homePackages + systemExempt

            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val apps = resolveInfos.mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (allExcluded.contains(pkg)) return@mapNotNull null
                val label = ri.loadLabel(pm).toString()
                val icon = try { ri.loadIcon(pm) } catch (_: Exception) { null }
                val iconBmp = try {
                    icon?.toBitmap(width = 96, height = 96)?.asImageBitmap()
                } catch (_: Exception) { null }
                InstalledAppInfo(
                    packageName = pkg,
                    appName = label,
                    isSystemApp = false,
                    icon = icon,
                    iconBitmap = iconBmp
                )
            }.distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }

            withContext(Dispatchers.Main) {
                // Also clean any previously saved excluded packages from selection
                selectedPackages.removeAll { allExcluded.contains(it) }
                installedApps = apps
                isLoading = false
            }
        }
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            HermitTopBar(
                title = title ?: stringResource(R.string.app_select_title),
                subtitle = if (maxSelection != null) {
                    stringResource(R.string.app_select_selected_of_max, selectedPackages.size, maxSelection)
                } else {
                    stringResource(R.string.profile_blocked_count, selectedPackages.size)
                },
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    TactileButton(
                        onClick = { onSaveSelection(selectedPackages.toList()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.app_select_save, selectedPackages.size))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.app_select_search_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick multi-selection toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredApps.size} apps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    if (maxSelection == null) {
                        TextButton(
                            onClick = {
                                val newToAdd = filteredApps.map { it.packageName }.filter { !selectedPackages.contains(it) }
                                selectedPackages.addAll(newToAdd)
                            }
                        ) {
                            Text(stringResource(R.string.app_select_all))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    TextButton(
                        onClick = {
                            selectedPackages.clear()
                        }
                    ) {
                        Text(stringResource(R.string.app_select_clear))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredApps.isEmpty() && !isLoading) {
                EmptyStateView(
                    icon = Icons.Default.Apps,
                    title = stringResource(R.string.app_select_empty),
                    description = searchQuery,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        AppItemRow(
                            app = app,
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) {
                                    selectedPackages.remove(app.packageName)
                                } else {
                                    if (maxSelection != null && selectedPackages.size >= maxSelection) {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.app_select_max_allowed, maxSelection),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        selectedPackages.add(app.packageName)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppItemRow(
    app: InstalledAppInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
