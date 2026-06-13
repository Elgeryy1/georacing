package com.georacing.georacing.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.data.local.UserPreferencesDataStore
import com.georacing.georacing.domain.model.WidgetType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDashboardScreen(
    navController: NavController,
    userPreferences: UserPreferencesDataStore
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val defaultList = com.georacing.georacing.domain.model.DashboardLayout.DEFAULT.widgets
    val savedLayout by userPreferences.dashboardLayout.collectAsState(initial = defaultList)

    var currentOrder by remember { mutableStateOf<List<WidgetType>>(emptyList()) }

    LaunchedEffect(savedLayout) {
        if (currentOrder.isEmpty()) {
            currentOrder = savedLayout
        }
    }

    val allItems = WidgetType.values().toList().filter { it != WidgetType.STAFF_ACTIONS }
    val hiddenItems = allItems.filter { !currentOrder.contains(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Personalizar Inicio",
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    // Save button
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                userPreferences.setDashboardLayout(currentOrder)
                                Toast.makeText(context, "✓ Diseño guardado", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF22C55E).copy(alpha = 0.15f),
                            contentColor = Color(0xFF22C55E)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Guardar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF080810))
            )
        },
        containerColor = Color(0xFF080810)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Section header: Visible widgets
            item {
                SectionHeader(
                    title = "Widgets activos",
                    subtitle = "${currentOrder.size} widgets · Arrastra para reordenar",
                    icon = Icons.Default.Visibility,
                    color = Color(0xFF22C55E)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Visible widget items
            itemsIndexed(currentOrder) { index, widget ->
                WidgetItemRow(
                    widget = widget,
                    index = index,
                    total = currentOrder.size,
                    onMoveUp = {
                        if (index > 0) {
                            val mutable = currentOrder.toMutableList()
                            java.util.Collections.swap(mutable, index, index - 1)
                            currentOrder = mutable
                        }
                    },
                    onMoveDown = {
                        if (index < currentOrder.size - 1) {
                            val mutable = currentOrder.toMutableList()
                            java.util.Collections.swap(mutable, index, index + 1)
                            currentOrder = mutable
                        }
                    },
                    onRemove = {
                        val mutable = currentOrder.toMutableList()
                        mutable.removeAt(index)
                        currentOrder = mutable
                    }
                )
            }

            // Section header: Hidden widgets
            if (hiddenItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = "Widgets disponibles",
                        subtitle = "${hiddenItems.size} widgets ocultos",
                        icon = Icons.Default.VisibilityOff,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(hiddenItems) { _, widget ->
                    HiddenWidgetRow(
                        widget = widget,
                        onAdd = {
                            val mutable = currentOrder.toMutableList()
                            mutable.add(widget)
                            currentOrder = mutable
                        }
                    )
                }
            }

            // Bottom spacing for nav bar
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                title,
                color = Color(0xFFF8FAFC),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Text(
                subtitle,
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun WidgetItemRow(
    widget: WidgetType,
    index: Int,
    total: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val isFirst = index == 0
    val isLast = index == total - 1

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121C)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = widget.accentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position number
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E2E)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Widget icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(widget.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = widget.icon,
                    contentDescription = null,
                    tint = widget.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Widget info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    widget.displayName,
                    color = Color(0xFFF8FAFC),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Reorder controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Move Up
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Subir",
                        tint = if (!isFirst) Color(0xFFCBD5E1) else Color(0xFF2A2A3A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Move Down
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Bajar",
                        tint = if (!isLast) Color(0xFFCBD5E1) else Color(0xFF2A2A3A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Remove
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Ocultar",
                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenWidgetRow(
    widget: WidgetType,
    onAdd: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E16)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAdd() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Widget icon (dimmed)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(widget.accentColor.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = widget.icon,
                    contentDescription = null,
                    tint = widget.accentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                widget.displayName,
                color = Color(0xFF64748B),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // Add button
            FilledTonalButton(
                onClick = onAdd,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF1E1E2E),
                    contentColor = Color(0xFFF8FAFC)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
