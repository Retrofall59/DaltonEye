package com.tomyn.daltoneye

/**
 * Tout ce qui a ete mesure sur UNE MEME image, regroupe en un seul objet immuable.
 * Le rapport de diagnostic lit cet objet d'un seul bloc : toutes les valeurs viennent donc de la meme
 * image (avant v1.26, certaines etaient mises a jour avant le test "affichage fige" et d'autres apres,
 * ce qui pouvait melanger des valeurs en direct et des valeurs figees dans un meme rapport).
 */
data class InstantaneMesure(
    val horodatageMs: Long,
    val brut: Triple<Int, Int, Int>,               // mediane brute de la camera (avant calibration)
    val corrigeInstantane: Triple<Int, Int, Int>,  // cette mesure apres calibration
    val affiche: Triple<Int, Int, Int>,            // valeur classee et affichee (moyenne des 8 dernieres images)
    val famille: String,
    val dispersion: String
)

/**
 * Mise en forme du rapport de diagnostic (aucun affichage a l'ecran, aucun seuil).
 * Sert a separer les sources d'erreur possibles : capteur (RGB brut), calibration (RGB corrige),
 * classement (ClassificateurCouleur.expliquer).
 */
object RapportMesure {

    /** Luminosite Y (Rec.601) de 0 a 255, calculee sur le RGB BRUT de la camera. */
    fun luminosite(r: Int, g: Int, b: Int): Int = (0.299 * r + 0.587 * g + 0.114 * b + 0.5).toInt()

    private fun rgb(t: Triple<Int, Int, Int>): String =
        "(${t.first}, ${t.second}, ${t.third}) = ${String.format("#%02X%02X%02X", t.first, t.second, t.third)}"

    /** "il y a 12 s" / "il y a 3 min" : permet de savoir si on copie une mesure fraiche. */
    fun age(millisecondes: Long): String {
        val s = (millisecondes / 1000).coerceAtLeast(0)
        return if (s < 120) "il y a $s s" else "il y a ${s / 60} min"
    }

    private fun bloc(titre: String, m: InstantaneMesure): String = buildString {
        appendLine(titre)
        appendLine("RGB camera brut : ${rgb(m.brut)}")
        appendLine("Luminosite brute (Y) : ${luminosite(m.brut.first, m.brut.second, m.brut.third)}/255")
        appendLine("RGB apres calibration : ${rgb(m.corrigeInstantane)}")
        appendLine("RGB classe et affiche (moyenne des 8 dernieres images) : ${rgb(m.affiche)}")
        appendLine("Dispersion de la zone : ${m.dispersion}")
        append(
            ClassificateurCouleur.formaterExplication(
                ClassificateurCouleur.expliquer(m.affiche.first, m.affiche.second, m.affiche.third)
            )
        )
    }

    /**
     * @param actuelle derniere image entierement traitee (jamais mise a jour pendant un gel de l'affichage)
     * @param validation la mesure au moment ou la lecture a ete jugee stable (celle qui declenche la
     *        vibration) : reste disponible meme si la piece a ete retiree avant de copier le rapport
     */
    fun formaterRapport(actuelle: InstantaneMesure?, validation: InstantaneMesure?, maintenantMs: Long): String = buildString {
        if (actuelle == null) {
            appendLine("Mesure actuelle : non disponible")
        } else {
            appendLine(bloc("--- Mesure actuelle (${age(maintenantMs - actuelle.horodatageMs)}) ---", actuelle))
        }
        appendLine()
        if (validation == null) {
            append("Derniere validation (lecture stable) : aucune depuis l'ouverture de l'appli")
        } else {
            append(
                bloc(
                    "--- Derniere validation : lecture stable, famille ${validation.famille} (${age(maintenantMs - validation.horodatageMs)}) ---",
                    validation
                )
            )
        }
    }
}
