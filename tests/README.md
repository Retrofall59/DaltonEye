# Tests de non-régression — ClassificateurCouleur

Ce dossier garde trace de tous les vrais bugs de classification de couleur trouvés et corrigés
sur le terrain, pour ne jamais les laisser revenir en modifiant une référence plus tard.

## Lancer les tests

```
cd tests
kotlinc ../app/src/main/java/com/tomyn/daltoneye/ClassificateurCouleur.kt TestsNonRegression.kt -include-runtime -d test.jar
java -jar test.jar
```

Et pour la dispersion des pixels (DispersionZone) :

```
kotlinc ../app/src/main/java/com/tomyn/daltoneye/DispersionZone.kt TestDispersion.kt -include-runtime -d test_disp.jar
java -jar test_disp.jar
```

Et pour l'explication du classement et les mesures brutes du rapport de diagnostic :

```
kotlinc ../app/src/main/java/com/tomyn/daltoneye/ClassificateurCouleur.kt \
        ../app/src/main/java/com/tomyn/daltoneye/RapportMesure.kt TestExplicationEtMesures.kt \
        -include-runtime -d test_expl.jar
java -jar test_expl.jar
```

## Contenu

- **`TestsNonRegression.kt`** — les cas réels (pince PCA bleue, bobine Anycubic Purple, gris
  neutres, cas d'écrêtage capteur, etc.), chacun avec une explication de quel bug il a révélé.
  Doivent **toujours** tous passer.
- **`TestDispersion.kt`** — vérifie que la médiane de la zone reste strictement identique à l'ancienne formule (la dispersion ajoutée au rapport de diagnostic ne change rien à la couleur mesurée) et que les percentiles sont corrects.
- **`TestExplicationEtMesures.kt`** — vérifie que l'explication du classement (règle appliquée, trois familles les plus proches) reste toujours cohérente avec le classificateur, sur une grille de 140 608 couleurs, et que la mise en forme des mesures brutes du rapport est correcte.
- **`nuancier_bambu_201_couleurs.csv`** — les 201 couleurs officielles du nuancier Bambu Lab
  (PLA Basic, Matte, ABS, PETG, gradients, bicolores...), avec la famille DaltonEye jugée
  raisonnable pour chacune. Sert d'outil de diagnostic large : le score de référence est
  **157/201**. Une grosse baisse de ce score après un changement est un signal à examiner —
  mais toutes les "erreurs" restantes ne sont pas forcément de vrais bugs : beaucoup sont des cas
  limites défendables (couleurs très sombres proches du noir, teintes pâles proches du
  transparent) dans un système à 14 familles volontairement larges.

## Avant de toucher une référence de couleur

1. Lance les tests, note le score de départ.
2. Fais le changement.
3. Relance les tests : les cas réels doivent rester à 100% ; si le score du nuancier Bambu
   baisse fortement, regarde le détail des écarts avant de livrer.
