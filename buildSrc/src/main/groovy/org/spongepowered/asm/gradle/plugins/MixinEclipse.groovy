

package org.spongepowered.asm.gradle.plugins

import groovy.xml.MarkupBuilder
import org.gradle.api.tasks.InputFiles

import java.util.Collections
import java.util.Enumeration
import java.util.Properties
import java.util.TreeMap

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction


public class MixinEclipse {
    
    
    static class EclipseJdtAptTask extends DefaultTask {

        @InputFile File mappingsIn
        @OutputFile File refmapOut = project.file("build/${name}/mixins.refmap.json")
        @OutputFile File mappingsOut = project.file("build/${name}/mixins.mappings.tsrg")
        @Input Map<String, String> processorOptions = new TreeMap<>()
        
        @OutputFile File genTestDir = project.file('build/.apt_generated_test')
        @OutputFile File genDir = project.file('build/.apt_generated')
        
        @OutputFile File output
        
        @Input boolean hasDiffplug = false;
        
        @TaskAction
        def run() {
            MixinExtension extension = project.extensions.findByType(MixinExtension.class)
            def props = new OrderedProperties()
            if (!hasDiffplug) {
                props.put('eclipse.preferences.version', '1')
                props.put('org.eclipse.jdt.apt.aptEnabled', 'true')
                props.put('org.eclipse.jdt.apt.reconcileEnabled', 'true')
                props.put('org.eclipse.jdt.apt.genSrcDir', genDir.canonicalPath)
                props.put('org.eclipse.jdt.apt.genSrcTestDir', genTestDir.canonicalPath)
            }
            props.arg('reobfTsrgFile', mappingsIn.canonicalPath)
            props.arg('outTsrgFile', mappingsOut.canonicalPath)
            props.arg('outRefMapFile', refmapOut.canonicalPath)
            props.arg('pluginVersion', MixinGradlePlugin.VERSION)
            
            if (extension.disableTargetValidator) {
                props.arg('disableTargetValidator', 'true')
            }
            
            if (extension.disableTargetExport) {
                props.arg('disableTargetExport', 'true')
            }
            
            if (extension.disableOverwriteChecker) {
                props.arg('disableOverwriteChecker', 'true')
            }
            
            if (extension.overwriteErrorLevel != null) {
                props.arg('overwriteErrorLevel', extension.overwriteErrorLevel.toString().trim())
            }
            
            if (extension.defaultObfuscationEnv != null) {
                props.arg('defaultObfuscationEnv', extension.defaultObfuscationEnv)
            }
            
            if (extension.mappingTypes.size() > 0) {
                props.arg('mappingTypes', extension.mappingTypes.join(','))
            }

            if (extension.tokens.size() > 0) {
                props.arg('tokens', extension.tokens.collect { token -> token.key + ' ' + token.value }.join(';'))
            }
            
            if (extension.extraMappings.size() > 0) {
                props.arg('reobfTsrgFiles', extension.extraMappings.collect { file -> project.file(file).toString() }.join(';'))
            }

            
            
            props.store(output.newWriter(hasDiffplug), null);
        }
    }

    
    static class EclipseFactoryPath extends DefaultTask {

        @InputFiles Configuration config
        @OutputFile File output
        
        @TaskAction
        def run() {
            output.withWriter {
                new MarkupBuilder(it).'factorypath' {
                    config.resolvedConfiguration.resolvedArtifacts.each { dep-> factorypathentry( kind: 'EXTJAR', id: dep.file.absolutePath, enabled: true, runInBatchMode: false) }
                }
            }    
        }
    }
    
    
    static class OrderedProperties extends Properties {
        def order = new LinkedHashSet<Object>()
        
        @Override
        public synchronized Enumeration<Object> keys() {
            return Collections.enumeration(order)
        }
        
        @Override
        public synchronized Object put(Object key, Object value) {
            order.add(key)
            return super.put(key, value)
        }
        
        public Object arg(String key, String value) {
            return put('org.eclipse.jdt.apt.processorOptions/' + key, value)
        }
    }

    
    static void configure(MixinExtension extension, Project project) {
        def hasEclipseModel = project.extensions.findByName('eclipse')
        if (!hasEclipseModel) {
            project.logger.lifecycle 'MixinGradle is skipping eclipse integration, util not found'
            return
        }

        def hasEclipseAptPlugin = project.plugins.findPlugin('com.diffplug.eclipse.apt') != null

        def settings = project.tasks.register('mixinEclipseJdtApt', EclipseJdtAptTask.class) {
            inputs.files project.tasks.createSrgToMcp
            inputs.files project.tasks.createMcpToSrg
            description = 'Creates the Eclipse JDT APT settings file'
            output = project.file('.settings/org.eclipse.jdt.apt.core.prefs')
            mappingsIn = extension.mappings
            hasDiffplug = hasEclipseAptPlugin
        }

        def factories = project.tasks.register('mixinEclipseFactorypath', EclipseFactoryPath.class) {
            config = project.configurations.annotationProcessor
            output = project.file('.factorypath')
        }

        if (!hasEclipseAptPlugin) {
                        project.logger.lifecycle 'MixinGradle did not locate the diffplug APT plugin, skipping eclipse task configuration'
            return
        }
        
        project.tasks.eclipseJdtApt.dependsOn(settings)
        project.tasks.eclipseFactorypath.dependsOn(factories)
    }

}
