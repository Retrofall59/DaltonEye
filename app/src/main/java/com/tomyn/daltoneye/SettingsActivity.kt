package com.tomyn.daltoneye

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val boutonRetour = findViewById<ImageButton>(R.id.boutonRetour)
        boutonRetour.setOnClickListener { finish() }

        val groupeTaille = findViewById<RadioGroup>(R.id.groupeTailleReticule)
        val optionPetit = findViewById<android.widget.RadioButton>(R.id.optionPetit)
        val optionMoyen = findViewById<android.widget.RadioButton>(R.id.optionMoyen)
        val optionGrand = findViewById<android.widget.RadioButton>(R.id.optionGrand)

        when (GestionnaireParametres.lireTailleReticule(this)) {
            TailleReticule.PETIT -> optionPetit.isChecked = true
            TailleReticule.MOYEN -> optionMoyen.isChecked = true
            TailleReticule.GRAND -> optionGrand.isChecked = true
        }
        groupeTaille.setOnCheckedChangeListener { _, checkedId ->
            val taille = when (checkedId) {
                R.id.optionPetit -> TailleReticule.PETIT
                R.id.optionGrand -> TailleReticule.GRAND
                else -> TailleReticule.MOYEN
            }
            GestionnaireParametres.ecrireTailleReticule(this, taille)
        }

        val groupeDelai = findViewById<RadioGroup>(R.id.groupeDelaiCalibration)
        val optionDelai30 = findViewById<android.widget.RadioButton>(R.id.optionDelai30)
        val optionDelai60 = findViewById<android.widget.RadioButton>(R.id.optionDelai60)
        val optionDelai120 = findViewById<android.widget.RadioButton>(R.id.optionDelai120)

        when (GestionnaireParametres.lireDelaiCalibrationMinutes(this)) {
            30 -> optionDelai30.isChecked = true
            120 -> optionDelai120.isChecked = true
            else -> optionDelai60.isChecked = true
        }
        groupeDelai.setOnCheckedChangeListener { _, checkedId ->
            val minutes = when (checkedId) {
                R.id.optionDelai30 -> 30
                R.id.optionDelai120 -> 120
                else -> 60
            }
            GestionnaireParametres.ecrireDelaiCalibrationMinutes(this, minutes)
        }

        val interrupteurVibration = findViewById<Switch>(R.id.interrupteurVibration)
        interrupteurVibration.isChecked = GestionnaireParametres.lireVibrationActivee(this)
        interrupteurVibration.setOnCheckedChangeListener { _, active ->
            GestionnaireParametres.ecrireVibrationActivee(this, active)
        }

        val groupeSeuilVibration = findViewById<RadioGroup>(R.id.groupeSeuilVibration)
        val optionVibrationRapide = findViewById<android.widget.RadioButton>(R.id.optionVibrationRapide)
        val optionVibrationNormale = findViewById<android.widget.RadioButton>(R.id.optionVibrationNormale)
        val optionVibrationLente = findViewById<android.widget.RadioButton>(R.id.optionVibrationLente)

        when (GestionnaireParametres.lireSeuilVibration(this)) {
            SeuilVibration.RAPIDE -> optionVibrationRapide.isChecked = true
            SeuilVibration.LENTE -> optionVibrationLente.isChecked = true
            SeuilVibration.NORMALE -> optionVibrationNormale.isChecked = true
        }
        groupeSeuilVibration.setOnCheckedChangeListener { _, checkedId ->
            val seuil = when (checkedId) {
                R.id.optionVibrationRapide -> SeuilVibration.RAPIDE
                R.id.optionVibrationLente -> SeuilVibration.LENTE
                else -> SeuilVibration.NORMALE
            }
            GestionnaireParametres.ecrireSeuilVibration(this, seuil)
        }

        val boutonReinitialiser = findViewById<android.widget.Button>(R.id.boutonReinitialiser)
        boutonReinitialiser.setOnClickListener {
            GestionnaireParametres.reinitialiser(this)
            recreate() // relance l'ecran pour reafficher toutes les valeurs par defaut
        }

        val texteVersion = findViewById<TextView>(R.id.texteVersion)
        texteVersion.text = try {
            val infos = packageManager.getPackageInfo(packageName, 0)
            "DaltonEye — version ${infos.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            "DaltonEye"
        }

        val boutonDiagnostic = findViewById<android.widget.Button>(R.id.boutonDiagnostic)
        boutonDiagnostic.setOnClickListener { copierInfosDiagnostic() }
    }

    /** Rassemble modele du telephone, version de l'appli, dernier hex et facteurs de calibration
     * dans le presse-papier, pour accelerer un signalement de bug (a moi ou sur le forum). */
    private fun copierInfosDiagnostic() {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) { "?" }

        val dernierHex = intent.getStringExtra("dernier_hex") ?: "non disponible"
        val mesures = intent.getStringExtra("mesures") ?: "non disponible"
        val tailleZone = GestionnaireParametres.lireTailleReticule(this)
        val decR = intent.getDoubleExtra("decalageR", 0.0)
        val decG = intent.getDoubleExtra("decalageG", 0.0)
        val decB = intent.getDoubleExtra("decalageB", 0.0)
        val echR = intent.getDoubleExtra("echelleR", 1.0)
        val echG = intent.getDoubleExtra("echelleG", 1.0)
        val echB = intent.getDoubleExtra("echelleB", 1.0)

        val texte = buildString {
            appendLine("=== Diagnostic DaltonEye ===")
            appendLine("Téléphone : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Version appli : $version")
            appendLine("Dernier hex affiché : $dernierHex")
            appendLine("Zone d'analyse : ${tailleZone.name} (${(tailleZone.fractionImage * 100).toInt()} % de l'image)")
            appendLine(mesures)
            appendLine("Calibration - décalage (R,G,B) : $decR, $decG, $decB")
            appendLine("Calibration - échelle (R,G,B) : $echR, $echG, $echB")
        }

        val gestionnaire = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        gestionnaire.setPrimaryClip(android.content.ClipData.newPlainText("Diagnostic DaltonEye", texte))
        android.widget.Toast.makeText(this, "Infos de diagnostic copiées", android.widget.Toast.LENGTH_SHORT).show()
    }
}
