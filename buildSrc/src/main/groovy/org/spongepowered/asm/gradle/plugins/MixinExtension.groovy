

package org.spongepowered.asm.gradle.plugins

import com.google.common.io.Files
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.util.VersionNumber
import org.spongepowered.asm.gradle.plugins.meta.Imports
import org.spongepowered.asm.gradle.plugins.struct.DynamicProperties
import java.util.Map.Entry


public class MixinExtension {
    
    
    static abstract class ConfigureReobfTask extends DefaultTask {
        
        
        @Internal
        Task reobfTask
        
        
        @Internal
        Set<File> mappingFiles = []
        
        ConfigureReobfTask() {
                        outputs.upToDateWhen { false }
        }
        
        @TaskAction
        def run() {
            for (File mappingFile : this.mappingFiles) {
                if (mappingFile.exists()) {
                    this.project.logger.info "Contributing tsrg mappings ({}) to {} in {}", mappingFile, reobfTask.name, reobfTask.project
                    this.addMappingFile(mappingFile)
                } else {
                    this.project.logger.debug "Tsrg file ({}) not found, skipping", mappingFile
                }
            }
        }

        
        protected abstract void addMappingFile(File mappingFile);
    
    }
    
    
    static class ConfigureReobfTaskForUserDev extends ConfigureReobfTask {
        
        @Override
        protected void addMappingFile(File mappingFile) {
            def extraMappings = this.reobfTask.properties.extraMappings
            
                        if (extraMappings instanceof ConfigurableFileCollection) {
                extraMappings.from(mappingFile)
                return
            }
            
                        this.reobfTask.extraMapping(mappingFile)
        }
        
    }

    
    static class ConfigureReobfTaskForPatcher extends ConfigureReobfTask {

        @Override
        protected void addMappingFile(File mappingFile) {
            def add = [
                '--srg-in', mappingFile.absolutePath
            ]
            if (this.reobfTask.args instanceof ListProperty) {
                this.reobfTask.args.addAll(add)
            } else {
                this.reobfTask.args += add
            }
        }

    }
    
    
    static class ArtefactSpecificRefmap extends File {
        
        
        File refMap
        
        ArtefactSpecificRefmap(File parent, String refMap) {
            super(parent, refMap)
            this.refMap = new File(refMap)
        }

    }

    
    static class AddMixinsToJarTask extends DefaultTask {
        
        @Internal
        MixinExtension extension

        @Input
        Jar remappedJar

        @Input
        Set<Task> reobfTasks

        @InputFiles
        Set<File> jarRefMaps = []
        
