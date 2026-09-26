package com.tomyn.daltoneye

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
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
    private lateinit var barreAccentResultat: android.view.View
    private lateinit var ecranCalibration: android.view.View
    private lateinit var boutonCalibrer: Button
    private lateinit var boutonCalibrerManuel: android.widget.ImageButton

    private lateinit var executeurCamera: ExecutorService
    private val gestionnaireCalibration = GestionnaireCalibration()

    // Derniere couleur brute captee (avant correction calibration), utilisee quand on appuie sur "Calibrer".
    @Volatile private var derniereCouleurBrute: Triple<Int, Int, Int>? = null

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

        previewCamera = findViewById(R.id.previewCamera)
        carreCouleurCaptee = findViewById(R.id.carreCouleurCaptee)
        nomFamilleCouleur = findViewById(R.id.nomFamilleCouleur)
        barreAccentResultat = findViewById(R.id.barreAccentResultat)
        ecranCalibration = findViewById(R.id.ecranCalibration)
        boutonCalibrer = ecranCalibration.findViewById(R.id.boutonCalibrer)
        boutonCalibrerManuel = findViewById(R.id.boutonCalibrerManuel)

        boutonCalibrer.setOnClickListener { validerCalibration() }
        boutonCalibrerManuel.setOnClickListener { ecranCalibration.visibility = android.view.View.VISIBLE }

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
        verifierCalibration()
    }

    private fun verifierCalibration() {
        if (gestionnaireCalibration.calibrationNecessaire(System.currentTimeMillis())) {
            ecranCalibration.visibility = android.view.View.VISIBLE
        }
    }

    private fun validerCalibration() {
        val brute = derniereCouleurBrute
        if (brute == null) {
            Toast.makeText(this, "Pointe d'abord la caméra vers le blanc de référence", Toast.LENGTH_SHORT).show()
            return
        }
        gestionnaireCalibration.calibrerSur(brute.first, brute.second, brute.third, System.currentTimeMillis())
        ecranCalibration.visibility = android.view.View.GONE
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
                cameraProvider.bindToLifecycle(this, selecteurCamera, preview, analyseImage)
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
            val (r, g, b) = couleurMoyenneZoneCentrale(image)
            derniereCouleurBrute = Triple(r, g, b)

            val corrige = gestionnaireCalibration.corriger(r, g, b)
            val famille = ClassificateurCouleur.classifier(corrige.first, corrige.second, corrige.third)

            runOnUiThread {
                // Teinte le carre arrondi sans perdre ses coins/bordure (mutation du GradientDrawable)
                (carreCouleurCaptee.background as? GradientDrawable)?.setColor(Color.rgb(r, g, b))
                if (ecranCalibration.visibility != android.view.View.VISIBLE) {
                    nomFamilleCouleur.text = famille.nom
                    barreAccentResultat.setBackgroundColor(Color.parseColor("#" + famille.hexReference))
                }
            }
        } finally {
            image.close()
        }
    }

    /**
     * Conversion YUV -> RGB simplifiee, moyennee sur un carre central d'environ 10% de la largeur/hauteur
     * de l'image (suffisant pour capter la couleur du plastique tenu devant la camera, sans etre
     * perturbe par le fond autour).
     */
    private fun couleurMoyenneZoneCentrale(image: ImageProxy): Triple<Int, Int, Int> {
        val largeur = image.width
        val hauteur = image.height
        val planY = image.planes[0]
        val planU = image.planes[1]
        val planV = image.planes[2]

        val tailleZone = (minOf(largeur, hauteur) * 0.10).toInt().coerceAtLeast(4)
        val centreX = largeur / 2
        val centreY = hauteur / 2

        var sommeR = 0L; var sommeG = 0L; var sommeB = 0L
        var nbPixels = 0

        val pasEchantillon = 2 // on echantillonne un pixel sur deux pour rester leger
        var y = centreY - tailleZone / 2
        while (y < centreY + tailleZone / 2) {
            var x = centreX - tailleZone / 2
            while (x < centreX + tailleZone / 2) {
                val (r, g, b) = pixelYuvVersRgb(planY, planU, planV, x, y, image.width, planY.rowStride, planU.rowStride, planU.pixelStride)
                sommeR += r; sommeG += g; sommeB += b
                nbPixels++
                x += pasEchantillon
            }
            y += pasEchantillon
        }

        if (nbPixels == 0) return Triple(128, 128, 128)
        return Triple((sommeR / nbPixels).toInt(), (sommeG / nbPixels).toInt(), (sommeB / nbPixels).toInt())
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
