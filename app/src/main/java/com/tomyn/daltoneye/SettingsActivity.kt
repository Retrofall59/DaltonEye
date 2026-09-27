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

        val texteVersion = findViewById<TextView>(R.id.texteVersion)
        texteVersion.text = try {
            val infos = packageManager.getPackageInfo(packageName, 0)
            "DaltonEye — version ${infos.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            "DaltonEye"
        }
    }
}