        @TaskAction
        def run() {
                        this.reobfTasks.each { reobfTask ->
                def jarTasks = reobfTask.dependsOn.findAll { it == remappedJar }.toSet()
                                                try {
                    if (reobfTask.input instanceof org.gradle.api.internal.provider.ValueSupplier) {
                        reobfTask.input.producer.visitProducerTasks {
                            if (it == remappedJar) {
                                jarTasks.add it
                            }
                        }
                    }
                } catch (Throwable e) {
                                    }
                jarTasks.each { jar ->
                    jarRefMaps.each { artefactSpecificRefMap ->
                        project.logger.info "Contributing refmap ({}) to {} in {}", artefactSpecificRefMap.refMap, jar.hasProperty('archiveFileName') ? jar.archiveFileName.get() : jar.archiveName, reobfTask.project
                        jar.getRefMaps().from(artefactSpecificRefMap)
                        jar.from(artefactSpecificRefMap) {
                            into artefactSpecificRefMap.refMap.parent
                        }
                    }
                    if (this.extension.configNames && !jar.manifest.attributes.containsKey("MixinConfigs")) {
                        def configNamesCsv = this.extension.configNames.join(',')
                        project.logger.info "Contributing configs ({}) to manifest of {} in {}", configNamesCsv, jar.hasProperty('archiveFileName') ? jar.archiveFileName.get() : jar.archiveName, reobfTask.project
                        jar.manifest.attributes['MixinConfigs'] = configNamesCsv
                    }
                }
            }
        }
    }

    
    @PackageScope final Project project

    
    @PackageScope final String projectType
    
    
    @PackageScope final int majorGradleVersion
    
    
    @PackageScope VersionNumber mixinVersionForErrors = null

    
    private boolean applyDefault = true
    
    
    private Set<SourceSet> sourceSets = []
    
    
    private Map<String, String> refMaps = [:]
    
    
    private Map<String, String> tokens = [:]
    
    
    @PackageScope DynamicProperties systemProperties = new DynamicProperties("mixin");
    
    
    @PackageScope Set<Task> reobfTasks = []
    
    
    @PackageScope Set<AddMixinsToJarTask> addMixinsToJarTasks = []
    
    
    @PackageScope Set<String> configNames = []
    
    
    HashMap<String, String> messages = [:]

    
    boolean disableRefMapWarning
    
    
    boolean disableTargetValidator
    
    
    boolean disableTargetExport
    
    
    boolean disableEclipseAddon
    
    
    boolean disableOverwriteChecker
    
    
    boolean disableAnnotationProcessorCheck
    
    
    Object overwriteErrorLevel
    
    
    String defaultObfuscationEnv = "searge"
    
    
    List<Object> mappingTypes = [ "tsrg" ]

    
    Object reobfSrgFile
    
    
    boolean quiet
    
    
    boolean showMessageTypes
    
    
    @PackageScope List<Object> extraMappings = []
    
    
    private Set<Object> importConfigs = []
    
    
    private Set<Object> importLibs = []

    
    MixinExtension(Project project) {
        this.project = project
        this.majorGradleVersion = MixinExtension.detectGradleMajorVersion(project)
        
        if (project.extensions.findByName('minecraft')) {
            this.projectType = 'userdev'
        } else if (project.extensions.findByName('patcher')) {
            this.projectType = 'patcher'
        } else {
            throw new InvalidUserDataException("Could not find property 'minecraft', or 'patcher' on $project, ensure ForgeGradle is applied.")
        }
        
        if (!this.disableEclipseAddon) {
            MixinEclipse.configure(this, this.project)
        }
        
        this.init(this.project, this.projectType)
    }
    
    
    private void init(Project project, String projectType) {
        this.project.afterEvaluate {
                        if (projectType == 'userdev') {
                                project.reobf.each { reobfTaskHandle ->
                    this.reobfTasks += reobfTaskHandle
                }
            } else if (projectType == 'patcher') {
                                this.reobfTasks += project.reobfJar
            }

                        project.sourceSets.each { set ->
                if (set.ext.has("refMap")) {
                    this.configure(set, projectType)
                }
            }

                        def configuration = majorGradleVersion >= 7 ? project.configurations.implementation : project.configurations.compile
            configuration.allDependencies.withType(ProjectDependency) { upstream ->
                def mixinExt = upstream.dependencyProject.extensions.findByName("mixin")
                if (mixinExt) {
                    project.reobf.each { reobfTaskWrapper ->
                        mixinExt.reobfTasks += reobfTaskWrapper
                    }
                }
            }
            
            this.applyDefault()
            this.configureRuns()
        }

        SourceSet.metaClass.getRefMap = {
            delegate.ext.refMap
        }
        
        SourceSet.metaClass.setRefMap = { value ->
            delegate.ext.refMap = value
        }
        
        AbstractArchiveTask.metaClass.getRefMaps = {
            if (!delegate.ext.has('refMaps')) {
                delegate.ext.refMaps = majorGradleVersion >= 7 ? project.objects.fileCollection() : project.layout.configurableFiles()
            }
            delegate.ext.refMaps
        }
        
        AbstractArchiveTask.metaClass.setRefMaps = { value ->
            delegate.ext.refMaps = value
        }
    }
    
    
    void disableRefMapWarning() {
        this.disableRefMapWarning = true
    }
    
    
    void disableTargetValidator() {
        this.disableTargetValidator = true
    }
    
    
    void disableTargetExport() {
        this.disableTargetExport = true
    }
    
    
    void disableEclipseAddon() {
        this.disableEclipseAddon = true
    }
    
    
    void disableOverwriteChecker() {
        this.disableOverwriteChecker = true
    }
    
    
    void disableAnnotationProcessorCheck() {
        this.disableAnnotationProcessorCheck = true
    }
    
    
    void quiet() {
        quiet = true
    }
    
    
    void showMessageTypes() {
        showMessageTypes = true
    }
    
    
    void overwriteErrorLevel(Object errorLevel) {
        this.overwriteErrorLevel = errorLevel
    }
    
    
    Object getMappings() {
        if (this.reobfSrgFile != null) {
            return project.file(this.reobfSrgFile)
        } else if (this.projectType == 'userdev') {
            return project.tasks.createMcpToSrg.outputs.files[0]
        } else if (this.projectType == 'patcher') {
            return project.tasks.createMcp2Srg.outputs.files[0]
        }
        return null
    }
    
    
    void extraMappings(Object file) {
        this.extraMappings += file
    }
    
    
    void token(Object name) {
        token(name, "true")
    }
    
    
    void token(Object name, Object value) {
        this.tokens.put(name.toString().trim(), value.toString().trim())
    }
    
    
    MixinExtension tokens(Map<String, ?> map) {
        for (Entry<String, ?> entry : map) {
            this.tokens.put(entry.key.trim(), entry.value.toString().trim())
        }
    }
    
    
    Map<String, String> getTokens() {
        Collections.unmodifiableMap(this.tokens)
    }
    
    
    @PackageScope void checkTokens() {
        this.tokens.find { it.value.contains(';') }.each {
            throw new InvalidUserDataException("Invalid token value '${it.value}' for token '${it.key}'")
        }
    }
    
    
    @PackageScope void applyDefault() {
        if (this.applyDefault) {
            this.applyDefault = false
            project.logger.info "No sourceSets added for mixin processing, applying defaults"
            this.disableRefMapWarning = true
            project.sourceSets.each { set ->
                if (!set.ext.has("refMap")) {
                    set.ext.refMap = "mixin.refmap.json"
                }
                this.configure(set, this.projectType)
            }
        }
    }
    
    
    @PackageScope void configureRuns() {
        if (this.projectType == 'userdev') {
            project.extensions.minecraft.runs.each { runConfig ->
                if (project.tasks.findByName('createSrgToMcp')) {
                    def srgToMcpFile = project.tasks.createSrgToMcp.outputs.files[0].path
                    
                                        runConfig.property 'net.minecraftforge.gradle.GradleStart.srg.srg-mcp', srgToMcpFile
                    
                                                                                                    if (!runConfig.properties.containsKey('mixin.env.remapRefMap')) {
                        runConfig.property 'mixin.env.remapRefMap', 'true'
                        runConfig.property 'mixin.env.refMapRemappingFile', srgToMcpFile
                    }
                }
                
                this.configNames.each { configName ->
                    runConfig.args '--mixin.config', configName
                } 
                
                this.systemProperties.args.each {
                    runConfig.property it.key, it.value.toString()
                }
            }
        }
    }
    
    
    @PackageScope void checkForAnnotationProcessors() {
        if (this.disableAnnotationProcessorCheck || (this.majorGradleVersion < 5 && this.majorGradleVersion > 0)) {
            return
        }
            
        def missingAPs = this.findMissingAnnotationProcessors()
        if (missingAPs) {
            def gradleVersion = this.majorGradleVersion > 4 ? "Gradle ${this.majorGradleVersion} " : "An unrecognised gradle version "
            def missingAPNames = missingAPs.collect { it.annotationProcessorConfigurationName }
            def addAPName = missingAPNames.size() > 1 ? '<configurationName>' : missingAPNames[0]
            def eachOfThese = missingAPNames.size() > 1 ? " where <configurationName> is each of $missingAPNames." : ''
            def mixinVersion = this.mixinVersionForErrors ?: '0.1.2-SNAPSHOT' 
            def message = "$gradleVersion was detected but the mixin dependency was missing from one or more Annotation Processor " +
                "configurations: $missingAPNames. To enable the Mixin AP please include the mixin processor artefact in each Annotation " +
                "Processor configuration. For example if you are using mixin dependency 'org.spongepowered:mixin:$mixinVersion' you " + 
                "should specify: dependencies { $addAPName 'org.spongepowered:mixin:$mixinVersion:processor' }$eachOfThese. If you " +
                "believe you are seeing this message in error, you can disable this check via by adding disableAnnotationProcessorCheck() " +
                "to your mixin { } block."
                
                        if (this.majorGradleVersion >= 5) {
                throw new MixinGradleException(message)
            } else {
                this.project.logger.error message
            }
        }
    }
    
    
    @PackageScope Set<SourceSet> findMissingAnnotationProcessors() {
        Set<SourceSet> missingAPs = []
        missingAPs += this.sourceSets.findResults { SourceSet sourceSet ->
            sourceSet.ext.mixinDependency = majorGradleVersion >= 7
                    ? this.findMixinDependency(sourceSet.implementationConfigurationName)
                    : (this.findMixinDependency(sourceSet.compileConfigurationName) ?: this.findMixinDependency(sourceSet.implementationConfigurationName))
            if (sourceSet.ext.mixinDependency) {
                VersionNumber mainVersion = this.getDependencyVersion(sourceSet.ext.mixinDependency)
                if (mainVersion > this.mixinVersionForErrors) {
                    this.mixinVersionForErrors = mainVersion
                } 
                sourceSet.ext.apDependency = this.findMixinDependency(sourceSet.annotationProcessorConfigurationName)
                if (sourceSet.ext.apDependency) {
                    VersionNumber apVersion = this.getDependencyVersion(sourceSet.ext.apDependency)
                    if (mainVersion > apVersion) {
                        this.project.logger.warn "Mixin AP version ($apVersion) in configuration '${sourceSet.annotationProcessorConfigurationName}' is older than compile version ($mainVersion)"
                    }
                } else {
                    return sourceSet
                }
            }
        }
        return missingAPs
    }
    
    
    @PackageScope def findMixinDependency(String configurationName) {
        def configuration = project.configurations[configurationName]
        return configuration.canBeResolved
            ? configuration.resolvedConfiguration.resolvedArtifacts.find { it.id =~ /:mixin:/ }
            : configuration.allDependencies.find { it.group =~ /spongepowered/ && it.name =~ /mixin/ }
    }
    
    
    @PackageScope VersionNumber getDependencyVersion(def dependency) {
        if (dependency instanceof ResolvedArtifact) {
            return VersionNumber.parse(dependency.moduleVersion.id.version)
        } else if (dependency instanceof Dependency) {
            return VersionNumber.parse(dependency.version)
        }
    }
    
    
    void config(String path) {
        this.configNames += path
    }
    
    
    def getDebug() {
        return this.systemProperties.debug
    }
    
    
    def setDebug(def value) {
        this.systemProperties.debug = value
    }
    
    
    def getChecks() {
        return this.systemProperties.checks
    }

    
    def setChecks(def value) {
        this.systemProperties.checks = value
    }
    
    
    def getDumpTargetOnFailure() {
        return this.systemProperties.dumpTargetOnFailure
    }
    
    
    def setDumpTargetOnFailure(def value) {
        this.systemProperties.dumpTargetOnFailure = value
    }
    
    
    def getIgnoreConstraints() {
        return this.systemProperties.ignoreConstraints
    }
    
    
    def setIgnoreConstraints(def value) {
        this.systemProperties.ignoreConstraints = value
    }
    
    
    def getHotSwap() {
        return this.systemProperties.hotSwap
    }
    
    
    def setHotSwap(def value) {
        this.systemProperties.hotSwap = value
    }
    
    
    def getEnv() {
        return this.systemProperties.env
    }
    
    
    def setEnv(def value) {
        this.systemProperties.env = value
    }
    
    
    def getInitialiserInjectionMode() {
        return this.systemProperties.initialiserInjectionMode
    }
    
    
    def setInitialiserInjectionMode(def mode) {
        this.systemProperties.initialiserInjectionMode = mode
    }
    
