# Changelog

## v1.6 (build 7)

**Ajout :**
- Le code hex de la couleur captée (après correction de calibration) s'affiche maintenant en petit à côté du nom de la famille. Objectif : en cas de mauvaise classification (ex. un bleu classé "Cyan"), pouvoir donner le hex exact plutôt que d'estimer à l'œil, pour corriger précisément les références de couleur avec de vraies données.

## v1.5 (build 6)

**Correctif majeur :**
- La balance des blancs de la caméra se réajustait automatiquement selon tout ce qui apparaît dans le champ (fond marron, etc.), ce qui faussait les couleurs même après une calibration correcte sur blanc — poser une pièce sur un fond marron donnait un résultat faux même si la calibration avait été bien faite sur une feuille blanche juste avant.
- La calibration verrouille maintenant la balance des blancs au niveau matériel (Camera2) juste après avoir capturé la référence blanche : la séquence est déverrouiller → laisser la caméra reconverger sur le blanc pointé → capturer → verrouiller. La balance ne bouge plus ensuite tant qu'on n'a pas recalibré (heure suivante ou bouton manuel). Aucun changement sur la fréquence de calibration elle-même.

## v1.4 (build 5)

**Ajouts :**
- Compte à rebours avant la prochaine calibration obligatoire, affiché en permanence dans l'en-tête (format mm:ss).
- Appui long sur l'écran caméra : fige l'affichage du résultat pendant 3 secondes (badge "Figé" visible), pratique le temps d'ajuster la position du plastique sans que le résultat change sous les yeux.
- Vibration courte quand la même couleur est détectée sur plusieurs images d'affilée (lecture stable) : une seule vibration par stabilisation, pas de répétition tant que la couleur ne change pas.

## v1.3 (build 4)

**Ajout :**
- Bouton de calibration manuelle dans l'en-tête (icône cible en haut à droite), pour lancer une calibration à tout moment sans attendre le délai automatique d'une heure.

## v1.2 (build 3)

**Ajout :**
- Nouvelle famille "Beige" (15e famille), pour distinguer les teintes beiges/crème du blanc. Attention : le beige étant une teinte proche du blanc (peu saturée), la distinction reste plus sensible aux variations d'éclairage que les autres familles ; à surveiller à l'usage.

## v1.1 (build 2)

**Correctif :**
- La calibration sur blanc forçait mathématiquement la surface calibrée à être classée "Transparent" plutôt que "Blanc" : la valeur cible de calibration (200,200,200) tombait presque exactement sur la référence de la famille "Transparent" (C9C9C9). La cible est maintenant à 240,240,240, proche de la référence "Blanc" (F5F5F5).

## v1.0 (build 1)

Première version.

- Flux caméra en direct (CameraX), aucune photo prise ni stockée.
- Analyse continue de la zone centrale de l'image, classement dans l'une des 14 familles de couleur (Rouge, Orange, Jaune, Vert, Cyan, Bleu, Violet, Magenta, Rose, Marron, Noir, Blanc, Gris/Argenté, Transparent) via la même formule de distance colorimétrique CIEDE2000 que BambuRfidReader.
- Écran affichant l'aperçu brut de la couleur captée + le nom de la famille détectée.
- Calibration sur blanc/gris neutre, imposée toutes les heures en continu (même en pleine session), bloquante tant qu'elle n'est pas faite.
