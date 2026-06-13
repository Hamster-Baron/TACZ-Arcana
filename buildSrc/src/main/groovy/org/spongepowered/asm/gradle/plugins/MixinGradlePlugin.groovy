

package org.spongepowered.asm.gradle.plugins

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.tasks.SourceSet
import org.gradle.util.VersionNumber

import javax.inject.Inject


public class MixinGradlePlugin implements Plugin<Project> {
    
    
    static final String VERSION = getCurrentVersion() ?: '0.7'

    
    @Override
    void apply(Project project) {
                this.checkEnvironment(project)
        
                project.extensions.create('mixin', MixinExtension.class, project)
    }
    
    
    private void checkEnvironment(Project project) {
        if (project.tasks.findByName('genSrgs')) {
            throw new InvalidUserDataException("Found a 'genSrgs' task on $project, this version of MixinGradle does not support ForgeGradle 2.x.")
        }

        if (!project.extensions.findByName('minecraft') && !project.extensions.findByName('patcher')) {
            throw new InvalidUserDataException("Could not find property 'minecraft', or 'patcher' on $project, ensure ForgeGradle is applied.")
        }
    }
    
    private static String getCurrentVersion() {
        try {
            def versionMatch = Class.forName('org.spongepowered.asm.gradle.plugins.MixinGradlePlugin').package.implementationVersion =~ /^(\d+\.\d+(\.\d+))/
            return versionMatch ? VersionNumber.parse(versionMatch[0][1]) : null
        } catch (Throwable th) {
                    }
    }
    
}
