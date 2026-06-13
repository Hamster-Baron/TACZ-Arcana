package group.taczexpands.coremod;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import net.minecraftforge.coremod.api.ASMAPI;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Set;

public class AttachmentTypeTransformer implements ITransformer<ClassNode> {
    @Override
    public @NotNull Set<Target> targets() {
        return Set.of(Target.targetClass("com.tacz.guns.api.item.attachment.AttachmentType"));
    }

    @Override
    public @NotNull TransformerVoteResult castVote(ITransformerVotingContext context) {
        if (context.getClassName().equals("com.tacz.guns.api.item.attachment.AttachmentType")) {
            return TransformerVoteResult.YES;
        }
        return TransformerVoteResult.NO;
    }


    @Override
    public @NotNull ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        if (!context.getClassName().equals("com.tacz.guns.api.item.attachment.AttachmentType")) return input;

        var laser2EnumName = "MODULE";
        var internalClassName = "com/tacz/guns/api/item/attachment/AttachmentType";
        var descriptor = "L" + internalClassName + ";";
        var arrayDescriptor = "[L" + internalClassName + ";";
        int newOrdinal = 0;
        for (FieldNode field : input.fields) {
            if ((field.access & Opcodes.ACC_ENUM) != 0) {
                newOrdinal++;
            }
        }

        int fieldAccess = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM;
        var laser2EnumField = new FieldNode(fieldAccess, laser2EnumName, descriptor, null, null);

        AnnotationNode serializedNameAnno = new AnnotationNode("Lcom/google/gson/annotations/SerializedName;");
        if (serializedNameAnno.values == null) {
            serializedNameAnno.values = new ArrayList<>();
        }
        serializedNameAnno.values.add("value");
        serializedNameAnno.values.add("module");

        if (laser2EnumField.visibleAnnotations == null) {
            laser2EnumField.visibleAnnotations = new ArrayList<>();
        }
        laser2EnumField.visibleAnnotations.add(serializedNameAnno);

        input.fields.add(laser2EnumField);

        MethodNode clinit = null;
        for (MethodNode method : input.methods) {
            if (method.name.equals("<clinit>")) {
                clinit = method;
                break;
            }
        }

        if (clinit == null) {
            return input;
        }

        InsnList initInsns = new InsnList();
        initInsns.add(new TypeInsnNode(Opcodes.NEW, internalClassName));
        initInsns.add(new InsnNode(Opcodes.DUP));
        initInsns.add(new LdcInsnNode(laser2EnumName));
        initInsns.add(new LdcInsnNode(newOrdinal));
        initInsns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, internalClassName, "<init>", "(Ljava/lang/String;I)V", false));
        initInsns.add(new FieldInsnNode(Opcodes.PUTSTATIC, internalClassName, laser2EnumName, descriptor));
        clinit.instructions.insert(initInsns);

        {
            ListIterator<AbstractInsnNode> iterator = clinit.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insn = iterator.next();

                if (insn.getOpcode() == Opcodes.PUTSTATIC) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    if (fieldInsn.owner.equals(internalClassName) && fieldInsn.name.equals("$VALUES") && fieldInsn.desc.equals(arrayDescriptor)) {

                        int oldArraySlot = clinit.maxLocals;
                        int newArraySlot = clinit.maxLocals + 1;
                        clinit.maxLocals += 2;

                        InsnList expansionInsns = new InsnList();

                        expansionInsns.add(new VarInsnNode(Opcodes.ASTORE, oldArraySlot));

                        expansionInsns.add(new LdcInsnNode(newOrdinal + 1));
                        expansionInsns.add(new TypeInsnNode(Opcodes.ANEWARRAY, internalClassName));
                        expansionInsns.add(new VarInsnNode(Opcodes.ASTORE, newArraySlot));

                        expansionInsns.add(new VarInsnNode(Opcodes.ALOAD, oldArraySlot));
                        expansionInsns.add(new LdcInsnNode(0));
                        expansionInsns.add(new VarInsnNode(Opcodes.ALOAD, newArraySlot));
                        expansionInsns.add(new LdcInsnNode(0));
                        expansionInsns.add(new LdcInsnNode(newOrdinal));
                        expansionInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false));

                        expansionInsns.add(new VarInsnNode(Opcodes.ALOAD, newArraySlot));
                        expansionInsns.add(new LdcInsnNode(newOrdinal));
                        expansionInsns.add(new FieldInsnNode(Opcodes.GETSTATIC, internalClassName, laser2EnumName, descriptor));
                        expansionInsns.add(new InsnNode(Opcodes.AASTORE));

                        expansionInsns.add(new VarInsnNode(Opcodes.ALOAD, newArraySlot));

                        clinit.instructions.insertBefore(insn, expansionInsns);

                        break;
                    }
                }
            }
        }

        MethodNode valuesMethod = null;
        for (MethodNode method : input.methods) {
            if (method.name.equals("$values")) {
                valuesMethod = method;
                break;
            }
        }

        if (valuesMethod != null) {
            ListIterator<AbstractInsnNode> iterator = valuesMethod.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insn = iterator.next();

                if (insn.getOpcode() == Opcodes.ARETURN) {
                    int oldArraySlot = valuesMethod.maxLocals;
                    int newArraySlot = valuesMethod.maxLocals + 1;
                    valuesMethod.maxLocals += 2;

                    InsnList expansionInsns = new InsnList();

                    expansionInsns.add(new VarInsnNode(Opcodes.ASTORE, oldArraySlot));

                    expansionInsns.add(new LdcInsnNode(8));
                    expansionInsns.add(new TypeInsnNode(Opcodes.ANEWARRAY, internalClassName));
                    expansionInsns.add(new VarInsnNode(Opcodes.ASTORE, newArraySlot));

                    expansionInsns.add(new VarInsnNode(Opcodes.ALOAD, oldArraySlot));
                    expansionInsns.add(new LdcInsnNode(0));
                    expansionInsns.add(new VarInsnNode(Opcodes.ALOAD, newArraySlot));
                    expansionInsns.add(new LdcInsnNode(0));
                    expansionInsns.add(new LdcInsnNode(7));
                    expansionInsns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false));

                    expansionInsns.add(new VarInsnNode(Opcodes.ALOAD, newArraySlot));
                    expansionInsns.add(new LdcInsnNode(7));
                    expansionInsns.add(new FieldInsnNode(Opcodes.GETSTATIC, internalClassName, laser2EnumName, descriptor));
                    expansionInsns.add(new InsnNode(Opcodes.AASTORE));

                    expansionInsns.add(new VarInsnNode(Opcodes.ALOAD, newArraySlot));

                    valuesMethod.instructions.insertBefore(insn, expansionInsns);
                    break;
                }
            }
        }

        return input;
    }
}
