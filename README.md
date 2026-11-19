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

## Donjons de guilde

Le système de donjon utilise une dimension dédiée `citizenguilds:dungeon`. Les salles et les pièges sont construits dans cette dimension isolée : le terrain du monde principal n'est pas creusé ni remplacé.

### Préparer un site (administrateur)

1. Définir les coordonnées minimales autorisées : `/guild dungeon min <x> <y> <z>`.
2. Se placer dans l'Overworld, dans une zone où les chunks sont déjà chargés, puis lancer `/guild dungeon scan`.
3. Vérifier le candidat avec `/guild dungeon site`.
4. Poser la porte avec `/guild dungeon portal create`.

Le scanner recherche autour de l'administrateur une surface naturelle, éloignée des joueurs et avec du relief. Il ne modifie aucun bloc du monde principal. Si WorldGuard est disponible sur le serveur Arclight, les régions protégées sont aussi exclues. L'apparition automatique vérifie les chunks déjà chargés et nécessite un administrateur en ligne dans l'Overworld pour effectuer le scan.

### Lancer une session

- `/guild dungeon start <nom de guilde>` ouvre une session pour une seule guilde.
- Les membres de cette guilde interagissent avec la porte pour entrer dans l'instance.
- La porte intérieure permet de revenir au site d'entrée.
- `/guild dungeon status` affiche la vague, les participants et l'état de la session.
- `/guild dungeon autoscan true|false` active ou désactive l'apparition automatique des portes (activée par défaut).
- `/guild dungeon interval <minutes>` règle le délai entre apparitions (5 à 1440 minutes, 30 par défaut).
- `/guild dungeon reward <quantite>` règle la récompense en minerai de Jade par survivant (0 à 64, 3 par défaut).
- `/guild dungeon end` ferme la session, retire les créatures suivies et ramène les participants connectés dans l'Overworld.
- `/guild top dungeons` classe les guildes par victoires en donjon ; `/guild top` conserve le classement d'activité de dépôt.
- `/guild dungeon portal remove` retire la porte extérieure après la fermeture.

### Déroulement

Le donjon comporte trois zones successives : une salle d'entrée, une salle intermédiaire avec pièges au sol, puis une salle de boss. Les vagues comprennent zombies et araignées, squelettes et husks, puis un Gardien des profondeurs (Ravager renforcé) accompagné de vindicateurs. Une mort élimine définitivement le joueur de la session. Les survivants reçoivent chacun 3 minerais de Pierre de Jade ; les récompenses des joueurs déconnectés sont conservées jusqu'à leur reconnexion. Les victoires et éliminations alimentent les statistiques de guilde.

Une seule session globale peut être active à la fois. Les sessions et les éliminations sont sauvegardées dans les données du monde.
