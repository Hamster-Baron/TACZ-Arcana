package group.taczexpands.client.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Live client settings for the appearance and placement of the MODULE paint editor. */
public final class PaintUiConfig {
    public enum Style { WORKSHOP, TACZ }

    public enum Layout { LEFT, RIGHT }

    private static ForgeConfigSpec.EnumValue<Style> style;
    private static ForgeConfigSpec.EnumValue<Layout> layout;

    private PaintUiConfig() {}

    public static void register(ForgeConfigSpec.Builder builder) {
        if (style != null) throw new IllegalStateException("Paint editor settings registered twice");
        builder.push("Paint Editor Settings");
        style = builder.comment(
                        "Paint editor look: WORKSHOP is dark slate with amber accents; TACZ matches TacZ's translucent cream buttons.",
                        "Allowed Values: WORKSHOP, TACZ")
                .defineEnum("paintEditorStyle", Style.WORKSHOP);
        layout = builder.comment(
                        "Paint editor position: LEFT takes over TacZ's property-diagram column while painting;"
                                + " RIGHT keeps the diagrams and docks beside the attachment lists.",
                        "Allowed Values: LEFT, RIGHT")
                .defineEnum("paintEditorLayout", Layout.LEFT);
        builder.pop();
    }

    /** Defaults apply before the CLIENT config is loaded, e.g. in isolated contract tests. */
    public static Style style() {
        try {
            return style == null ? Style.WORKSHOP : style.get();
        } catch (IllegalStateException notLoaded) {
            return Style.WORKSHOP;
        }
    }

    public static Layout layout() {
        try {
            return layout == null ? Layout.LEFT : layout.get();
        } catch (IllegalStateException notLoaded) {
            return Layout.LEFT;
        }
    }

    public static ForgeConfigSpec.EnumValue<Style> styleValue() {
        return style;
    }

    public static ForgeConfigSpec.EnumValue<Layout> layoutValue() {
        return layout;
    }
}
