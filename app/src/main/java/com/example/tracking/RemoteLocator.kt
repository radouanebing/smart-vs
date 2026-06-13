package com.example.tracking

import java.util.Locale

class RemoteLocator {

    fun generateMapsMarkerUrl(lat: Double, lng: Double): String {
        return "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
    }

    fun reverseGeocodeSimulatedName(lat: Double, lng: Double): String {
        // High quality simulated localized landmarks in Algiers, Algeria
        return when {
            lat == 36.7538 && lng == 3.0588 -> "Centre Ville, Algiers, Algeria"
            lat in 36.75..36.76 && lng in 3.05..3.06 -> "Grande Poste Office District, Alger Centre (Algeria)"
            lat in 36.72..36.74 && lng in 3.01..3.04 -> "Riadh El Feth Monument Area, Belouizdad"
            else -> "Wilaya d'Alger, Northern Algeria District [Lat: $lat, Lng: $lng]"
        }
    }
}
