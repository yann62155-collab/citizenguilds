# Citizen Guilds

Mod de gestion des guildes pour **Minecraft Java 1.21.1**, **NeoForge 21.1.215** et **Java 21**.

## Fonctionnalites
- Donnees de guilde sauvegardees dans le monde : membres, grades, invitations, banque et journal.
- Creation, invitation, acceptation, depart, exclusion et promotion.
- Banque partagee : depot de l'objet tenu en main et retrait par emplacement.
- Permissions : chef/officier pour gerer et retirer; tous les membres peuvent deposer.
- **Pierre de Jade** : minerai brut obtenu comme butin dans les coffres de donjons, fondu en lingots.
- **Armure complète en Jade** : casque, plastron, jambières et bottes, avec protection de haut niveau.
- Butin Jade ajouté aux coffres de donjons vanilla ciblés et aux tables de butin de mods dont le chemin contient dungeon, catacomb ou crypt.
- Aucune génération de minerai dans les grottes ou le monde ouvert.
- Interface sombre fantasy accessible avec **G** (tableau de bord visuel en cours de raccordement aux données).
- Compilation automatisée via GitHub Actions.

## Obtention et fabrication de la Jade
Le minerai brut `citizenguilds:raw_jade` est ajouté avec 12 % de chance aux coffres de donjons ciblés : donjons simples, mines abandonnées, couloirs/croisements de forteresse et cité antique. Les tables de butin de mods sont aussi prises en compte si leur chemin contient dungeon, catacomb ou crypt.
Le minerai ne se génère pas dans le monde et ne se mine pas. Fondez-le dans un four ou un haut fourneau pour obtenir `citizenguilds:jade_ingot`. Les lingots servent à fabriquer les quatre pièces d'armure et à réparer l'équipement.

## Commandes de guilde
- `/guild create <nom>`
- `/guild invite <joueur>` puis `/guild accept`
- `/guild info`
- `/guild deposit` et `/guild withdraw <emplacement> [quantite]`
- `/guild promote <joueur>`, `/guild kick <joueur>`, `/guild leave`, `/guild disband`

## Installation
Compilez avec Java 21/Gradle, puis placez le JAR dans le dossier `mods` du client et du serveur. Le mod doit être présent des deux côtés.
