# Citizen Guilds

Mod de gestion des guildes pour **Minecraft Java 1.21.1**, **NeoForge 21.1.215** et **Java 21**.

## Fonctionnalites
- Donnees de guilde sauvegardees dans le monde : membres, grades, invitations, réserve de ressources et journal.
- Creation, invitation, acceptation, depart, exclusion et promotion.
- **Réserve de ressources** partagée : dépôt de l'objet tenu en main et retrait par emplacement.
- Permissions : chef/officier pour gérer la réserve et retirer; tous les membres peuvent déposer.
- **Pierre de Jade** : minerai de craft obtenu comme butin dans les coffres de donjons, fondu en lingots. Ce n'est jamais une monnaie et n'a aucune valeur de paiement.
- **Armure complète en Jade** : casque, plastron, jambières et bottes, avec protection de haut niveau.
- Butin Jade ajouté aux coffres de donjons vanilla ciblés et aux tables de butin de mods dont le chemin contient dungeon, catacomb ou crypt.
- Aucune génération de minerai dans les grottes ou le monde ouvert.
- Interface sombre fantasy accessible avec **G**, avec synchronisation des membres, grades, réserve de ressources et journal depuis le serveur.
- Ramassage automatique de la ressource configurée vers la réserve de ressources de la guilde du joueur. Le système est générique et pourra accueillir d'autres minerais dans de futures extensions.
- Ressource à dépôt automatique configurable par un administrateur avec `/guild resourceitem namespace:objet` (par défaut `citizenguilds:raw_jade`).
- Annuaire des guildes et renommage par le chef.
- Compilation automatisée via GitHub Actions.

## Obtention et fabrication de la Jade
Le minerai brut `citizenguilds:raw_jade` est ajouté avec 12 % de chance aux coffres de donjons ciblés : donjons simples, mines abandonnées, couloirs/croisements de forteresse et cité antique. Les tables de butin de mods sont aussi prises en compte si leur chemin contient dungeon, catacomb ou crypt. La Pierre de Jade est une ressource de fabrication, jamais une monnaie : aucun achat, paiement, prix ou solde monétaire n'utilise la Jade.
Le minerai ne se génère pas dans le monde et ne se mine pas. Fondez-le dans un four ou un haut fourneau pour obtenir `citizenguilds:jade_ingot`. Les lingots servent à fabriquer les quatre pièces d'armure et à réparer l'équipement.

## Commandes de guilde
- `/guild create <nom>`
- `/guild invite <joueur>` puis `/guild accept`
- `/guild list` et `/guild info`
- `/guild rename \"Nouveau nom\"`
- `/guild deposit` et `/guild withdraw <emplacement> [quantite]` pour la réserve de ressources
- `/guild promote <joueur>`, `/guild kick <joueur>`, `/guild leave`, `/guild disband`
- `/guild resourceitem <namespace:objet>` (administrateur) et `/guild jadeinfo`

## Réserve de ressources et extensions
La réserve stocke des objets physiques, pas de monnaie. La Pierre de Jade peut y être déposée comme minerai de fabrication. Les statistiques peuvent compter les quantités de minerais stockées, mais jamais les traiter comme une richesse. Le mécanisme de dépôt automatique est basé sur un identifiant d'objet configurable afin de permettre l'ajout d'autres minerais et matériaux de craft dans de futures extensions.

## Installation
Compilez avec Java 21/Gradle, puis placez le JAR dans le dossier `mods` du client et du serveur. Le mod doit être présent des deux côtés.
