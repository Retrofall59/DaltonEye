package com.tomyn.daltoneye

/**
 * Gere la calibration sur blanc/gris neutre : corrige la derive de teinte due a l'eclairage
 * ambiant (lumiere du jour vs lumiere artificielle), et impose une recalibration bloquante
 * toutes les heures (le delai court en continu tant que l'appli tourne, meme en pleine session).
 */
class GestionnaireCalibration {

    companion object {
        const val DELAI_CALIBRATION_MS = 60 * 60 * 1000L // 1 heure
        // Cible neutre : ce que devrait mesurer la camera sur un blanc/gris parfaitement neutre.
        // Choisie proche de la reference "Blanc" (F5F5F5) pour qu'apres calibration sur une feuille
        // blanche, celle-ci soit bien classee "Blanc" et non "Transparent" (bug corrige : 200 tombait
        // presque exactement sur la reference "Transparent" C9C9C9, d'ou la confusion).
        const val CIBLE_R = 240
        const val CIBLE_G = 240
        const val CIBLE_B = 240
    }

    private var facteurR = 1.0
    private var facteurG = 1.0
    private var facteurB = 1.0
    private var dateDerniereCalibration: Long = 0L

    /** A appeler avec la couleur moyenne captee pendant que le telephone pointe sur le blanc de reference. */
    fun calibrerSur(r: Int, g: Int, b: Int, maintenant: Long) {
        facteurR = if (r > 0) CIBLE_R.toDouble() / r else 1.0
        facteurG = if (g > 0) CIBLE_G.toDouble() / g else 1.0
        facteurB = if (b > 0) CIBLE_B.toDouble() / b else 1.0
        dateDerniereCalibration = maintenant
    }

    fun calibrationNecessaire(maintenant: Long): Boolean =
        dateDerniereCalibration == 0L || (maintenant - dateDerniereCalibration) >= DELAI_CALIBRATION_MS

    /** Temps restant avant la prochaine calibration obligatoire, en millisecondes (0 si deja due). */
    fun tempsRestantMs(maintenant: Long): Long {
        if (dateDerniereCalibration == 0L) return 0L
        val restant = DELAI_CALIBRATION_MS - (maintenant - dateDerniereCalibration)
        return if (restant > 0L) restant else 0L
    }

    /** Applique la correction de calibration a une couleur brute captee par la camera. */
    fun corriger(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        fun ajuste(valeur: Int, facteur: Double): Int =
            (valeur * facteur).toInt().coerceIn(0, 255)
        return Triple(ajuste(r, facteurR), ajuste(g, facteurG), ajuste(b, facteurB))
    }
}
