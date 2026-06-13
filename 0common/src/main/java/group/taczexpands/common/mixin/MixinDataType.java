package group.taczexpands.common.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.resource.network.DataType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(value = DataType.class, remap = false)
public class MixinDataType {
    @Shadow
    @Final
    @Mutable
    private static DataType[] $VALUES;

    private static final DataType AMMO_DATA = addVariant("AMMO_DATA");

    private static final DataType RESISTANCE_TABLE_DATA = addVariant("RESISTANCE_TABLE_DATA");


    @Invoker("<init>")
    public static DataType initInvoker(String internalName, int internalID) {
        throw new AssertionError();
    }

    private static DataType addVariant(String internalName) {
        ArrayList<DataType> variants = new ArrayList<>(Arrays.asList($VALUES));
        DataType type = initInvoker(internalName, variants.get(variants.size() - 1).ordinal() + 1);
        variants.add(type);
        $VALUES = variants.toArray(new DataType[0]);
        return type;
    }
}
