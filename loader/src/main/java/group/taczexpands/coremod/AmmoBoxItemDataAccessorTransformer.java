package group.taczexpands.coremod;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Set;

public class AmmoBoxItemDataAccessorTransformer implements ITransformer<ClassNode> {
    @Override
    public @NotNull Set<Target> targets() {
        return Set.of(Target.targetClass("com.tacz.guns.api.item.nbt.AmmoBoxItemDataAccessor"));
    }

    @Override
    public @NotNull TransformerVoteResult castVote(ITransformerVotingContext context) {
        if (context.getClassName().equals("com.tacz.guns.api.item.nbt.AmmoBoxItemDataAccessor")) {
            return TransformerVoteResult.YES;
        }
        return TransformerVoteResult.NO;
    }


    @Override
    public @NotNull ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        if (!context.getClassName().equals("com.tacz.guns.api.item.nbt.AmmoBoxItemDataAccessor")) return input;
        input.methods.forEach((method) -> {
            if (method.name.equals("isAmmoBoxOfGun")) {
                method.instructions.clear();
                var insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 2));
                insnList.add(
                        new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "group/taczexpands/common/coremod/PluginWrapper",
                                "isAmmoBoxOfGun",
                                "(Lcom/tacz/guns/api/item/nbt/AmmoBoxItemDataAccessor;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
                                false
                        )
                );
                insnList.add(new InsnNode(Opcodes.IRETURN));
                method.instructions.insert(insnList);
            }
        });
        return input;
    }
}
