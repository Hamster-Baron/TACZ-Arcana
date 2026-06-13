package group.taczexpands.client.mixin;

import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import group.taczexpands.client.accessor.IAccessorBedrockModelPOJO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BedrockModelPOJO.class, remap = false)
public class MixinBedrockModelPOJO implements IAccessorBedrockModelPOJO {
    @Unique
    private transient boolean taczexpands$isPatched = false;

    @Override
    public boolean taczexpands$isPatched() {
        return taczexpands$isPatched;
    }

    @Override
    public void taczexpands$setPatched(boolean value) {
        taczexpands$isPatched = value;
    }
}
