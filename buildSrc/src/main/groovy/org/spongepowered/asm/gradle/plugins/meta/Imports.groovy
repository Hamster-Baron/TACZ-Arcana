

package org.spongepowered.asm.gradle.plugins.meta


class Imports {
    
    
    private static Map<File, Import> imports = [:]

    
    static Import getAt(File file) {
        Import imp = Imports.imports.get(file)
        if (imp == null) {
            imp = new Import(file).read()
            Imports.imports.put(file, imp)
        }
        return imp
    }
    
}