    def messages(Closure closure) {
        closure.delegate = this.messages
        closure()
    }
    
    
    void add(String set) {
        this.add(project.sourceSets[set])
    }
    
    
    void add(SourceSet set) {
        try {
            set.getRefMap()
        } catch (e) {
            throw new InvalidUserDataException(sprintf('No \'refMap\' or \'ext.refMap\' defined on %s. Call \'add(sourceSet, refMapName)\' instead.', set))
        }
        this.manuallyAdd(set)
    }
    
    
    void add(String set, Object refMapName) {
        SourceSet sourceSet = project.sourceSets.findByName(set)
        if (sourceSet == null) {
            throw new InvalidUserDataException(sprintf('No sourceSet \'%s\' was found', set))
        }
        sourceSet.ext.refMap = refMapName
        this.manuallyAdd(sourceSet)
    }
    
    
    void add(SourceSet set, Object refMapName) {
        set.ext.refMap = refMapName.toString()
        this.manuallyAdd(set)
    }
    
    
    void manuallyAdd(SourceSet set) {
                this.applyDefault = false
        String pType = this.projectType
        project.afterEvaluate {
            this.configure(set, pType)
        }
    }
    
    
    void configure(SourceSet set, String projectType) {
                if (!this.sourceSets.add(set)) {
            project.logger.info "Not adding {} to mixin processor, sourceSet already added", set
            return
        }

        project.logger.info "Adding {} to mixin processor", set
        
                def compileTask = project.tasks[set.compileJavaTaskName]
        if (!(compileTask instanceof JavaCompile)) {
            throw new InvalidUserDataException(sprintf('Cannot add non-java %s to mixin processor', set))
        }
        
                this.applyDefault = false

                def refMaps = this.refMaps
        
                def refMapFile = project.file("${compileTask.temporaryDir}/${compileTask.name}-refmap.json")
        
                def tsrgFile = project.file("${compileTask.temporaryDir}/${compileTask.name}-mappings.tsrg")
        
                        compileTask.ext.outTsrgFile = tsrgFile
        compileTask.ext.refMapFile = refMapFile
        set.ext.refMapFile = refMapFile
        compileTask.ext.refMap = set.ext.refMap.toString()

                        if (this.projectType == 'userdev') {
            compileTask.dependsOn("createMcpToSrg")
        } else if (this.projectType == 'patcher') {
            compileTask.dependsOn("createMcp2Srg")
        }
        
                compileTask.doFirst {
            if (!this.disableRefMapWarning && refMaps[compileTask.ext.refMap]) {
                project.logger.warn "Potential refmap conflict. Duplicate refmap name {} specified for sourceSet {}, already defined for sourceSet {}",
                    compileTask.ext.refMap, set.name, refMaps[compileTask.ext.refMap]
            } else {
                refMaps[compileTask.ext.refMap] = set.name
            }
            
            refMapFile.delete()
            tsrgFile.delete()
            
            this.checkTokens()
            this.applyCompilerArgs(compileTask)
        }

                                        File taskSpecificRefMap = new ArtefactSpecificRefmap(refMapFile.parentFile, compileTask.ext.refMap)

                        compileTask.doLast {
                        taskSpecificRefMap.delete()

                        if (refMapFile.exists()) {
                taskSpecificRefMap.parentFile.mkdirs()
                Files.copy(refMapFile, taskSpecificRefMap) 
            }
        }
        
                                                project.tasks.withType(Jar.class) { jarTask ->
            this.addMixinsToJarTasks.add(project.tasks.maybeCreate("addMixinsTo${jarTask.name.capitalize()}", AddMixinsToJarTask.class).configure {
                doFirst {
                    this.checkForAnnotationProcessors()
                }
                extension = this
                dependsOn(compileTask)
                remappedJar = jarTask
                reobfTasks = this.reobfTasks
                jarRefMaps += taskSpecificRefMap
                jarTask.dependsOn(delegate)
            })
            if (projectType == 'patcher') {                 if ('universalJar' == jarTask.name) {
                    project.logger.info "Contributing refmap ({}) to {} in {}", taskSpecificRefMap, jarTask.hasProperty('archiveFileName') ? jarTask.archiveFileName.get() : jarTask.archiveName, project
                    jarTask.getRefMaps().from(taskSpecificRefMap)
                    jarTask.from(taskSpecificRefMap)
                }
            }
        }

                this.reobfTasks.each { reobfTask ->
                                                def configureReobfTaskType = this.projectType == 'patcher' ? ConfigureReobfTaskForPatcher.class : this.projectType == 'userdev' ? ConfigureReobfTaskForUserDev.class : null
            if (configureReobfTaskType != null) {
                def configureReobfTaskTask = project.tasks.maybeCreate("configureReobfTaskFor${reobfTask.name.capitalize()}", configureReobfTaskType).configure {
                    delegate.reobfTask = reobfTask
                    mappingFiles += tsrgFile
                                        dependsOn(compileTask)
                }
                reobfTask.dependsOn configureReobfTaskTask
            }
        }
    }
    
    
    @PackageScope void applyCompilerArgs(JavaCompile compileTask) {
        compileTask.options.compilerArgs += [
            "-AreobfTsrgFile=${this.mappings.canonicalPath}",
            "-AoutTsrgFile=${compileTask.outTsrgFile.canonicalPath}",
            "-AoutRefMapFile=${compileTask.refMapFile.canonicalPath}",
            "-AmappingTypes=tsrg",
            "-ApluginVersion=${MixinGradlePlugin.VERSION}"
        ]
        
        if (this.disableTargetValidator) {
            compileTask.options.compilerArgs += '-AdisableTargetValidator=true'
        }
        
        if (this.disableTargetExport) {
            compileTask.options.compilerArgs += '-AdisableTargetExport=true'
        }
        
        if (this.disableOverwriteChecker) {
            compileTask.options.compilerArgs += '-AdisableOverwriteChecker=true'
        }
        
        if (this.overwriteErrorLevel != null) {
            compileTask.options.compilerArgs += '-AoverwriteErrorLevel=${this.overwriteErrorLevel.toString().trim()}'
        }
        
        if (this.defaultObfuscationEnv != null) {
            compileTask.options.compilerArgs += "-AdefaultObfuscationEnv=${this.defaultObfuscationEnv}"
        }
        
        if (this.mappingTypes.size() > 0) {
            compileTask.options.compilerArgs += listToArg("mappingTypes", this.mappingTypes, ",")
        }

        if (this.tokens.size() > 0) {
            compileTask.options.compilerArgs += mapToArg("tokens", this.tokens)
        }
        
        if (this.extraMappings.size() > 0) {
            compileTask.options.compilerArgs += listToArg("reobfTsrgFiles", this.extraMappings.collect { file -> this.project.file(file).toString() })
        }

        File importsFile = this.generateImportsFile(compileTask)
        if (importsFile != null) {
            compileTask.options.compilerArgs += "-AdependencyTargetsFile=${importsFile.canonicalPath}"
        }
        
        if (this.quiet) {
            compileTask.options.compilerArgs += '-Aquiet=true'
        }
        
        if (this.showMessageTypes) {
            compileTask.options.compilerArgs += '-AshowMessageTypes=true'
        }
        
        this.messages.each { property, level ->
            if (property =~ /^[A-Z]+[A-Z_]+$/ && level =~ /^(note|warning|error|disabled)$/) {
                compileTask.options.compilerArgs += "-AMSG_$property=$level"
            }
        }
    }
    
