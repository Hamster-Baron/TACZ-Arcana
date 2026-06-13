package group.taczexpands.coremod;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import net.minecraftforge.coremod.api.ASMAPI;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Set;

public class GunItemDataAccessorTransformer implements ITransformer<ClassNode> {
    @Override
    public @NotNull Set<Target> targets() {
        return Set.of(Target.targetClass("com.tacz.guns.api.item.nbt.GunItemDataAccessor"));
    }

    @Override
    public @NotNull TransformerVoteResult castVote(ITransformerVotingContext context) {
        if (context.getClassName().equals("com.tacz.guns.api.item.nbt.GunItemDataAccessor")) {
            return TransformerVoteResult.YES;
        }
        return TransformerVoteResult.NO;
    }


    @Override
    public @NotNull ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        if (!context.getClassName().equals("com.tacz.guns.api.item.nbt.GunItemDataAccessor")) return input;
        input.methods.forEach((method) -> {
            if (method.name.equals("getGunId")) {
                method.instructions.clear();
                var insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                insnList.add(new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "group/taczexpands/common/coremod/PluginWrapper",
                                "getGunId",
                                "(Lcom/tacz/guns/api/item/nbt/GunItemDataAccessor;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;",
                                false
                        )
                );
                insnList.add(new InsnNode(Opcodes.ARETURN));
                method.instructions.insert(insnList);
            }

            if (!method.name.contains("Attachment")) {
                for (var i = 0; i < method.instructions.size(); i++) {
                    var insn = method.instructions.get(i);
                    if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        var invokeInsn = (MethodInsnNode) insn;
                        if (invokeInsn.owner.equals("net/minecraft/world/item/ItemStack")
                                && invokeInsn.name.equals(ASMAPI.mapMethod("m_41784_"))
                                && invokeInsn.desc.equals("()Lnet/minecraft/nbt/CompoundTag;")) {

                            var newInsn = new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "group/taczexpands/common/coremod/PluginWrapper",
                                    "getOrCreateTag",
                                    "(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/nbt/CompoundTag;",
                                    false
                            );

                            method.instructions.set(insn, newInsn);
                        }
                    }
                }
            }

            if (method.name.equals("getAimingZoom")) {
                for (var i = 0; i < method.instructions.size(); i++) {
                    var insn = method.instructions.get(i);
                    if (insn.getOpcode() == Opcodes.ICONST_1) {
                        method.instructions.set(insn, new InsnNode(Opcodes.ICONST_0));
                    }
                }
            }

            if (method.name.equals("getAttachmentTag")) {
                for (var i = 0; i < method.instructions.size(); i++) {
                    var insn = method.instructions.get(i);
                    if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        var invokeInsn = (MethodInsnNode) insn;
                        if (invokeInsn.owner.equals("net/minecraft/world/item/ItemStack")
                                && invokeInsn.name.equals(ASMAPI.mapMethod("m_41784_"))
                                && invokeInsn.desc.equals("()Lnet/minecraft/nbt/CompoundTag;")) {

                            var insnList = new InsnList();
                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 2));
                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 3));
                            insnList.add(new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "group/taczexpands/common/coremod/PluginWrapper",
                                    "getAttachmentTagPreCheck",
                                    "(Lcom/tacz/guns/api/item/nbt/GunItemDataAccessor;Lnet/minecraft/world/item/ItemStack;Lcom/tacz/guns/api/item/attachment/AttachmentType;Lnet/minecraft/nbt/CompoundTag;)V",
                                    false)
                            );

                            method.instructions.insert(method.instructions.get(i + 1), insnList);
                        }
                    }
                }
            }
        });
        return input;
    }
}
