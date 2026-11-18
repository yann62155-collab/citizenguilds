# Citizen Guilds

Mod de gestion des guildes pour **Minecraft Java 1.21.1**, **NeoForge 21.1.215** et **Java 21**.

## Fonctionnalites implementees
- Donnees de guilde sauvegardees dans le monde (membres, grades, invitations, banque, Jade et journal).
- Creation, invitation, acceptation, depart, exclusion et promotion.
- Banque partagee : depot de l'objet tenu en main et retrait par emplacement.
- Permissions : chef/officier pour gerer et retirer; tous les membres peuvent deposer.
- Tresor de Jade collectif avec collecte automatique de l'objet configure.
- Historique des actions recentes.
- Interface sombre fantasy accessible avec **G**, avec un onglet d'aide aux commandes.
- Compilation automatisee via GitHub Actions.

## Installation
1. Compilez avec Java 21 et Gradle (le workflow GitHub construit le JAR).
2. Placez le JAR dans le dossier `mods` du client et du serveur.
3. Redemarrez le serveur.

## Commandes en jeu
- `/guild create <nom>`
- `/guild invite <joueur>` puis le joueur utilise `/guild accept`
- `/guild info`
- `/guild deposit` : depose toute la pile tenue en main.
- `/guild withdraw <emplacement> [quantite]` : les emplacements commencent a 1.
- `/guild promote <joueur>` : le chef alterne membre/officier.
- `/guild kick <joueur>`, `/guild leave`, `/guild disband`

## Configuration de la Pierre de Jade
L'identifiant de l'objet varie selon le mod qui l'ajoute. Un administrateur (permission 2) doit donc le definir une fois :
`/guild jadeitem namespace:identifiant`

Exemple de forme : `/guild jadeitem monmod:jade` (remplacez par l'identifiant reel de votre objet).
Lorsqu'un membre possedant cet objet est dans une guilde, le mod convertit automatiquement les objets de son inventaire en points de Jade de guilde, une fois par seconde. Les objets sont consommes et le total est conserve dans les donnees du monde.

## Limites connues
- Le nom de l'objet Pierre de Jade doit etre configure par un administrateur; aucun identifiant de mod externe n'est suppose.
- La banque est geree par commandes pour cette version; l'interface est un tableau de bord et un aide-memoire, pas encore un coffre interactif avec synchronisation reseau.
- Les donnees sont propres a chaque monde/serveur et sont sauvegardees dans le dossier de donnees du monde.
