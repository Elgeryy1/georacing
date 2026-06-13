package com.georacing.georacing.ui.screens.roadmap

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RoadmapViewModel : ViewModel() {

    private val _roadmapData = MutableStateFlow<List<FeatureCategory>>(emptyList())
    val roadmapData: StateFlow<List<FeatureCategory>> = _roadmapData.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val data = listOf(
            FeatureCategory(
                title = "A. MÓDULO: NAVEGACIÓN & PISTA (El Core)",
                features = listOf(
                    Feature(
                        "Mapa Vectorial Offline",
                        "Navegación total sin internet.",
                        FeatureStatus.DONE,
                        Icons.Default.Map
                    ),
                    Feature(
                        "Guía AR al Asiento",
                        "Realidad aumentada \"Last Mile\".",
                        FeatureStatus.WIP,
                        Icons.Default.ViewInAr
                    ),
                    Feature(
                        "Rutas Anti-Colas",
                        "Algoritmo dinámico de desvío.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.AltRoute
                    ),
                    Feature(
                        "Accesibilidad Total",
                        "Rutas sin escaleras/adaptadas.",
                        FeatureStatus.DONE,
                        Icons.Default.Accessible
                    ),
                    Feature(
                        "Auto-Posicionamiento QR",
                        "Ubicación rápida escaneando hitos.",
                        FeatureStatus.WIP,
                        Icons.Default.QrCode
                    ),
                    Feature(
                        "Micro-posicionamiento BLE",
                        "Precisión en interiores con beacons.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Bluetooth
                    ),
                    Feature(
                        "Modo Emergencia/Evacuación",
                        "Rutas de salida dinámicas.",
                        FeatureStatus.DONE,
                        Icons.Default.Warning
                    ),
                    Feature(
                        "Predicción de Tiempos",
                        "\"Estás a 12 min de tu puerta\".",
                        FeatureStatus.DONE,
                        Icons.Default.Timer
                    )
                )
            ),
            FeatureCategory(
                title = "B. MÓDULO: MOTOR & COCHE (Android Auto)",
                features = listOf(
                    Feature(
                        "Ruta Inteligente al Circuit",
                        "Gestión de tráfico previa.",
                        FeatureStatus.DONE,
                        Icons.Default.Directions
                    ),
                    Feature(
                        "Asignación de Parking",
                        "Te dice en qué zona aparcar.",
                        FeatureStatus.DONE,
                        Icons.Default.LocalParking
                    ),
                    Feature(
                        "Guía \"Last Mile\" Coche",
                        "Del peaje a la plaza de parking.",
                        FeatureStatus.WIP,
                        Icons.Default.DirectionsCar
                    ),
                    Feature(
                        "Find My Car",
                        "Guarda GPS + Foto del coche.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.CarRental
                    ),
                    Feature(
                        "Modo Transición",
                        "UI cambia automáticamente de \"Coche\" a \"Andando\".",
                        FeatureStatus.WIP,
                        Icons.Default.TransferWithinAStation
                    ),
                    Feature(
                        "Alertas de Tráfico",
                        "Avisos en tiempo real al llegar.",
                        FeatureStatus.DONE,
                        Icons.Default.Traffic
                    )
                )
            ),
            FeatureCategory(
                title = "C. MÓDULO: FAN EXPERIENCE (La Diversión)",
                features = listOf(
                    Feature(
                        "GeoRacing Wrapped",
                        "Resumen estadístico de tu visita.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.AutoGraph
                    ),
                    Feature(
                        "Pedidos Click & Collect",
                        "Comida sin colas (con alérgenos).",
                        FeatureStatus.WIP,
                        Icons.Default.ShoppingCart
                    ),
                    Feature(
                        "Gamificación & Badges",
                        "Retos (ej: \"Camina 5km\", \"Visita la Curva 4\").",
                        FeatureStatus.BACKLOG,
                        Icons.Default.EmojiEvents
                    ),
                    Feature(
                        "Momento360",
                        "Captura de recuerdos inmersivos.",
                        FeatureStatus.WIP,
                        Icons.Default.Camera
                    ),
                    Feature(
                        "Fan Zone Personalizada",
                        "Recomendaciones según tus gustos.",
                        FeatureStatus.WIP,
                        Icons.Default.Person
                    ),
                    Feature(
                        "Coleccionables Digitales",
                        "Cromos/NFTs del evento.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Collections
                    ),
                    Feature(
                        "Trivia F1 & Duolingo",
                        "Preguntas tipo quiz en tiempos muertos.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Quiz
                    ),
                    Feature(
                        "Pop-ups Curiosidades",
                        "Datos \"Sabías que...\" según ubicación.",
                        FeatureStatus.DONE,
                        Icons.Default.Info
                    ),
                    Feature(
                        "EcoMeter",
                        "Mide tu impacto ecológico y da consejos.",
                        FeatureStatus.WIP,
                        Icons.Default.Eco
                    )
                )
            ),
            FeatureCategory(
                title = "D. MÓDULO: SOCIAL & COMUNIDAD",
                features = listOf(
                    Feature(
                        "Seguir al Grupo",
                        "Ver dónde están tus amigos en el mapa.",
                        FeatureStatus.WIP,
                        Icons.Default.Group
                    ),
                    Feature(
                        "Punto de Reunión",
                        "Coordenada fija compartida.",
                        FeatureStatus.DONE,
                        Icons.Default.MeetingRoom
                    ),
                    Feature(
                        "Compartir Perfil",
                        "Conectar redes sociales vía QR/NFC.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Share
                    ),
                    Feature(
                        "Clubes & Grupos",
                        "Crear \"Peñas\" para el evento.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Groups
                    ),
                    Feature(
                        "Chat de Proximidad",
                        "Opcional para grupos (Mesh).",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Chat
                    )
                )
            ),
            FeatureCategory(
                title = "E. MÓDULO: UTILIDADES & GESTIÓN",
                features = listOf(
                    Feature(
                        "Wallet de Entradas",
                        "Acceso rápido offline.",
                        FeatureStatus.DONE,
                        Icons.Default.Wallet
                    ),
                    Feature(
                        "Plan del Día",
                        "Horarios y agenda personalizable.",
                        FeatureStatus.DONE,
                        Icons.Default.CalendarToday
                    ),
                    Feature(
                        "Centro de Alertas",
                        "Avisos oficiales de dirección de carrera.",
                        FeatureStatus.DONE,
                        Icons.Default.Notifications
                    ),
                    Feature(
                        "Botón SOS",
                        "Ubicación directa a seguridad/médicos.",
                        FeatureStatus.DONE,
                        Icons.Default.Sos
                    ),
                    Feature(
                        "Feedback Rápido",
                        "Reportar suciedad/averías con 1 toque.",
                        FeatureStatus.WIP,
                        Icons.Default.Feedback
                    ),
                    Feature(
                        "Interfaz IA Adaptativa",
                        "Menús cambian según hora/contexto.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.AutoAwesome
                    ),
                    Feature(
                        "Soporte Multi-Idioma",
                        "Traducción automática y modo turista.",
                        FeatureStatus.DONE,
                        Icons.Default.Language
                    )
                )
            ),
            FeatureCategory(
                title = "F. BACKLOG v2: SEGURIDAD & EVACUACIÓN",
                features = listOf(
                    Feature(
                        "Muster Point Check-in",
                        "Reunificación post-evacuación: confirma que estás a salvo en el punto de encuentro.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.HowToReg
                    ),
                    Feature(
                        "Shelter-in-place por zona",
                        "Instrucciones de refugio diferenciadas según tu zona del recinto.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Shield
                    ),
                    Feature(
                        "Buscar persona perdida (BLE)",
                        "Localiza a un acompañante por proximidad Bluetooth.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.PersonSearch
                    ),
                    Feature(
                        "Botón de pánico de staff",
                        "Alerta inmediata a seguridad desde el personal del evento.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Campaign
                    ),
                    Feature(
                        "Modo niño / pulsera digital",
                        "Identificación segura del menor y reunificación rápida con tutores.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.ChildCare
                    )
                )
            ),
            FeatureCategory(
                title = "G. BACKLOG v2: FLUJO & ACCESIBILIDAD",
                features = listOf(
                    Feature(
                        "Malla BLE multi-salto",
                        "Propagación de alertas sin red mediante saltos entre dispositivos.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Hub
                    ),
                    Feature(
                        "Salida escalonada",
                        "Egress escalonado para evitar avalanchas a la salida.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.ExitToApp
                    ),
                    Feature(
                        "Cola virtual con franja horaria",
                        "Reserva tu turno y evita esperas físicas.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Schedule
                    ),
                    Feature(
                        "Accesibilidad en vivo",
                        "Estado de rampas/ascensores crowdsourced en tiempo real.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Accessible
                    ),
                    Feature(
                        "Ruta Calm / modo sensorial",
                        "Itinerarios de baja estimulación para perfiles sensibles.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.SelfImprovement
                    ),
                    Feature(
                        "Turn-by-turn háptico",
                        "Indicaciones por vibración en wearable, sin mirar la pantalla.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.Vibration
                    )
                )
            ),
            FeatureCategory(
                title = "H. BACKLOG v2: RESILIENCIA & OPERACIONES",
                features = listOf(
                    Feature(
                        "Failover descentralizado",
                        "El evento sigue operativo aunque caiga el backend central.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.CloudOff
                    ),
                    Feature(
                        "Salud predictiva de balizas",
                        "Monitoriza la flota de beacons y anticipa fallos de batería/cobertura.",
                        FeatureStatus.BACKLOG,
                        Icons.Default.MonitorHeart
                    )
                )
            )
        )
        _roadmapData.value = data
    }
}
