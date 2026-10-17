# IA-Planification

# Projet Sokoban - Planification Automatique

## Fichiers créés, ajoutés ou modifiés
* `src/main/java/sokoban/Agent.java` : Modifié (Implémentation de la boucle de jeu, génération du problème et intégration du planificateur FF de PDDL4J via l'extraction de la méthode `actions()`).
* `src/main/resources/pddl/domain.pddl` : Créé (Définition des règles du domaine Sokoban avec les types, prédicats et les actions `move` / `push`).
* `pom.xml` : Modifié (Ajout de la dépendance externe `fr.uga:pddl4j:4.0.0`).
* `pddl4j-4.0.0.jar` : Ajouté à la racine (Archive de la bibliothèque requise pour l'installation locale).

## Instructions de compilation (Portabilité)
Le projet utilise des chemins relatifs (`src/main/resources/pddl/domain.pddl`) pour garantir la portabilité entre les systèmes. 

Afin de pouvoir compiler le projet sur une nouvelle machine, la bibliothèque PDDL4J fournie à la racine doit d'abord être installée dans le cache Maven local :

1. Enregistrer la dépendance locale :
mvn install:install-file -Dfile="pddl4j-4.0.0.jar" -DgroupId=fr.uga -DartifactId=pddl4j -Dversion=4.0.0 -Dpackaging=jar

2. Compiler le projet :
mvn package

3. Exécuter le jeu :
java --add-opens java.base/java.lang=ALL-UNNAMED -server -Xms2048m -Xmx2048m -cp target/sokoban-1.0-SNAPSHOT-jar-with-dependencies.jar sokoban.SokobanMain
