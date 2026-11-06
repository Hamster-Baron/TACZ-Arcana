package group.taczexpands.client.render.paint;

import group.taczexpands.common.paint.PaintConstants;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import group.taczexpands.client.paint.*;
import group.taczexpands.client.accessor.IAccessorPaintGunDisplay;
import group.taczexpands.client.accessor.IAccessorPaintAttachmentDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.io.IOException;
import java.io.DataInputStream;
import java.util.concurrent.CompletableFuture;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import java.util.*;

/** Publishes decoded paint inputs after TacZ publishes its new displays and indexes. */
public final class PaintResources {
    private static long epoch;
    private static Map<ResourceLocation, Gun> guns = Map.of();
    private static Map<ResourceLocation, Recipe> recipes = Map.of();
    private static Map<Object, Attachment> attachments = Map.of();
    private PaintResources() {}

    record Base(PaintInputs.Slot<PaintPbr.Image> image, PaintConfig.Pbr pbr) {}
    private static PaintInputs inputs;
    static final class Surface {
        private final ResourceLocation original, mask;
        private final int width, height;
        private final PaintConfig.Pbr pbr;
        private final Map<String, Base> bases;
        private final PaintInputs owner;
        private final PaintInputs.Slot<PaintPbr.Image> image;
        private final PaintInputs.Slot<PaintCompositor.Template> template;
        private CompletableFuture<Void> preparation;
        boolean prepared;
        volatile boolean valid;
        Surface(ResourceLocation original, ResourceLocation mask, int width, int height,
                PaintConfig.Pbr pbr, PaintInputs owner) {
            this(original, mask, width, height, pbr, Map.of(), owner);
        }

        Surface(ResourceLocation original, ResourceLocation mask, int width, int height,
                PaintConfig.Pbr pbr, Map<String, Base> bases, PaintInputs owner) {
            this.original = original;
            this.mask = mask;
            this.width = width;
            this.height = height;
            this.pbr = pbr;
            this.bases = bases;
            this.owner = owner;
            image = owner.image(original, width, height);
            template = owner.template(mask, width, height);
            owner.surfaces.add(this);
        }

        ResourceLocation original() {
            return original;
        }

        ResourceLocation mask() {
            return mask;
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }

        PaintConfig.Pbr pbr() {
            return pbr;
        }

        Base base(String name) {
            return name == null ? null : bases.get(name);
        }

        PaintInputs.Slot<PaintPbr.Image> image() {
            return image;
        }

        PaintInputs.Slot<PaintCompositor.Template> template() {
            return template;
        }

        PaintInputs owner() {
            return owner;
        }

        void poll() {
            if (preparation != null && preparation.isDone()) {
                preparation.join();
                preparation = null;
                prepared = true;
            }
        }

        boolean ready() {
            poll();
            if (!prepared && preparation == null) {
                preparation = CompletableFuture.runAsync(() -> owner.prepare(this), PaintTexture.COMPOSITOR);
            }
            return prepared && valid;
        }

        boolean hasRegion(int region) {
            return template.counts != null && template.counts[region] > 0;
        }
    }
    record Gun(ResourceLocation displayId, PaintConfig.Surface config, ResourceLocation model,
               ResourceLocation lodModel, Surface near, Surface far) {}
    static final class Recipe {
        private final ResourceLocation displayId;
        volatile PaintConfig.Recipe config;
        private final PaintInputs owner;
        private final PaintInputs.Slot<PaintCompositor.Pattern>[] patterns;
        private CompletableFuture<Void> preparation;
        boolean prepared;
        Recipe(ResourceLocation displayId, PaintConfig.Recipe config, PaintInputs owner,
                PaintInputs.Slot<PaintCompositor.Pattern>[] patterns) {
            this.displayId = displayId;
            this.config = config;
            this.owner = owner;
            this.patterns = patterns;
            owner.recipes.add(this);
        }

        ResourceLocation displayId() {
            return displayId;
        }

        PaintConfig.Recipe config() {
            return config;
        }

        PaintInputs.Slot<PaintCompositor.Pattern>[] patterns() {
            return patterns;
        }

        void poll() {
            if (preparation != null && preparation.isDone()) {
                preparation.join();
                preparation = null;
                prepared = true;
            }
        }

        boolean ready() {
            poll();
            if (!prepared && preparation == null) {
                preparation = CompletableFuture.runAsync(() -> owner.prepare(this), PaintTexture.COMPOSITOR);
            }
            return prepared && !config.empty();
        }
    }

    static boolean ready(Surface surface, Recipe recipe) {
        return surface.owner().ready(surface, recipe);
    }

