package fr.citizen.guilds;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CitizenGuilds.MOD_ID)
public final class GuildCommands {
    private GuildCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guild")
            .then(Commands.literal("list").executes(c -> {
                var guilds = GuildSavedData.get(c.getSource().getServer()).allGuilds();
                c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§6[Guildes] §fGuildes existantes : " + guilds.size()), false);
                guilds.forEach(g -> c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§e" + g.name + " §7(" + g.members.size() + " membre(s))"), false));
                return 1;
            }))
            .then(Commands.literal("create").then(Commands.argument("nom", StringArgumentType.string()).executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException();
                if(GuildSavedData.get(p.getServer()).create(p,StringArgumentType.getString(c,"nom"))==null) return fail(c.getSource(),"Creation impossible : nom deja utilise, nom invalide (3 a 24 caracteres) ou tu appartiens deja a une guilde.");
                return ok(c.getSource(),"Guilde creee. Tu en es le chef.");
            })))
            .then(Commands.literal("rename").then(Commands.argument("nom", StringArgumentType.string()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                return GuildSavedData.get(p.getServer()).rename(p, StringArgumentType.getString(c, "nom")) ? ok(c.getSource(), "Guilde renommee.") : fail(c.getSource(), "Seul le chef peut renommer (3 a 24 caracteres).");
            })))
            .then(Commands.literal("invite").then(Commands.argument("numero_hibou", IntegerArgumentType.integer(1)).executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException();
                int owlNumber=IntegerArgumentType.getInteger(c,"numero_hibou");
                return GuildSavedData.get(p.getServer()).inviteByHibou(p,owlNumber)
                    ?1:fail(c.getSource(),"Invitation impossible : numéro de hibou inconnu, joueur déjà membre/invité ou droits insuffisants.");
            })))
            .then(Commands.literal("accept").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException();
                return GuildSavedData.get(p.getServer()).accept(p)?ok(c.getSource(),"Tu as rejoint la guilde."):fail(c.getSource(),"Aucune invitation valide.");
            }))
            .then(Commands.literal("refuse").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException();
                return GuildSavedData.get(p.getServer()).refuse(p)?ok(c.getSource(),"Invitation refusée."):fail(c.getSource(),"Aucune invitation à refuser.");
            }))
            .then(Commands.literal("leave").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException();
                return GuildSavedData.get(p.getServer()).leave(p)?ok(c.getSource(),"Tu as quitte la guilde."):fail(c.getSource(),"Tu es chef ou tu n'appartiens a aucune guilde.");
            }))
            .then(Commands.literal("kick").then(Commands.argument("joueur",net.minecraft.commands.arguments.EntityArgument.player()).executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException(), target=net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"joueur");
                return GuildSavedData.get(p.getServer()).kick(p,target)?ok(c.getSource(),"Joueur exclu."):fail(c.getSource(),"Exclusion refusee.");
            })))
            .then(Commands.literal("promote").then(Commands.argument("joueur",net.minecraft.commands.arguments.EntityArgument.player()).executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException(), target=net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"joueur");
                return GuildSavedData.get(p.getServer()).promote(p,target)?ok(c.getSource(),"Grade modifie."):fail(c.getSource(),"Promotion refusee.");
            })))
            .then(Commands.literal("deposit").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException(); ItemStack hand=p.getMainHandItem();
                int count=hand.getCount();
                return GuildSavedData.get(p.getServer()).deposit(p,hand)?ok(c.getSource(),"Depot de "+count+" objet(s) effectue."):fail(c.getSource(),"Depot impossible.");
            }))
            .then(Commands.literal("withdraw").then(Commands.argument("slot",IntegerArgumentType.integer(1)).executes(c -> withdraw(c.getSource(),IntegerArgumentType.getInteger(c,"slot"),64))
                .then(Commands.argument("quantite",IntegerArgumentType.integer(1,64)).executes(c -> withdraw(c.getSource(),IntegerArgumentType.getInteger(c,"slot"),IntegerArgumentType.getInteger(c,"quantite"))))))
            .then(Commands.literal("info").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException(); GuildSavedData.Guild g=GuildSavedData.get(p.getServer()).guildOf(p);
                if(g==null)return fail(c.getSource(),"Tu n'appartiens a aucune guilde.");
                c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§6"+g.name+" §7| Membres: "+g.members.size()),false);
                g.members.forEach((uuid,name)->c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§e"+name+" §7- "+g.roles.getOrDefault(uuid,"MEMBER")),false));
                return 1;
            }))
            .then(Commands.literal("stats").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException();
                GuildSavedData.Guild g=GuildSavedData.get(p.getServer()).guildOf(p);
                if(g==null)return fail(c.getSource(),"Tu n'appartiens a aucune guilde.");
                c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(
                    "§6[Statistiques] §f"+g.name+" | Membres: "+g.members.size()+" | Objets deposes: "+g.totalDepositedItems+" | Emplacements: "+g.bank.size()+"/54 | Donjons gagnes: "+g.dungeonWins+" | Eliminations: "+g.dungeonDeaths),false);
                return 1;
            }))
            .then(Commands.literal("top")
                .executes(c -> {
                    var guilds=GuildSavedData.get(c.getSource().getServer()).allGuilds();
                    c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§6[Classement des guildes] §7(activité de dépôt, pas une valeur monétaire)"),false);
                    guilds.stream().sorted(java.util.Comparator.comparingLong((GuildSavedData.Guild g)->g.totalDepositedItems).reversed()).limit(10).forEach(g ->
                        c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§e"+g.name+" §7- "+g.totalDepositedItems+" objet(s) déposé(s)"),false));
                    return 1;
                })
                .then(Commands.literal("dungeons").executes(c -> {
                    var guilds=GuildSavedData.get(c.getSource().getServer()).allGuilds();
                    c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§6[Classement des donjons] §7(victoires de guilde)"),false);
                    guilds.stream().sorted(java.util.Comparator.comparingInt((GuildSavedData.Guild g)->g.dungeonWins).reversed()).limit(10).forEach(g ->
                        c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§e"+g.name+" §7- "+g.dungeonWins+" victoire(s) | "+g.dungeonDeaths+" élimination(s)"),false));
                    return 1;
                })))
            .then(Commands.literal("admin").requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(c -> {
                    var guilds=GuildSavedData.get(c.getSource().getServer()).allGuilds();
                    c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§c[Administration] §f"+guilds.size()+" guilde(s)"),false);
                    guilds.forEach(g->c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§e"+g.name+" §7| "+g.members.size()+" membre(s) | "+g.id),false));
                    return 1;
                }))
                .then(Commands.literal("disband").then(Commands.argument("nom",StringArgumentType.greedyString()).executes(c -> {
                    String name=StringArgumentType.getString(c,"nom");
                    return GuildSavedData.get(c.getSource().getServer()).adminDisband(name)
                        ?ok(c.getSource(),"Guilde "+name+" supprimee par administration.")
                        :fail(c.getSource(),"Guilde introuvable.");
                }))))
            .then(Commands.literal("dungeon")
                .then(Commands.literal("min").requires(source -> source.hasPermission(2))
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                            .then(Commands.argument("z", IntegerArgumentType.integer()).executes(c -> {
                                int x = IntegerArgumentType.getInteger(c, "x");
                                int y = IntegerArgumentType.getInteger(c, "y");
                                int z = IntegerArgumentType.getInteger(c, "z");
                                DungeonData.get(c.getSource().getServer()).setMinimum(x, y, z);
                                return ok(c.getSource(), "Zone des donjons : X >= " + x + ", Y >= " + y + ", Z >= " + z + ".");
                            })))))
                .then(Commands.literal("autoscan").requires(source -> source.hasPermission(2))
                    .then(Commands.argument("active", BoolArgumentType.bool()).executes(c -> {
                        boolean enabled = BoolArgumentType.getBool(c, "active");
                        DungeonData.get(c.getSource().getServer()).setAutoSpawnEnabled(enabled);
                        if (enabled) DungeonData.get(c.getSource().getServer()).setNextSpawnGameTime(0L);
                        return ok(c.getSource(), "Apparition automatique des portes : " + (enabled ? "activee (premiere porte dans ~2 min)" : "desactivee") + ".");
                    })))
                .then(Commands.literal("interval").requires(source -> source.hasPermission(2))
                    .then(Commands.argument("minutes", IntegerArgumentType.integer(5, 1440)).executes(c -> {
                        int minutes = IntegerArgumentType.getInteger(c, "minutes");
                        DungeonData.get(c.getSource().getServer()).setSpawnIntervalMinutes(minutes);
                        return ok(c.getSource(), "Intervalle entre les apparitions : " + minutes + " minute(s).");
                    })))
                .then(Commands.literal("reward").requires(source -> source.hasPermission(2))
                    .then(Commands.argument("quantite", IntegerArgumentType.integer(0, 64)).executes(c -> {
                        int amount = IntegerArgumentType.getInteger(c, "quantite");
                        DungeonData.get(c.getSource().getServer()).setRewardJade(amount);
                        return ok(c.getSource(), "Recompense de donjon definie a " + amount + " minerai(s) de Pierre de Jade par survivant.");
                    })))
                .then(Commands.literal("spawnnow").requires(source -> source.hasPermission(2)).executes(c ->
                    result(c.getSource(), DungeonService.spawnNow(c.getSource().getServer()))))
                .then(Commands.literal("autostatus").executes(c ->
                    ok(c.getSource(), DungeonService.statusLine(c.getSource().getServer()))))
                .then(Commands.literal("scan").requires(source -> source.hasPermission(2)).executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    if (player.serverLevel() != player.getServer().overworld())
                        return fail(c.getSource(), "Le scan doit etre lance dans l'Overworld.");
                    if (DungeonService.isPortalPresent(player.getServer()))
                        return fail(c.getSource(), "Une porte existe deja. Retire-la ou termine l'expedition avant un nouveau scan.");
                    DungeonData data = DungeonData.get(player.getServer());
                    var search = DungeonSiteFinder.findNear(player.serverLevel(), player, data);
                    if (search.site() == null) return fail(c.getSource(), search.note() + " Eloigne-toi des constructions et relance le scan.");
                    data.clearLastSite();
                    data.setLastSite(search.site());
                    var site = search.site();
                    return ok(c.getSource(), "Site naturel detecte : X " + site.getX() + ", Y " + site.getY() + ", Z " + site.getZ()
                        + ". Aucun bloc modifie. Utilise /guild dungeon portal create pour y construire la porte.");
                }))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2)).executes(c -> {
                    if (DungeonSessionData.get(c.getSource().getServer()).active()) return fail(c.getSource(), "Ferme d'abord l'expedition active.");
                    DungeonService.removePortal(c.getSource().getServer());
                    return ok(c.getSource(), "Dernier site et porte eventuelle effaces. Prochaine porte automatique programmee.");
                }))
                .then(Commands.literal("portal").requires(source -> source.hasPermission(2))
                    .then(Commands.literal("create").executes(c -> {
                        var server = c.getSource().getServer();
                        if (DungeonService.isPortalPresent(server)) return fail(c.getSource(), "Une porte est deja ouverte.");
                        DungeonData data = DungeonData.get(server);
                        var site = data.lastSite();
                        if (site == null) return fail(c.getSource(), "Aucun site valide. Lance d'abord /guild dungeon scan (ou /guild dungeon spawnnow).");
                        data.clearLastSite();
                        return result(c.getSource(), RiftDungeonController.begin(server, site));
                    }))
                    .then(Commands.literal("remove").executes(c -> {
                        var server = c.getSource().getServer();
                        if (DungeonSessionData.get(server).active()) return fail(c.getSource(), "Termine d'abord l'expedition avec /guild dungeon end.");
                        if (!DungeonService.isPortalPresent(server)) return fail(c.getSource(), "Aucune porte n'est ouverte.");
                        DungeonService.removePortal(server);
                        return ok(c.getSource(), "Porte retiree. Prochaine porte automatique programmee.");
                    })))
                .then(Commands.literal("start")
                    .executes(c -> {
                        ServerPlayer requester = c.getSource().getPlayerOrException();
                        GuildSavedData.Guild guild = GuildSavedData.get(c.getSource().getServer()).guildOf(requester);
                        if (guild == null || !guild.canManage(requester.getUUID().toString()))
                            return fail(c.getSource(), "Seuls le chef et les officiers peuvent lancer le donjon de leur guilde.");
                        return result(c.getSource(), DungeonService.startSession(c.getSource().getServer(), guild));
                    })
                    .then(Commands.argument("guilde", StringArgumentType.greedyString()).executes(c -> {
                        if (!c.getSource().hasPermission(2)) return fail(c.getSource(), "Seul un administrateur peut lancer le donjon pour une autre guilde.");
                        String name = StringArgumentType.getString(c, "guilde");
                        GuildSavedData.Guild guild = GuildSavedData.get(c.getSource().getServer()).allGuilds().stream()
                            .filter(g -> g.name.equalsIgnoreCase(name)).findFirst().orElse(null);
                        if (guild == null) return fail(c.getSource(), "Guilde introuvable.");
                        return result(c.getSource(), DungeonService.startSession(c.getSource().getServer(), guild));
                    })))
                .then(Commands.literal("end").requires(source -> source.hasPermission(2)).executes(c ->
                    result(c.getSource(), DungeonService.endSession(c.getSource().getServer(),
                        "§7[Donjon] L'expedition a ete fermee par un administrateur."))))
                .then(Commands.literal("status").executes(c -> {
                    DungeonSessionData session = DungeonSessionData.get(c.getSource().getServer());
                    if (!session.active()) return ok(c.getSource(), "Aucune expedition en cours. " + DungeonService.statusLine(c.getSource().getServer()));
                    String state = session.finished() ? (session.victory() ? "VICTOIRE" : "ECHEC") : "EN COURS";
                    return ok(c.getSource(), "Expedition " + session.guildName() + " | Etat : " + state + " | Vague : " + session.wave() + "/3 | Participants : " + session.participantCount() + " | En vie : " + session.aliveCount() + ".");
                }))
                .then(Commands.literal("site").executes(c -> {
                    var site = DungeonData.get(c.getSource().getServer()).lastSite();
                    if (site == null) return fail(c.getSource(), "Aucun site de donjon enregistre.");
                    return ok(c.getSource(), "Site enregistre : X " + site.getX() + ", Y " + site.getY() + ", Z " + site.getZ() + " (Overworld).");
                }))
            )
            .then(Commands.literal("rift").requires(source -> source.hasPermission(2))
                .then(Commands.literal("set").executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    return result(c.getSource(), RiftDungeonController.setHubPosition(
                        c.getSource().getServer(), player.blockPosition()));
                }))
                .then(Commands.literal("move").executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    return result(c.getSource(), RiftDungeonController.setHubPosition(
                        c.getSource().getServer(), player.blockPosition()));
                }))
                .then(Commands.literal("remove").executes(c ->
                    result(c.getSource(), RiftDungeonController.removeHubRift(c.getSource().getServer()))))
                .then(Commands.literal("status").executes(c ->
                    ok(c.getSource(), RiftDungeonController.status(c.getSource().getServer()))))
                .then(Commands.literal("open").executes(c ->
                    result(c.getSource(), RiftDungeonController.openFracture(c.getSource().getServer()))))
                .then(Commands.literal("close").executes(c ->
                    result(c.getSource(), RiftDungeonController.closeFracture(c.getSource().getServer()))))
                .then(Commands.literal("deformation").then(Commands.argument("pourcentage", IntegerArgumentType.integer(0, 100)).executes(c -> {
                    int percent = IntegerArgumentType.getInteger(c, "pourcentage");
                    DungeonData.get(c.getSource().getServer()).setFractureDeformationPercent(percent);
                    return ok(c.getSource(), "Deformation maximale de la fracture reglee a " + percent + "%. La fracture evoluera progressivement jusqu'a cette intensite.");
                })))
                .then(Commands.literal("deformationstatus").executes(c -> {
                    DungeonData data = DungeonData.get(c.getSource().getServer());
                    return ok(c.getSource(), "Deformation cible : " + data.fractureDeformationPercent() + "%.");
                })))
            .then(Commands.literal("journal").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException();
                GuildSavedData.Guild g=GuildSavedData.get(p.getServer()).guildOf(p);
                if(g==null)return fail(c.getSource(),"Tu n'appartiens a aucune guilde.");
                c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§6[Guildes] §fJournal de "+g.name),false);
                if(g.journal.isEmpty()) return ok(c.getSource(),"Aucune activite enregistree.");
                g.journal.stream().limit(20).forEach(line -> c.getSource().sendSuccess(
                    () -> net.minecraft.network.chat.Component.literal("§7• "+line),false));
                return 1;
            }))
            .then(Commands.literal("bank").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException();
                GuildSavedData.Guild g=GuildSavedData.get(p.getServer()).guildOf(p);
                if(g==null)return fail(c.getSource(),"Tu n'appartiens a aucune guilde.");
                c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§6[Guildes] §fRéserve de ressources de "+g.name+" ("+g.bank.size()+" emplacements)"),false);
                for(int i=0;i<g.bank.size();i++){
                    ItemStack stack=g.bank.get(i);
                    final int slot=i+1;
                    c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§e"+slot+". §f"+stack.getCount()+"x "+stack.getHoverName().getString()),false);
                }
                return 1;
            }))
            .then(Commands.literal("resourceitem").requires(source -> source.hasPermission(2))
                .then(Commands.argument("item", StringArgumentType.word()).executes(c -> {
                    String id = StringArgumentType.getString(c, "item");
                    net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.tryParse(id);
                    if (key == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key))
                        return fail(c.getSource(), "Identifiant d'objet invalide ou inconnu.");
                    GuildSavedData.get(c.getSource().getServer()).setAutoDepositItemId(key.toString());
                    return ok(c.getSource(), "Ressource configuree : " + key + ". Les prochains ramassages alimenteront la reserve de ressources de guilde.");
                })))
            .then(Commands.literal("jadeinfo").executes(c -> {
                c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(
                    "§6[Guildes] §fLa Pierre de Jade est un minerai de fabrication : raw_jade -> four/haut-fourneau -> jade_ingot -> armure complète. Aucune valeur monétaire."),false);
                return 1;
            }))
            .then(Commands.literal("disband").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException(); GuildSavedData data=GuildSavedData.get(p.getServer());
                GuildSavedData.Guild g=data.guildOf(p);
                if(g==null || !g.leader.equals(p.getUUID().toString()))return fail(c.getSource(),"Seul le chef peut dissoudre la guilde.");
                data.disband(p); return ok(c.getSource(),"Guilde dissoute.");
            }))
        );
    }
    private static int result(net.minecraft.commands.CommandSourceStack source, DungeonService.Result result) {
        return result.ok() ? ok(source, result.message()) : fail(source, result.message());
    }

    private static int withdraw(net.minecraft.commands.CommandSourceStack source,int slot,int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p=source.getPlayerOrException();
        ItemStack stack=GuildSavedData.get(p.getServer()).withdraw(p,slot-1,count);
        if(stack.isEmpty())return fail(source,"Retrait impossible (grade ou emplacement invalide).");
        if(!p.getInventory().add(stack))p.drop(stack,false);
        return ok(source,"Retrait effectue.");
    }
    private static int ok(net.minecraft.commands.CommandSourceStack s,String msg){s.sendSuccess(()->net.minecraft.network.chat.Component.literal("§a[Guildes] §f"+msg),false);return 1;}
    private static int fail(net.minecraft.commands.CommandSourceStack s,String msg){s.sendFailure(net.minecraft.network.chat.Component.literal("§c[Guildes] §f"+msg));return 0;}
}
