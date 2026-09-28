import com.tomyn.daltoneye.DispersionZone
import kotlin.random.Random

/**
 * Test de DispersionZone : (1) la mediane est STRICTEMENT identique a l'ancienne formule
 * (aucun effet sur la couleur mesuree), (2) les percentiles sont corrects, (3) le texte du rapport.
 *
 * Lancer :
 *   kotlinc ../app/src/main/java/com/tomyn/daltoneye/DispersionZone.kt TestDispersion.kt -include-runtime -d test_disp.jar
 *   java -jar test_disp.jar
 */
var echecs = 0
fun check(nom: String, ok: Boolean, detail: String = "") {
    println((if (ok) "  OK   " else "  ECHEC") + " $nom" + if (detail.isNotEmpty()) "  [$detail]" else "")
    if (!ok) echecs++
}

fun main() {
    // 1) Mediane identique a l'ancienne formule (liste triee, element central) sur 2000 listes aleatoires
    val hasard = Random(42)
    var toutesIdentiques = true
    repeat(2000) {
        val n = hasard.nextInt(1, 500)
        val liste = MutableList(n) { hasard.nextInt(0, 256) }
        val ancienne = liste.sorted()[n / 2]
        val nouvelle = DispersionZone.resumer(liste.toMutableList()).mediane
        if (ancienne != nouvelle) toutesIdentiques = false
    }
    check("mediane identique a l'ancienne formule (2000 listes aleatoires)", toutesIdentiques)

    // 2) Percentiles sur un cas connu : 1..100
    val cent = DispersionZone.resumer((1..100).toMutableList())
    check("1..100 : mediane 51 (element central de l'ancienne formule)", cent.mediane == 51, "obtenu=${cent.mediane}")
    check("1..100 : p10 = 10", cent.p10 == 10, "obtenu=${cent.p10}")
    check("1..100 : p90 = 90", cent.p90 == 90, "obtenu=${cent.p90}")

    // Un seul pixel : pas d'erreur d'index
    val un = DispersionZone.resumer(mutableListOf(137))
    check("1 seul pixel : mediane, p10 et p90 valent la valeur", un.mediane == 137 && un.p10 == 137 && un.p90 == 137)

    // Zone homogene vs zone melangee (moitie rouge, moitie noir) : l'ecart p10-p90 doit le montrer
    val homogene = DispersionZone.resumer(MutableList(200) { 120 + (it % 5) })
    val melangee = DispersionZone.resumer(MutableList(200) { if (it < 100) 220 else 10 })
    check("zone homogene : ecart p10-p90 faible", homogene.p90 - homogene.p10 <= 4, "ecart=${homogene.p90 - homogene.p10}")
    check("zone melangee : ecart p10-p90 tres grand", melangee.p90 - melangee.p10 >= 200, "ecart=${melangee.p90 - melangee.p10}")

    // 3) Texte du rapport
    val texte = DispersionZone.formater(
        DispersionZone.Canal(120, 118, 131), DispersionZone.Canal(90, 88, 99), DispersionZone.Canal(60, 58, 70), 208
    )
    check("texte du rapport", texte == "R 118-131, G 88-99, B 58-70 (p10-p90, brut) sur 208 pixels", texte)

    println(if (echecs == 0) "\n=> TOUT PASSE" else "\n=> $echecs ECHEC(S)")
    if (echecs > 0) System.exit(1)
}
