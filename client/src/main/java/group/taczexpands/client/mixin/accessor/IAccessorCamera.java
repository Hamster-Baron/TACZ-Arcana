package group.taczexpands.client.mixin.accessor;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.openjdk.nashorn.internal.objects.annotations.Setter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface IAccessorCamera {
    @Invoker("setRotation")
    void setRotation(float yRot, float xRot);

    @Invoker("setPosition")
    void setPosition(Vec3 pos);

    @Invoker("move")
    void move(double x, double y, double z);

    @Accessor("eyeHeight")
    void setEyeHeight(float value);

    @Accessor("eyeHeightOld")
    void setEyeHeightOld(float value);
}
