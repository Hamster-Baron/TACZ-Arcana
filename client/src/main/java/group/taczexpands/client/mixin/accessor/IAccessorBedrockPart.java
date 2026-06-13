package group.taczexpands.client.mixin.accessor;

import com.tacz.guns.client.model.bedrock.BedrockPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BedrockPart.class, remap = false)
public interface IAccessorBedrockPart {
    @Accessor("parent")
    void setParent(BedrockPart parent);
}
