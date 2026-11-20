# Citizen Guilds

Mod de gestion des guildes pour **Minecraft Java 1.21.1**, **NeoForge 21.1.215** et **Java 21**.

## Dépendance
- **Hibou Postal (`hiboupostal`) est requis** sur le serveur et les clients pour le recrutement par numéro de hibou.

## Fonctionnalites
- Donnees de guilde sauvegardees dans le monde : membres, grades, invitations, réserve de ressources et journal.
- Recrutement par numéro de hibou CitizenHibou (UUID vérifié dans le registre du mod), acceptation/refus, départ, exclusion et promotion.
- **Réserve de ressources** partagée : dépôt de l'objet tenu en main et retrait par emplacement.
- Permissions : chef/officier pour gérer la réserve et retirer; tous les membres peuvent déposer.
- **Pierre de Jade** : minerai de craft obtenu comme butin dans les coffres de donjons, fondu en lingots. Ce n'est jamais une monnaie et n'a aucune valeur de paiement.
- **Armure complète en Jade** : casque, plastron, jambières et bottes, avec protection de haut niveau.
- Butin Jade ajouté aux coffres de donjons vanilla ciblés et aux tables de butin de mods dont le chemin contient dungeon, catacomb ou crypt.
- Aucune génération de minerai dans les grottes ou le monde ouvert.
- Menu de guilde médiéval accessible avec **N** (onglets Accueil, Membres, Réserve, Donjon, Journal, Admin) : chaque bouton agit directement sur le serveur, la réserve affiche les vrais objets, les membres hors ligne sont gérables (promouvoir, exclure), et le menu se met à jour toutes les 5 secondes.
- Ramassage automatique de la ressource configurée vers la réserve de ressources de la guilde du joueur. Le système est générique et pourra accueillir d'autres minerais dans de futures extensions.
- Ressource à dépôt automatique configurable par un administrateur avec `/guild resourceitem namespace:objet` (par défaut `citizenguilds:raw_jade`).
- Annuaire des guildes et renommage par le chef.
- Compilation automatisée via GitHub Actions.

## Obtention et fabrication de la Jade
Le minerai brut `citizenguilds:raw_jade` est ajouté avec 12 % de chance aux coffres de donjons ciblés : donjons simples, mines abandonnées, couloirs/croisements de forteresse et cité antique. Les tables de butin de mods sont aussi prises en compte si leur chemin contient dungeon, catacomb ou crypt. La Pierre de Jade est une ressource de fabrication, jamais une monnaie : aucun achat, paiement, prix ou solde monétaire n'utilise la Jade.
Le minerai ne se génère pas dans le monde et ne se mine pas. Fondez-le dans un four ou un haut fourneau pour obtenir `citizenguilds:jade_ingot`. Les lingots servent à fabriquer les quatre pièces d'armure et à réparer l'équipement.

## Commandes de guilde
- `/guild create <nom>`
- `/guild invite <numero_hibou>` : invite le joueur correspondant au numéro CitizenHibou
- `/guild accept` : accepte une invitation reçue
- `/guild refuse` : refuse une invitation reçue
- `/guild list` et `/guild info`
- `/guild rename \"Nouveau nom\"`
- `/guild deposit` et `/guild withdraw <emplacement> [quantite]` pour la réserve de ressources
- `/guild promote <joueur>`, `/guild kick <joueur>`, `/guild leave`, `/guild disband`
- `/guild resourceitem <namespace:objet>` (administrateur) et `/guild jadeinfo`

## Réserve de ressources et extensions
La réserve stocke des objets physiques, pas de monnaie. La Pierre de Jade peut y être déposée comme minerai de fabrication. Les statistiques peuvent compter les quantités de minerais stockées, mais jamais les traiter comme une richesse. Le mécanisme de dépôt automatique est basé sur un identifiant d'objet configurable afin de permettre l'ajout d'autres minerais et matériaux de craft dans de futures extensions.

## Installation
Installez **Hibou Postal (`hiboupostal`)** et **Citizen Guilds** dans le dossier `mods` du serveur et de chaque client. Les deux mods sont nécessaires pour l'intégration des numéros de hibou.

## Donjons de guilde

Le système de donjon utilise une dimension dédiée `citizenguilds:dungeon` (redémarrage complet du serveur après installation). Le terrain du monde principal n'est jamais creusé : seule la porte (petit monument) est posée puis retirée.

### Cycle automatique
1. Toutes les `interval` minutes (30 par défaut, première porte ~2 min après le démarrage), une **porte ancienne** apparaît sur un terrain naturel et plat de la zone autorisée (X ≥ minX, Y ≥ minY, Z ≥ minZ). La recherche se fait autour des joueurs présents dans la zone, sinon sur du terrain déjà généré de la zone (jamais de génération de nouveaux chunks). En cas d'échec, nouvel essai chaque minute.
2. **Le premier membre d'une guilde qui fait clic droit sur la porte ouvre l'expédition** pour sa guilde et entre. Les autres membres de la guilde le rejoignent par la même porte.
3. Trois salles : zombies/araignées, squelettes/husks avec pièges runiques, puis le Gardien des profondeurs. Une seule vie.
4. Victoire ou défaite : retour automatique à la surface 20 secondes plus tard, la porte disparaît et la suivante est programmée.
5. Une porte que personne n'utilise se referme après 2 heures. Une expédition abandonnée (personne dans le donjon pendant 10 min, ou personne entré après 5 min) est fermée automatiquement.

### Commandes administrateur
- `/guild dungeon autostatus` : état du cycle automatique et raison du dernier échec de recherche.
- `/guild dungeon spawnnow` : fait apparaître une porte tout de suite.
- `/guild dungeon autoscan true|false`, `/guild dungeon interval <minutes>`, `/guild dungeon reward <quantite>`.
- `/guild dungeon min <x> <y> <z>` : zone autorisée.
- `/guild dungeon scan` puis `/guild dungeon portal create` : placement manuel ; `/guild dungeon portal remove`, `/guild dungeon reset`.
- `/guild dungeon start [guilde]`, `/guild dungeon end`, `/guild dungeon status`, `/guild top dungeons`.

Tout cela est aussi disponible dans l'onglet **Admin** du menu (touche N).
