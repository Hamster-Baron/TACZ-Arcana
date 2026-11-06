package group.taczexpands.client.render.surface;


import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.tacz.guns.util.ResourceScanner;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class SurfaceResources {
    private static final Gson GSON = new Gson();
    private static Map<ResourceLocation, JsonElement> resources = Map.of();

    private SurfaceResources() {}

    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(
                new SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>>() {
                    @Override
                    protected Map<ResourceLocation, JsonElement> prepare(
                            ResourceManager manager, ProfilerFiller profiler) {
                        return ResourceScanner.scanDirectory(manager, "expansions/surfaces", GSON);
                    }

                    @Override
                    protected void apply(
                            Map<ResourceLocation, JsonElement> files,
                            ResourceManager manager,
                            ProfilerFiller profiler) {
                        resources = new TreeMap<>(files);
                        SurfaceResources.reload();
                    }
                });
    }

    public static void reload() {
        SurfaceConfig combined = new SurfaceConfig();
        for (var file : resources.entrySet()) {
            var id = file.getKey();
            String source =
                    "assets/" + id.getNamespace() + "/expansions/surfaces/" + id.getPath() + ".json";
            addDefinition(combined, id.toString(), file.getValue(), source);
        }
        Path local = FMLPaths.CONFIGDIR.get().resolve("arcana-surfaces.json");
        if (Files.exists(local)) {
            try (Reader reader = Files.newBufferedReader(local)) {
                JsonObject overrides = GSON.fromJson(reader, JsonObject.class);
                if (overrides == null
                        || !overrides.has("feeds")
                        || !overrides.get("feeds").isJsonObject()) {
                    throw new IllegalArgumentException("feeds must be an object");
                }
                for (var entry : overrides.getAsJsonObject("feeds").entrySet()) {
                    addDefinition(
                            combined,
                            entry.getKey(),
                            entry.getValue(),
                            local + "#feeds/" + entry.getKey());
                }
            } catch (IOException | JsonParseException | IllegalArgumentException ex) {
                LogUtils.getLogger().error("Failed to load surface overrides {}", local, ex);
            }
        }
        SurfaceEngine.install(combined);
        LogUtils.getLogger()
                .info("Arcana Surfaces loaded {} surface definitions", combined.feeds.size());
    }

    private static void addDefinition(
            SurfaceConfig target, String id, JsonElement json, String source) {
        try {
            SurfaceConfig.Feed definition = GSON.fromJson(json, SurfaceConfig.Feed.class);
            SurfaceConfig.validateFeed(id, definition);
            target.feeds.put(id, definition);
        } catch (JsonParseException | IllegalArgumentException ex) {
            LogUtils.getLogger().error("Failed to load surface definition {}", source, ex);
        }
    }

}
