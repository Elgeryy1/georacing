package com.georacing.georacing.ui.components.racecontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.domain.model.DriverInfo
import com.georacing.georacing.domain.model.RaceSessionInfo
import com.georacing.georacing.ui.theme.*

@Composable
fun RaceControlWidget(
    state: CircuitState,
    modifier: Modifier = Modifier
) {
    // Defines header color based on flag status
    val statusColor = when (state.mode) {
        CircuitMode.NORMAL, CircuitMode.GREEN_FLAG -> CircuitGreen
        CircuitMode.YELLOW_FLAG, CircuitMode.SAFETY_CAR, CircuitMode.VSC -> StatusAmber
        CircuitMode.RED_FLAG, CircuitMode.EVACUATION -> CircuitStop
        else -> NeutralGrey
    }

    val statusText = when (state.mode) {
        CircuitMode.NORMAL, CircuitMode.GREEN_FLAG -> "TRACK CLEAR"
        CircuitMode.YELLOW_FLAG -> "YELLOW SECTOR"
        CircuitMode.SAFETY_CAR -> "SAFETY CAR"
        CircuitMode.VSC -> "VIRTUAL SC"
        CircuitMode.RED_FLAG -> "SESSION STOPPED"
        CircuitMode.EVACUATION -> "EVACUATE"
        else -> "OFFLINE"
    }

    // Use session info from state or fallback to "Simulated" demo data if null
    val session = state.sessionInfo ?: RaceSessionInfo(
        sessionTime = "00:45:12",
        currentLap = 24,
        totalLaps = 66,
        topDrivers = listOf(
            DriverInfo(1, "VERSTAPPEN", "RBR", "Leader"),
            DriverInfo(2, "NORRIS", "MCL", "+2.4s"),
            DriverInfo(3, "ALONSO", "AMR", "+8.1s")
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AsphaltGrey)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RACE CONTROL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color(0xFF080810)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color(0xFF080810)
                )
            }

            // Body
            Column(modifier = Modifier.padding(12.dp)) {
                // Time & Lap Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SESSION TIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp
                            ),
                            color = NeutralGrey
                        )
                        Text(
                            text = session.sessionTime,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LAP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp
                            ),
                            color = NeutralGrey
                        )
                        Text(
                            text = "${session.currentLap}/${session.totalLaps}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = OutlineLight
                )

                // Top 3 Drivers
                session.topDrivers.forEach { driver ->
                    DriverRow(driver)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DriverRow(driver: DriverInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position Box
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (driver.position == 1) RacingRed else MetalGrey,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = driver.position.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFF8FAFC)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Name & Team
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = driver.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }
        
        // Gap
        Text(
            text = driver.gap,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = if(driver.position == 1) StatusGreen else TextSecondary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Tire (Circle)
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color(0xFFF8FAFC), androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, Color(0xFF080810), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = driver.tireCompound,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                color = Color(0xFF080810)
            )
        }
    }
}
