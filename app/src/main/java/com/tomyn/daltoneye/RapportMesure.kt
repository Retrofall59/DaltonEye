package com.tomyn.daltoneye

/**
 * Mise en forme des mesures pour le rapport de diagnostic (aucun affichage a l'ecran, aucun seuil).
 * Sert a separer trois sources d'erreur possibles : le capteur (RGB brut), la calibration
 * (RGB corrige) et le classement (voir ClassificateurCouleur.expliquer).
 */
object RapportMesure {

    /** Luminosite Y (Rec.601) de 0 a 255, calculee sur le RGB BRUT de la camera. */
    fun luminosite(r: Int, g: Int, b: Int): Int = (0.299 * r + 0.587 * g + 0.114 * b + 0.5).toInt()

    private fun rgb(t: Triple<Int, Int, Int>?): String =
        if (t == null) "non disponible"
        else "(${t.first}, ${t.second}, ${t.third}) = ${String.format("#%02X%02X%02X", t.first, t.second, t.third)}"

    /**
     * @param brut mediane brute de la derniere image (avant calibration)
     * @param corrigeInstantane cette meme mesure apres calibration
     * @param affiche valeur classee et affichee (moyenne des 8 dernieres images corrigees)
     */
    fun formaterMesures(
        brut: Triple<Int, Int, Int>?,
        corrigeInstantane: Triple<Int, Int, Int>?,
        affiche: Triple<Int, Int, Int>?
    ): String = buildString {
        appendLine("RGB camera brut (derniere image) : ${rgb(brut)}")
        appendLine(
            "Luminosite brute (Y) : " +
                if (brut == null) "non disponible" else "${luminosite(brut.first, brut.second, brut.third)}/255"
        )
        appendLine("RGB apres calibration (derniere image) : ${rgb(corrigeInstantane)}")
        append("RGB classe et affiche (moyenne des 8 dernieres images) : ${rgb(affiche)}")
    }
}
