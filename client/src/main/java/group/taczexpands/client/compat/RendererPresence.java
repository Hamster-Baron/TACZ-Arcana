package group.taczexpands.client.compat;

import net.minecraftforge.fml.loading.FMLLoader;

/** Reads installed renderer mod IDs from the Forge loading list. */
public final class RendererPresence {
    public static final boolean OCULUS_PRESENT =
            FMLLoader.getLoadingModList().getModFileById("oculus") != null;
    public static final boolean EMBEDDIUM_PRESENT =
            FMLLoader.getLoadingModList().getModFileById("embeddium") != null;

    private RendererPresence() {}
}
