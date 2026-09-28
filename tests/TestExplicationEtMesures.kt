import com.tomyn.daltoneye.ClassificateurCouleur
import com.tomyn.daltoneye.RapportMesure

/**
 * Test des donnees de diagnostic passives (v1.25) : explication du classement et mesures brutes.
 *
 *   kotlinc ../app/src/main/java/com/tomyn/daltoneye/ClassificateurCouleur.kt \
 *           ../app/src/main/java/com/tomyn/daltoneye/RapportMesure.kt TestExplicationEtMesures.kt \
 *           -include-runtime -d test_expl.jar
 *   java -jar test_expl.jar
 */
var echecs = 0
fun check(nom: String, ok: Boolean, detail: String = "") {
    println((if (ok) "  OK   " else "  ECHEC") + " $nom" + if (detail.isNotEmpty()) "  [$detail]" else "")
    if (!ok) echecs++
}

fun main() {
    // 1) Coherence : sans regle speciale annoncee, la famille la plus proche EST celle du classificateur.
    //    Un ecart ici voudrait dire que expliquer() ne reproduit plus les conditions de classifier().
    var testes = 0; var incoherents = 0; var exemple = ""
    var avecRegle = 0
    for (r in 0..255 step 5) for (g in 0..255 step 5) for (b in 0..255 step 5) {
        val e = ClassificateurCouleur.expliquer(r, g, b)
        val attendu = ClassificateurCouleur.classifier(r, g, b).nom
        if (e.gagnant != attendu) { incoherents++; exemple = "($r,$g,$b)" }
        if (e.regle == null) {
            testes++
            if (e.plusProches[0].first != e.gagnant) { incoherents++; if (exemple.isEmpty()) exemple = "($r,$g,$b) regle=null mais plus proche=${e.plusProches[0].first} gagnant=${e.gagnant}" }
        } else avecRegle++
    }
    check("gagnant de expliquer() = classifier() ET, sans regle, plus proche = gagnant",
        incoherents == 0, "grille de ${testes + avecRegle} couleurs ($avecRegle avec regle speciale), incoherences=$incoherents $exemple")

    // 2) Cas reels connus
    val pince = ClassificateurCouleur.expliquer(9, 146, 242)
    check("pince bleue #0992F2 : Bleu, par distance", pince.gagnant == "Bleu" && pince.regle == null, pince.gagnant + " / " + pince.regle)
    val creme = ClassificateurCouleur.expliquer(255, 255, 123)
    check("creme surexposee #FFFF7B : Beige, regle capteur sature", creme.gagnant == "Beige" && creme.regle?.startsWith("capteur sature") == true, creme.regle ?: "null")
    val gris = ClassificateurCouleur.expliquer(81, 81, 81)
    check("gris pur #515151 : Gris, regle quasi neutre", gris.gagnant == "Gris / Argenté" && gris.regle?.startsWith("couleur quasi neutre") == true, gris.regle ?: "null")
    check("toujours 3 familles proches, triees par distance",
        pince.plusProches.size == 3 && pince.plusProches.zipWithNext().all { it.first.second <= it.second.second })

    // 3) Formatage lisible
    val t1 = ClassificateurCouleur.formaterExplication(pince)
    check("texte sans regle : 'par distance CIEDE2000' + 3 distances", t1.startsWith("Classement : Bleu - par distance CIEDE2000") && t1.count { it == ',' } == 2, t1)
    val t2 = ClassificateurCouleur.formaterExplication(creme)
    check("texte avec regle : mentionne la regle et 'indicatives'", t2.contains("regle : capteur sature") && t2.contains("indicatives"), t2)

    // 4) Mesures brutes
    check("luminosite blanc = 255", RapportMesure.luminosite(255, 255, 255) == 255)
    check("luminosite noir = 0", RapportMesure.luminosite(0, 0, 0) == 0)
    check("luminosite (173,82,65) = 107", RapportMesure.luminosite(173, 82, 65) == 107, "obtenu=${RapportMesure.luminosite(173, 82, 65)}")
    val m = RapportMesure.formaterMesures(Triple(173, 82, 65), Triple(160, 74, 61), Triple(161, 75, 62))
    check("mesures : brut, luminosite, corrige, affiche",
        m.contains("(173, 82, 65) = #AD5241") && m.contains("107/255") && m.contains("(160, 74, 61) = #A04A3D") && m.contains("(161, 75, 62) = #A14B3E"), m)
    val vide = RapportMesure.formaterMesures(null, null, null)
    check("sans mesure : 'non disponible', pas d'erreur", vide.contains("non disponible") && !vide.contains("null"), vide)

    println(if (echecs == 0) "\n=> TOUT PASSE" else "\n=> $echecs ECHEC(S)")
    if (echecs > 0) System.exit(1)
}
