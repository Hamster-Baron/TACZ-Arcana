package group.taczexpands.client.mixin;

import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.resource.pojo.data.gun.GunRecoil;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.ViewportEvent;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CameraSetupEvent.class, remap = false)
public class MixinCameraSetupEvent {
    @Shadow
    private static org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction pitchSplineFunction;

    @Shadow
    private static org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction yawSplineFunction;


    @Redirect(method = "initialCameraRecoil", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunRecoil;genYawSplineFunction(F)Lorg/apache/commons/math3/analysis/polynomials/PolynomialSplineFunction;"))
    private static PolynomialSplineFunction modifyYaw(GunRecoil instance, float baseModifier) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            var inaccuracyType = InaccuracyType.getInaccuracyType(player);
            Float modifier = GunExtras.INSTANCE.getRecoilModifier(Minecraft.getInstance().player, player.getMainHandItem(), inaccuracyType);
            if (modifier != null) {
                return instance.genYawSplineFunction(baseModifier * modifier);
            }
        }
        return instance.genYawSplineFunction(baseModifier);
    }

    @Redirect(method = "initialCameraRecoil", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunRecoil;genPitchSplineFunction(F)Lorg/apache/commons/math3/analysis/polynomials/PolynomialSplineFunction;"))
    private static PolynomialSplineFunction modifyPitch(GunRecoil instance, float baseModifier) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            var inaccuracyType = InaccuracyType.getInaccuracyType(player);
            Float modifier = GunExtras.INSTANCE.getRecoilModifier(Minecraft.getInstance().player, player.getMainHandItem(), inaccuracyType);
            if (modifier != null) {
                return instance.genPitchSplineFunction(baseModifier * modifier);
            }
        }
        return instance.genPitchSplineFunction(baseModifier);
    }

    @Inject(method = "applyScopeMagnification", at = @At("HEAD"), cancellable = true)
    private static void applyScopeMagnification(ViewportEvent.ComputeFov event, CallbackInfo ci) {
        if (TACZExpandsClient.Companion.isAdvancedRendering() && !TACZExpandsClient.Companion.getPatchMainTargetDraw())
            ci.cancel();
    }


}
