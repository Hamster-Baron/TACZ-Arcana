package group.taczexpands.client.render.paint;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import group.taczexpands.client.paint.PaintPbr;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.Minecraft;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.concurrent.Executor;

/** Stable material identity backed by a private resident that owns its GL texture name. */
final class PaintTexture extends AbstractTexture {
    static final Executor COMPOSITOR = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "Arcana Paint Composer");
        thread.setDaemon(true);
        return thread;
    });
    private PaintInput input;
    private PaintResidentTexture resident;
    private boolean closed;
    private boolean dirty = true;
    private boolean preparePbr;
    private CompletableFuture<Prepared> preparation;
    private long touched = -1;
    private long bytes;

    PaintTexture(PaintInput input) {
        this.input = input;
    }

    @Override public int getId() {
        RenderSystem.assertOnRenderThreadOrInit();
        if (closed) return MissingTextureAtlasSprite.getTexture().getId();
        touched = PaintBudget.frame();
        if (preparation == null && (dirty || resident == null)) {
            PaintInput next = input;
            boolean pbr = preparePbr;
            preparePbr = false;
            preparation = CompletableFuture.supplyAsync(() -> prepareMaterial(next, pbr), COMPOSITOR);
            dirty = false;
        }
        installPrepared();
        return resident == null ? Minecraft.getInstance().getTextureManager().getTexture(input.surface().original()).getId()
                : resident.getId();
    }

    void installPrepared() {
        RenderSystem.assertOnRenderThreadOrInit();
        if (!closed && preparation != null && preparation.isDone()) {
            Prepared pixels = preparation.join();
            preparation = null;
            try (pixels) {
                PaintResidentTexture next = new PaintResidentTexture(pixels, pixels.patternPbr() ? null : this::requestPbr);
                next.setFilter(blur, mipmap);
                if (resident != null) resident.close();
                resident = next;
                bytes = (long) input.width() * input.height() * 4 * pixels.maps();
            }
        }
    }

    private void requestPbr() {
        if (!closed && input.hasPatternPbr()) {
            preparePbr = true;
            dirty = true;
        }
    }

    private static NativeImage prepare(PaintInput input) {
        return pixels(input, input.compose());
    }

    private static NativeImage pixels(PaintInput input, int[] result) {
        if (result == null) return null;
        NativeImage pixels = new NativeImage(input.width(), input.height(), false);
        boolean complete = false;
        try {
            int[] index = {0};
            // NativeImage's whole-image buffer operation avoids per-pixel coordinate checks.
            pixels.applyToAllPixels(ignored -> PaintResources.swapRedBlue(result[index[0]++]));
            complete = true;
            return pixels;
        } finally {
            if (!complete) pixels.close();
        }
    }

    record Prepared(NativeImage color, NativeImage normal, NativeImage specular, PaintPbr.Sources pbr, boolean patternPbr)
            implements AutoCloseable {
        int maps() { return 1 + (normal == null ? 0 : 1) + (specular == null ? 0 : 1); }
        @Override public void close() {
            color.close();
            if (normal != null) normal.close();
            if (specular != null) specular.close();
        }
    }

    private static Prepared prepareMaterial(PaintInput input, boolean pbr) {
        NativeImage color = prepare(input), normal = null, specular = null;
        boolean complete = false;
        try {
            if (pbr) {
                normal = pixels(input, input.composePbr(true));
                specular = pixels(input, input.composePbr(false));
            }
            Prepared result = new Prepared(color, normal, specular, input.pbr(), pbr);
            complete = true;
            return result;
        } finally {
            if (!complete) {
                color.close();
                if (normal != null) normal.close();
                if (specular != null) specular.close();
            }
        }
    }

    private void discardPreparation() {
        if (preparation != null) {
            preparation.thenAccept(Prepared::close);
            preparation = null;
        }
    }

    void update(PaintInput next) {
        if (closed || input.equals(next)) return;
        discardPreparation();
        input = next;
        preparePbr = false;
        dirty = true;
    }

    PaintInput input() { return input; }

    long touched() {
        return touched;
    }

    void touch() {
        touched = PaintBudget.frame();
    }

    long bytes() {
        return resident == null ? 0 : bytes;
    }

    void releaseResident() {
        discardPreparation();
        preparePbr = false;
        dirty = true;
        if (resident != null) {
            resident.close();
            resident = null;
        }
        bytes = 0;
    }

    @Override public void releaseId() {
        if (RenderSystem.isOnRenderThreadOrInit()) releaseResident();
        else RenderSystem.recordRenderCall(this::releaseResident);
    }

    @Override public void setFilter(boolean blur, boolean mipmap) {
        this.blur = blur;
        this.mipmap = mipmap;
        if (resident != null) resident.setFilter(blur, mipmap);
    }

    @Override public void load(ResourceManager manager) {
        // Prepared before publication.
    }

    @Override public void reset(TextureManager manager, ResourceManager resources, ResourceLocation id, Executor executor) {
        // Resource publication creates the next generation; this callback releases the old resident.
        if (!closed) releaseId();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        releaseId();
        input = null;
    }
}
