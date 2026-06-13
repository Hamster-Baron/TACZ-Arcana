package group.taczexpands.common.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.attachment.AttachmentType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(value = AttachmentType.class, remap = false)
public class MixinAttachmentType {
    @Shadow
    @Final
    @Mutable
    private static AttachmentType[] $VALUES;


    @Invoker("<init>")
    public static AttachmentType initInvoker(String internalName, int internalID) {
        throw new AssertionError();
    }

    private static AttachmentType addVariant(String internalName) {
        ArrayList<AttachmentType> variants = new ArrayList<>(Arrays.asList($VALUES));
        AttachmentType type = initInvoker(internalName, variants.get(variants.size() - 1).ordinal() + 1);
        variants.add(type);
        $VALUES = variants.toArray(new AttachmentType[0]);
        return type;
    }
}
