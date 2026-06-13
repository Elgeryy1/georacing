# GeoRacing — Documentación Técnica Global

## Índice

1. [Descripción General del Proyecto](#descripción-general-del-proyecto)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Componentes Principales](#componentes-principales)
4. [Flujo de Datos Global](#flujo-de-datos-global)
5. [Base de Datos](#base-de-datos)
6. [Protocolo BLE (Bluetooth Low Energy)](#protocolo-ble)
7. [API REST](#api-rest)
8. [Tecnologías Utilizadas por Componente](#tecnologías-utilizadas-por-componente)
9. [Estructura de Carpetas del Repositorio](#estructura-de-carpetas-del-repositorio)

---

## Descripción General del Proyecto

**GeoRacing** es un sistema de gestión y experiencia para eventos de automovilismo (tipo Fórmula 1 o similares). El objetivo es proporcionar:

- **Información en tiempo real** a los asistentes del evento (aficionados) a través de apps móviles.
- **Gestión centralizada** del evento a través de un panel web para el personal de organización.
- **Señalización física inteligente** mediante beacons (pantallas con PC o Raspberry Pi) distribuidas por el circuito que muestran estado del circuito, rutas de evacuación, direcciones, etc.

El sistema se compone de **cinco grandes módulos**, cada uno documentado en su propia carpeta:

| Carpeta | Tecnología | Descripción |
|---|---|---|
| `android/` | Kotlin + Jetpack Compose | Aplicación para asistentes Android |
| `ios/` | Swift + SwiftUI | Aplicación para asistentes iOS |
| `web/` | React + TypeScript + Vite | Panel de control web para organizadores |
| `windows/` | C# + WPF (.NET) | Beacon de Windows (pantalla de señalización) |
| `api/` | Node.js + Express + MySQL | API REST principal |

---

## Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                     NUBE / BACKEND                          │
│                                                             │
│   ┌─────────────────┐       ┌──────────────────────┐       │
│   │  API REST Node  │◄─────►│  Base de Datos MySQL  │       │
│   │  (georacing.example)  │       │   (GeoRacingDB)       │       │
│   └────────┬────────┘       └──────────────────────┘       │
│            │                                                │
│   ┌────────▼────────┐                                       │
│   │  Firebase Auth  │  (Autenticación del Panel Web)        │
│   │  + Firestore    │  (Datos auxiliares iOS)               │
│   └─────────────────┘                                       │
└──────────────────┬───────────────────────────────┬──────────┘
                   │                               │
        ┌──────────▼──────────┐         ┌──────────▼──────────┐
        │   Clientes Móviles  │         │     Beacons          │
        │                     │         │                       │
        │  ┌──────────────┐   │         │  ┌───────────────┐   │
        │  │ Android App  │   │         │  │ Beacon Windows│   │
        │  │ (Kotlin)     │   │         │  │ (WPF C#)      │   │
        │  └──────────────┘   │         │  └───────────────┘   │
        │                     │         │                       │
        │  ┌──────────────┐   │         │                       │
        │  │  iOS App     │   │         │                       │
        │  │  (Swift)     │   │         │                       │
        │  └──────────────┘   │         │                       │
        └─────────────────────┘         └──────────────────────┘
                   ▲                               │
                   │                               │
                   │      BLE Advertising          │
                   └───────────────────────────────┘
                   (Los beacons emiten señales BLE que
                    las apps móviles detectan para
                    calcular proximidad y zona)

        ┌─────────────────────────┐
        │       Panel Web         │
        │   (React + TypeScript)  │
        │   Dashboard de control  │
        └─────────────────────────┘
```

---

## Componentes Principales

### 1. Android App (`android/`)
Aplicación Android para aficionados que asisten al evento.  
Ver documentación completa: [`android/documentation.md`](../android/documentation.md)

**Características principales:**
- Mapa del circuito con POIs (puntos de interés)
- Navegación peatonal dentro del circuito
- Estado del circuito en tiempo real (bandera roja, safety car, evacuación)
- Sistema de grupos (compartir ubicación con amigos)
- Tienda/pedidos de comida
- Integración con Android Auto (CarPlay para Android)
- Modo supervivencia de batería
- Bluetooth Low Energy (BLE) para detectar beacons cercanos
- Realidad Aumentada (AR) básica

### 2. iOS App (`ios/`)
Aplicación iOS para aficionados, con paridad de características respecto a Android.  
Ver documentación completa: [`ios/FEATURES.md`](../ios/FEATURES.md)

**Características principales:**
- Mapa con MapKit + rutas de navegación
- Integración con CarPlay
- Gestión de grupos y ubicación compartida
- Escaneo BLE de beacons
- Noticias y Fan Zone
- Quiz y gamificación
- Sistema de pedidos con carrito

### 3. Panel Web (`web/`)
Panel de control para el personal de organización del evento.  
Ver documentación completa: [`web/README.md`](../web/README.md)

**Características principales:**
- Dashboard de métricas en tiempo real
- Gestión completa de beacons (configurar, enviar comandos, visualizar estado)
- Gestión de emergencias y evacuaciones
- Estado global del circuito
- Gestión de incidentes
- Mapa de zonas
- Gestión de pedidos y productos
- Noticias para fans
- Gestión de usuarios

### 4. Beacon de Windows (`windows/`)
Software de beacon con interfaz gráfica WPF para Windows.  
Ver documentación completa: [`windows/BeaconApp/README.md`](../windows/BeaconApp/README.md)

**Características principales:**
- Pantalla de señalización a pantalla completa
- Polling de la API cada 300ms para recibir comandos
- Heartbeat cada 10 segundos para registrar presencia
- BLE Advertising para emitir señal de zona
- Múltiples modos: NORMAL, CONGESTIÓN, EMERGENCIA, EVACUACIÓN, MANTENIMIENTO
- Flechas de dirección configurables desde el panel web
- Control remoto de brillo de pantalla
- Soporte de comandos: RESTART, SHUTDOWN, CLOSE_APP, UPDATE_CONFIG

### 5. API (`api/`)
API REST en Node.js + Express respaldada por MySQL.  
Ver documentación completa: [`api/README.md`](../api/README.md)

**Características principales:**
- Fuente única de verdad para beacons, comandos, zonas y estado del circuito
- Endpoints genéricos de consulta/actualización sobre la base de datos
- Crea y valida su esquema al arrancar

---

## Flujo de Datos Global

### Flujo de una evacuación de emergencia

```
OPERADOR (Panel Web)
    │
    ▼
Activa evacuación global
    │
    ▼
API REST ← Panel escribe en tabla "circuit_state" (global_mode = "EVACUATION")
    │           y en tabla "beacons" (mode = "EVACUATION" para cada beacon)
    │
    ├──► Los beacons leen la API cada 300ms
    │       │
    │       └──► Cambian pantalla a modo EVACUACIÓN (fondo rojo + flechas)
    │       └──► Actualizan payload BLE con modo=0x03 (EVACUATION)
    │
    └──► Apps móviles (Android/iOS) leen la API / Firestore
             │
             └──► Notifican al usuario de la evacuación
             └──► Detectan beacons BLE cercanos para mostrar
                  dirección de evacuación más próxima
```

### Flujo de configuración de un beacon

```
OPERADOR (Panel Web)
    │
    ▼
Modifica la configuración del beacon en la UI
    │
    ▼
Panel escribe comando "UPDATE_CONFIG" en tabla "commands"
    y actualiza registro en tabla "beacons"
    │
    ▼
El beacon detecta el nuevo comando en el siguiente poll (300ms)
    │
    ▼
El beacon actualiza su pantalla: modo, mensaje, flechas, color, brillo
    │
    ▼
El beacon marca el comando como ejecutado (DELETE del comando)
    │
    ▼
Panel web refleja el nuevo estado en el dashboard
```

---

## Base de Datos

La base de datos es MySQL, alojada en un NAS QNAP con el servidor API en `georacing.example.com:4010`.

### Tablas principales

| Tabla | Descripción |
|---|---|
| `beacons` | Registro de todos los beacons físicos. Incluye estado, modo, flecha, mensaje, batería, posición GPS, zona, etc. |
| `commands` | Cola de comandos pendientes para beacons. Los beacons hacen polling y consumen estos comandos. |
| `circuit_state` | Estado global del circuito (una sola fila con id=1). Contiene `global_mode`, temperatura, mensaje, ruta de evacuación. |
| `zones` | Zonas del circuito (GRADA, PADDOCK, FANZONE, VIAL, PARKING). |
| `incidents` | Incidentes reportados por usuarios o staff. |
| `emergency_logs` | Registro de acciones de emergencia (quién activó, cuándo, qué tipo). |
| `beacon_logs` | Logs de actividad de cada beacon (heartbeats, cambios de estado, errores). |
| `orders` | Pedidos de comida/productos realizados desde la app. |
| `products` | Catálogo de productos/comida disponibles. |
| `food_stands` | Puestos de comida y sus productos. |
| `news` | Noticias y novedades para mostrar a los fans. |
| `users` | Usuarios del sistema (staff y fans registrados). |

### Campos clave de la tabla `beacons`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | INT (auto) | ID numérico interno |
| `beacon_uid` | VARCHAR | Identificador único del beacon (UUID) |
| `name` | VARCHAR | Nombre descriptivo |
| `description` | TEXT | Descripción del punto |
| `zone_id` | INT | FK a zonas |
| `latitude` / `longitude` | DOUBLE | Posición GPS |
| `has_screen` | BOOLEAN | Si tiene pantalla (1=sí) |
| `mode` | VARCHAR | NORMAL/CONGESTION/EMERGENCY/EVACUATION/MAINTENANCE |
| `arrow_direction` | VARCHAR | NONE/LEFT/RIGHT/UP/DOWN/etc. |
| `message` | TEXT | Texto a mostrar en pantalla |
| `color` | VARCHAR | Color de fondo en hex (#RRGGBB) |
| `brightness` | INT | Brillo 0-100 |
| `battery_level` | INT | Nivel de batería 0-100 |
| `last_heartbeat` | DATETIME | Última vez que el beacon envió un heartbeat |
| `configured` | BOOLEAN | Si ha sido configurada manualmente |

---

## Protocolo BLE

Los beacons emiten señales Bluetooth Low Energy (BLE) con un payload personalizado de **9 bytes** usando el campo `ManufacturerData`:

| Byte | Campo | Descripción |
|---|---|---|
| 0 | Version | Siempre `0x01` |
| 1-2 | Zone ID | ID de zona en Big Endian |
| 3 | Mode | `0x00`=NORMAL, `0x01`=CONGESTION, `0x02`=EMERGENCY/RED_FLAG, `0x03`=EVACUATION |
| 4 | Flags | Reservado, siempre `0x00` |
| 5-6 | Sequence | Contador incremental en Big Endian (indica frescura del dato) |
| 7 | TTL | Tiempo de validez en segundos (normalmente `0x0A` = 10 segundos) |
| 8 | Temperature | Temperatura en grados Celsius (entero sin signo) |

**Manufacturer ID:** `0x1234` (ID de prueba/desarrollo)

Las apps móviles (Android/iOS) escanean continuamente las señales BLE para:
1. Identificar en qué zona del circuito se encuentra el usuario.
2. Mostrar el modo del circuito más cercano (especialmente importante en evacuaciones).
3. Guiar al usuario a la salida más próxima.

---

## API REST

La API está alojada en `https://georacing.example.com:4010/api` y expone los siguientes endpoints genéricos:

| Endpoint | Método | Descripción |
|---|---|---|
| `/health` | GET | Comprobación de disponibilidad del servidor |
| `/_get` | POST | Consulta genérica: `{ table, where }` |
| `/_upsert` | POST | Inserción/actualización genérica: `{ table, data }` |
| `/_delete` | POST | Eliminación genérica: `{ table, where }` |
| `/_ensure_table` | POST | Crea una tabla si no existe |
| `/_ensure_column` | POST | Añade una columna si no existe |
| `/beacons/heartbeat` | POST | Registra el heartbeat de un beacon |
| `/commands/pending/:uid` | GET | Obtiene comandos pendientes para un beacon |
| `/commands/:id/execute` | POST | Marca un comando como ejecutado |
| `/api/state` | GET | Estado del circuito (endpoint legacy del beacon) |

---

## Tecnologías Utilizadas por Componente

| Tecnología | Versión | Usada en | Propósito |
|---|---|---|---|
| **Kotlin** | 2.x | Android App | Lenguaje principal de la app Android |
| **Jetpack Compose** | latest | Android App | Framework declarativo de UI para Android |
| **Hilt / Manual DI** | - | Android App | Inyección de dependencias |
| **Room Database** | 2.x | Android App | Base de datos local SQLite (offline-first) |
| **Retrofit** | 2.x | Android App | Cliente HTTP para la API REST |
| **Firebase (Android)** | 10.x | Android App | Autenticación y Firestore |
| **MapLibre** | latest | Android App | Mapa open-source personalizable |
| **WorkManager** | 2.x | Android App | Tareas en segundo plano (sincronización) |
| **Android Auto** | - | Android App | Integración con pantalla del coche |
| **Health Connect** | - | Android App | Acceso a datos de salud del usuario |
| **Swift** | 5.x | iOS App | Lenguaje principal de la app iOS |
| **SwiftUI** | - | iOS App | Framework declarativo de UI para iOS |
| **Firebase (iOS)** | latest | iOS App | Auth + Firestore |
| **GoogleSignIn** | latest | iOS App | Inicio de sesión con Google |
| **MapKit** | - | iOS App | Mapas de Apple |
| **CarPlay** | - | iOS App | Integración con pantalla del coche |
| **CoreBluetooth** | - | iOS App | Escaneo de señales BLE |
| **React** | 18.2 | Web Panel | Framework UI del panel web |
| **TypeScript** | 5.2 | Web Panel | Tipado estático |
| **Vite** | 5.0 | Web Panel | Bundler/dev server |
| **Tailwind CSS** | 3.3 | Web Panel | Framework de CSS utilitario |
| **Firebase (Web)** | 10.14 | Web Panel | Autenticación del panel |
| **React Router** | 6.20 | Web Panel | Enrutamiento SPA |
| **lucide-react** | 0.294 | Web Panel | Iconos |
| **C#** | latest | Beacon de Windows | Lenguaje del beacon |
| **.NET (WPF)** | 7/8 | Beacon de Windows | Framework UI de Windows (XAML) |
| **WinRT BLE API** | - | Beacon de Windows | Bluetooth Low Energy en Windows |
| **System.Text.Json** | - | Beacon de Windows | Serialización JSON nativa .NET |
| **MySQL** | 8.x | API | Base de datos relacional |
| **Node.js API** | - | API | Servidor REST personalizado |
| **Firebase Auth** | - | Web + Móvil | Autenticación centralizada |
| **Firebase Firestore** | - | Web + iOS | Base de datos NoSQL en la nube |

---

## Estructura de Carpetas del Repositorio

```
GeoRacing/
├── README.md                    # Descripción breve del repositorio
├── docs/documentation.md        # ← Este archivo (visión global)
│
├── android/                     # App Android para asistentes
│   ├── documentation.md         # Documentación completa Android
│   ├── app/src/main/java/com/georacing/georacing/
│   │   ├── MainActivity.kt      # Punto de entrada de la app
│   │   ├── di/                  # Inyección de dependencias (AppContainer)
│   │   ├── domain/              # Lógica de negocio y casos de uso
│   │   ├── data/                # Repositorios, fuentes de datos, BLE, etc.
│   │   ├── ui/                  # Pantallas, ViewModels, componentes UI
│   │   ├── services/            # Servicios en segundo plano
│   │   ├── infrastructure/      # Car, Health, BLE services de infraestructura
│   │   ├── car/                 # Módulo Android Auto
│   │   ├── navigation/          # Motor de navegación peatonal
│   │   └── utils/               # Utilidades (TTS, distancias, etc.)
│   ├── GeoRacingDB.sql          # Script SQL de la base de datos
│   └── google-services.json     # Config Firebase (Android)
│
├── ios/                         # App iOS para asistentes
│   ├── FEATURES.md              # Lista de características iOS
│   ├── GeoRacing/               # Código fuente principal
│   │   ├── GeoRacingApp.swift   # Punto de entrada de la app
│   │   ├── ContentView.swift    # Vista raíz
│   │   ├── Core/                # Extensiones, utilidades, constantes
│   │   ├── Domain/              # Modelos, servicios de dominio, features
│   │   ├── Data/                # Repositorios, BLE, servicios de datos
│   │   ├── Presentation/        # Vistas SwiftUI, ViewModels, componentes
│   │   ├── CarPlay/             # Módulo CarPlay
│   │   └── Resources/           # Assets, fuentes, localización
│   └── Podfile                  # Dependencias CocoaPods
│
├── web/                         # Panel web de control (operadores)
│   ├── README.md                # Documentación del panel web
│   ├── src/
│   │   ├── App.tsx              # Componente raíz + rutas
│   │   ├── main.tsx             # Punto de entrada React
│   │   ├── pages/               # Páginas de la aplicación
│   │   ├── components/          # Componentes reutilizables
│   │   ├── services/            # API client, beacon service, etc.
│   │   ├── hooks/               # Custom hooks de React
│   │   ├── context/             # Contextos (Auth, Toast)
│   │   ├── types/               # Tipos TypeScript
│   │   ├── firebase/            # Config y app Firebase
│   │   ├── beacon_renderer/     # Renderizador visual de beacons
│   │   └── utils/               # Funciones de utilidad
│   ├── package.json             # Dependencias npm
│   └── tailwind.config.js       # Config Tailwind CSS
│
├── api/                         # API REST (Node.js + Express + MySQL)
│   ├── README.md                # Documentación de la API
│   ├── server.js                # Servidor REST
│   └── lib/                     # Acceso a datos y utilidades
│
└── windows/                     # Beacon de Windows con UI (WPF)
    ├── BeaconApp/
    │   ├── README.md            # Documentación del beacon
    │   ├── MainWindow.xaml      # UI principal (XAML)
    │   ├── MainWindow.xaml.cs   # Code-behind de la ventana
    │   ├── Models/              # Modelos de datos (DTOs)
    │   ├── Services/            # ApiClient, BleBeaconService, FileLogger
    │   ├── ViewModels/          # MainViewModel (lógica de negocio)
    │   └── Config/              # Gestión de configuración
    └── GeoRacingBeacon.sln      # Solución Visual Studio
```
