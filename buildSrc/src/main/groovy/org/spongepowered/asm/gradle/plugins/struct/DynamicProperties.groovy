

package org.spongepowered.asm.gradle.plugins.struct


public class DynamicProperties {
    
    
    private final String name
    
    
    private final Map<String, DynamicProperties> properties = [:]
    
    
    private String value
    
    DynamicProperties(String name) {
        this.name = name
    }
    
    
    def propertyMissing(String name) {
        if (!this.properties[name]) {
            this.properties[name] = new DynamicProperties(this.name + '.' + name)
        }
        this.properties[name]
    }
    
    
    def propertyMissing(String name, def value) {
        if (!this.properties[name]) {
            this.properties[name] = new DynamicProperties(this.name + '.' + name)
        }
        this.properties[name].value = value
    }
    
    
    def methodMissing(String name, def args) {
        if (args.length == 1) {
            this.propertyMissing(name, args[0])
        }
    }
    
    
    def getArgs() {
        def args = [:]
        if (this.value) {
            args[this.name] = value 
        }
        this.properties.each {
            args += it.value.args
        }
        return args
    }
    
}
