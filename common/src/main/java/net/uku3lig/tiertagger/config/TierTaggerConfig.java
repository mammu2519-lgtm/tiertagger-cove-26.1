package net.uku3lig.tiertagger.config;

import com.google.gson.internal.LinkedTreeMap;
import net.uku3lig.tiertagger.TierCache;
import net.uku3lig.tiertagger.model.GameMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.uku3lig.ukulib.config.option.StringTranslatable;

import java.io.Serializable;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TierTaggerConfig implements Serializable {
    public static final String COVE_API_URL = "https://cove-tiertagger.vercel.app/api";

    private boolean enabled = true;
    private String gameMode = "vanilla";
    private boolean showRetired = true;
    private HighestMode highestMode = HighestMode.NOT_FOUND;
    private boolean showIcons = true;
    private boolean playerList = true;
    private int retiredColor = 0xa2d6ff;
    // note: this is a GSON internal class. this *might* break in the future
    private LinkedTreeMap<String, Integer> tierColors = defaultColors();

    // === internal stuff ===

    /**
     * <p>Kept only so old configs deserialize cleanly. TierTagger always uses CoveTiers now.</p>
     * <p>previous name(s): {@code baseUrl}</p>
     */
    private String apiUrl = COVE_API_URL;

    public String getApiUrl() {
        return COVE_API_URL;
    }

    public void setApiUrl(String ignored) {
        this.apiUrl = COVE_API_URL;
    }

    public GameMode getGameMode() {
        this.gameMode = GameMode.canonicalId(this.gameMode);
        Optional<GameMode> opt = TierCache.findMode(this.gameMode);
        if (opt.isPresent()) {
            return opt.get();
        } else {
            GameMode first = TierCache.getGamemodes().getFirst();
            if (!first.isNone()) this.gameMode = first.id();
            return first;
        }
    }

    private static LinkedTreeMap<String, Integer> defaultColors() {
        LinkedTreeMap<String, Integer> colors = new LinkedTreeMap<>();
        colors.put("HT1", 0xe8ba3a);
        colors.put("LT1", 0xd5b355);
        colors.put("HT2", 0xc4d3e7);
        colors.put("LT2", 0xa0a7b2);
        colors.put("HT3", 0xf89f5a);
        colors.put("LT3", 0xc67b42);
        colors.put("HT4", 0x81749a);
        colors.put("LT4", 0x655b79);
        colors.put("HT5", 0x8f82a8);
        colors.put("LT5", 0x655b79);

        return colors;
    }

    @Getter
    @AllArgsConstructor
    public enum HighestMode implements StringTranslatable {
        NEVER("never", "tiertagger.highest.never"),
        NOT_FOUND("not_found", "tiertagger.highest.not_found"),
        ALWAYS("always", "tiertagger.highest.always"),
        ;

        private final String name;
        private final String translationKey;
    }
}
