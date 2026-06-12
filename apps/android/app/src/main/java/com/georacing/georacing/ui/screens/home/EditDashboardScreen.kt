package com.georacing.georacing.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    
    // Default fallback if empty
    val defaultList = com.georacing.georacing.domain.model.DashboardLayout.DEFAULT.widgets
    
    // Load current layout
    val savedLayout by userPreferences.dashboardLayout.collectAsState(initial = defaultList)
    
    // Mutable state for editing
    // We only init this once from savedLayout to avoid reset on every recomposition, 
    // but need to handle initial load.
    var currentOrder by remember { mutableStateOf<List<WidgetType>>(emptyList()) }
    
    LaunchedEffect(savedLayout) {
        if (currentOrder.isEmpty()) {
            currentOrder = savedLayout
        }
    }
    
    // Available widgets (all possible types minus protected ones potentially)
    val allItems = WidgetType.values().toList().filter { it != WidgetType.STAFF_ACTIONS } 


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "EDITAR DASHBOARD",
                        color = Color(0xFFF8FAFC),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            userPreferences.setDashboardLayout(currentOrder)
                            Toast.makeText(context, "Diseño guardado", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF22C55E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF080810))
            )
        },
        containerColor = Color(0xFF080810)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(Brush.verticalGradient(listOf(Color(0xFF080810), Color(0xFF0A0A16), Color(0xFF080810))))
                .padding(16.dp)
        ) {
            Text(
                "ORGANIZA TU PANTALLA DE INICIO",
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                itemsIndexed(currentOrder) { index, widget ->
                     WidgetItemRow(
                         widget = widget,
                         isFirst = index == 0,
                         isLast = index == currentOrder.size - 1,
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
                         onToggleVisibility = {
                             // Remove from list
                             val mutable = currentOrder.toMutableList()
                             mutable.removeAt(index)
                             currentOrder = mutable
                         }
                     )
                     Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Add hidden items section
                 item {
                     Spacer(modifier = Modifier.height(24.dp))
                     Text(
                         "WIDGETS OCULTOS",
                         color = Color(0xFF64748B),
                         fontWeight = FontWeight.Bold,
                         letterSpacing = 1.5.sp
                     )
                     Spacer(modifier = Modifier.height(8.dp))
                 }
                 
                 val hiddenItems = allItems.filter { !currentOrder.contains(it) }
                 itemsIndexed(hiddenItems) { _, widget ->
                     Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E18)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                     ) {
                         Row(
                             modifier = Modifier.fillMaxWidth().padding(16.dp),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text(widget.name, color = Color(0xFF64748B))
                             Button(
                                 onClick = {
                                     val mutable = currentOrder.toMutableList()
                                     mutable.add(widget)
                                     currentOrder = mutable
                                 },
                                 colors = ButtonDefaults.buttonColors(
                                     containerColor = Color(0xFFE8253A),
                                     contentColor = Color(0xFFF8FAFC)
                                 ),
                                 shape = RoundedCornerShape(8.dp)
                             ) {
                                 Text("AÑADIR +", letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                             }
                         }
                     }
                 }
            }
        }
    }
}

@Composable
fun WidgetItemRow(
    widget: WidgetType,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleVisibility: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DragHandle, contentDescription = "Arrastrar para reordenar", tint = Color(0xFF64748B))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(widget.name, color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold)
                Text("VISIBLE", color = Color(0xFF22C55E), fontSize = 12.sp, letterSpacing = 1.sp)
            }
            
            // Reorder Buttons
            if (!isFirst) {
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = Color(0xFFF8FAFC))
                }
            }
            if (!isLast) {
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = Color(0xFFF8FAFC))
                }
            }
            
            // Remove Button
             IconButton(onClick = onToggleVisibility) {
                //Icon(Icons.Default.VisibilityOff, contentDescription = "Hide", tint = Color.Red.copy(0.7f))
                Text("X", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }
        }
    }
}
