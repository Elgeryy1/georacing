import Foundation
import Combine

@MainActor
class LocalizationUtils {
    
    // Simple localization dictionary for MVP
    // Key: [LanguageCode: String]
    private static let translations: [String: [String: String]] = [
        "Home": ["en": "Home", "es": "Inicio", "ca": "Inici"],
        "Map": ["en": "Map", "es": "Mapa", "ca": "Mapa"],
        "Shop": ["en": "Shop", "es": "Tienda", "ca": "Botiga"],
        "Report": ["en": "Report", "es": "Reportar", "ca": "Reportar"],
        "Settings": ["en": "Settings", "es": "Ajustes", "ca": "Configuració"],
        "Alerts": ["en": "Alerts", "es": "Alertas", "ca": "Alertes"],
        "Seat": ["en": "Seat", "es": "Asiento", "ca": "Seient"],
        
        // Home
        "Good Morning": ["en": "Good Morning", "es": "Buenos días", "ca": "Bon dia"],
        "Good Afternoon": ["en": "Good Afternoon", "es": "Buenas tardes", "ca": "Bona tarda"],
        "Good Evening": ["en": "Good Evening", "es": "Buenas noches", "ca": "Bona nit"],
        
        "Food": ["en": "Food", "es": "Comida", "ca": "Menjar"],
        "WC": ["en": "WC", "es": "Baños", "ca": "Banys"],
        "Parking": ["en": "Parking", "es": "Parking", "ca": "Pàrquing"],
        "Schedule": ["en": "Schedule", "es": "Horario", "ca": "Horari"],
        "Social": ["en": "Social", "es": "Social", "ca": "Social"],
        "Incidents": ["en": "Incidents", "es": "Incidencias", "ca": "Incidències"],
        
        // Settings
        "General": ["en": "General", "es": "General", "ca": "General"],
        "Language": ["en": "Language", "es": "Idioma", "ca": "Idioma"],
        "Appearance": ["en": "Appearance", "es": "Apariencia", "ca": "Aparença"],
        "My Seat Location": ["en": "My Seat Location", "es": "Mi Asiento", "ca": "El meu seient"],
        "Grandstand": ["en": "Grandstand", "es": "Tribuna", "ca": "Tribuna"],
        "Zone": ["en": "Zone", "es": "Zona", "ca": "Zona"],
        "Row": ["en": "Row", "es": "Fila", "ca": "Fila"],
        "Seat Number": ["en": "Seat Number", "es": "Asiento", "ca": "Seient"],
        "Save Seat Config": ["en": "Save Seat Config", "es": "Guardar Asiento", "ca": "Desar"],
        
        // Orders
        "Fan Shop": ["en": "Fan Shop", "es": "Tienda", "ca": "Botiga"],
        "Add": ["en": "Add", "es": "Añadir", "ca": "Afegir"],
        "Cart Total: $": ["en": "Cart Total: $", "es": "Total: $", "ca": "Total: $"],
        "Checkout": ["en": "Checkout", "es": "Comprar", "ca": "Comprar"],
        
        // Report Incident
        "Report Incident": ["en": "Report Incident", "es": "Reportar Incidencia", "ca": "Reportar Incidència"],
        "Category": ["en": "Category", "es": "Categoría", "ca": "Categoria"],
        "Description": ["en": "Description", "es": "Descripción", "ca": "Descripció"],
        "Select": ["en": "Select", "es": "Seleccionar", "ca": "Seleccionar"],
        "Submit Report": ["en": "Submit Report", "es": "Enviar Reporte", "ca": "Enviar"],
        "Report submitted successfully.": ["en": "Report submitted successfully.", "es": "Reporte enviado con éxito.", "ca": "Report enviat."],
        "Success": ["en": "Success", "es": "Éxito", "ca": "Èxit"],
        
        // Alerts
        "Alerts Title": ["en": "Alerts", "es": "Alertas", "ca": "Alertes"],
        
        // Dynamic & Badges
        "LIVE EVENT": ["en": "LIVE EVENT", "es": "EN VIVO", "ca": "EN DIRECTE"],
        "NEWS": ["en": "NEWS", "es": "NOTICIAS", "ca": "NOTÍCIES"],
        
        // Track Status
        "TRACK CLEAR": ["en": "TRACK CLEAR", "es": "PISTA LIBRE", "ca": "PISTA LLIURE"],
        "YELLOW FLAG": ["en": "YELLOW FLAG", "es": "BANDERA AMARILLA", "ca": "BANDERA GROGA"],
        "RED FLAG": ["en": "RED FLAG", "es": "BANDERA ROJA", "ca": "BANDERA VERMELLA"],
        "SAFETY CAR": ["en": "SAFETY CAR", "es": "SAFETY CAR", "ca": "COTXE DE SEGURETAT"],
        "VIRTUAL SC": ["en": "VIRTUAL SC", "es": "VIRTUAL SC", "ca": "VIRTUAL SC"],
        
        "Track is Green. Racing resumes.": ["en": "Track is Green. Racing resumes.", "es": "Pista libre. Carrera reanudada.", "ca": "Pista lliure. Cursa reanudada."],
        "Hazard reported. Slow down.": ["en": "Hazard reported. Slow down.", "es": "Peligro en pista. Reduzca velocidad.", "ca": "Perill en pista. Reduïu velocitat."],
        "Session Suspended. Return to pits.": ["en": "Session Suspended. Return to pits.", "es": "Sesión suspendida. Volver a boxes.", "ca": "Sessió suspesa. Tornar a boxes."],
        "Safety Car deployed.": ["en": "Safety Car deployed.", "es": "Safety Car desplegado.", "ca": "Cotxe de seguretat desplegat."],
        
        // Common UI Actions
        "Close": ["en": "Close", "es": "Cerrar", "ca": "Tancar"],
        "Cancel": ["en": "Cancel", "es": "Cancelar", "ca": "Cancel·lar"],
        "Save": ["en": "Save", "es": "Guardar", "ca": "Desar"],
        "Loading...": ["en": "Loading...", "es": "Cargando...", "ca": "Carregant..."],
        "Search...": ["en": "Search...", "es": "Buscar...", "ca": "Cercar..."],
        "All": ["en": "All", "es": "Todo", "ca": "Tot"],
        "Sign Out": ["en": "Sign Out", "es": "Cerrar sesión", "ca": "Tancar sessió"],
        "Sign Out?": ["en": "Sign Out?", "es": "¿Cerrar sesión?", "ca": "Tancar sessió?"],
        "Share": ["en": "Share", "es": "Compartir", "ca": "Compartir"],
        "Exit": ["en": "Exit", "es": "Salir", "ca": "Sortir"],
        "Loading products...": ["en": "Loading products...", "es": "Cargando productos...", "ca": "Carregant productes..."],
        "No results found": ["en": "No results found", "es": "No se encontraron resultados", "ca": "No s'han trobat resultats"],
        "Create New Group": ["en": "Create New Group", "es": "Crear Nuevo Grupo", "ca": "Crear Nou Grup"],
        "Leave Group": ["en": "Leave Group", "es": "Salir del Grupo", "ca": "Sortir del Grup"],
        "Order processed successfully": ["en": "Your order has been processed. Thank you!", "es": "Tu pedido ha sido procesado correctamente. ¡Gracias!", "ca": "La teva comanda s'ha processat correctament. Gràcies!"],
        "Customize Home": ["en": "Customize Home", "es": "Personalizar Inicio", "ca": "Personalitzar Inici"],
        "Save Seat": ["en": "Save Seat", "es": "Guardar Localidad", "ca": "Desar Localitat"],
        "Enable Notifications": ["en": "Enable Notifications", "es": "Habilitar Notificaciones", "ca": "Activar Notificacions"],
        "Open Settings": ["en": "Open Settings", "es": "Abrir Ajustes", "ca": "Obrir Configuració"],
        "Loading orders...": ["en": "Loading orders...", "es": "Cargando pedidos...", "ca": "Carregant comandes..."],
        "Add Widget": ["en": "Add Widget", "es": "Añadir Widget", "ca": "Afegir Widget"],
        "My Seat": ["en": "My Seat", "es": "Mi Localidad", "ca": "La meva Localitat"],
        "Seat Setup": ["en": "Seat Setup", "es": "Configuración de asiento", "ca": "Configuració de seient"],
        "Fan Settings": ["en": "Fan Settings", "es": "Configuración Fan", "ca": "Configuració Fan"],
        
        // Search function (used as placeholder)
        "Search function...": ["en": "Search function...", "es": "Buscar función...", "ca": "Cercar funció..."],
        
        // Error messages 
        "Description cannot be empty.": ["en": "Description cannot be empty.", "es": "La descripción no puede estar vacía.", "ca": "La descripció no pot estar buida."],
        "You must be logged in to report.": ["en": "You must be logged in to report.", "es": "Debes iniciar sesión para reportar.", "ca": "Has d'iniciar sessió per reportar."],
        "Could not get current location": ["en": "Could not get current location", "es": "No se pudo obtener la ubicación actual", "ca": "No s'ha pogut obtenir la ubicació actual"],
        "No route found": ["en": "No route found", "es": "No se encontró una ruta disponible", "ca": "No s'ha trobat cap ruta disponible"],
        "No routes found.": ["en": "No routes found.", "es": "No se encontraron rutas.", "ca": "No s'han trobat rutes."],
        
        // Staff Mode
        "Staff Mode": ["en": "Staff Mode", "es": "Modo Staff", "ca": "Mode Staff"],
        "Enter access PIN": ["en": "Enter access PIN", "es": "Introduce el PIN de acceso", "ca": "Introdueix el PIN d'accés"],
        "Control Panel": ["en": "Control Panel", "es": "Panel de Control", "ca": "Panell de Control"],
        "Section": ["en": "Section", "es": "Sección", "ca": "Secció"],
        "Status": ["en": "Status", "es": "Estado", "ca": "Estat"],
        "Send Alert": ["en": "Send Alert", "es": "Enviar Alerta", "ca": "Enviar Alerta"],
        "General Alert": ["en": "General Alert", "es": "Alerta General", "ca": "Alerta General"],
        "Send message to all users": ["en": "Send message to all users", "es": "Enviar mensaje a todos los usuarios", "ca": "Enviar missatge a tots els usuaris"],
        "Emergency": ["en": "Emergency", "es": "Emergencia", "ca": "Emergència"],
        "Activate emergency protocol": ["en": "Activate emergency protocol", "es": "Activar protocolo de emergencia", "ca": "Activar protocol d'emergència"],
        "Announcement": ["en": "Announcement", "es": "Anuncio", "ca": "Anunci"],
        "Send general information": ["en": "Send general information", "es": "Enviar información general", "ca": "Enviar informació general"],
        "Beacon Control": ["en": "Beacon Control", "es": "Control de Beacons", "ca": "Control de Beacons"],
        "Circuit Status": ["en": "Circuit Status", "es": "Estado del Circuito", "ca": "Estat del Circuit"],
        "Current Status": ["en": "Current Status", "es": "Estado Actual", "ca": "Estat Actual"],
        "GREEN FLAG": ["en": "GREEN FLAG", "es": "BANDERA VERDE", "ca": "BANDERA VERDA"],
        "Active Users": ["en": "Active Users", "es": "Usuarios Activos", "ca": "Usuaris Actius"],
        "Pending Alerts": ["en": "Pending Alerts", "es": "Alertas Pendientes", "ca": "Alertes Pendents"],
        "Active Beacons": ["en": "Active Beacons", "es": "Beacons Activos", "ca": "Beacons Actius"],
        
        // QR Scanner
        "Scan QR": ["en": "Scan QR", "es": "Escanear QR", "ca": "Escanejar QR"],
        "Point at QR code": ["en": "Point at QR code", "es": "Apunta al código QR", "ca": "Apunta al codi QR"],
        "Camera access needed": ["en": "Camera access needed", "es": "Se necesita acceso a la cámara", "ca": "Cal accés a la càmera"],
        "Could not access camera": ["en": "Could not access camera", "es": "No se pudo acceder a la cámara", "ca": "No s'ha pogut accedir a la càmera"],
        "Could not activate flash": ["en": "Could not activate flash", "es": "No se pudo activar el flash", "ca": "No s'ha pogut activar el flaix"],
        
        // Place names (Staff beacons)
        "Main Entrance": ["en": "Main Entrance", "es": "Entrada Principal", "ca": "Entrada Principal"],
        "Grandstand A": ["en": "Grandstand A", "es": "Tribuna A", "ca": "Tribuna A"],
        "Grandstand B": ["en": "Grandstand B", "es": "Tribuna B", "ca": "Tribuna B"],
        
        // SideMenuView
        "Quick Access": ["en": "Quick Access", "es": "Acceso Rápido", "ca": "Accés Ràpid"],
        "My Orders": ["en": "My Orders", "es": "Mis Pedidos", "ca": "Les meves Comandes"],
        "Purchase History": ["en": "Purchase History", "es": "Historial de compras", "ca": "Historial de compres"],
        "POI List": ["en": "POI List", "es": "Lista de POIs", "ca": "Llista de POIs"],
        "Points of Interest": ["en": "Points of Interest", "es": "Puntos de interés", "ca": "Punts d'interès"],
        "Project Progress": ["en": "Project Progress", "es": "Progreso del proyecto", "ca": "Progrés del projecte"],
        "GeoRacing Features": ["en": "GeoRacing Features", "es": "Funciones GeoRacing", "ca": "Funcions GeoRacing"],
        "Overview": ["en": "Overview", "es": "Vista General", "ca": "Vista General"],
        
        // GroupView
        "Connect with your group": ["en": "Connect with your group", "es": "Conéctate con tu grupo", "ca": "Connecta't amb el teu grup"],
        "Create a group to share location in real time at the circuit.": ["en": "Create a group to share location in real time at the circuit.", "es": "Crea un grupo para compartir ubicación en tiempo real en el circuito.", "ca": "Crea un grup per compartir ubicació en temps real al circuit."],
        "Group Code": ["en": "Group Code", "es": "Código de Grupo", "ca": "Codi de Grup"],
        "Join": ["en": "Join", "es": "Unirse", "ca": "Unir-se"],
        "Or": ["en": "Or", "es": "O", "ca": "O"],
        "View on Map": ["en": "View on Map", "es": "Ver en Mapa", "ca": "Veure al Mapa"],
        
        // SeatSetupView
        "Seat saved": ["en": "Seat saved", "es": "Localidad guardada", "ca": "Localitat desada"],
        "Your seat has been saved. You can use it to navigate directly to your seat.": ["en": "Your seat has been saved. You can use it to navigate directly to your seat.", "es": "Tu localidad ha sido guardada. Podrás usarla para navegar directamente a tu asiento.", "ca": "La teva localitat s'ha desat. Podràs usar-la per navegar directament al teu seient."],
        "Configure your seat": ["en": "Configure your seat", "es": "Configura tu asiento", "ca": "Configura el teu seient"],
        "Save your seat to navigate directly to it from anywhere in the circuit.": ["en": "Save your seat to navigate directly to it from anywhere in the circuit.", "es": "Guarda tu localidad para poder navegar directamente a ella desde cualquier punto del circuito.", "ca": "Desa la teva localitat per poder navegar directament des de qualsevol punt del circuit."],
        "e.g. Main Grandstand": ["en": "e.g. Main Grandstand", "es": "Ej: Tribuna Principal", "ca": "Ex: Tribuna Principal"],
        "e.g. Zone A": ["en": "e.g. Zone A", "es": "Ej: Zona A", "ca": "Ex: Zona A"],
        "e.g. Row 12": ["en": "e.g. Row 12", "es": "Ej: Fila 12", "ca": "Ex: Fila 12"],
        "e.g. 24": ["en": "e.g. 24", "es": "Ej: 24", "ca": "Ex: 24"],
        
        // ParkingHomeView
        "Parking Management": ["en": "Parking Management", "es": "Gestión de Parking", "ca": "Gestió de Pàrquing"],
        "View Route": ["en": "View Route", "es": "Ver Ruta", "ca": "Veure Ruta"],
        "Release / Change Assignment": ["en": "Release / Change Assignment", "es": "Liberar / Cambiar Asignación", "ca": "Alliberar / Canviar Assignació"],
        "Loading assignment...": ["en": "Loading assignment...", "es": "Cargando asignación...", "ca": "Carregant assignació..."],
        "No parking assigned": ["en": "No parking assigned", "es": "No tienes parking asignado", "ca": "No tens pàrquing assignat"],
        "Assign my spot": ["en": "Assign my spot", "es": "Asignar mi plaza", "ca": "Assignar la meva plaça"],
        
        // ParkingWizardViews
        "Step 1 of 3": ["en": "Step 1 of 3", "es": "Paso 1 de 3", "ca": "Pas 1 de 3"],
        "Scan your ticket": ["en": "Scan your ticket", "es": "Escanea tu entrada", "ca": "Escaneja la teva entrada"],
        "Simulating camera...": ["en": "Simulating camera...", "es": "Simulando cámara...", "ca": "Simulant càmera..."],
        "Simulate Scan": ["en": "Simulate Scan", "es": "Simular Escaneo", "ca": "Simular Escaneig"],
        "Ticket Detected": ["en": "Ticket Detected", "es": "Ticket Detectado", "ca": "Tiquet Detectat"],
        "Enter code manually": ["en": "Enter code manually", "es": "Introducir código manualmente", "ca": "Introduir codi manualment"],
        "Continue": ["en": "Continue", "es": "Continuar", "ca": "Continuar"],
        "Step 2 of 3": ["en": "Step 2 of 3", "es": "Paso 2 de 3", "ca": "Pas 2 de 3"],
        "Enter your license plate": ["en": "Enter your license plate", "es": "Introduce tu matrícula", "ca": "Introdueix la teva matrícula"],
        "Required to validate your parking access.": ["en": "Required to validate your parking access.", "es": "Necesaria para validar tu acceso al parking.", "ca": "Necessària per validar el teu accés al pàrquing."],
        "Confirm": ["en": "Confirm", "es": "Confirmar", "ca": "Confirmar"],
        "Review details": ["en": "Review details", "es": "Revisa los datos", "ca": "Revisa les dades"],
        "Ticket": ["en": "Ticket", "es": "Entrada", "ca": "Entrada"],
        "Valid until end of day": ["en": "Valid until end of day", "es": "Válido hasta el final del día", "ca": "Vàlid fins al final del dia"],
        "Vehicle": ["en": "Vehicle", "es": "Vehículo", "ca": "Vehicle"],
        "License Plate": ["en": "License Plate", "es": "Matrícula", "ca": "Matrícula"],
        "Confirm and Assign": ["en": "Confirm and Assign", "es": "Confirmar y Asignar", "ca": "Confirmar i Assignar"],
        "Spot Assigned!": ["en": "Spot Assigned!", "es": "¡Plaza Asignada!", "ca": "Plaça Assignada!"],
        "Go to Zone": ["en": "Go to Zone", "es": "Dirígete a la Zona", "ca": "Dirigeix-te a la Zona"],
        "Virtual Spot": ["en": "Virtual Spot", "es": "Plaza Virtual", "ca": "Plaça Virtual"],
        "Staff Validation": ["en": "Staff Validation", "es": "Validación Staff", "ca": "Validació Staff"],
        "This QR code is your confirmed access pass. Show it to security staff to enter your zone.": ["en": "This QR code is your confirmed access pass. Show it to security staff to enter your zone.", "es": "Este código QR es tu pase de acceso confirmado. Muéstralo al personal de seguridad para entrar a tu zona.", "ca": "Aquest codi QR és el teu passi d'accés confirmat. Mostra'l al personal de seguretat per entrar a la teva zona."],
        "Go to Home": ["en": "Go to Home", "es": "Ir al Inicio", "ca": "Anar a l'Inici"],
        
        // ParkingDetailViews
        "Validation Code": ["en": "Validation Code", "es": "Código de Validación", "ca": "Codi de Validació"],
        "This QR code validates your access to the assigned zone. Keep brightness high when scanning.": ["en": "This QR code validates your access to the assigned zone. Keep brightness high when scanning.", "es": "Este código QR valida tu acceso a la zona asignada. Mantén brillo alto al escanear.", "ca": "Aquest codi QR valida el teu accés a la zona assignada. Mantén la brillantor alta en escanejar."],
        "Access Instructions": ["en": "Access Instructions", "es": "Instrucciones de Acceso", "ca": "Instruccions d'Accés"],
        "Follow signs to Zone %@. Upon arrival, show this code to staff or scan the QR at the barrier if available.": ["en": "Follow signs to Zone %@. Upon arrival, show this code to staff or scan the QR at the barrier if available.", "es": "Sigue las señales hacia la Zona %@. Al llegar, muestra este código al personal o escanea el QR en la barrera si está disponible.", "ca": "Segueix els senyals cap a la Zona %@. En arribar, mostra aquest codi al personal o escaneja el QR a la barrera si està disponible."],
        "View location on map": ["en": "View location on map", "es": "Ver ubicación en mapa", "ca": "Veure ubicació al mapa"],
        "No active assignment": ["en": "No active assignment", "es": "No hay asignación activa", "ca": "No hi ha assignació activa"],
        "Parking Detail": ["en": "Parking Detail", "es": "Detalle Parking", "ca": "Detall Pàrquing"],
        "Guidance In Progress": ["en": "Guidance In Progress", "es": "Guiado en Curso", "ca": "Guiatge en Curs"],
        "Head to Zone %@": ["en": "Head to Zone %@", "es": "Dirígete hacia la Zona %@", "ca": "Dirigeix-te cap a la Zona %@"],
        "End Navigation": ["en": "End Navigation", "es": "Terminar Navegación", "ca": "Finalitzar Navegació"],
        "FAQ": ["en": "FAQ", "es": "Preguntas Frecuentes", "ca": "Preguntes Freqüents"],
        "Date": ["en": "Date", "es": "Fecha", "ca": "Data"],
        
        // CarPlayTemplateFactory
        "Destinations": ["en": "Destinations", "es": "Destinos", "ca": "Destinacions"],
        "Navigate": ["en": "Navigate", "es": "Navegar", "ca": "Navegar"],
        "Save My Location": ["en": "Save My Location", "es": "Guardar Mi Ubicación", "ca": "Desar La Meva Ubicació"],
        "Remember where you parked": ["en": "Remember where you parked", "es": "Recuerda dónde aparcaste", "ca": "Recorda on has aparcat"],
        "Go to My Car": ["en": "Go to My Car", "es": "Ir a Mi Coche", "ca": "Anar al Meu Cotxe"],
        "Your Parking": ["en": "Your Parking", "es": "Tu Parking", "ca": "El teu Pàrquing"],
        "Available Parkings": ["en": "Available Parkings", "es": "Parkings Disponibles", "ca": "Pàrquings Disponibles"],
        "Event": ["en": "Event", "es": "Evento", "ca": "Esdeveniment"],
        "Time": ["en": "Time", "es": "Hora", "ca": "Hora"],
        "Gates": ["en": "Gates", "es": "Puertas", "ca": "Portes"],
        "Open": ["en": "Open", "es": "Abiertas", "ca": "Obertes"],
        "Closed": ["en": "Closed", "es": "Cerradas", "ca": "Tancades"],
        "Offline Mode": ["en": "Offline Mode", "es": "Modo Offline", "ca": "Mode Offline"],
        "Enabled": ["en": "Enabled", "es": "Activado", "ca": "Activat"],
        "Disabled": ["en": "Disabled", "es": "Desactivado", "ca": "Desactivat"],
        "Version": ["en": "Version", "es": "Versión", "ca": "Versió"],
        "Finish": ["en": "Finish", "es": "Terminar", "ca": "Finalitzar"],
        "Main Entry": ["en": "Main Entry", "es": "Entrada Principal", "ca": "Entrada Principal"],
        "Paddock Access": ["en": "Paddock Access", "es": "Acceso Paddock", "ca": "Accés Paddock"],
        "Pass only": ["en": "Pass only", "es": "Solo con pase", "ca": "Només amb passi"],
        "Main Straight": ["en": "Main Straight", "es": "Recta Meta", "ca": "Recta Meta"],
        "Near entrance": ["en": "Near entrance", "es": "Cercano a entrada", "ca": "Proper a entrada"],
        "Credentials only": ["en": "Credentials only", "es": "Solo con acreditación", "ca": "Només amb acreditació"],
        "Saved location": ["en": "Saved location", "es": "Ubicación guardada", "ca": "Ubicació desada"],
        "Saved": ["en": "Saved", "es": "Guardado", "ca": "Desat"],
        "Parking location saved": ["en": "Parking location saved", "es": "Ubicación del parking guardada", "ca": "Ubicació del pàrquing desada"],
        
        // NavigationService / TransportMode
        "Car": ["en": "Car", "es": "Coche", "ca": "Cotxe"],
        "On foot": ["en": "On foot", "es": "A pie", "ca": "A peu"],
        "Transit": ["en": "Transit", "es": "Transporte", "ca": "Transport"],
        
        // MapViewModel errors
        "Could not get your location": ["en": "Could not get your location", "es": "No se pudo obtener tu ubicación", "ca": "No s'ha pogut obtenir la teva ubicació"],
        "Route calculation error": ["en": "Route calculation error: %@", "es": "Error al calcular ruta: %@", "ca": "Error en calcular ruta: %@"],
        "You have arrived at your destination.": ["en": "You have arrived at your destination.", "es": "Has llegado a tu destino.", "ca": "Has arribat a la teva destinació."],
        
        // OrdersViewModel errors
        "You must be logged in to place an order": ["en": "You must be logged in to place an order", "es": "Debes iniciar sesión para realizar un pedido", "ca": "Has d'iniciar sessió per fer una comanda"],
        "Cart is empty": ["en": "Cart is empty", "es": "El carrito está vacío", "ca": "El carret està buit"],
        "Order processing error": ["en": "Order processing error: %@", "es": "Error al procesar el pedido: %@", "ca": "Error en processar la comanda: %@"],
        
        // PublicTransportViewModel errors
        "Connection error": ["en": "Connection error: %@", "es": "Error de conexión: %@", "ca": "Error de connexió: %@"],
        
        // ParkingModels errors
        "Invalid license plate.": ["en": "The license plate entered is not valid.", "es": "La matrícula introducida no es válida.", "ca": "La matrícula introduïda no és vàlida."],
        "Invalid ticket.": ["en": "The ticket is not valid or could not be read.", "es": "El ticket no es válido o no se ha podido leer.", "ca": "El tiquet no és vàlid o no s'ha pogut llegir."],
        "Could not save the assignment.": ["en": "Could not save the assignment.", "es": "No se ha podido guardar la asignación.", "ca": "No s'ha pogut desar l'assignació."],
        "An unknown error occurred.": ["en": "An unknown error occurred.", "es": "Ha ocurrido un error desconocido.", "ca": "S'ha produït un error desconegut."],
        
        // GuidanceViewModel TTS
        "Walk to %@. You are %d meters away.": ["en": "Walk to %@. You are %d meters away.", "es": "Camina hacia %@. Estás a %d metros.", "ca": "Camina cap a %@. Ets a %d metres."],
        "Board bus %@ towards %@.": ["en": "Board bus %@ towards %@.", "es": "Sube al autobús %@ hacia %@.", "ca": "Puja a l'autobús %@ cap a %@."],
        "Take train %@ direction %@.": ["en": "Take train %@ direction %@.", "es": "Toma el tren %@ dirección %@.", "ca": "Agafa el tren %@ direcció %@."],
        "Head to %@": ["en": "Head to %@", "es": "Dirígete a %@", "ca": "Dirigeix-te a %@"],
        
        // RoadmapView status
        "Completed": ["en": "Completed", "es": "Completado", "ca": "Completat"],
        "In Progress": ["en": "In Progress", "es": "En progreso", "ca": "En progrés"],
        "Planned": ["en": "Planned", "es": "Planeado", "ca": "Planejat"],
        "Future": ["en": "Future", "es": "Futuro", "ca": "Futur"],
        
        // SettingsView
        "Notifications": ["en": "Notifications", "es": "Notificaciones", "ca": "Notificacions"],
        "Push Notifications": ["en": "Push Notifications", "es": "Notificaciones push", "ca": "Notificacions push"],
        "Configure my seat": ["en": "Configure my seat", "es": "Configurar mi localidad", "ca": "Configurar la meva localitat"],
        
        // CircuitMapView
        "Go to Circuit": ["en": "Go to Circuit", "es": "Ir al Circuito", "ca": "Anar al Circuit"],
        "Follow the route": ["en": "Follow the route", "es": "Sigue la ruta", "ca": "Segueix la ruta"],
        
        // PublicTransportSheetView / Transit
        "Open in Apple Maps": ["en": "Open in Apple Maps", "es": "Abrir en Apple Maps", "ca": "Obrir a Apple Maps"],
        "Public Transport": ["en": "Public Transport", "es": "Transporte Público", "ca": "Transport Públic"],
        "Transit opens in Apple Maps": ["en": "Transit directions will open in Apple Maps", "es": "Las indicaciones de transporte público se abrirán en Apple Maps", "ca": "Les indicacions de transport públic s'obriran a Apple Maps"],
        
        // OnboardingView
        "The ultimate circuit experience. Follow the race, track status and locate services.": ["en": "The ultimate circuit experience. Follow the race, track status and locate services.", "es": "La experiencia definitiva en el circuito. Sigue la carrera, el estado de la pista y localiza servicios.", "ca": "L'experiència definitiva al circuit. Segueix la cursa, l'estat de la pista i localitza serveis."],
        "Find food, WC, parking and your friends on the interactive circuit map.": ["en": "Find food, WC, parking and your friends on the interactive circuit map.", "es": "Encuentra comida, WC, parking y a tus amigos en el mapa interactivo del circuito.", "ca": "Troba menjar, WC, pàrquing i els teus amics al mapa interactiu del circuit."],
        "To alert you about Safety Cars and emergencies, we need to send you notifications.": ["en": "To alert you about Safety Cars and emergencies, we need to send you notifications.", "es": "Para avisarte de Safety Cars y emergencias, necesitamos enviarte notificaciones.", "ca": "Per avisar-te de Safety Cars i emergències, necessitem enviar-te notificacions."],
        
        // FeaturePlaceholderView
        "Simulation Environment": ["en": "Simulation Environment", "es": "Entorno de Simulación", "ca": "Entorn de Simulació"],
        
        // GuidanceView
        "Route Guidance": ["en": "Route Guidance", "es": "Guiado en Ruta", "ca": "Guiatge en Ruta"],
        
        // ItineraryDetailSheet
        "Trip Detail": ["en": "Trip Detail", "es": "Detalle del Viaje", "ca": "Detall del Viatge"],
        
        // FanZoneView
        "Checkpoint: Trivia": ["en": "Checkpoint: Trivia", "es": "Punto de Control: Trivia", "ca": "Punt de Control: Trivia"],
        
        // PoiListView
        "Entries": ["en": "Entries", "es": "Entradas", "ca": "Entrades"],
        
        // NavigationScreen (GPS)
        "Destination": ["en": "Destination", "es": "Destino", "ca": "Destinació"],
        "Start Navigation": ["en": "Start Navigation", "es": "Iniciar Navegación", "ca": "Iniciar Navegació"],
        "Calculating route...": ["en": "Calculating route...", "es": "Calculando ruta...", "ca": "Calculant ruta..."],
        "Recalculating route": ["en": "Recalculating route", "es": "Recalculando ruta", "ca": "Recalculant ruta"],
        "You have arrived!": ["en": "You have arrived!", "es": "¡Has llegado!", "ca": "Has arribat!"],
        "Arrival": ["en": "Arrival", "es": "Llegada", "ca": "Arribada"],
        "remaining": ["en": "remaining", "es": "restante", "ca": "restant"],
        "distance": ["en": "distance", "es": "distancia", "ca": "distància"],
        "Retry": ["en": "Retry", "es": "Reintentar", "ca": "Reintentar"],
        "Location permission required": ["en": "Location permission required", "es": "Se necesita permiso de ubicación", "ca": "Cal permís d'ubicació"],
        "No GPS permission": ["en": "No GPS permission", "es": "Sin permiso GPS", "ca": "Sense permís GPS"],
        "Searching GPS...": ["en": "Searching GPS...", "es": "Buscando GPS...", "ca": "Cercant GPS..."],
        "Low GPS accuracy": ["en": "Low GPS accuracy", "es": "Precisión GPS baja", "ca": "Precisió GPS baixa"],
        
        // Fan Zone
        "Fan Zone": ["en": "Fan Zone", "es": "Fan Zone", "ca": "Fan Zone"],
        "Choose Your Team": ["en": "Choose Your Team", "es": "Elige Tu Equipo", "ca": "Tria el Teu Equip"],
        "Done": ["en": "Done", "es": "Listo", "ca": "Fet"],
        "Trivia": ["en": "Trivia", "es": "Trivia", "ca": "Trivia"],
        "played": ["en": "played", "es": "jugadas", "ca": "jugades"],
        "News": ["en": "News", "es": "Noticias", "ca": "Notícies"],
        "articles": ["en": "articles", "es": "artículos", "ca": "articles"],
        "Cards": ["en": "Cards", "es": "Cromos", "ca": "Cromos"],
        "Latest News": ["en": "Latest News", "es": "Últimas Noticias", "ca": "Últimes Notícies"],
        "See all": ["en": "See all", "es": "Ver todo", "ca": "Veure tot"],
        "Loading news...": ["en": "Loading news...", "es": "Cargando noticias...", "ca": "Carregant notícies..."],
        "Quick Trivia": ["en": "Quick Trivia", "es": "Trivia Rápida", "ca": "Trivia Ràpida"],
        "My Collection": ["en": "My Collection", "es": "Mi Colección", "ca": "La Meva Col·lecció"],
        "Card Unlocked!": ["en": "Card Unlocked!", "es": "¡Cromo Desbloqueado!", "ca": "Cromo Desbloquejat!"],
        "Select Team": ["en": "Select Team", "es": "Seleccionar Equipo", "ca": "Seleccionar Equip"],

        // Quiz
        "Question": ["en": "Question", "es": "Pregunta", "ca": "Pregunta"],
        "Streak": ["en": "Streak", "es": "Racha", "ca": "Ratxa"],
        "Correct!": ["en": "Correct!", "es": "¡Correcto!", "ca": "Correcte!"],
        "Incorrect": ["en": "Incorrect", "es": "Incorrecto", "ca": "Incorrecte"],
        "Next": ["en": "Next", "es": "Siguiente", "ca": "Següent"],
        "See Results": ["en": "See Results", "es": "Ver Resultados", "ca": "Veure Resultats"],
        "Quiz Complete!": ["en": "Quiz Complete!", "es": "¡Quiz Completado!", "ca": "Quiz Completat!"],
        "Perfect!": ["en": "Perfect!", "es": "¡Perfecto!", "ca": "Perfecte!"],
        "Excellent!": ["en": "Excellent!", "es": "¡Excelente!", "ca": "Excel·lent!"],
        "Good job!": ["en": "Good job!", "es": "¡Buen trabajo!", "ca": "Bon treball!"],
        "Keep trying!": ["en": "Keep trying!", "es": "¡Sigue intentándolo!", "ca": "Segueix intentant-ho!"],
        "Play Again": ["en": "Play Again", "es": "Jugar de Nuevo", "ca": "Jugar de Nou"],
        "Back to Fan Zone": ["en": "Back to Fan Zone", "es": "Volver al Fan Zone", "ca": "Tornar al Fan Zone"],
        "Accuracy": ["en": "Accuracy", "es": "Precisión", "ca": "Precisió"],
        "Best Streak": ["en": "Best Streak", "es": "Mejor Racha", "ca": "Millor Ratxa"],
        "Total": ["en": "Total", "es": "Total", "ca": "Total"],
        "Loading questions...": ["en": "Loading questions...", "es": "Cargando preguntas...", "ca": "Carregant preguntes..."],

        // Fan News
        "Updated": ["en": "Updated", "es": "Actualizado", "ca": "Actualitzat"],
        "Never": ["en": "Never", "es": "Nunca", "ca": "Mai"],
        "Just now": ["en": "Just now", "es": "Ahora", "ca": "Ara"],
        "No news available": ["en": "No news available", "es": "No hay noticias", "ca": "No hi ha notícies"],
        "Refresh": ["en": "Refresh", "es": "Actualizar", "ca": "Actualitzar"],

        // Card Collection
        "Collection Progress": ["en": "Collection Progress", "es": "Progreso de Colección", "ca": "Progrés de Col·lecció"],
        "Unlocked": ["en": "Unlocked", "es": "Desbloqueados", "ca": "Desbloquejats"],
        "Locked": ["en": "Locked", "es": "Bloqueados", "ca": "Bloquejats"],
        "Common": ["en": "Common", "es": "Común", "ca": "Comú"],
        "Rare": ["en": "Rare", "es": "Raro", "ca": "Rar"],
        "Epic": ["en": "Epic", "es": "Épico", "ca": "Èpic"],
        "Legendary": ["en": "Legendary", "es": "Legendario", "ca": "Llegendari"],
        "No cards in this filter": ["en": "No cards in this filter", "es": "No hay cromos en este filtro", "ca": "No hi ha cromos en aquest filtre"],
        "Formula 1": ["en": "Formula 1", "es": "Fórmula 1", "ca": "Fórmula 1"]
    ]
    
    static func string(_ key: String) -> String {
        let lang = UserPreferences.shared.languageCode
        return translations[key]?[lang] ?? key
    }
    
    static var locale: Locale {
        return Locale(identifier: UserPreferences.shared.languageCode)
    }
}
