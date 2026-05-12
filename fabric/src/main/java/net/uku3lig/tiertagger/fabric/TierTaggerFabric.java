package net.uku3lig.tiertagger.fabric;

import com.llamalad7.mixinextras.lib.semver.Version;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.uku3lig.tiertagger.TierCache;
import net.uku3lig.tiertagger.TierTagger;
import net.uku3lig.tiertagger.model.GameMode;
import net.uku3lig.tiertagger.model.PlayerInfo;
import net.uku3lig.ukulib.fabric.PlayerArgumentType;

import java.util.Map;
import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class TierTaggerFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // noinspection OptionalGetWithoutIsPresent: the mod is you know, loaded so yeah
        String versionString = FabricLoader.getInstance().getModContainer(TierTagger.MOD_ID).get().getMetadata().getVersion().getFriendlyString();

        TierTagger.onInitialize(Version.parse(versionString));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> dispatcher.register(
                literal(TierTagger.MOD_ID)
                        .then(argument("player", PlayerArgumentType.player())
                                .executes(TierTaggerFabric::displayTierInfo))));
    }

    private static int displayTierInfo(CommandContext<FabricClientCommandSource> ctx) {
        PlayerArgumentType.PlayerSelector selector = ctx.getArgument("player", PlayerArgumentType.PlayerSelector.class);

        Optional<Map<String, PlayerInfo.Ranking>> rankings = ctx.getSource().getLevel().players().stream()
                .filter(p -> p.getScoreboardName().equalsIgnoreCase(selector.name()) || p.getStringUUID().equalsIgnoreCase(selector.name()))
                .findFirst()
                .map(Entity::getUUID)
                .flatMap(TierCache::getPlayerRankings);

        if (rankings.isPresent()) {
            ctx.getSource().sendFeedback(printPlayerInfo(selector.name(), rankings.get()));
        } else {
            ctx.getSource().sendFeedback(Component.literal("[TierTagger] Searching..."));
            TierCache.searchPlayer(selector.name())
                    .thenAccept(p -> Minecraft.getInstance().execute(() -> ctx.getSource().sendFeedback(printPlayerInfo(selector.name(), p.rankings()))))
                    .exceptionally(_ -> {
                        ctx.getSource().sendError(Component.literal("Could not find player " + selector.name()));
                        return null;
                    });
        }

        return 0;
    }

    private static Component printPlayerInfo(String name, Map<String, PlayerInfo.Ranking> rankings) {
        if (rankings.isEmpty()) {
            return Component.literal(name + " does not have any tiers.");
        } else {
            MutableComponent text = Component.empty().append("=== Rankings for " + name + " ===");

            rankings.forEach((m, r) -> {
                if (m == null) return;
                GameMode mode = TierCache.findModeOrUgly(m);
                Component tierText = TierTagger.getRankingText(r, true);
                text.append(Component.literal("\n").append(mode.asStyled(true)).append(": ").append(tierText));
            });

            return text;
        }
    }
}
