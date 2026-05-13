package net.uku3lig.tiertagger;

import net.uku3lig.tiertagger.model.GameMode;
import net.uku3lig.tiertagger.model.PlayerInfo;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class TierCache {
    private static final List<GameMode> GAMEMODES = new ArrayList<>();
    private static final Map<UUID, Optional<Map<String, PlayerInfo.Ranking>>> TIERS = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Map<String, PlayerInfo.Ranking>>> TIERS_BY_NAME = new ConcurrentHashMap<>();

    public static void init() {
        try {
            GAMEMODES.clear();
            GAMEMODES.addAll(GameMode.fetchGamemodes(TierTagger.getClient()).get());
            TierTagger.getLogger().info("Found {} CoveTiers gamemodes: {}", GAMEMODES.size(), GAMEMODES.stream().map(GameMode::id).toList());
        } catch (ExecutionException e) {
            TierTagger.getLogger().error("Failed to load gamemodes!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static List<GameMode> getGamemodes() {
        if (GAMEMODES.isEmpty()) {
            return Collections.singletonList(GameMode.NONE);
        } else {
            return GAMEMODES;
        }
    }

    public static Optional<Map<String, PlayerInfo.Ranking>> getPlayerRankings(UUID uuid) {
        return TIERS.computeIfAbsent(uuid, _ -> {
            PlayerInfo.getRankings(TierTagger.getClient(), uuid).thenAccept(info -> TIERS.put(uuid, Optional.ofNullable(info)));
            return Optional.empty();
        });
    }

    public static Optional<Map<String, PlayerInfo.Ranking>> getPlayerRankings(String name, UUID fallbackUuid) {
        String cacheKey = normalizeName(name);
        if (cacheKey.isEmpty()) {
            return getPlayerRankings(fallbackUuid);
        }

        return TIERS_BY_NAME.computeIfAbsent(cacheKey, _ -> {
            PlayerInfo.search(TierTagger.getClient(), name).thenAccept(info -> {
                if (info == null || info.uuid() == null) {
                    return;
                }

                cacheProfile(info, fallbackUuid);
            }).exceptionally(t -> {
                TierTagger.getLogger().warn("Error getting player rankings by name ({})", name, t);
                return null;
            });

            return Optional.empty();
        });
    }

    public static CompletableFuture<PlayerInfo> searchPlayer(String query) {
        return PlayerInfo.search(TierTagger.getClient(), query).thenApply(p -> {
            if (p == null || p.uuid() == null) {
                throw new NoSuchElementException("No CoveTiers profile found for " + query);
            }

            cacheProfile(p, null);
            return p;
        });
    }

    public static void clearCache() {
        TIERS.clear();
        TIERS_BY_NAME.clear();
    }

    public static GameMode findNextMode(GameMode current) {
        if (GAMEMODES.isEmpty()) {
            return GameMode.NONE;
        } else {
            return GAMEMODES.get((GAMEMODES.indexOf(current) + 1) % GAMEMODES.size());
        }
    }

    public static Optional<GameMode> findMode(String id) {
        String canonicalId = GameMode.canonicalId(id);
        return GAMEMODES.stream().filter(m -> m.id().equals(canonicalId)).findFirst();
    }

    public static GameMode findModeOrUgly(String id) {
        return findMode(id).orElseGet(() -> new GameMode(id, id));
    }

    private static UUID parseUUID(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (Exception e) {
            long mostSignificant = Long.parseUnsignedLong(uuid.substring(0, 16), 16);
            long leastSignificant = Long.parseUnsignedLong(uuid.substring(16), 16);
            return new UUID(mostSignificant, leastSignificant);
        }
    }

    private static void cacheProfile(PlayerInfo info, UUID fallbackUuid) {
        Optional<Map<String, PlayerInfo.Ranking>> rankings = Optional.of(info.rankings());
        TIERS_BY_NAME.put(normalizeName(info.name()), rankings);

        try {
            TIERS.put(parseUUID(info.uuid()), rankings);
            if (fallbackUuid != null) {
                TIERS.put(fallbackUuid, rankings);
            }
        } catch (Exception e) {
            if (fallbackUuid != null) {
                TIERS.put(fallbackUuid, rankings);
            } else {
                TierTagger.getLogger().warn("Could not parse CoveTiers UUID for {}", info.name(), e);
            }
        }
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private TierCache() {
    }
}
