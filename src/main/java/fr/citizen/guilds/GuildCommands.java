package fr.citizen.guilds;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
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
