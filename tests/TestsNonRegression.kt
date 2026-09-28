import com.tomyn.daltoneye.ClassificateurCouleur
import java.io.File

/**
 * Suite de non-regression pour ClassificateurCouleur.kt.
 *
 * A COMPILER ET LANCER AVANT DE MODIFIER UNE REFERENCE DE COULEUR (meme methode qu'utilisee
 * pendant le developpement) :
 *
 *   cd tests
 *   kotlinc ../app/src/main/java/com/tomyn/daltoneye/ClassificateurCouleur.kt TestsNonRegression.kt \
 *       -include-runtime -d test.jar
 *   java -jar test.jar
 *
 * Contient :
 * 1. Les cas reels rencontres sur le terrain par Tomyn (pince PCA bleue, bobine Anycubic Purple,
 *    cas d'ecretage capteur sur piece beige/creme, gris parfaitement neutres...) qui ont chacun
 *    revele un vrai bug avant d'etre corriges. Ne JAMAIS les laisser repasser au rouge.
 * 2. Le nuancier complet des 201 couleurs officielles Bambu Lab (nuancier_bambu_201_couleurs.csv),
 *    utilise comme outil de diagnostic large - un score qui baisse fortement d'une version a
 *    l'autre est un signal d'alerte, mais toutes les "erreurs" ne sont pas forcement de vrais
 *    bugs (certaines sont des cas limites defendables dans un systeme a 14 familles larges).
 */

data class CasTest(val r: Int, val g: Int, val b: Int, val attendu: String, val description: String)

val casReelsValides = listOf(
    // --- Pince PCA bleue (hex reel affiche par l'appli), a corrige la reference Bleu en v1.8 ---
    CasTest(9, 146, 242, "Bleu", "Pince PCA #0992F2, bleu vif classe a tort Cyan avant correction"),

    // --- Bobine Anycubic "Purple" officielle, a corrige la reference Violet en v1.11 ---
    CasTest(106, 109, 205, "Violet", "Hex officiel Anycubic Purple #6A6DCD, classe a tort Bleu avant correction"),

    // --- Gris parfaitement neutres, classes a tort Marron avant le garde-fou achromatique v1.12 ---
    CasTest(81, 81, 81, "Gris / Argenté", "#515151, gris pur Bambu (Dark Gray)"),
    CasTest(84, 84, 84, "Gris / Argenté", "#545454, gris pur Bambu (Dark Gray)"),
    CasTest(86, 86, 86, "Gris / Argenté", "#565656, gris pur Bambu (Titan Gray)"),
    CasTest(95, 99, 103, "Gris / Argenté", "#5F6367, gris legerement bleute Bambu (Titan Gray)"),

    // --- Cas d'ecretage capteur (R et G satures a 255) sur piece beige/creme, corrige en v1.15 ---
    CasTest(255, 255, 123, "Beige", "#FFFF7B, piece creme surexposee, classee a tort Jaune avant correction"),
    CasTest(255, 255, 248, "Blanc", "#FFFFF8, piece creme tres surexposee"),
    CasTest(255, 255, 203, "Beige", "#FFFFCB, piece creme surexposee"),

    // --- Calibration : la cible blanc (240,240,240) doit toujours etre classee Blanc ---
    CasTest(240, 240, 240, "Blanc", "Cible de calibration blanc (v1.1 : tombait alors sur l'ancienne famille Transparent)"),

    // --- Famille "Transparent" retiree en v1.24 : ces pastels reels Bambu lui etaient attribues a tort ---
    CasTest(0x96, 0xDC, 0xB9, "Vert", "Bambu Mint #96DCB9 (etait classe Transparent)"),
    CasTest(0xA8, 0xC6, 0xEE, "Cyan", "Bambu Baby Blue #A8C6EE (etait classe Transparent)"),
    CasTest(0xE7, 0xC1, 0xD5, "Rose", "Bambu Cotton Candy #E7C1D5 (etait classe Transparent)"),

    // --- Repere de non-regression sur les autres familles (jamais buggees, a garder stables) ---
    CasTest(230, 20, 20, "Rouge", "Rouge franc de reference"),
    CasTest(240, 220, 0, "Jaune", "Jaune franc de reference (non ecrete)"),
    CasTest(20, 150, 60, "Vert", "Vert franc de reference"),
    CasTest(0, 184, 217, "Cyan", "Cyan franc de reference"),
    CasTest(210, 60, 140, "Magenta", "Magenta franc de reference"),
    CasTest(240, 180, 200, "Rose", "Rose clair de reference"),
    CasTest(216, 194, 160, "Beige", "Beige typique (non ecrete)"),
    CasTest(107, 63, 29, "Marron", "Marron reel de reference"),
    CasTest(10, 10, 10, "Noir", "Noir pur de reference"),
    CasTest(0, 206, 209, "Cyan", "DarkTurquoise, vrai cyan a ne pas confondre avec Bleu")
)

fun executerCasReels(): Boolean {
    println("=== Cas reels valides (doivent TOUS passer) ===")
    var toutPasse = true
    // Decision v1.24 : pas de famille "Transparent" (la camera lit le fond a travers la piece)
    val aTransparent = ClassificateurCouleur.familles.any { it.nom == "Transparent" }
    println((if (!aTransparent) "[OK  ] " else "[RATE] ") + "aucune famille Transparent (decision v1.24) : ${ClassificateurCouleur.familles.size} familles")
    if (aTransparent) toutPasse = false
    for (cas in casReelsValides) {
        val f = ClassificateurCouleur.classifier(cas.r, cas.g, cas.b)
        val ok = f.nom == cas.attendu
        if (!ok) toutPasse = false
        val statut = if (ok) "OK  " else "RATE"
        println("[$statut] ${cas.description} : attendu=${cas.attendu}, obtenu=${f.nom}")
    }
    println()
    return toutPasse
}

fun executerNuancierBambu() {
    val fichierCsv = File("nuancier_bambu_201_couleurs.csv")
    if (!fichierCsv.exists()) {
        println("(nuancier_bambu_201_couleurs.csv introuvable depuis ce dossier, diagnostic large ignore)")
        return
    }
    val lignes = fichierCsv.readLines().drop(1) // en-tete
    var correct = 0
    var total = 0
    val erreurs = mutableListOf<String>()
    for (ligne in lignes) {
        val (hex, nom, familleAttendue) = ligne.split(",")
        val r = hex.substring(0, 2).toInt(16)
        val g = hex.substring(2, 4).toInt(16)
        val b = hex.substring(4, 6).toInt(16)
        val f = ClassificateurCouleur.classifier(r, g, b)
        total++
        if (f.nom == familleAttendue) correct++
        else erreurs.add("#$hex ($nom) attendu=$familleAttendue obtenu=${f.nom}")
    }
    println("=== Diagnostic large : nuancier des 201 couleurs Bambu ===")
    println("Score : $correct / $total")
    println("(un score qui baisse fortement d'une version a l'autre est un signal d'alerte a examiner ;")
    println(" toutes les erreurs ne sont pas forcement de vrais bugs - cf. commentaire en tete de fichier)")
    if (erreurs.isNotEmpty()) {
        println()
        println("Detail des ecarts :")
        erreurs.forEach { println("  $it") }
    }
}

fun main() {
    val casReelsOk = executerCasReels()
    executerNuancierBambu()
    println()
    if (casReelsOk) {
        println("✓ Tous les cas reels valides passent.")
    } else {
        println("✗ AU MOINS UN CAS REEL VALIDE A REGRESSE - a corriger avant de livrer.")
    }
}
