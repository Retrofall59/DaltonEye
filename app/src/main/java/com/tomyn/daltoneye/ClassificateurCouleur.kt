package com.tomyn.daltoneye

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Une famille de couleur affichee a l'ecran, avec sa teinte de reference. */
data class FamilleCouleur(val nom: String, val hexReference: String)

/**
 * Classe une couleur RGB captee par la camera dans l'une des familles larges definies avec Tomyn.
 * Volontairement peu de familles (tolerance large sur les nuances proches) pour rester fiable
 * malgre les variations d'eclairage residuelles, meme apres calibration.
 * Distance de couleur : CIEDE2000, meme formule que dans BambuRfidReader (EquivalenceBambu.kt).
 */
object ClassificateurCouleur {

    val familles = listOf(
        FamilleCouleur("Rouge", "E51C1C"),
        FamilleCouleur("Orange", "FF8A00"),
        FamilleCouleur("Jaune", "F5E400"),
        FamilleCouleur("Vert", "1FA83C"),
        FamilleCouleur("Cyan", "00B8D9"),
        FamilleCouleur("Bleu", "0D6EFD"),
        FamilleCouleur("Violet", "6A1CC7"),
        FamilleCouleur("Magenta", "D6008F"),
        FamilleCouleur("Rose", "F27EB6"),
        FamilleCouleur("Beige", "D8C4A0"),
        FamilleCouleur("Marron", "6B3F1D"),
        FamilleCouleur("Noir", "0A0A0A"),
        FamilleCouleur("Blanc", "F5F5F5"),
        FamilleCouleur("Gris / Argenté", "9A9A9A"),
        FamilleCouleur("Transparent", "C9C9C9")
    )

    /**
     * @param r, g, b composantes 0-255 de la couleur captee (deja corrigee par la calibration).
     * @return la famille dont la reference est perceptuellement la plus proche.
     */
    fun classifier(r: Int, g: Int, b: Int): FamilleCouleur {
        val hexCapte = String.format("%02X%02X%02X", r, g, b)
        val labCapte = versLab(hexCapte)
        var meilleure = familles[0]
        var meilleurEcart = Double.MAX_VALUE
        for (f in familles) {
            val e = ecart2000(labCapte, versLab(f.hexReference))
            if (e < meilleurEcart) {
                meilleurEcart = e
                meilleure = f
            }
        }
        return meilleure
    }

    // ------------------------------------------------------------------ CIEDE2000 (portee depuis EquivalenceBambu.kt)

    private fun versLab(hex: String): DoubleArray {
        fun lineaire(canal: Int): Double {
            val v = canal / 255.0
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        val r = lineaire(hex.substring(0, 2).toInt(16))
        val g = lineaire(hex.substring(2, 4).toInt(16))
        val b = lineaire(hex.substring(4, 6).toInt(16))
        val x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047
        val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
        val z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883
        val eps = 216.0 / 24389.0
        val kappa = 24389.0 / 27.0
        fun f(t: Double): Double = if (t > eps) t.pow(1.0 / 3.0) else (kappa * t + 16.0) / 116.0
        val fx = f(x)
        val fy = f(y)
        val fz = f(z)
        return doubleArrayOf(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))
    }

    private fun degres(rad: Double): Double = Math.toDegrees(rad)
    private fun radians(deg: Double): Double = Math.toRadians(deg)

    private fun ecart2000(lab1: DoubleArray, lab2: DoubleArray): Double {
        val l1 = lab1[0]; val a1 = lab1[1]; val b1 = lab1[2]
        val l2 = lab2[0]; val a2 = lab2[1]; val b2 = lab2[2]

        val c1 = hypot(a1, b1)
        val c2 = hypot(a2, b2)
        val cMoy = (c1 + c2) / 2.0
        val g = 0.5 * (1.0 - sqrt(cMoy.pow(7) / (cMoy.pow(7) + 25.0.pow(7))))
        val a1p = (1.0 + g) * a1
        val a2p = (1.0 + g) * a2
        val c1p = hypot(a1p, b1)
        val c2p = hypot(a2p, b2)

        fun teinte(b: Double, ap: Double, cp: Double): Double {
            if (cp == 0.0) return 0.0
            var h = degres(atan2(b, ap))
            if (h < 0) h += 360.0
            return h
        }
        val h1p = teinte(b1, a1p, c1p)
        val h2p = teinte(b2, a2p, c2p)

        val dLp = l2 - l1
        val dCp = c2p - c1p
        val dhp = if (c1p * c2p == 0.0) 0.0 else {
            val dh = h2p - h1p
            if (abs(dh) <= 180.0) dh else if (dh > 180.0) dh - 360.0 else dh + 360.0
        }
        val dHp = 2.0 * sqrt(c1p * c2p) * sin(radians(dhp / 2.0))

        val lpMoy = (l1 + l2) / 2.0
        val cpMoy = (c1p + c2p) / 2.0
        val hpMoy = if (c1p * c2p == 0.0) h1p + h2p
        else if (abs(h1p - h2p) <= 180.0) (h1p + h2p) / 2.0
        else if (h1p + h2p < 360.0) (h1p + h2p + 360.0) / 2.0
        else (h1p + h2p - 360.0) / 2.0

        val t = 1.0 - 0.17 * cos(radians(hpMoy - 30.0)) + 0.24 * cos(radians(2.0 * hpMoy)) +
                0.32 * cos(radians(3.0 * hpMoy + 6.0)) - 0.20 * cos(radians(4.0 * hpMoy - 63.0))
        val dTheta = 30.0 * exp(-((hpMoy - 275.0) / 25.0).pow(2))
        val rc = 2.0 * sqrt(cpMoy.pow(7) / (cpMoy.pow(7) + 25.0.pow(7)))
        val sl = 1.0 + 0.015 * (lpMoy - 50.0).pow(2) / sqrt(20.0 + (lpMoy - 50.0).pow(2))
        val sc = 1.0 + 0.045 * cpMoy
        val sh = 1.0 + 0.015 * cpMoy * t
        val rt = -sin(radians(2.0 * dTheta)) * rc

        val termeL = dLp / sl
        val termeC = dCp / sc
        val termeH = dHp / sh
        return sqrt(termeL * termeL + termeC * termeC + termeH * termeH + rt * termeC * termeH)
    }
}
