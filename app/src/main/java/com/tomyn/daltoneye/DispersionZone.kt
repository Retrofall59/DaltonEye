package com.tomyn.daltoneye

/**
 * Resume statistique d'un canal de couleur sur la zone analysee.
 * La mediane est calculee EXACTEMENT comme avant (element central de la liste triee), donc
 * l'ajout de la dispersion ne change en rien la couleur mesuree ni la classification.
 */
object DispersionZone {

    data class Canal(val mediane: Int, val p10: Int, val p90: Int)

    /** Trie la liste (en place) et en tire la mediane et les percentiles 10 et 90 (rang inferieur). */
    fun resumer(valeurs: MutableList<Int>): Canal {
        valeurs.sort()
        val n = valeurs.size
        val mediane = valeurs[n / 2]
        val p10 = valeurs[(n - 1) * 10 / 100]
        val p90 = valeurs[(n - 1) * 90 / 100]
        return Canal(mediane, p10, p90)
    }

    /**
     * Texte pour le rapport de diagnostic. Valeurs BRUTES de la camera (avant calibration) :
     * un ecart p10-p90 large sur un canal = zone non homogene (pieces multicolores, reflets,
     * texture de couches, fond qui deborde dans le cercle...).
     */
    fun formater(r: Canal, g: Canal, b: Canal, nbPixels: Int): String =
        "R ${r.p10}-${r.p90}, G ${g.p10}-${g.p90}, B ${b.p10}-${b.p90} (p10-p90, brut) sur $nbPixels pixels"
}
