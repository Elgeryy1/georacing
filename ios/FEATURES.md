# Mapa de Funcionalidades GeoRacing (Mobile Parity)

Documento vivo que rastrea el estado de paridad de funcionalidades entre Android y iOS.
Fuente de verdad en código: `FeatureRegistry.swift`.

## Resumen de Estado

- **Total Features**: 35+
- **Completas (Parity)**: ~8 (Core + Social QR + Status)
- **Básicas (MVP)**: ~4 (Map Offline, Social Group, Staff)
- **Placeholder**: Resto (Future proofing)

## Detalle por Categoría

### 1. Core (Esencial)

| ID                   | Título                  | Status         | Vista iOS                     | Notas                               |
| :------------------- | :---------------------- | :------------- | :---------------------------- | :---------------------------------- |
| `core.circuit_state` | Estado del Circuito     | ✅ Complete    | `CircuitControlView` / Banner | Polling polling funcionando         |
| `core.context_card`  | Card Contextual         | ✅ Complete    | `HomeView` Card               |                                     |
| `core.offline_map`   | Mapa Vivo Offline       | ⚠️ Basic       | `CircuitMapView`              | Falta persistencia robusta de tiles |
| `core.pois`          | Puntos de Interés       | ✅ Complete    | `CircuitMapView`              | API real                            |
| `core.qr_position`   | Posicionamiento QR      | 🚧 Placeholder | `SocialView` (temp)           | Escáner stub                        |
| `core.ble`           | Beacons Inteligentes    | ✅ Complete    | `BeaconScanner`               | Lógica BLE background               |
| `core.offline_mode`  | Modo Sin Conexión       | ⚠️ Basic       | N/A                           | Cache local funciona                |
| `core.alerts`        | Centro de Alertas       | ⚠️ Basic       | `AlertsView`                  | UI básica                           |
| `core.notifications` | Notificaciones Críticas | ✅ Complete    | `LocalNotificationManager`    |                                     |
| `core.feedback`      | Incidencias             | ✅ Complete    | `IncidentReportView`          |                                     |

### 2. Navegación

| ID                 | Título              | Status         | Vista iOS        | Notas          |
| :----------------- | :------------------ | :------------- | :--------------- | :------------- |
| `nav.ar_guide`     | Guía AR             | 🚧 Placeholder | -                |                |
| `nav.anticalas`    | Rutas Anti-colas    | 🚧 Placeholder | -                |                |
| `nav.services`     | Rutas a Servicios   | 🚧 Placeholder | -                | OSRM pendiente |
| `nav.state_routes` | Rutas Dinámicas     | 🚧 Placeholder | -                |                |
| `nav.evacuation`   | Evacuación Dinámica | ✅ Complete    | `EvacuationView` | Overlay activo |

### 3. Social

| ID                    | Título             | Status         | Vista iOS        | Notas            |
| :-------------------- | :----------------- | :------------- | :--------------- | :--------------- |
| `social.follow_group` | Seguir al Grupo    | ⚠️ Basic       | `CircuitMapView` | Wiring pendiente |
| `social.meetup`       | Punto de Encuentro | 🚧 Placeholder | -                |                  |

### 4. Fan Experience

| ID              | Título        | Status         | Vista iOS | Notas |
| :-------------- | :------------ | :------------- | :-------- | :---- |
| `fan.immersive` | Fan Immersive | 🚧 Placeholder | -         |       |
| `fan.360`       | Momento 360   | 🚧 Placeholder | -         |       |

### 5. Staff & Ops

| ID                    | Título         | Status         | Vista iOS | Notas |
| :-------------------- | :------------- | :------------- | :-------- | :---- |
| `staff.panel`         | Panel Interno  | ⚠️ Basic       | -         |       |
| `staff.beacon_remote` | Control Remoto | 🚧 Placeholder | -         |       |

### 6. Avanzado & Visionario

_Ver `FeatureRegistry.swift` para la lista completa de features futuras (FlowSense, Neural Network, etc)._

## Arquitectura de Navegación

El sistema utiliza un `FeatureRegistry` como fuente de verdad.

- **Factory**: `FeatureViewFactory` decide qué vista mostrar.
- **Placeholder**: `FeaturePlaceholderView` provee una UI estándar para features en desarrollo.
- **Routing**: `ContentView` gestiona la navegación modal.

## Cómo añadir una nueva feature

1. Definirla en `FeatureRegistry.swift` (añadir al array `allFeatures`).
2. Implementar la vista real en SwiftUI.
3. Mapear el ID en `FeatureViewFactory.swift`.
4. (Opcional) Actualizar este documento.
5. ¡Listo! Aparece automáticamente en Menú y Buscador.
