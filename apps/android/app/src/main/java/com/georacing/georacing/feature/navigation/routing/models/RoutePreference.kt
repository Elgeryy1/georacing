package com.georacing.georacing.feature.navigation.routing.models

/**
 * Las posibles preferencias que un peatón puede seleccionar en su ruta.
 */
enum class RoutePreference {
    /** 
     * Minimiza la distancia sin importar el sol (Comportamiento clásico). 
     */
    FASTEST,
    
    /** 
     * Minimiza la exposición solar aplicando grandes penalizaciones a 
     * tramos descubiertos cuando hace calor, priorizando rutas más largas 
     * pero por la sombra de las tribunas/árboles.
     */
    COOLEST
}
