package group.taczexpands.client.mixin;

import com.tacz.guns.resource.GunPackLoader;
import com.tacz.guns.util.GetJarResources;
import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.config.PreLoadConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Mixin(value = GunPackLoader.class, remap = false)
public class MixinGunPackLoader {

    @Redirect(method = "discoverExtensions", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/util/GetJarResources;copyModDirectory(Ljava/lang/Class;Ljava/lang/String;Ljava/nio/file/Path;Ljava/lang/String;)V"))
    private void redirectCopyModDirectory(Class<?> clazz, String path, Path resourcePath, String srcPath) {
        GetJarResources.copyModDirectory(clazz, path, resourcePath, srcPath);
        PreLoadConfig.INSTANCE.load(FMLPaths.CONFIGDIR.get());
        if (srcPath.equals("tacz_default_gun") && PreLoadConfig.INSTANCE.getEnableDefaultStateMachineReplacement().get()) {
            try {
                try (var newFileResource = TACZExpandsClient.class.getResourceAsStream("/assets/taczexpands/custom/tacz_default_gun/assets/tacz/scripts/default_state_machine.lua")) {
                    var filePath = resourcePath.resolve("tacz_default_gun/assets/tacz/scripts/default_state_machine.lua");
                    Files.write(filePath, newFileResource.readAllBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
