# DaltonEye

Appli Android pour trier des chutes/ratés d'impression 3D par couleur primaire, avant de les broyer et de les refondre en filament recyclé.

## Fonctionnement

- Ouvre l'appli, la caméra s'allume en flux direct (aucune photo n'est prise ni stockée).
- Approche le morceau de plastique du réticule au centre de l'écran.
- En bas de l'écran : un carré affiche la couleur brute captée par le téléphone, et le nom de la famille de couleur détectée s'affiche à côté.
- Si la couleur affichée ne correspond pas à ce que voit l'œil, bouge légèrement le morceau ou relance l'analyse (elle tourne en continu, pas besoin d'appuyer sur un bouton).

## Familles de couleur détectées

Rouge, Orange, Jaune, Vert, Cyan, Bleu, Violet, Magenta, Rose, Beige, Marron, Noir, Blanc, Gris / Argenté, Transparent.

Volontairement peu nombreuses et larges (pas de nuances fines type "rouge bordeaux" vs "rouge vif") : l'objectif est un tri fiable par couleur primaire, pas une identification exacte de teinte. Deux nuances proches d'une même famille (ex. noir profond et noir mat) sont classées ensemble, ce qui est le comportement voulu.

## Calibration

L'éclairage d'un atelier change au fil de la journée (lumière naturelle, lumière artificielle le soir), ce qui peut décaler la perception des couleurs par la caméra. Pour compenser :

- Au lancement et **toutes les heures** (le délai court en continu tant que l'appli est ouverte, même en pleine session), un écran de calibration bloquant s'affiche.
- Pointe la caméra vers une feuille blanche ou un objet neutre, appuie sur "Calibrer sur ce blanc".
- L'analyse reprend normalement pour l'heure suivante.

## Distribution

Comme pour [BambuRfidReader](https://github.com/Retrofall59/BambuRfidReader) : chaque mise à jour est livrée sous forme de zip complet du dépôt à uploader sur GitHub, ce qui déclenche automatiquement la compilation de l'APK via GitHub Actions (onglet Actions du dépôt, artefact téléchargeable une fois le build terminé).

## Historique des versions

Voir [CHANGELOG.md](CHANGELOG.md).
