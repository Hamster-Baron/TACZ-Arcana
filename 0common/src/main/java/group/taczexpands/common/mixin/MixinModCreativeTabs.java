package group.taczexpands.common.mixin;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.init.ModCreativeTabs;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.TACZExpandsCommon;
import group.taczexpands.common.accessor.IAccessorMiscPOJO;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Comparator;

@Mixin(value = ModCreativeTabs.class, remap = false)
public class MixinModCreativeTabs {

}
