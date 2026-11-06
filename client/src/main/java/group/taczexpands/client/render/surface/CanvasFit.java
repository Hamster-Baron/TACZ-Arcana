package group.taczexpands.client.render.surface;

public record CanvasFit(float sx, float sy, float x, float y) {
    public static CanvasFit of(String mode, int cw, int ch, int tw, int th) {
        float sx = (float) tw / cw, sy = (float) th / ch;
        switch (mode) {
            case "stretch" -> {}
            case "contain" -> sx = sy = Math.min(sx, sy);
            case "cover" -> sx = sy = Math.max(sx, sy);
            case "center_crop" -> sx = sy = 1;
            default -> throw new IllegalArgumentException("Unknown fit: " + mode);
        }
        return new CanvasFit(sx, sy, (tw - cw * sx) / 2, (th - ch * sy) / 2);
    }
}
