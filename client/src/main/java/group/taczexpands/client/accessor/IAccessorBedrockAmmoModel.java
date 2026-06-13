package group.taczexpands.client.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.animation.AnimationListenerSupplier;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

public interface IAccessorBedrockAmmoModel {
    void taczexpands$render(EntityKineticBullet entity, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay);

    Vector3f taczexpands$getHookPos();

    static AnimationListenerSupplier getAnimated(BedrockAmmoModel model) {
        return (AnimationListenerSupplier) model;
    }
}
