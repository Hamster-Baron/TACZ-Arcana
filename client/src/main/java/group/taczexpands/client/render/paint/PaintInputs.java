package group.taczexpands.client.render.paint;

import com.mojang.blaze3d.platform.NativeImage;
import group.taczexpands.client.paint.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

/** One resource generation's shared, evictable CPU inputs; never owns finished GPU textures. */
final class PaintInputs {
    private static final long BUDGET = 128L * 1024 * 1024;
    private static final long IDLE_NANOS = 5_000_000_000L;
    private final ResourceManager manager;
    private final Map<ResourceLocation, Slot<PaintPbr.Image>> images = new HashMap<>();
    private final Map<ResourceLocation, Slot<PaintCompositor.Template>> templates = new HashMap<>();
    private final Map<ResourceLocation, Slot<PaintCompositor.Pattern>> patterns = new HashMap<>();
    private final Map<ResourceLocation, PaintResources.Dimensions> pbrDimensions = new HashMap<>();
    private final Set<ResourceLocation> missingPbr = new HashSet<>();
    private final Set<Slot<?>> pinned = new HashSet<>();
    final List<PaintResources.Surface> surfaces = new ArrayList<>();
    final List<PaintResources.Recipe> recipes = new ArrayList<>();
    private volatile boolean retired;
    private record Request(PaintResources.Surface surface, PaintResources.Recipe recipe) {}
    private final Set<Request> awaiting = new HashSet<>();

    PaintInputs(ResourceManager manager) {
        this.manager = manager;
    }

    final class Slot<T> {
        private final long bytes;
        private final Supplier<T> loader;
        private volatile T value;
        volatile boolean failed;
        private int users;
        private long touched;
        volatile int[] counts;

        Slot(long bytes, Supplier<T> loader) {
            this.bytes = bytes;
            this.loader = loader;
        }

        synchronized T load() {
            T current = value;
            if (current != null || failed) return current;
            current = loader.get();
            synchronized (PaintInputs.this) {
                failed = current == null;
                if (!retired) value = current;
            }

            return current;
        }
    }

    synchronized Slot<PaintPbr.Image> image(ResourceLocation id, int width, int height) {
        return images.computeIfAbsent(id, ignored -> new Slot<>((long) width * height * 4, () -> read(id)));
    }

    private synchronized Slot<PaintPbr.Image> pbrImage(ResourceLocation id) {
        if (id == null || missingPbr.contains(id)) return null;
        var cached = images.get(id);
        if (cached != null) return cached;
        if (manager.getResource(id).isEmpty()) {
            missingPbr.add(id);
            return null;
        }
        var size = PaintResources.dimensions(id, manager, pbrDimensions, missingPbr);
        return size == null ? null : image(id, size.width(), size.height());
    }

    Slot<PaintCompositor.Template> template(ResourceLocation id, int width, int height) {
        return templates.computeIfAbsent(id, ignored -> new Slot<>((long) width * height * 3, () -> {
            var image = read(id);
            if (image == null) return null;
            var template = PaintCompositor.prepare(image.width(), image.height(), image.argb());
            var slot = templates.get(id);
            boolean first = slot.counts == null;
            if (first) {
                slot.counts = new int[]{template.count(0), template.count(1), template.count(2)};
            }

            if (first && template.invalidPixels() > 0 && !retired) {
                PaintResources.diagnostic(id + " contains " + template.invalidPixels()
                        + " mixed-channel pixels; those pixels retain the original");
            }

            return template;
        }));
    }

    Slot<PaintCompositor.Pattern> pattern(ResourceLocation id, int width, int height) {
        return patterns.computeIfAbsent(id, ignored -> new Slot<>((long) width * height * 4, () -> {
            var image = read(id);
            if (image == null) return null;
            try {
                return new PaintCompositor.Pattern(image.width(), image.height(), image.argb());
            } catch (IllegalArgumentException ex) {
                PaintResources.diagnostic(id + ": " + ex.getMessage());
                return null;
            }
        }));
    }

