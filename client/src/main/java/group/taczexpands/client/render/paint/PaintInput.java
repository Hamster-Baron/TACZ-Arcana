package group.taczexpands.client.render.paint;

import group.taczexpands.client.paint.PaintConfig;
import group.taczexpands.client.paint.PaintParameters;
import group.taczexpands.client.paint.PaintPbr;

/** Prepared composition inputs shared by formal and preview textures. */
record PaintInput(PaintConfig.Surface config, PaintResources.Surface surface,
                 PaintResources.Recipe recipe, PaintParameters.Resolved parameters) {
    int width() {
        return surface.width();
    }

    int height() {
        return surface.height();
    }

    int[] compose() {
        return surface.owner().compose(this);
    }

    int[] composePbr(boolean normal) {
        return surface.owner().composePbr(this, normal);
    }

    boolean hasPatternPbr() {
        for (int i = 0; i < 3; i++) {
            var channel = recipe.config().channel(i);
            if (parameters.channel(i) != null && channel != null && channel.pbr() != null) return true;
        }
        return false;
    }

    PaintResources.Base base() {
        var base = surface.base(recipe.config().base());
        return base == null || base.image().failed ? null : base;
    }

    PaintPbr.Sources pbr() {
        var base = base();
        return PaintPbr.select(surface.original(), base == null ? null : base.pbr(),
                surface.pbr(), recipe.config().switchPbr());
    }
}
