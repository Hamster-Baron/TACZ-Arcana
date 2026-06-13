

package org.spongepowered.asm.gradle.plugins.meta

import static org.objectweb.asm.Opcodes.*

import groovy.transform.PackageScope
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class Import {
    
    
    File file
    
    
    List<String> targets = []
    
    
    private boolean generated = false

    Import(File file) {
        this.file = file
    }
    
    
    Import read() {
        if (this.generated) {
            return this
        }
        
        if (file.file) {
            this.readFile()
        }
        
        this.generated = true
        return this
    }
    
    
    @PackageScope void readFile() {
        this.targets.clear()
        
        new ZipInputStream(this.file.newInputStream()).withStream { zin ->
            for (ZipEntry entry = null; (entry = zin.nextEntry) != null;) {
                if (entry.directory || !entry.name.endsWith('.class')) {
                    continue
                }
                
                                MixinScannerVisitor mixin = new MixinScannerVisitor()
                new ClassReader(zin).accept(mixin, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)

                for (String target : mixin.targets) {
                    this.targets.add(sprintf("%s\t%s", mixin.name, target))
                }                
            }
        }
    }
    
    
    Import appendTo(PrintWriter writer) {
        this.read()
        for (String target : this.targets) {
            writer.println(target)
        }
        return this
    }

    
    private static class MixinScannerVisitor extends ClassVisitor {
        
        
        AnnotationNode mixin = null
        
        
        String name

        MixinScannerVisitor() {
            super(ASM5)
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.name = name
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(desc)) {
                return this.mixin = new AnnotationNode(desc)
            }
            super.visitAnnotation(desc, visible)
        }
        
        List<String> getTargets() {
            if (this.mixin == null) {
                return []
            }
            
            List<String> targets = []
            List<Type> publicTargets = this.getAnnotationValue("value");
            List<String> privateTargets = this.getAnnotationValue("targets");
            
            if (publicTargets != null) {
                for (Type type : publicTargets) {
                    targets += type.getClassName().replace(".", "/")
                }
            }
            
            if (privateTargets != null) {
                for (String type : privateTargets) {
                    targets += type.replace(".", "/")
                }
            }
            
            return targets
        }
        
        private <T> T getAnnotationValue(String key) {
            boolean getNextValue = false
    
            if (this.mixin.values == null) {
                return null
            }
    
                                    for (Object value : this.mixin.values) {
                if (getNextValue) {
                    return (T) value
                }
                if (value.equals(key)) {
                    getNextValue = true
                }
            }
    
            return null
        }
    
    }
        
}