    private PaintPbr.Image read(ResourceLocation id) {
        try (var stream = manager.open(id); var image = NativeImage.read(stream)) {
            int[] pixels = image.getPixelsRGBA();
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = PaintResources.swapRedBlue(pixels[i]);
            }

            return new PaintPbr.Image(image.getWidth(), image.getHeight(), pixels);
        } catch (IOException | IllegalArgumentException | ArithmeticException ex) {
            PaintResources.diagnostic("cannot prepare " + id + ": " + ex.getMessage());
            return null;
        }
    }

    private synchronized void acquire(Collection<Slot<?>> slots) {
        long now = System.nanoTime();
        for (var slot : slots) {
            slot.users++;
            slot.touched = now;
        }
    }

    private synchronized void release(Collection<Slot<?>> slots) {
        for (var slot : slots) slot.users--;
        trim();
    }

    private List<Slot<?>> slots(PaintInput input) {
        var result = new ArrayList<Slot<?>>();
        result.add(input.surface().image());
        result.add(input.surface().template());
        var base = input.base();
        if (base != null) result.add(base.image());
        for (var pattern : input.recipe().patterns()) {
            if (pattern != null) result.add(pattern);
        }

        return result;
    }

    void prepare(PaintResources.Surface surface) {
        var slots = List.<Slot<?>>of(surface.image(), surface.template());
        acquire(slots);
        try {
            surface.valid = surface.image().load() != null && surface.template().load() != null;
        } finally {
            release(slots);
        }
    }

    void prepare(PaintResources.Recipe recipe) {
        var slots = new ArrayList<Slot<?>>();
        for (var slot : recipe.patterns()) {
            if (slot != null) slots.add(slot);
        }

        acquire(slots);
        try {
            var config = recipe.config();
            var channels = new PaintConfig.Channel[]{config.r(), config.g(), config.b()};
            for (int i = 0; i < 3; i++) {
                if (recipe.patterns()[i] != null && recipe.patterns()[i].load() == null) channels[i] = null;
            }

            recipe.config = new PaintConfig.Recipe(channels[0], channels[1], channels[2], config.switchPbr(), config.base());
        } finally {
            release(slots);
        }
    }

    int[] compose(PaintInput input) {
        var slots = slots(input);
        acquire(slots);
        try {
            var base = input.base();
            var image = base == null ? null : base.image().load();
            if (image == null) image = input.surface().image().load();
            var template = input.surface().template().load();
            var patterns = new PaintCompositor.Pattern[3];
            for (int i = 0; i < 3; i++) {
                if (input.recipe().patterns()[i] != null) {
                    patterns[i] = input.recipe().patterns()[i].load();
                }
            }

            if (image == null || template == null) {
                throw new IllegalStateException("Prepared paint inputs unavailable");
            }

            return PaintCompositor.compose(image.width(), image.height(), image.argb(), template,
                    input.config(), input.recipe().config(), input.parameters(), patterns);
        } finally {
            release(slots);
        }
    }

    int[] composePbr(PaintInput input, boolean normal) {
        var slots = new ArrayList<Slot<?>>();
        @SuppressWarnings("unchecked")
        Slot<PaintPbr.Image>[] maps = new Slot[3];
        boolean active = false;
        for (int i = 0; i < 3; i++) {
            var channel = input.recipe().config().channel(i);
            if (input.parameters().channel(i) == null || channel == null || channel.pbr() == null) continue;
            maps[i] = pbrImage(normal ? channel.pbr().normal() : channel.pbr().specular());
            if (maps[i] != null) {
                slots.add(maps[i]);
                active = true;
            }
        }
        if (!active) return null;
        var source = input.pbr();
        var background = pbrImage(normal ? source.normal() : source.specular());
        if (background != null) slots.add(background);
        slots.add(input.surface().template());
        acquire(slots);
        try {
            var patterns = new PaintPbr.Image[3];
            for (int i = 0; i < 3; i++) {
                if (maps[i] != null) patterns[i] = maps[i].load();
            }
            return PaintCompositor.composePbr(input.surface().template().load(), input.config(),
                    input.parameters(), background == null ? null : background.load(), patterns, normal);
        } finally {
            release(slots);
        }
    }

    boolean ready(PaintResources.Surface surface, PaintResources.Recipe recipe) {
        boolean ready = surface.ready() & recipe.ready();
        if (!ready && (!surface.prepared || !recipe.prepared)) {
            awaiting.add(new Request(surface, recipe));
        }

        return ready;
    }

    synchronized void pin(PaintInput input) {
        pinned.addAll(slots(input));
    }

    void endFrame() {
        for (var surface : surfaces) surface.poll();
        for (var recipe : recipes) recipe.poll();
        var changed = new HashSet<PaintResources.Surface>();
        var iterator = awaiting.iterator();
        while (iterator.hasNext()) {
            var request = iterator.next();
            if (!request.surface().prepared || !request.recipe().prepared) continue;
            if (request.surface().valid && !request.recipe().config().empty()) {
                changed.add(request.surface());
            }

            iterator.remove();
        }

        if (!changed.isEmpty()) PaintExternalOwners.inputsReady(changed);
        synchronized (this) {
            pinned.clear();
            for (var entry : PaintPreviewCache.entries()) pin(entry.texture().input());
            trim();
        }
    }

    private void trim() {
        var slots = new ArrayList<Slot<?>>();
        slots.addAll(images.values());
        slots.addAll(templates.values());
        slots.addAll(patterns.values());
        long bytes = 0, now = System.nanoTime();
        for (var slot : slots) {
            if (slot.value != null) bytes += slot.bytes;
        }

        slots.sort(Comparator.comparingLong(slot -> slot.touched));
        for (var slot : slots) {
            if (slot.value == null || slot.users != 0 || pinned.contains(slot)) continue;
            if (retired || bytes > BUDGET || now - slot.touched >= IDLE_NANOS) {
                slot.value = null;
                bytes -= slot.bytes;
            }
        }
    }

    synchronized void retire() {
        retired = true;
        pinned.clear();
        for (var slots : List.of(images.values(), templates.values(), patterns.values())) {
            for (var slot : slots) slot.value = null;
        }
    }
}
