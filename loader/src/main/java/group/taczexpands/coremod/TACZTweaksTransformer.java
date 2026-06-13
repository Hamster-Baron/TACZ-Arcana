package group.taczexpands.coremod;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import net.minecraftforge.coremod.api.ASMAPI;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Set;

public class TACZTweaksTransformer implements ITransformer<ClassNode> {
    @Override
    public @NotNull Set<Target> targets() {
        return Set.of(
                Target.targetClass("me.muksc.tacztweaks.mixin.features.EntityKineticBulletMixin"),
                Target.targetClass("me.muksc.tacztweaks.mixin.features.ModernKineticGunScriptAPIMixin")
        );
    }

    @Override
    public @NotNull TransformerVoteResult castVote(ITransformerVotingContext context) {
        var className = context.getClassName();
        if (className.equals("me.muksc.tacztweaks.mixin.features.EntityKineticBulletMixin")
                || className.equals("me.muksc.tacztweaks.mixin.features.ModernKineticGunScriptAPIMixin")) {
            return TransformerVoteResult.YES;
        }
        return TransformerVoteResult.NO;
    }


    @Override
    public @NotNull ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        var className = context.getClassName();
        if (className.equals("me.muksc.tacztweaks.mixin.features.EntityKineticBulletMixin")) {
            input.methods.forEach((method) -> {
                if (method.name.equals("tacztweaks$onBulletTick$finishRayTracing")) {
                    method.instructions.clear();
                    var insnList = new InsnList();
                    insnList.add(new InsnNode(Opcodes.RETURN));
                    method.instructions.insert(insnList);
                }
            });
        } else if (className.equals("me.muksc.tacztweaks.mixin.features.ModernKineticGunScriptAPIMixin")) {
            input.methods.forEach((method) -> {
                if (method.name.equals("tacztweaks$shootOnce$apply")) {
                    method.visibleAnnotations.stream().filter(annotationNode -> "Lorg/spongepowered/asm/mixin/injection/ModifyArg;".equals(annotationNode.desc))
                            .findFirst()
                            .ifPresent(annotation -> {
                                if (annotation.values != null) {
                                    for (int i = 0; i < annotation.values.size(); i += 2) {
                                        Object name = annotation.values.get(i);
                                        if ("index".equals(name)) {
                                            annotation.values.set(i, "ordinal");
                                            break;
                                        }
                                    }
                                }
                            });
                }
            });
        }
        return input;
    }
}
