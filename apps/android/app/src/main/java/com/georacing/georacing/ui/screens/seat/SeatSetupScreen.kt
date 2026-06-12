package com.georacing.georacing.ui.screens.seat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.georacing.georacing.data.local.UserPreferencesDataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSetupScreen(
    navController: NavController,
    userPreferences: UserPreferencesDataStore
) {
    val viewModel: SeatViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SeatViewModel(userPreferences)
            }
        }
    )

    val seatInfo by viewModel.seatInfo.collectAsState()

    var grandstand by remember(seatInfo) { mutableStateOf(seatInfo?.grandstand ?: "") }
    var zone by remember(seatInfo) { mutableStateOf(seatInfo?.zone ?: "") }
    var row by remember(seatInfo) { mutableStateOf(seatInfo?.row ?: "") }
    var seat by remember(seatInfo) { mutableStateOf(seatInfo?.seat ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Localidad") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    com.georacing.georacing.ui.components.HomeIconButton {
                        navController.navigate(com.georacing.georacing.ui.navigation.Screen.Home.route) {
                            popUpTo(com.georacing.georacing.ui.navigation.Screen.Home.route) { inclusive = true }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (seatInfo != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Localidad Guardada",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tribuna: ${seatInfo?.grandstand}")
                        Text("Zona: ${seatInfo?.zone}")
                        Text("Fila: ${seatInfo?.row} - Asiento: ${seatInfo?.seat}")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = "Configurar nueva localidad",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = grandstand,
                onValueChange = { grandstand = it },
                label = { Text("Tribuna / Grada") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = zone,
                onValueChange = { zone = it },
                label = { Text("Zona") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = row,
                onValueChange = { row = it },
                label = { Text("Fila") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = seat,
                onValueChange = { seat = it },
                label = { Text("Asiento") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.saveSeat(grandstand, zone, row, seat)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = grandstand.isNotBlank() && zone.isNotBlank()
            ) {
                Text("Guardar mi localidad")
            }
        }
    }
}
