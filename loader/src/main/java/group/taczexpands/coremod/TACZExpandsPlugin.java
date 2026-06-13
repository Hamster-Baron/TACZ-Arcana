package group.taczexpands.coremod;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class TACZExpandsPlugin implements ITransformationService {
    @Override
    public @NotNull String name() {
        return "taczexpands_coremod";
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {

    }

    @Override
    public @NotNull List<ITransformer> transformers() {
        return List.of(new AmmoBoxItemDataAccessorTransformer(), new AmmoItemDataAccessorTransformer(), new GunItemDataAccessorTransformer(), new TACZTweaksTransformer(), new AttachmentTypeTransformer());
    }
}
