package com.tomyn.daltoneye

import android.content.Context

/** Tailles de zone d'analyse proposees dans les parametres. */
enum class TailleReticule(val fractionImage: Double, val dpEcran: Int) {
    PETIT(0.03, 30),
    MOYEN(0.05, 45),
    GRAND(0.10, 90)
}

/**
 * Lit/ecrit les parametres utilisateur (SharedPreferences), partages entre MainActivity
 * et SettingsActivity pour eviter de dupliquer les cles.
 */
object GestionnaireParametres {

    private const val FICHIER = "daltoneye_parametres"
    private const val CLE_TAILLE_RETICULE = "taille_reticule"
    private const val CLE_DELAI_CALIBRATION_MIN = "delai_calibration_minutes"
    private const val CLE_VIBRATION_ACTIVEE = "vibration_activee"

    fun lireTailleReticule(context: Context): TailleReticule {
        val prefs = context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
        val nom = prefs.getString(CLE_TAILLE_RETICULE, TailleReticule.MOYEN.name)
        return try { TailleReticule.valueOf(nom ?: TailleReticule.MOYEN.name) } catch (e: Exception) { TailleReticule.MOYEN }
    }

    fun ecrireTailleReticule(context: Context, taille: TailleReticule) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit()
            .putString(CLE_TAILLE_RETICULE, taille.name).apply()
    }

    fun lireDelaiCalibrationMinutes(context: Context): Int =
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).getInt(CLE_DELAI_CALIBRATION_MIN, 60)

    fun ecrireDelaiCalibrationMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit()
            .putInt(CLE_DELAI_CALIBRATION_MIN, minutes).apply()
    }

    fun lireVibrationActivee(context: Context): Boolean =
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).getBoolean(CLE_VIBRATION_ACTIVEE, true)

    fun ecrireVibrationActivee(context: Context, activee: Boolean) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit()
            .putBoolean(CLE_VIBRATION_ACTIVEE, activee).apply()
    }
}
