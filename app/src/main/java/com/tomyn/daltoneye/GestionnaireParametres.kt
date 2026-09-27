package com.tomyn.daltoneye

import android.content.Context

/** Tailles de zone d'analyse proposees dans les parametres. */
enum class TailleReticule(val fractionImage: Double, val dpEcran: Int) {
    PETIT(0.03, 30),
    MOYEN(0.05, 45),
    GRAND(0.10, 90)
}

/** Rapidite de declenchement de la vibration (nombre d'images stables avant de vibrer). */
enum class SeuilVibration(val nbFrames: Int) {
    RAPIDE(4),
    NORMALE(8),
    LENTE(15)
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
    private const val CLE_SEUIL_VIBRATION = "seuil_vibration"

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

    fun lireSeuilVibration(context: Context): SeuilVibration {
        val prefs = context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
        val nom = prefs.getString(CLE_SEUIL_VIBRATION, SeuilVibration.NORMALE.name)
        return try { SeuilVibration.valueOf(nom ?: SeuilVibration.NORMALE.name) } catch (e: Exception) { SeuilVibration.NORMALE }
    }

    fun ecrireSeuilVibration(context: Context, seuil: SeuilVibration) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit()
            .putString(CLE_SEUIL_VIBRATION, seuil.name).apply()
    }

    /** Remet tous les parametres a leurs valeurs par defaut. */
    fun reinitialiser(context: Context) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
