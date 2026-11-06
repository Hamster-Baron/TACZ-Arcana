package group.taczexpands.client.render.gun;
import net.minecraft.resources.ResourceLocation;
/** Assign once before publishing the key to the cache; cached keys are immutable. */
public interface AppearanceKeyExtension {
 ResourceLocation taczexpands$getTextureKey();
 void taczexpands$initTextureKey(ResourceLocation value);
}