    void importConfig(Object config) {
        if (config == null) {
            throw new InvalidUserDataException("Cannot import from null config")
        }
        this.importConfigs += config
    }
    
    void importLibrary(Object lib) {
        if (lib == null) {
            throw new InvalidUserDataException("Cannot import null library")
        }
        this.importLibs += lib
    }

    
    private File generateImportsFile(JavaCompile compileTask) {
        File importsFile = new File(compileTask.temporaryDir, "mixin.imports.json")
        importsFile.delete()
        
        Set<File> libs = []
        
        for (Object cfg : this.importConfigs) {
            def config = (cfg instanceof Configuration) ? cfg : project.configurations.findByName(cfg.toString())
            if (config != null) {
                for (File file : config.files) {
                    libs += file
                }
            }
        }

        for (Object lib : this.importLibs) {
            libs += project.file(lib)
        }

        if (libs.size() == 0) {
            return null
        }
        
        importsFile.newOutputStream().withStream { stream ->
            PrintWriter writer = new PrintWriter(stream)
            for (File lib : libs) {
                Imports[lib].appendTo(writer)
            }
            writer.flush()
        }
        
        return importsFile
    }

        
    @PackageScope static String mapToArg(String argName, Map<?, ?> map, String separator = ";") {
        map.size() < 1 ? "" : "-A${argName}=" + map.collect { token -> token.key << "=" << token.value }.join(separator)
    }
    
    @PackageScope static String listToArg(String argName, List<Object> list, String separator = ";") {
        list.size() < 1 ? "" : "-A${argName}=${list.join(separator)}"
    }
    
    private static int detectGradleMajorVersion(Project project) {
        def strMajorVersion = (project.gradle.gradleVersion =~ /^([0-9]+)\./).findAll()[0][1]
        return strMajorVersion.isInteger() ? strMajorVersion as Integer : 0
    } 
}
