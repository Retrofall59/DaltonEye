package com.tomyn.daltoneye

/**
 * Gere la calibration en DEUX points (blanc + noir), pour corriger a la fois :
 * - le desequilibre de teinte proportionnel (facteur multiplicatif, calcule via le point blanc,
 *   comme avant) : derive due a l'eclairage ambiant (jour vs LED).
 * - le "voile" de teinte constant (offset additif, calcule via le point noir) : une petite
 *   dominante de couleur qui reste quasi invisible sur du blanc (tres lumineux, l'ecart est
 *   negligeable en proportion) mais devient tres visible sur du noir (peu de lumiere, le meme
 *   ecart devient enorme en proportion) - c'est ce qui faisait apparaitre du "Marron" sur des
 *   objets reellement noirs, meme apres une calibration correcte sur blanc seul.
 *
 * Sequence complete : pointer sur du blanc -> "Calibrer sur ce blanc" (verrouille aussi la
 * balance des blancs materielle) -> pointer sur du noir -> "Calibrer sur ce noir" (mesure le
 * voile, termine la calibration). Impose une recalibration bloquante toutes les heures (le delai
 * court en continu tant que l'appli tourne, meme en pleine session).
 */
class GestionnaireCalibration {

    enum class Etape { POINT_BLANC, POINT_NOIR }

    companion object {
        const val DELAI_CALIBRATION_MS_DEFAUT = 60 * 60 * 1000L // 1 heure, valeur par defaut
        // Cibles neutres : ce que devrait mesurer la camera sur un blanc et un noir parfaitement
        // neutres. CIBLE_BLANC proche de la reference "Blanc" (F5F5F5) pour qu'apres calibration
        // une feuille blanche soit bien classee "Blanc" et non "Transparent". CIBLE_NOIR proche
        // de la reference "Noir" (0A0A0A).
        const val CIBLE_BLANC = 240
        const val CIBLE_NOIR = 10
    }

    /** Reglable depuis l'ecran Parametres (30 min / 1h / 2h). */
    var delaiCalibrationMs: Long = DELAI_CALIBRATION_MS_DEFAUT

    private var etapeEnCours = Etape.POINT_BLANC

    private var mesureBlancR = 0
    private var mesureBlancG = 0
    private var mesureBlancB = 0

    // Offset (point noir) et echelle (calcules une fois les deux points captures)
    private var decalageR = 0.0
    private var decalageG = 0.0
    private var decalageB = 0.0
    private var echelleR = 1.0
    private var echelleG = 1.0
    private var echelleB = 1.0

    private var dateDerniereCalibration: Long = 0L

    fun etapeActuelle(): Etape = etapeEnCours

    /** Premiere etape : pointer sur une feuille blanche / surface neutre claire. */
    fun capturerPointBlanc(r: Int, g: Int, b: Int) {
        mesureBlancR = r
        mesureBlancG = g
        mesureBlancB = b
        etapeEnCours = Etape.POINT_NOIR
    }

    /** Deuxieme etape : pointer sur une zone noire. Termine la calibration. */
    fun capturerPointNoir(r: Int, g: Int, b: Int, maintenant: Long) {
        fun calculer(mesureBlanc: Int, mesureNoir: Int, cibleBlanc: Int, cibleNoir: Int): Pair<Double, Double> {
            val ecartMesure = mesureBlanc - mesureNoir
            val echelle = if (ecartMesure != 0) (cibleBlanc - cibleNoir).toDouble() / ecartMesure else 1.0
            return Pair(mesureNoir.toDouble(), echelle)
        }
        val (decR, echR) = calculer(mesureBlancR, r, CIBLE_BLANC, CIBLE_NOIR)
        val (decG, echG) = calculer(mesureBlancG, g, CIBLE_BLANC, CIBLE_NOIR)
        val (decB, echB) = calculer(mesureBlancB, b, CIBLE_BLANC, CIBLE_NOIR)
        decalageR = decR; decalageG = decG; decalageB = decB
        echelleR = echR; echelleG = echG; echelleB = echB
        dateDerniereCalibration = maintenant
        etapeEnCours = Etape.POINT_BLANC // pret pour la prochaine calibration complete
    }

    /** Calibration necessaire si jamais terminee, ou si le delai est ecoule. */
    fun calibrationNecessaire(maintenant: Long): Boolean =
        dateDerniereCalibration == 0L || (maintenant - dateDerniereCalibration) >= delaiCalibrationMs

    /** Temps restant avant la prochaine calibration obligatoire, en millisecondes (0 si deja due). */
    fun tempsRestantMs(maintenant: Long): Long {
        if (dateDerniereCalibration == 0L) return 0L
        val restant = delaiCalibrationMs - (maintenant - dateDerniereCalibration)
        return if (restant > 0L) restant else 0L
    }

    /** Applique la correction (soustrait le voile, puis remet a l'echelle) a une couleur brute captee. */
    fun corriger(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        fun ajuste(valeur: Int, decalage: Double, echelle: Double, cibleNoir: Int): Int =
            (((valeur - decalage) * echelle) + cibleNoir).toInt().coerceIn(0, 255)
        return Triple(
            ajuste(r, decalageR, echelleR, CIBLE_NOIR),
            ajuste(g, decalageG, echelleG, CIBLE_NOIR),
            ajuste(b, decalageB, echelleB, CIBLE_NOIR)
        )
    }

    /** Etat a sauvegarder pour retrouver la calibration apres une fermeture complete de l'appli. */
    data class EtatCalibration(
        val decalageR: Double, val decalageG: Double, val decalageB: Double,
        val echelleR: Double, val echelleG: Double, val echelleB: Double,
        val dateDerniereCalibration: Long
    )

    fun etatPourSauvegarde(): EtatCalibration = EtatCalibration(
        decalageR, decalageG, decalageB, echelleR, echelleG, echelleB, dateDerniereCalibration
    )

    /** Restaure un etat sauvegarde (appele au demarrage de l'appli). */
    fun restaurer(etat: EtatCalibration) {
        decalageR = etat.decalageR; decalageG = etat.decalageG; decalageB = etat.decalageB
        echelleR = etat.echelleR; echelleG = etat.echelleG; echelleB = etat.echelleB
        dateDerniereCalibration = etat.dateDerniereCalibration
        etapeEnCours = Etape.POINT_BLANC
    }
}
