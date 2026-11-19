package fr.citizen.guilds;

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
                if(GuildSavedData.get(p.getServer()).create(p,StringArgumentType.getString(c,"nom"))==null) return fail(c.getSource(),"Tu appartiens deja a une guilde.");
                return ok(c.getSource(),"Guilde creee. Tu en es le chef.");
            })))
            .then(Commands.literal("rename").then(Commands.argument("nom", StringArgumentType.string()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                return GuildSavedData.get(p.getServer()).rename(p, StringArgumentType.getString(c, "nom")) ? ok(c.getSource(), "Guilde renommee.") : fail(c.getSource(), "Seul le chef peut renommer (3 a 24 caracteres).");
            })))
            .then(Commands.literal("invite").then(Commands.argument("joueur", net.minecraft.commands.arguments.EntityArgument.player()).executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException(), target=net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"joueur");
                return GuildSavedData.get(p.getServer()).invite(p,target)?ok(c.getSource(),"Invitation envoyee a "+target.getGameProfile().getName()+"."):fail(c.getSource(),"Invitation impossible (droits, guilde ou joueur deja membre).");
            })))
            .then(Commands.literal("accept").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException();
                return GuildSavedData.get(p.getServer()).accept(p)?ok(c.getSource(),"Tu as rejoint la guilde."):fail(c.getSource(),"Aucune invitation valide.");
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
            .then(Commands.literal("top").executes(c -> {
                var guilds=GuildSavedData.get(c.getSource().getServer()).allGuilds();
                c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§6[Classement des guildes] §7(activité de dépôt, pas une valeur monétaire)"),false);
                guilds.stream().sorted(java.util.Comparator.comparingLong((GuildSavedData.Guild g)->g.totalDepositedItems).reversed()).limit(10).forEach(g ->
                    c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§e"+g.name+" §7- "+g.totalDepositedItems+" objet(s) déposé(s)"),false));
                return 1;
            }))
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
                                return ok(c.getSource(), "Limite minimale des donjons definie : X " + x + ", Y " + y + ", Z " + z + ".");
                            })))))
                .then(Commands.literal("scan").requires(source -> source.hasPermission(2)).executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    if (player.serverLevel() != player.getServer().overworld())
                        return fail(c.getSource(), "Le scan doit etre lance dans l'Overworld.");
                    DungeonData data = DungeonData.get(player.getServer());
                    var site = DungeonSiteFinder.find(player.serverLevel(), player, data.minX(), data.minY(), data.minZ());
                    if (site == null)
                        return fail(c.getSource(), "Aucun site naturel adapte trouve a proximite. Eloigne-toi des constructions et relance le scan.");
                    data.setLastSite(site);
                    player.getServer().getPlayerList().broadcastSystemMessage(
                        net.minecraft.network.chat.Component.literal("§6[Donjon] §fSite naturel potentiel detecte : X " + site.getX() + ", Y " + site.getY() + ", Z " + site.getZ() + " (Overworld). Aucun bloc n'a ete modifie."), false);
                    return 1;
                }))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2)).executes(c -> {
                    DungeonData.get(c.getSource().getServer()).clearLastSite();
                    return ok(c.getSource(), "Dernier site de donjon efface. Aucun bloc du monde n'a ete modifie.");
                }))
                .then(Commands.literal("portal").requires(source -> source.hasPermission(2))
                    .then(Commands.literal("create").executes(c -> {
                        DungeonData data = DungeonData.get(c.getSource().getServer());
                        var site = data.lastSite();
                        if (site == null) return fail(c.getSource(), "Aucun site valide. Lance d'abord /guild dungeon scan.");
                        var level = c.getSource().getServer().overworld();
                        if (!level.hasChunkAt(site)) return fail(c.getSource(), "Le chunk du site n'est pas charge. Rends-toi sur place puis relance.");
                        if (!level.getBlockState(site).isAir()) return fail(c.getSource(), "L'emplacement de la porte n'est plus libre.");
                        if (!level.getBlockState(site.above()).isAir()) return fail(c.getSource(), "L'espace au-dessus de la porte est obstrue.");
                        level.setBlock(site, CitizenDungeonBlocks.DUNGEON_GATE.get().defaultBlockState(), 3);
                        return ok(c.getSource(), "Porte temporaire placee en X " + site.getX() + ", Y " + site.getY() + ", Z " + site.getZ() + ".");
                    }))
                    .then(Commands.literal("remove").executes(c -> {
                        DungeonSessionData session = DungeonSessionData.get(c.getSource().getServer());
                        if (session.active()) return fail(c.getSource(), "Termine d'abord la session avec /guild dungeon end.");
                        DungeonData data = DungeonData.get(c.getSource().getServer());
                        var site = data.lastSite();
                        if (site == null) return fail(c.getSource(), "Aucun site enregistre.");
                        var level = c.getSource().getServer().overworld();
                        if (!level.hasChunkAt(site) || !level.getBlockState(site).is(CitizenDungeonBlocks.DUNGEON_GATE.get()))
                            return fail(c.getSource(), "Aucune porte Citizen n'est presente sur le site.");
                        level.removeBlock(site, false);
                        return ok(c.getSource(), "Porte temporaire retiree. Le terrain naturel autour n'a pas ete touche.");
                    })))
                .then(Commands.literal("start").requires(source -> source.hasPermission(2))
                    .then(Commands.argument("guilde", StringArgumentType.greedyString()).executes(c -> {
                        String name = StringArgumentType.getString(c, "guilde");
                        var guild = GuildSavedData.get(c.getSource().getServer()).allGuilds().stream()
                            .filter(g -> g.name.equalsIgnoreCase(name)).findFirst().orElse(null);
                        if (guild == null) return fail(c.getSource(), "Guilde introuvable.");
                        var site = DungeonData.get(c.getSource().getServer()).lastSite();
                        if (site == null) return fail(c.getSource(), "Aucun site valide. Lance d'abord un scan.");
                        var level = c.getSource().getServer().overworld();
                        if (!level.hasChunkAt(site) || !level.getBlockState(site).is(CitizenDungeonBlocks.DUNGEON_GATE.get()))
                            return fail(c.getSource(), "La porte temporaire n'est pas placee ou son chunk n'est pas charge.");
                        ServerLevel dungeon = DungeonArena.prepare(c.getSource().getServer());
                        if (dungeon == null) return fail(c.getSource(), "Le monde d'instance CITIZEN n'est pas disponible. Verifie le datapack puis redemarre le serveur.");
                        DungeonSessionData session = DungeonSessionData.get(c.getSource().getServer());
                        if (!session.start(guild.id, guild.name, site)) return fail(c.getSource(), "Une session est deja active.");
                        c.getSource().getServer().getPlayerList().broadcastSystemMessage(
                            net.minecraft.network.chat.Component.literal("§6[Donjon] §fLa session de " + guild.name + " est ouverte. Les membres peuvent interagir avec la porte en X " + site.getX() + ", Y " + site.getY() + ", Z " + site.getZ() + "."), false);
                        return 1;
                    })))
                .then(Commands.literal("end").requires(source -> source.hasPermission(2)).executes(c -> {
                    DungeonSessionData session = DungeonSessionData.get(c.getSource().getServer());
                    if (!session.active()) return fail(c.getSource(), "Aucune session active.");
                    String guildName = session.guildName();
                    var site = session.site();
                    var dungeonLevel = c.getSource().getServer().getLevel(CitizenDimensions.DUNGEON);
                    if (dungeonLevel != null) session.removeActiveMobs(dungeonLevel);
                    var overworld = c.getSource().getServer().overworld();
                    for (String uuid : session.participantIds()) {
                        ServerPlayer participant = c.getSource().getServer().getPlayerList().getPlayer(java.util.UUID.fromString(uuid));
                        if (participant != null && participant.serverLevel() != overworld) {
                            BlockPos returnPos = site == null ? overworld.getSharedSpawnPos() : site.offset(2, 0, 2);
                            participant.teleportTo(overworld, returnPos.getX() + 0.5D, returnPos.getY(), returnPos.getZ() + 0.5D,
                                participant.getYRot(), participant.getXRot());
                        }
                    }
                    session.end();
                    var level = overworld;
                    if (site != null && level.hasChunkAt(site) && level.getBlockState(site).is(CitizenDungeonBlocks.DUNGEON_GATE.get()))
                        level.removeBlock(site, false);
                    return ok(c.getSource(), "Session de " + guildName + " fermee. La porte a ete retiree si son emplacement etait intact.");
                }))
                .then(Commands.literal("status").executes(c -> {
                    DungeonSessionData session = DungeonSessionData.get(c.getSource().getServer());
                    if (!session.active()) return ok(c.getSource(), "Aucune session de donjon active.");
                    String state = session.finished() ? (session.victory() ? "VICTOIRE" : "ECHEC") : "EN COURS";
                    return ok(c.getSource(), "Session " + session.guildName() + " | Etat : " + state + " | Vague : " + session.wave() + "/3 | Participants : " + session.participantCount() + " | Encore en vie : " + session.aliveCount() + " | Porte : " + session.site().getX() + " " + session.site().getY() + " " + session.site().getZ() + ".");
                }))
                .then(Commands.literal("site").executes(c -> {
                    DungeonData data = DungeonData.get(c.getSource().getServer());
                    var site = data.lastSite();
                    if (site == null) return fail(c.getSource(), "Aucun site de donjon n'a encore ete valide.");
                    return ok(c.getSource(), "Dernier site detecte : X " + site.getX() + ", Y " + site.getY() + ", Z " + site.getZ() + " (Overworld).");
                }))
            )
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
