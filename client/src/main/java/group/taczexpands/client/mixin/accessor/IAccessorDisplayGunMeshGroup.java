package group.taczexpands.client.mixin.accessor;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.world.WorldMeshGroup;
import com.tacz.guns.client.renderer.block.DisplayGunMeshGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(value = DisplayGunMeshGroup.class, remap = false)
public interface IAccessorDisplayGunMeshGroup {
    @Accessor("group") static WorldMeshGroup<?> taczexpands$getGroup() { throw new AssertionError(); }
    @Accessor("APPEARANCES") static Map<?, ?> taczexpands$getAppearances() { throw new AssertionError(); }
}
