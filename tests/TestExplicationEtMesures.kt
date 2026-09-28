import com.tomyn.daltoneye.ClassificateurCouleur
import com.tomyn.daltoneye.InstantaneMesure
import com.tomyn.daltoneye.RapportMesure

/**
 * Test des donnees de diagnostic passives (v1.25/v1.26) : explication du classement, mesures brutes,
 * instantane unique et rapport (mesure actuelle + derniere validation).
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
    // 5) Rapport a instantane unique (v1.26)
    val bleu = InstantaneMesure(
        horodatageMs = 1_000_000L, brut = Triple(10, 120, 230), corrigeInstantane = Triple(9, 146, 242),
        affiche = Triple(9, 146, 242), famille = "Bleu", dispersion = "R 5-12, G 110-130, B 220-240 (p10-p90, brut) sur 208 pixels"
    )
    val rouge = InstantaneMesure(
        horodatageMs = 1_030_000L, brut = Triple(173, 82, 65), corrigeInstantane = Triple(160, 74, 61),
        affiche = Triple(161, 75, 62), famille = "Rouge", dispersion = "R 150-190, G 60-100, B 50-80 (p10-p90, brut) sur 208 pixels"
    )
    val now = 1_042_000L

    // Cas type : la piece a ete validee Bleu, puis retiree ; on copie le rapport 42 s apres la validation
    val rapport = RapportMesure.formaterRapport(actuelle = rouge, validation = bleu, maintenantMs = now)
    val (blocActuel, blocValidation) = rapport.split("--- Derniere validation").let { it[0] to "--- Derniere validation" + it[1] }

    check("titre de la mesure actuelle avec son age (12 s)", blocActuel.contains("--- Mesure actuelle (il y a 12 s) ---"), blocActuel.lines().first())
    check("titre de la validation : famille + age (42 s)", blocValidation.contains("lecture stable, famille Bleu (il y a 42 s)"), blocValidation.lines().first())
    check("bloc ACTUEL : toutes les valeurs viennent de l'instantane actuel (rouge)",
        blocActuel.contains("(173, 82, 65) = #AD5241") && blocActuel.contains("107/255") && blocActuel.contains("(160, 74, 61) = #A04A3D") &&
        blocActuel.contains("(161, 75, 62) = #A14B3E") && blocActuel.contains("R 150-190") && blocActuel.contains("Classement : ") &&
        !blocActuel.contains("(10, 120, 230)"))
    check("bloc VALIDATION : toutes les valeurs viennent de l'instantane de validation (bleu)",
        blocValidation.contains("(10, 120, 230) = #0A78E6") && blocValidation.contains("(9, 146, 242) = #0992F2") &&
        blocValidation.contains("R 5-12") && blocValidation.contains("Classement : Bleu") && !blocValidation.contains("(173, 82, 65)"))
    check("l'explication de chaque bloc est celle de SA valeur affichee",
        blocActuel.contains(ClassificateurCouleur.formaterExplication(ClassificateurCouleur.expliquer(161, 75, 62))) &&
        blocValidation.contains(ClassificateurCouleur.formaterExplication(ClassificateurCouleur.expliquer(9, 146, 242))))
    check("pas de ligne vide parasite en fin de rapport", !rapport.endsWith("\n"))

    // Sans validation ni mesure
    val sansValidation = RapportMesure.formaterRapport(rouge, null, now)
    check("sans validation : le dit clairement", sansValidation.contains("aucune depuis l'ouverture de l'appli"), sansValidation.lines().last())
    val rien = RapportMesure.formaterRapport(null, null, now)
    check("sans aucune mesure : 'non disponible', pas d'erreur, pas de 'null'", rien.contains("non disponible") && !rien.contains("null"), rien)

    // Age
    check("age : 0 s", RapportMesure.age(0) == "il y a 0 s")
    check("age : 119 s reste en secondes", RapportMesure.age(119_000) == "il y a 119 s")
    check("age : 3 min", RapportMesure.age(200_000) == "il y a 3 min", RapportMesure.age(200_000))
    check("age : jamais negatif (horloge qui recule)", RapportMesure.age(-5_000) == "il y a 0 s")

    println(if (echecs == 0) "\n=> TOUT PASSE" else "\n=> $echecs ECHEC(S)")
    if (echecs > 0) System.exit(1)
}
