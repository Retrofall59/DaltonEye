# Changelog

## v1.20 (build 21)

**Ajouts :**
- L'écran ne se met plus en veille automatiquement pendant l'utilisation de l'appli.
- Appui long sur le code hex affiché : le copie dans le presse-papier (utile pour signaler un cas de classification douteuse).
- Nouveau bouton "Copier les infos de diagnostic" dans les Paramètres : rassemble modèle du téléphone, version de l'appli, dernier hex affiché et facteurs de calibration en un coup, pour accélérer un futur signalement.

## v1.19 (build 20)

**Ajouts au menu Paramètres :**
- **Rapidité de la vibration** (Rapide/Normale par défaut/Lente) : règle le nombre d'images stables nécessaires avant la vibration de confirmation.
- **Carte "Bon à savoir"** : rappel de quand recalibrer manuellement (changement de fond, torche, éclairage) plutôt que d'attendre le délai automatique.
- **Bouton "Réinitialiser les paramètres"** : remet tout aux valeurs par défaut en un geste.

**Non retenu :** choix de caméra macro — vérifié que le Redmi 15C 5G n'a pas d'objectif macro (confirmé par la FAQ Xiaomi officielle), ce réglage n'aurait rien eu à proposer.

## v1.18 (build 19)

**Ajout : écran Paramètres** (icône engrenage dans l'en-tête)
- **Taille de la zone d'analyse** : Petit / Moyen (défaut) / Grand, redimensionne aussi le réticule à l'écran en conséquence. Plus besoin de repasser par une mise à jour pour ajuster ça.
- **Délai avant recalibration** : 30 min / 1h (défaut) / 2h.
- **Vibration sur lecture stable** : on/off.
- **À propos** : affiche le numéro de version en cours, utile pour signaler un retour sur le forum.

## v1.17 (build 18)

**Ajouts :**
- Calibration persistante : elle survit maintenant à une fermeture complète de l'appli, pas seulement à une rotation d'écran. Sauvegardée dans les préférences internes du téléphone.
- Badge d'avertissement "Trop de lumière" affiché quand le capteur sature (rouge et vert collés à 255, cf. correctif v1.15) — visible plutôt qu'invisible, pour comprendre pourquoi une lecture peut être instable et savoir qu'il faut s'éloigner ou changer l'éclairage.
- Nouveau dossier `tests/` dans le dépôt : rejoue automatiquement tous les cas réels rencontrés (pince PCA, Anycubic Purple, gris neutres, écrêtage capteur...) pour éviter de recasser un bug déjà corrigé, plus le nuancier complet des 201 couleurs Bambu comme outil de diagnostic. Rien de visible pour l'usage quotidien de l'appli.

## v1.16 (build 17)

**Ajout :**
- L'appli n'est plus bloquée en portrait, elle s'adapte à l'orientation du téléphone. Point important géré au passage : l'activité n'est plus recréée lors d'une rotation (configChanges), donc la calibration en cours, la caméra et la torche ne sont plus réinitialisées à chaque pivot du téléphone.

## v1.15 (build 16)

**Correctif ciblé, sans effet de bord :**
- Sur une pièce très claire (crème/ivoire), le capteur peut saturer complètement les canaux rouge et vert (les deux collés à 255) ; seul le bleu garde de la marge pour varier, rendant la classification instable au moindre micro-mouvement (1mm suffisait à faire basculer "Beige" en "Jaune"). Cas identifié via 3 hex réels : #FFFF7B, #FFFFF8, #FFFFCB.
- Correctif local : quand rouge ET vert sont saturés (≥250), la classification se fait uniquement sur le bleu (Blanc/Beige/Jaune selon sa valeur), sans passer par le calcul habituel. Les couleurs qui ne saturent pas le capteur ne sont pas concernées par ce changement — vérifié sur l'ensemble des cas déjà validés (bleu, violet, cyan, magenta, rose, gris, marron, jaune normal, etc.), rien n'a bougé.
- Une correction d'exposition globale avait été envisagée puis écartée : elle aurait affecté toutes les mesures, pas seulement les pièces très claires.

## v1.14 (build 15)

**Ajustement :**
- Zone d'analyse réduite de 10% à 5% de l'image, et réticule visuel réduit en conséquence (90dp → 45dp) pour mieux correspondre à ce qui est réellement mesuré. Corrige les petites chutes (brims, jupes d'adhérence) où la zone précédente, trop grande, mélangeait la couleur de la pièce avec le fond autour. Sur une pièce minuscule, il peut rester nécessaire de rapprocher le téléphone pour bien remplir le réticule.

## v1.13 (build 14)

**Correctif majeur : calibration en deux points (blanc + noir).**
- Jusqu'ici la calibration ne corrigeait qu'un déséquilibre proportionnel (calé sur le blanc). Un léger "voile" de teinte constant dans l'éclairage ambiant restait quasi invisible sur du blanc (très lumineux) mais devenait énorme en proportion sur du noir (peu de lumière) — c'est ce qui faisait apparaître "Marron" sur des objets réellement noirs (boîtier brillant, raté d'impression mat), même après une calibration correcte sur blanc.
- La calibration se fait maintenant en deux étapes : d'abord un point blanc (comme avant, verrouille aussi la balance des blancs matérielle), puis un point noir (mesure et soustrait le voile). L'écran de calibration affiche maintenant l'étape en cours ("Étape 2 : le noir") et le bouton change de texte en conséquence.
- Vérifié par simulation : un voile chaud artificiel appliqué à un objet noir est bien neutralisé après calibration deux points, le blanc et un gris médian restent corrects.

## v1.12 (build 13)

**Correctif :**
- Ajout d'un garde-fou pour les couleurs quasi neutres (R, G, B très proches) : elles sont maintenant classées directement en Noir/Gris/Blanc selon leur clarté, sans passer par la comparaison habituelle avec les 15 familles. Corrige un vrai bug trouvé en testant les 201 couleurs officielles du nuancier Bambu Lab : des gris parfaitement neutres (ex. #515151) étaient classés à tort "Marron", et certains gris légèrement bleutés (ex. #5F6367) étaient classés "Violet". Vérifié : aucune régression sur les cas déjà validés (bleu, violet, cyan, magenta, rose, beige, marron, etc.).
- Note méthode : un remplacement complet des 15 références par la moyenne du nuancier Bambu a été testé puis écarté — il corrigeait certains cas mais en cassait d'autres (notamment le Cyan et le Violet officiels de Bambu), donc pas retenu. Le nuancier des 201 couleurs Bambu reste un bon outil de diagnostic pour de futurs ajustements ciblés.

## v1.11 (build 12)

**Correctif :**
- Référence "Violet" recalée de #6A1CC7 (violet profond, très peu de vert) vers #6A78C7. Cas identifié via un test Anycubic : le hex officiel du fabricant pour sa couleur "Purple" (#6A6DCD) était classé à tort "Bleu" par l'appli. Ma référence Violet était trop pure/saturée pour représenter les violets-bleutés courants ("periwinkle", "slate blue") que plusieurs fabricants nomment "Purple". Vérifié sur un jeu de cas de non-régression (bleus vifs réels, cyans, magenta, rose, et les 9 autres familles) : rien d'autre n'est affecté. Point noté en passant : un violet théorique très spécifique (rouge=bleu, vert nul, type "Purple" web #800080) penche maintenant plutôt vers Magenta — teinte à la frontière naturelle entre les deux, pas un vrai produit de l'inventaire de Tomyn.

## v1.10 (build 11)

**Ajout :**
- Bouton lampe torche dans l'en-tête (icône éclair), piloté directement par l'appli via CameraX. Nécessaire car les réglages rapides du téléphone ne peuvent pas accéder au flash tant que DaltonEye tient la caméra. Le bouton s'assombrit quand la torche est éteinte, s'éclaircit quand elle est active. Torche coupée automatiquement si l'appli passe en arrière-plan.

## v1.9 (build 10)

**Correctif :**
- La mesure d'exposition (luminosité) était basée sur toute la scène visible par la caméra, pas sur la zone du réticule. Une zone lumineuse dans le champ (fenêtre, carton clair, etc.) pouvait faire sous-exposer la pièce visée même si elle était elle-même bien éclairée — cas identifié via un test Anycubic où le hex capté était deux fois plus sombre que le hex officiel du fabricant, faisant basculer un vrai "Bleu" en "Violet". L'exposition est maintenant verrouillée sur le centre de l'écran (réticule), en même temps que la mise au point (v1.7), et relancée régulièrement.

## v1.8 (build 9)

**Correctif :**
- Référence "Bleu" recalée de #1449E0 (bleu profond/marine) vers #0D6EFD (bleu vif courant), à partir d'un cas réel remonté par Tomyn (#0992F2, une pince bleue classée à tort "Cyan"). L'ancienne référence était trop sombre/pure pour représenter les bleus vifs courants (type "bleu web", DodgerBlue), qui basculaient à tort côté Cyan. Vérifié sur un jeu de cas de non-régression (bleus profonds, cyans et turquoises réels, toutes les autres familles) : rien d'autre n'est affecté.

## v1.7 (build 8)

**Ajout :**
- Mise au point forcée en continu sur le centre de l'écran (là où se trouve le réticule), relancée toutes les 3 secondes. Corrige le flou rapporté sur les pièces tenues de près : l'autofocus par défaut avait tendance à faire la moyenne entre la pièce et l'arrière-plan au lieu de se concentrer sur ce qui est sous le réticule.

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
