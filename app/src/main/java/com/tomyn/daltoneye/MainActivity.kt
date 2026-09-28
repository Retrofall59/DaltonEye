package com.tomyn.daltoneye

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Ecran unique : camera en direct plein ecran, analyse continue de la zone centrale
 * (aucune photo n'est prise ni stockee), classement dans l'une des familles de couleur
 * larges definies avec Tomyn, affichage de l'apercu brut + du nom de la famille.
 * Une calibration sur blanc est imposee (bloquante) toutes les heures, en continu meme
 * en pleine session, pour compenser la derive d'eclairage dans l'atelier.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var previewCamera: PreviewView
    private lateinit var carreCouleurCaptee: android.view.View
    private lateinit var nomFamilleCouleur: TextView
    private lateinit var texteHexCapte: TextView
    private lateinit var barreAccentResultat: android.view.View
    private lateinit var ecranCalibration: android.view.View
    private lateinit var boutonCalibrer: Button
    private lateinit var titreCalibration: TextView
    private lateinit var messageCalibration: TextView
    private lateinit var boutonCalibrerManuel: android.widget.ImageButton
    private lateinit var boutonParametres: android.widget.ImageButton
    private lateinit var reticule: android.view.View

    @Volatile private var fractionZoneAnalyse: Double = TailleReticule.MOYEN.fractionImage
    @Volatile private var vibrationActivee: Boolean = true
    private lateinit var boutonFlash: android.widget.ImageButton
    private lateinit var texteCompteACalibration: TextView
    private lateinit var badgeFige: TextView
    private lateinit var badgeSature: TextView

    private lateinit var executeurCamera: ExecutorService
    private val gestionnaireCalibration = GestionnaireCalibration()
    private val gestionnaireUi = Handler(Looper.getMainLooper())
    private var vibreur: Vibrator? = null
    private var camera: Camera? = null

    // Derniere couleur brute captee (avant correction calibration), utilisee quand on appuie sur "Calibrer".
    @Volatile private var derniereCouleurBrute: Triple<Int, Int, Int>? = null

    // Rapport de diagnostic uniquement (rien a l'ecran, aucun seuil, aucun effet sur le classement) :
    // un SEUL objet par image, remplace d'un bloc, donc toutes les valeurs du rapport viennent de la meme image.
    @Volatile private var instantane: InstantaneMesure? = null            // derniere image traitee (jamais pendant un gel)
    @Volatile private var instantaneValidation: InstantaneMesure? = null  // mesure au moment ou la lecture a ete jugee stable

    // Figeage temporaire de l'affichage (appui long sur l'ecran), pour lire tranquillement un resultat.
    @Volatile private var affichageFige = false

    // Suivi de la stabilite de la lecture, pour ne vibrer qu'une fois par stabilisation.
    private var derniereFamilleVue: String? = null
    private var comptageStabilite = 0
    private var dejaVibrePourCetteStabilite = false
    @Volatile private var seuilFramesStable = SeuilVibration.NORMALE.nbFrames

    // Lissage temporel : moyenne des dernieres lectures pour reduire le bruit d'une image a l'autre.
    // La calibration, elle, utilise toujours la derniere lecture brute instantanee (reactivite).
    private val tamponCouleurs = ArrayDeque<Triple<Int, Int, Int>>()
    private val TAILLE_TAMPON_LISSAGE = 8
    private val SEUIL_CHANGEMENT_SCENE = 60 // ecart RGB (sur 255) au-dela duquel on considere que la piece a change

    @Volatile private var torcheActive = false

    private val demandePermissionCamera = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { accordee ->
        if (accordee) demarrerCamera()
        else {
            Toast.makeText(this, "La caméra est nécessaire pour utiliser DaltonEye", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Empeche la mise en veille pendant l'utilisation (session de tri en continu)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        restaurerCalibrationSauvegardee()

        previewCamera = findViewById(R.id.previewCamera)
        carreCouleurCaptee = findViewById(R.id.carreCouleurCaptee)
        nomFamilleCouleur = findViewById(R.id.nomFamilleCouleur)
        texteHexCapte = findViewById(R.id.texteHexCapte)
        texteHexCapte.setOnLongClickListener {
            val gestionnaire = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            gestionnaire.setPrimaryClip(android.content.ClipData.newPlainText("Couleur DaltonEye", texteHexCapte.text))
            Toast.makeText(this, "Hex copié : ${texteHexCapte.text}", Toast.LENGTH_SHORT).show()
            true
        }
        barreAccentResultat = findViewById(R.id.barreAccentResultat)
        ecranCalibration = findViewById(R.id.ecranCalibration)
        boutonCalibrer = ecranCalibration.findViewById(R.id.boutonCalibrer)
        titreCalibration = ecranCalibration.findViewById(R.id.titreCalibration)
        messageCalibration = ecranCalibration.findViewById(R.id.messageCalibration)
        boutonCalibrerManuel = findViewById(R.id.boutonCalibrerManuel)
        boutonParametres = findViewById(R.id.boutonParametres)
        reticule = findViewById(R.id.reticule)
        boutonParametres.setOnClickListener {
            val etat = gestionnaireCalibration.etatPourSauvegarde()
            val intention = android.content.Intent(this, SettingsActivity::class.java).apply {
                putExtra("dernier_hex", texteHexCapte.text.toString())
                putExtra("mesures", RapportMesure.formaterRapport(instantane, instantaneValidation, System.currentTimeMillis()))
                putExtra("decalageR", etat.decalageR)
                putExtra("decalageG", etat.decalageG)
                putExtra("decalageB", etat.decalageB)
                putExtra("echelleR", etat.echelleR)
                putExtra("echelleG", etat.echelleG)
                putExtra("echelleB", etat.echelleB)
            }
            startActivity(intention)
        }
        boutonFlash = findViewById(R.id.boutonFlash)

        boutonFlash.setOnClickListener { basculerTorche() }
        texteCompteACalibration = findViewById(R.id.texteCompteACalibration)
        badgeFige = findViewById(R.id.badgeFige)
        badgeSature = findViewById(R.id.badgeSature)

        boutonCalibrer.setOnClickListener { validerCalibration() }
        boutonCalibrerManuel.setOnClickListener {
            afficherEtapeCalibration()
            ecranCalibration.visibility = android.view.View.VISIBLE
        }

        // Appui long sur l'ecran camera : fige l'affichage 3 secondes le temps de bien lire le resultat.
        previewCamera.setOnLongClickListener {
            figerAffichage()
            true
        }

        @Suppress("DEPRECATION")
        vibreur = getSystemService(VIBRATOR_SERVICE) as? Vibrator

        executeurCamera = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            demarrerCamera()
        } else {
            demandePermissionCamera.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        appliquerParametres()
        verifierCalibration()
        gestionnaireUi.post(tickCompteACalibration)
        gestionnaireUi.post(tickMiseAuPoint)
    }

    /** Recharge et applique les reglages utilisateur (taille de zone, delai calibration, vibration). */
    private fun appliquerParametres() {
        val taille = GestionnaireParametres.lireTailleReticule(this)
        fractionZoneAnalyse = taille.fractionImage
        val tailleDp = taille.dpEcran
        val densite = resources.displayMetrics.density
        val taillePx = (tailleDp * densite).toInt()
        reticule.layoutParams = reticule.layoutParams.apply {
            width = taillePx
            height = taillePx
        }
        reticule.requestLayout()

        gestionnaireCalibration.delaiCalibrationMs =
            GestionnaireParametres.lireDelaiCalibrationMinutes(this) * 60 * 1000L

        vibrationActivee = GestionnaireParametres.lireVibrationActivee(this)
        seuilFramesStable = GestionnaireParametres.lireSeuilVibration(this).nbFrames
    }

    override fun onPause() {
        super.onPause()
        gestionnaireUi.removeCallbacks(tickCompteACalibration)
        gestionnaireUi.removeCallbacks(tickMiseAuPoint)
        if (torcheActive) {
            torcheActive = false
            camera?.cameraControl?.enableTorch(false)
            boutonFlash.alpha = 0.5f
        }
    }

    private val tickCompteACalibration: Runnable = object : Runnable {
        override fun run() {
            val restantMs = gestionnaireCalibration.tempsRestantMs(System.currentTimeMillis())
            if (ecranCalibration.visibility == android.view.View.VISIBLE) {
                texteCompteACalibration.text = ""
            } else {
                val minutes = (restantMs / 60000L).toInt()
                val secondes = ((restantMs % 60000L) / 1000L).toInt()
                texteCompteACalibration.text = String.format(Locale.FRANCE, "%02d:%02d", minutes, secondes)
            }
            gestionnaireUi.postDelayed(this, 1000L)
        }
    }

    /** Fige l'affichage du resultat pendant 3 secondes, pour lire tranquillement sans que ca change sous les yeux. */
    private fun figerAffichage() {
        affichageFige = true
        badgeFige.visibility = android.view.View.VISIBLE
        gestionnaireUi.postDelayed({
            affichageFige = false
            badgeFige.visibility = android.view.View.GONE
        }, 3000L)
    }

    private fun vibrerCourt() {
        if (!vibrationActivee) return
        val v = vibreur ?: return
        if (v.hasVibrator()) {
            v.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun verifierCalibration() {
        if (gestionnaireCalibration.calibrationNecessaire(System.currentTimeMillis())) {
            afficherEtapeCalibration()
            ecranCalibration.visibility = android.view.View.VISIBLE
        }
    }

    /**
     * Calibration en deux etapes. Etape blanc : deverrouille la balance des blancs, laisse la
     * camera re-converger sur ce qui est vise, capture, verrouille la balance des blancs (comme
     * avant). Etape noir : capture directe (la balance des blancs reste verrouillee, pas de
     * nouvelle convergence sur une scene sombre qui donnerait un mauvais reglage materiel),
     * calcule la correction finale (voile + echelle) et termine la calibration.
     */
    private fun validerCalibration() {
        when (gestionnaireCalibration.etapeActuelle()) {
            GestionnaireCalibration.Etape.POINT_BLANC -> validerPointBlanc()
            GestionnaireCalibration.Etape.POINT_NOIR -> validerPointNoir()
        }
    }

    private fun validerPointBlanc() {
        deverrouillerBalanceDesBlancs()
        boutonCalibrer.isEnabled = false
        boutonCalibrer.text = "Calibration en cours…"
        gestionnaireUi.postDelayed({
            val brute = derniereCouleurBrute
            if (brute == null) {
                Toast.makeText(this, "Pointe d'abord la caméra vers le blanc de référence", Toast.LENGTH_SHORT).show()
            } else {
                gestionnaireCalibration.capturerPointBlanc(brute.first, brute.second, brute.third)
                verrouillerBalanceDesBlancs()
                afficherEtapeCalibration()
            }
            boutonCalibrer.isEnabled = true
        }, 400L)
    }

    private fun validerPointNoir() {
        val brute = derniereCouleurBrute
        if (brute == null) {
            Toast.makeText(this, "Pointe d'abord la caméra vers une zone noire", Toast.LENGTH_SHORT).show()
            return
        }
        gestionnaireCalibration.capturerPointNoir(brute.first, brute.second, brute.third, System.currentTimeMillis())
        sauvegarderCalibration()
        tamponCouleurs.clear() // les anciennes valeurs lissees venaient de l'ancienne calibration
        ecranCalibration.visibility = android.view.View.GONE
    }

    /** Sauvegarde la calibration pour qu'elle survive a une fermeture complete de l'appli (pas juste une rotation). */
    private fun sauvegarderCalibration() {
        val etat = gestionnaireCalibration.etatPourSauvegarde()
        getSharedPreferences("daltoneye_calibration", MODE_PRIVATE).edit()
            .putFloat("decalageR", etat.decalageR.toFloat())
            .putFloat("decalageG", etat.decalageG.toFloat())
            .putFloat("decalageB", etat.decalageB.toFloat())
            .putFloat("echelleR", etat.echelleR.toFloat())
            .putFloat("echelleG", etat.echelleG.toFloat())
            .putFloat("echelleB", etat.echelleB.toFloat())
            .putLong("dateDerniereCalibration", etat.dateDerniereCalibration)
            .apply()
    }

    private fun restaurerCalibrationSauvegardee() {
        val prefs = getSharedPreferences("daltoneye_calibration", MODE_PRIVATE)
        val date = prefs.getLong("dateDerniereCalibration", 0L)
        if (date == 0L) return // rien de sauvegarde
        gestionnaireCalibration.restaurer(
            GestionnaireCalibration.EtatCalibration(
                decalageR = prefs.getFloat("decalageR", 0f).toDouble(),
                decalageG = prefs.getFloat("decalageG", 0f).toDouble(),
                decalageB = prefs.getFloat("decalageB", 0f).toDouble(),
                echelleR = prefs.getFloat("echelleR", 1f).toDouble(),
                echelleG = prefs.getFloat("echelleG", 1f).toDouble(),
                echelleB = prefs.getFloat("echelleB", 1f).toDouble(),
                dateDerniereCalibration = date
            )
        )
    }

    /** Met a jour le texte de l'ecran de calibration selon l'etape en cours (blanc ou noir). */
    private fun afficherEtapeCalibration() {
        when (gestionnaireCalibration.etapeActuelle()) {
            GestionnaireCalibration.Etape.POINT_BLANC -> {
                titreCalibration.text = "Calibration nécessaire"
                messageCalibration.text = "Pointe l'appareil vers une feuille blanche ou un objet neutre, bien éclairé, puis valide."
                boutonCalibrer.text = "Calibrer sur ce blanc"
            }
            GestionnaireCalibration.Etape.POINT_NOIR -> {
                titreCalibration.text = "Étape 2 : le noir"
                messageCalibration.text = "Pointe maintenant l'appareil vers une zone bien noire (tissu noir, tiroir fermé...), puis valide."
                boutonCalibrer.text = "Calibrer sur ce noir"
            }
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun deverrouillerBalanceDesBlancs() {
        val cam = camera ?: return
        Camera2CameraControl.from(cam.cameraControl).captureRequestOptions =
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, false)
                .build()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun verrouillerBalanceDesBlancs() {
        val cam = camera ?: return
        Camera2CameraControl.from(cam.cameraControl).captureRequestOptions =
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
                .build()
    }

    /**
     * Force la mise au point ET la mesure d'exposition sur le centre de l'ecran (le reticule),
     * plutot que de laisser la camera choisir sa propre zone pour l'une comme pour l'autre.
     * Sans ca, la camera peut regler son exposition globale sur toute la scene visible (une
     * fenetre ou une zone tres lumineuse dans le champ, par exemple), ce qui sous-expose la
     * piece pointee meme si elle est elle-meme bien eclairee. Relancee regulierement pour
     * suivre les changements de piece/distance/eclairage.
     */
    private fun declencherMiseAuPointCentree() {
        val cam = camera ?: return
        val point = previewCamera.meteringPointFactory.createPoint(0.5f, 0.5f)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .disableAutoCancel()
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    private val tickMiseAuPoint: Runnable = object : Runnable {
        override fun run() {
            declencherMiseAuPointCentree()
            gestionnaireUi.postDelayed(this, 3000L)
        }
    }

    /** Bascule la lampe torche. CameraX la pilote directement pendant que l'appli tient la camera. */
    private fun basculerTorche() {
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit() != true) {
            Toast.makeText(this, "Pas de flash disponible sur ce téléphone", Toast.LENGTH_SHORT).show()
            return
        }
        torcheActive = !torcheActive
        cam.cameraControl.enableTorch(torcheActive)
        boutonFlash.alpha = if (torcheActive) 1.0f else 0.5f
    }

    private fun demarrerCamera() {
        val fournisseurCamera = ProcessCameraProvider.getInstance(this)
        fournisseurCamera.addListener({
            val cameraProvider = fournisseurCamera.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewCamera.surfaceProvider)
            }

            val analyseImage = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(executeurCamera) { image -> analyserImage(image) }
                }

            val selecteurCamera = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, selecteurCamera, preview, analyseImage)
            } catch (e: Exception) {
                Toast.makeText(this, "Impossible de démarrer la caméra : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Lit la zone centrale de l'image (format YUV_420_888 fourni par CameraX), calcule la
     * couleur RGB moyenne de cette zone, l'affiche brute dans le carre d'apercu, la corrige
     * via la calibration puis la classe dans une famille de couleur.
     * Aucune image n'est sauvegardee : tout est jete apres analyse.
     */
    private fun analyserImage(image: ImageProxy) {
        try {
            val (couleurBrute, dispersion) = couleurMoyenneZoneCentrale(image)
            val (r, g, b) = couleurBrute
            derniereCouleurBrute = Triple(r, g, b)

            // Affichage fige (appui long) : on garde la derniere couleur brute a jour pour la calibration,
            // mais on ne touche pas au resultat affiche ni au suivi de stabilite pendant le gel.
            if (affichageFige) return

            val corrigeInstantane = gestionnaireCalibration.corriger(r, g, b)

            // Detecte un changement brusque de scene (nouvelle piece sous le reticule) : si la
            // lecture instantanee s'ecarte trop de la moyenne du tampon, on vide le tampon au lieu
            // de le mélanger progressivement, pour eviter un affichage "flou" pendant la transition.
            if (tamponCouleurs.isNotEmpty()) {
                val moyR = tamponCouleurs.sumOf { it.first } / tamponCouleurs.size
                val moyG = tamponCouleurs.sumOf { it.second } / tamponCouleurs.size
                val moyB = tamponCouleurs.sumOf { it.third } / tamponCouleurs.size
                val ecart = maxOf(
                    kotlin.math.abs(corrigeInstantane.first - moyR),
                    kotlin.math.abs(corrigeInstantane.second - moyG),
                    kotlin.math.abs(corrigeInstantane.third - moyB)
                )
                if (ecart > SEUIL_CHANGEMENT_SCENE) tamponCouleurs.clear()
            }

            // Lissage temporel : on ajoute la lecture corrigee au tampon, et on classe/affiche
            // la MOYENNE du tampon plutot que la lecture instantanee, pour reduire le bruit
            // residuel visible d'une image a l'autre.
            tamponCouleurs.addLast(corrigeInstantane)
            if (tamponCouleurs.size > TAILLE_TAMPON_LISSAGE) tamponCouleurs.removeFirst()
            val corrige = Triple(
                tamponCouleurs.sumOf { it.first } / tamponCouleurs.size,
                tamponCouleurs.sumOf { it.second } / tamponCouleurs.size,
                tamponCouleurs.sumOf { it.third } / tamponCouleurs.size
            )

            val famille = ClassificateurCouleur.classifier(corrige.first, corrige.second, corrige.third)
            val sature = corrige.first >= 250 && corrige.second >= 250
            val mesure = InstantaneMesure(System.currentTimeMillis(), Triple(r, g, b), corrigeInstantane, corrige, famille.nom, dispersion)
            instantane = mesure

            // Suivi de stabilite : vibration courte une seule fois quand la meme famille tient
            // sur plusieurs images d'affilee, pas a chaque frame.
            if (famille.nom == derniereFamilleVue) {
                comptageStabilite++
            } else {
                derniereFamilleVue = famille.nom
                comptageStabilite = 1
                dejaVibrePourCetteStabilite = false
            }
            if (comptageStabilite >= seuilFramesStable && !dejaVibrePourCetteStabilite) {
                dejaVibrePourCetteStabilite = true
                instantaneValidation = mesure
                runOnUiThread { vibrerCourt() }
            }

            runOnUiThread {
                // Teinte le carre arrondi sans perdre ses coins/bordure (mutation du GradientDrawable)
                (carreCouleurCaptee.background as? GradientDrawable)?.setColor(Color.rgb(r, g, b))
                if (ecranCalibration.visibility != android.view.View.VISIBLE) {
                    nomFamilleCouleur.text = famille.nom
                    texteHexCapte.text = String.format("#%02X%02X%02X", corrige.first, corrige.second, corrige.third)
                    barreAccentResultat.setBackgroundColor(Color.parseColor("#" + famille.hexReference))
                    badgeSature.visibility = if (sature) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
        } finally {
            image.close()
        }
    }

    /** Rejette les pixels aberrants (reflet, bord de piece) : mediane plutot que moyenne.
     * Zone CIRCULAIRE, coherente avec le reticule affiche a l'ecran (avant : zone carree,
     * les coins hors du cercle visible influençaient quand meme le calcul). */
    private fun couleurMoyenneZoneCentrale(image: ImageProxy): Pair<Triple<Int, Int, Int>, String> {
        val largeur = image.width
        val hauteur = image.height
        val planY = image.planes[0]
        val planU = image.planes[1]
        val planV = image.planes[2]

        val tailleZone = (minOf(largeur, hauteur) * fractionZoneAnalyse).toInt().coerceAtLeast(4)
        val rayon = tailleZone / 2
        val rayonCarre = rayon * rayon
        val centreX = largeur / 2
        val centreY = hauteur / 2

        val valeursR = mutableListOf<Int>()
        val valeursG = mutableListOf<Int>()
        val valeursB = mutableListOf<Int>()

        val pasEchantillon = 2 // on echantillonne un pixel sur deux pour rester leger
        var y = centreY - rayon
        while (y < centreY + rayon) {
            var x = centreX - rayon
            while (x < centreX + rayon) {
                // Ne garder que les pixels DANS le cercle (coherent avec le reticule affiche)
                val dx = x - centreX
                val dy = y - centreY
                if (dx * dx + dy * dy <= rayonCarre) {
                    val (r, g, b) = pixelYuvVersRgb(planY, planU, planV, x, y, image.width, planY.rowStride, planU.rowStride, planU.pixelStride)
                    valeursR.add(r); valeursG.add(g); valeursB.add(b)
                }
                x += pasEchantillon
            }
            y += pasEchantillon
        }

        if (valeursR.isEmpty()) return Pair(Triple(128, 128, 128), "non disponible")

        val canalR = DispersionZone.resumer(valeursR)
        val canalG = DispersionZone.resumer(valeursG)
        val canalB = DispersionZone.resumer(valeursB)
        return Pair(
            Triple(canalR.mediane, canalG.mediane, canalB.mediane),
            DispersionZone.formater(canalR, canalG, canalB, valeursR.size)
        )
    }

    private fun pixelYuvVersRgb(
        planY: ImageProxy.PlaneProxy,
        planU: ImageProxy.PlaneProxy,
        planV: ImageProxy.PlaneProxy,
        x: Int,
        y: Int,
        largeurImage: Int,
        rowStrideY: Int,
        rowStrideUV: Int,
        pixelStrideUV: Int
    ): Triple<Int, Int, Int> {
        val yValeur = lireOctet(planY.buffer, y * rowStrideY + x)
        val uvX = x / 2
        val uvY = y / 2
        val uValeur = lireOctet(planU.buffer, uvY * rowStrideUV + uvX * pixelStrideUV)
        val vValeur = lireOctet(planV.buffer, uvY * rowStrideUV + uvX * pixelStrideUV)

        val yD = yValeur.toDouble()
        val uD = uValeur.toDouble() - 128.0
        val vD = vValeur.toDouble() - 128.0

        val r = (yD + 1.402 * vD).toInt().coerceIn(0, 255)
        val g = (yD - 0.344136 * uD - 0.714136 * vD).toInt().coerceIn(0, 255)
        val b = (yD + 1.772 * uD).toInt().coerceIn(0, 255)
        return Triple(r, g, b)
    }

    private fun lireOctet(buffer: ByteBuffer, index: Int): Int {
        val i = index.coerceIn(0, buffer.limit() - 1)
        return buffer.get(i).toInt() and 0xFF
    }

    override fun onDestroy() {
        super.onDestroy()
        executeurCamera.shutdown()
    }
}
