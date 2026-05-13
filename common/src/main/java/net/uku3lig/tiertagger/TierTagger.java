package net.uku3lig.tiertagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.llamalad7.mixinextras.lib.semver.Version;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.uku3lig.tiertagger.config.TierTaggerConfig;
import net.uku3lig.tiertagger.model.GameMode;
import net.uku3lig.tiertagger.model.PlayerInfo;
import net.uku3lig.ukulib.config.ConfigManager;
import net.uku3lig.ukulib.utils.Ukutils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.Optional;
import java.util.UUID;

public class TierTagger {
    public static final String MOD_ID = "tiertagger";

    public static final Gson GSON = new GsonBuilder().create();

    @Getter
    private static final ConfigManager<TierTaggerConfig> manager = ConfigManager.createDefault(TierTaggerConfig.class, MOD_ID);
    @Getter
    private static final Logger logger = LoggerFactory.getLogger(TierTagger.class);
    @Getter
    private static final HttpClient client = HttpClient.newHttpClient();

    @Getter
    private static Version currentVersion;

    public static void onInitialize(Version currentVer) {
        currentVersion = currentVer;

        TierCache.init();

        Ukutils.registerKeybinding(new KeyMapping("tiertagger.keybind.gamemode", GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tiertagger", "key"))),
                mc -> {
                    GameMode next = TierCache.findNextMode(manager.getConfig().getGameMode());
                    manager.getConfig().setGameMode(next.id());

                    if (mc.player != null) {
                        Component message = Component.literal("Displayed gamemode: ").append(next.asStyled(false));
                        mc.player.sendOverlayMessage(message);
                    }
                });
    }

    public static Component appendTier(UUID uuid, Component text) {
        return appendTier(null, uuid, text);
    }

    public static Component appendTier(String name, UUID uuid, Component text) {
        MutableComponent following = getPlayerTier(name, uuid)
                .map(entry -> {
                    Component tierText = getRankingText(entry.ranking(), false);

                    if (manager.getConfig().isShowIcons() && entry.mode() != null && entry.mode().icon().isPresent()) {
                        return Component.literal(entry.mode().icon().get().toString()).append(tierText);
                    } else {
                        return tierText.copy();
                    }
                })
                .orElse(null);

        if (following != null) {
            following.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));
            return following.append(text);
        }

        return text;
    }

    public static Optional<PlayerInfo.NamedRanking> getPlayerTier(UUID uuid) {
        return getPlayerTier(null, uuid);
    }

    public static Optional<PlayerInfo.NamedRanking> getPlayerTier(String name, UUID uuid) {
        GameMode mode = manager.getConfig().getGameMode();

        return TierCache.getPlayerRankings(name, uuid)
                .map(rankings -> {
                    PlayerInfo.Ranking ranking = rankings.get(mode.id());
                    Optional<PlayerInfo.NamedRanking> highest = PlayerInfo.getHighestRanking(rankings);
                    TierTaggerConfig.HighestMode highestMode = manager.getConfig().getHighestMode();

                    if (ranking == null) {
                        if (highestMode != TierTaggerConfig.HighestMode.NEVER && highest.isPresent()) {
                            return highest.get();
                        } else {
                            return null;
                        }
                    } else {
                        if (highestMode == TierTaggerConfig.HighestMode.ALWAYS && highest.isPresent()) {
                            return highest.get();
                        } else {
                            return ranking.asNamed(mode);
                        }
                    }
                });
    }

    private static MutableComponent getTierText(int tier, int pos, boolean retired) {
        StringBuilder text = new StringBuilder();
        if (retired) text.append("R");
        text.append(pos == 0 ? "H" : "L").append("T").append(tier);

        int color = TierTagger.getTierColor(text.toString());
        return Component.literal(text.toString()).withStyle(s -> s.withColor(color));
    }

    public static Component getRankingText(PlayerInfo.Ranking ranking, boolean showPeak) {
        if (ranking.retired() && ranking.peakTier() != null && ranking.peakPos() != null) {
            return getTierText(ranking.peakTier(), ranking.peakPos(), true);
        } else {
            MutableComponent tierText = getTierText(ranking.tier(), ranking.pos(), false);

            if (showPeak && ranking.comparablePeak() < ranking.comparableTier()) {
                // warning caused by potential NPE by unboxing of peak{Tier,Pos} which CANNOT happen, see impl of comparablePeak
                // noinspection DataFlowIssue
                tierText.append(Component.literal(" (peak: ").withStyle(s -> s.withColor(ChatFormatting.GRAY)))
                        .append(getTierText(ranking.peakTier(), ranking.peakPos(), false))
                        .append(Component.literal(")").withStyle(s -> s.withColor(ChatFormatting.GRAY)));
            }

            return tierText;
        }
    }

    public static int getTierColor(String tier) {
        if (tier.startsWith("R")) {
            return manager.getConfig().getRetiredColor();
        } else {
            return manager.getConfig().getTierColors().getOrDefault(tier, 0xD3D3D3);
        }
    }

}
