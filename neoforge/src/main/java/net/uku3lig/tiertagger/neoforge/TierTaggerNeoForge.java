package net.uku3lig.tiertagger.neoforge;

import com.llamalad7.mixinextras.lib.semver.Version;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.uku3lig.tiertagger.TierTagger;
import net.uku3lig.tiertagger.config.UkulibIntegration;
import net.uku3lig.ukulib.neoforge.UkulibNFProvider;

@Mod(value = TierTagger.MOD_ID, dist = Dist.CLIENT)
public class TierTaggerNeoForge {
    public TierTaggerNeoForge(ModContainer container) {
        String versionString = container.getModInfo().getVersion().toString();
        TierTagger.onInitialize(Version.parse(versionString));
        container.registerExtensionPoint(UkulibNFProvider.class, UkulibIntegration::new);
    }
}
