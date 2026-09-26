# Changelog

## v1.1 (build 2)

**Correctif :**
- La calibration sur blanc forçait mathématiquement la surface calibrée à être classée "Transparent" plutôt que "Blanc" : la valeur cible de calibration (200,200,200) tombait presque exactement sur la référence de la famille "Transparent" (C9C9C9). La cible est maintenant à 240,240,240, proche de la référence "Blanc" (F5F5F5).

## v1.0 (build 1)

Première version.

- Flux caméra en direct (CameraX), aucune photo prise ni stockée.
- Analyse continue de la zone centrale de l'image, classement dans l'une des 14 familles de couleur (Rouge, Orange, Jaune, Vert, Cyan, Bleu, Violet, Magenta, Rose, Marron, Noir, Blanc, Gris/Argenté, Transparent) via la même formule de distance colorimétrique CIEDE2000 que BambuRfidReader.
- Écran affichant l'aperçu brut de la couleur captée + le nom de la famille détectée.
- Calibration sur blanc/gris neutre, imposée toutes les heures en continu (même en pleine session), bloquante tant qu'elle n'est pas faite.