    static void endFrame() {
        if (inputs != null) inputs.endFrame();
    }

    record Attachment(String kind, ResourceLocation displayId, PaintConfig.Surface config,
                      ResourceLocation model, ResourceLocation lodModel, Surface near, Surface far) {}
    static long epoch() {
        return epoch;
    }

    static Gun gun(ResourceLocation id) {
        return guns.get(id);
    }

    static Recipe recipe(ResourceLocation id) {
        return recipes.get(id);
    }

    static Attachment attachment(Object index) {
        return attachments.get(index);
    }

    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, manager, prepareProfiler, applyProfiler,
                                      prepareExecutor, gameExecutor) ->
                CompletableFuture.runAsync(PaintResources::clear, gameExecutor).thenCompose(barrier::wait));
    }

    public static void clear() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            Minecraft.getInstance().execute(PaintResources::clear);
            return;
        }
        if (inputs != null) {
            inputs.retire();
            inputs = null;
        }
        PaintPreviewCache.clear();
        PaintTextureCache.clear();
        PaintMaterialKeys.clear();
        PaintResolver.clear();
        guns = Map.of();
        recipes = Map.of();
        attachments = Map.of();
        epoch++;
    }

    public static void reload() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            Minecraft.getInstance().execute(PaintResources::reload);
            return;
        }
        // Publish after ClientIndexManager.reload completes so the new indexes are available.
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (inputs != null) inputs.retire();
        inputs = new PaintInputs(manager);
        Map<ResourceLocation, Dimensions> images = new HashMap<>();
        Set<ResourceLocation> failed = new HashSet<>();
        Map<ResourceLocation, Gun> nextGuns = new HashMap<>();
        Map<ResourceLocation, Recipe> nextRecipes = new HashMap<>();
        Map<Object, Attachment> nextAttachments = new IdentityHashMap<>();
        for (var entry : ClientAssetsManager.INSTANCE.getGunDisplays()) {
            var display = entry.getValue();
            if (!(display instanceof IAccessorPaintGunDisplay extension)) continue;
            extension.taczexpands$compilePaint(entry.getKey(),
                    () -> defaults(manager, "guns", entry.getKey()), PaintResources::diagnostic);
            PaintConfig.Surface config = extension.taczexpands$getPaint();
            if (config == null) continue;
            Surface near = surface(entry.getKey(), display.getModelTexture(), config, false, manager, images, failed);
            var lod = display.getGunLod();
            boolean shared = lod == null || (Objects.equals(lod.getModelLocation(), display.getModelLocation())
                    && Objects.equals(lod.getModelTexture(), display.getModelTexture()));
            Surface far = shared ? near : config.lodMask() == null ? null
                    : surface(entry.getKey(), lod.getModelTexture(), config, true, manager, images, failed);
            if (!shared && config.lodMask() == null) {
                diagnostic(entry.getKey()
                        + ": independent gun LOD has no paint.lod_mask; retaining LOD original");
            }
            nextGuns.put(entry.getKey(), new Gun(entry.getKey(), config, display.getModelLocation(),
                    lod == null ? null : lod.getModelLocation(), near, far));
        }
        for (var entry : TimelessAPI.getAllCommonAttachmentIndex()) {
            var index = entry.getValue();
            Object clientIndex = ClientIndexManager.ATTACHMENT_INDEX.get(entry.getKey());
            if (clientIndex == null) continue;
            ResourceLocation displayId = index.getPojo().getDisplay();
            var display = ClientAssetsManager.INSTANCE.getAttachmentDisplay(displayId);
            if (!(display instanceof IAccessorPaintAttachmentDisplay extension)) continue;
            extension.taczexpands$compilePaint(displayId,
                    () -> defaults(manager, "attachments", displayId), PaintResources::diagnostic);
            prepareAttachment(nextAttachments, clientIndex, "attachment", displayId,
                    display, extension, manager, images, failed);
            if (index.getType() != AttachmentType.valueOf(PaintConstants.MODULE)) continue;
            PaintConfig.Recipe config = extension.taczexpands$getPaint();
            if (config == null) continue;
            PaintConfig.Channel[] channels = new PaintConfig.Channel[3];
            @SuppressWarnings("unchecked")
            PaintInputs.Slot<PaintCompositor.Pattern>[] patterns = new PaintInputs.Slot[3];
            for (int i = 0; i < 3; i++) {
                var channel = config.channel(i);
                if (channel == null) continue;
                channels[i] = channel;
                if (channel.texture() != null) {
                    Dimensions size = dimensions(channel.texture(), manager, images, failed);
                    if (size != null) {
                        patterns[i] = inputs.pattern(channel.texture(), size.width(), size.height());
                    } else {
                        channels[i] = null;
                    }
                }
                if (channels[i] != null && channel.pbr() != null) {
                    var pbr = new PaintConfig.Pbr(
                            patternMap(channel.pbr().normal(), manager, images, failed),
                            patternMap(channel.pbr().specular(), manager, images, failed));
                    channels[i] = new PaintConfig.Channel(channel.baseRgb(), channel.h(), channel.s(), channel.v(),
                            channel.mix(), channel.texture(), channel.scale(),
                            pbr.normal() == null && pbr.specular() == null ? null : pbr);
                }
            }
            var valid = new PaintConfig.Recipe(channels[0], channels[1], channels[2], config.switchPbr(), config.base());
            if (!valid.empty()) nextRecipes.put(entry.getKey(), new Recipe(displayId, valid, inputs, patterns));
        }
        for (var entry : TimelessAPI.getAllCommonAttachmentVariantIndex()) {
            Object clientIndex = ClientIndexManager.ATTACHMENT_VARIANT_INDEX.get(entry.getKey());
            if (clientIndex == null) continue;
            ResourceLocation displayId = entry.getValue().getPojo().getDisplay();
            var display = ClientAssetsManager.INSTANCE.getAttachmentVariantDisplay(displayId);
            if (!(display instanceof IAccessorPaintAttachmentDisplay extension)) continue;
            extension.taczexpands$compilePaint(displayId,
                    () -> defaults(manager, "attachment_variants", displayId), PaintResources::diagnostic);
            prepareAttachment(nextAttachments, clientIndex, "variant", displayId,
                    display, extension, manager, images, failed);
        }
        for (var entry : TimelessAPI.getAllCommonSlotAdapterIndex()) {
            Object clientIndex = ClientIndexManager.SLOT_ADAPTER_INDEX.get(entry.getKey());
            if (clientIndex == null) continue;
            ResourceLocation displayId = entry.getValue().getPojo().getDisplay();
            var display = ClientAssetsManager.INSTANCE.getSlotAdapterDisplay(displayId);
            if (!(display instanceof IAccessorPaintAttachmentDisplay extension)) continue;
            extension.taczexpands$compilePaint(displayId,
                    () -> defaults(manager, "slot_adapters", displayId), PaintResources::diagnostic);
            prepareAttachment(nextAttachments, clientIndex, "adapter", displayId,
                    display, extension, manager, images, failed);
        }
        guns = Map.copyOf(nextGuns);
        recipes = Map.copyOf(nextRecipes);
        attachments = Collections.unmodifiableMap(nextAttachments);
        LogUtils.getLogger().info("Arcana paint epoch {}: {} gun displays, {} MODULE recipes, {} attachment displays",
                epoch, guns.size(), recipes.size(), attachments.size());
    }

    private static void prepareAttachment(Map<Object, Attachment> target, Object index, String kind,
                                          ResourceLocation displayId, AttachmentDisplay display,
                                          IAccessorPaintAttachmentDisplay extension, ResourceManager manager,
                                          Map<ResourceLocation, Dimensions> images, Set<ResourceLocation> failed) {
        PaintConfig.Surface config = extension.taczexpands$getPaintSurface();
        if (config == null) return;
        Surface near = surface(displayId, display.getTexture(), config, false, manager, images, failed);
        var lod = display.getAttachmentLod();
        boolean shared = lod == null || (Objects.equals(lod.getModelLocation(), display.getModel())
                && Objects.equals(lod.getModelTexture(), display.getTexture()));
        Surface far = shared ? near : config.lodMask() == null ? null
                : surface(displayId, lod.getModelTexture(), config, true, manager, images, failed);
        if (!shared && config.lodMask() == null) {
            diagnostic(kind + " " + displayId + ": independent attachment LOD has no paint.lod_mask; retaining original");
        }
        target.put(index, new Attachment(kind, displayId, config, display.getModel(),
                lod == null ? null : lod.getModelLocation(), near, far));
    }

    /**
     * Pack-supplied paint for a display that defines none, mirroring TacZ's display layout:
     * {@code assets/<ns>/paint_defaults/<folder>/<path>.json} holds the same object as a display's
     * {@code paint} field. Lets add-on packs give existing gun packs templates without replacing
     * their display files; the display's own field always wins.
     */
    private static JsonElement defaults(ResourceManager manager, String folder, ResourceLocation displayId) {
        ResourceLocation file = new ResourceLocation(displayId.getNamespace(),
                "paint_defaults/" + folder + "/" + displayId.getPath() + ".json");
        var resource = manager.getResource(file);
        if (resource.isEmpty()) return null;
        try (var reader = resource.get().openAsReader()) {
            // TacZ pack JSON commonly carries comments; parse with the same leniency.
            var json = new JsonReader(reader);
            json.setLenient(true);
            return JsonParser.parseReader(json);
        } catch (IOException | RuntimeException ex) {
            diagnostic(file + ": unreadable paint defaults: " + ex.getMessage());
            return null;
        }
    }

    private static Surface surface(ResourceLocation displayId, ResourceLocation original,
                                   PaintConfig.Surface config, boolean lod, ResourceManager manager,
                                   Map<ResourceLocation, Dimensions> images, Set<ResourceLocation> failed) {
        ResourceLocation mask = lod ? config.lodMask() : config.mask();
        Dimensions base = dimensions(original, manager, images, failed);
        Dimensions template = dimensions(mask, manager, images, failed);
        if (base == null || template == null) return null;
        if (base.width() != template.width() || base.height() != template.height()) {
            diagnostic(displayId + ": mask " + mask + " dimensions differ from " + original);
            return null;
        }
        Map<String, Base> bases = Map.of();
        if (!config.bases().isEmpty()) {
            bases = new HashMap<>();
            for (var entry : config.bases().entrySet()) {
                var declared = entry.getValue();
                ResourceLocation texture = lod ? declared.lodTexture() : declared.texture();
                if (texture == null) {
                    diagnostic(displayId + ": paint.bases." + entry.getKey()
                            + " has no lod_texture; retaining LOD original base");
                    continue;
                }
                Dimensions size = dimensions(texture, manager, images, failed);
                if (size == null) continue;
                if (size.width() != template.width() || size.height() != template.height()) {
                    diagnostic(displayId + ": base " + texture + " dimensions differ from mask " + mask);
                    continue;
                }
                bases.put(entry.getKey(), new Base(inputs.image(texture, size.width(), size.height()),
                        availablePbr(displayId, declared.pbr(), manager)));
            }
        }
        return new Surface(original, mask, base.width(), base.height(),
                availablePbr(displayId, lod ? config.lodPbr() : config.pbr(), manager), bases, inputs);
    }

    private static PaintConfig.Pbr availablePbr(ResourceLocation displayId, PaintConfig.Pbr pbr,
                                                 ResourceManager manager) {
        return pbr == null ? null : new PaintConfig.Pbr(
                pbrMap(displayId, pbr.normal(), manager), pbrMap(displayId, pbr.specular(), manager));
    }

    private static ResourceLocation pbrMap(ResourceLocation displayId, ResourceLocation map, ResourceManager manager) {
        if (map == null || manager.getResource(map).isPresent()) return map;
        diagnostic(displayId + ": painted PBR map " + map + " is missing; using fallback for that map");
        return null;
    }

    private static ResourceLocation patternMap(ResourceLocation map, ResourceManager manager,
                                                Map<ResourceLocation, Dimensions> images, Set<ResourceLocation> failed) {
        if (map == null) return null;
        Dimensions size = dimensions(map, manager, images, failed);
        if (size == null) return null;
        inputs.image(map, size.width(), size.height());
        return map;
    }

    record Dimensions(int width, int height) {}

    static Dimensions dimensions(ResourceLocation id, ResourceManager manager,
                                         Map<ResourceLocation, Dimensions> sizes, Set<ResourceLocation> failed) {
        if (id == null || failed.contains(id)) return null;
        var cached = sizes.get(id);
        if (cached != null) return cached;
        try (var stream = new DataInputStream(manager.open(id))) {
            if (stream.readLong() != 0x89504e470d0a1a0aL || stream.readInt() != 13 || stream.readInt() != 0x49484452) {
                throw new IOException("Expected PNG IHDR");
            }
            int width = stream.readInt(), height = stream.readInt();
            if (width <= 0 || height <= 0) throw new IOException("Invalid PNG dimensions");
            var size = new Dimensions(width, height);
            sizes.put(id, size);
            return size;
        } catch (IOException ex) {
            failed.add(id);
            diagnostic("cannot prepare " + id + ": " + ex.getMessage());
            return null;
        }
    }

    static int swapRedBlue(int pixel) {
        return (pixel & 0xff00ff00) | ((pixel & 255) << 16) | ((pixel >>> 16) & 255);
    }

    static void diagnostic(String message) {
        LogUtils.getLogger().warn("Arcana paint: {}", message);
    }
}
