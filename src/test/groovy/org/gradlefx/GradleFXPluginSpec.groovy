package org.gradlefx

import spock.lang.Specification
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import org.gradlefx.conventions.GradleFxConvention
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
import org.gradle.api.artifacts.Configuration

/**
 * Created by IntelliJ IDEA.
 * User: irichter
 * Date: 9/6/11
 * Time: 11:30 PM
 * To change this template use File | Settings | File Templates.
 */
class GradleFXPluginSpec extends Specification {
    Project project
    def setup() {
        project = ProjectBuilder.builder().build()
    }

    def "adds convention to root project"() {
        when:
        project.plugins.apply(GradleFxPlugin)

        then:
        project.convention.plugins.flex instanceof GradleFxConvention
    }

    def "add configurations to root project"() {
        when:
        project.plugins.apply(GradleFxPlugin)

        then:
        project.configurations.getAll().size() == 7
        project.configurations.findByName('default')
        project.configurations.findByName('internal')
        project.configurations.findByName('external')
        project.configurations.findByName('merged')
        project.configurations.findByName('rsl')
        project.configurations.findByName('test')
        project.configurations.findByName('flexSDK')
    }

    def "add tasks to root project"() {
        when:
        project.plugins.apply(GradleFxPlugin)

        then:
        project.tasks.getAll().size() == 6
        project.tasks.findByName('build')
    }

    def "FLEXHOME is provided"(Project project) {
        GradleFxPlugin plugin = project.plugins.getPlugin(GradleFxPlugin)

        expect:
        plugin.isFlexHomeProvided()

        where:
        project << createProjectWithFlexHome()
    }

    def "FLEXHOME is not provided"(Project project) {
        GradleFxPlugin plugin = project.plugins.getPlugin(GradleFxPlugin)

        expect:
        ! plugin.isFlexHomeProvided()

        where:
        project << createProjectWithoutFlexHome()
    }

    def "FlexOSS repository should be available to project"() {
        when:
        project.plugins.apply(GradleFxPlugin)

        then:
        project.repositories.getAll().size() == 2
        project.repositories.findByName('FlexOSS')
    }

    def createProjectWithFlexHome() {
        Project project = createProjectWithoutFlexHome()
        project.flexHome = 'test/flexsdk'

        project
    }

    def createProjectWithoutFlexHome() {
        Project project = ProjectBuilder.builder().build()

        project.plugins.apply(GradleFxPlugin)

        project
    }
//    def ""
//    @Rule
//    public TemporaryFolder tmpDir = new TemporaryFolder()
//    private final Project project = HelperUtil.createRootProject()
//    private final JavaPlugin javaPlugin = new JavaPlugin()
//
//    @Test public void appliesBasePluginsAndAddsConventionObject() {
//        javaPlugin.apply(project)
//
//        assertThat(project.convention.plugins.embeddedJavaProject, instanceOf(EmbeddableJavaProject))
//        assertThat(project.convention.plugins.embeddedJavaProject.rebuildTasks, equalTo([BasePlugin.CLEAN_TASK_NAME, JavaBasePlugin.BUILD_TASK_NAME]))
//        assertThat(project.convention.plugins.embeddedJavaProject.buildTasks, equalTo([JavaBasePlugin.BUILD_TASK_NAME]))
//        assertThat(project.convention.plugins.embeddedJavaProject.runtimeClasspath, notNullValue())
//    }
//
//    @Test public void addsConfigurationsToTheProject() {
//        javaPlugin.apply(project)
//
//        def configuration = project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)
//        assertFalse(configuration.visible)
//        assertTrue(configuration.transitive)
//
//        configuration = project.configurations.getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME)
//        assertThat(Configurations.getNames(configuration.extendsFrom, false), equalTo(toSet(JavaPlugin.COMPILE_CONFIGURATION_NAME)))
//        assertFalse(configuration.visible)
//        assertTrue(configuration.transitive)
//
//        configuration = project.configurations.getByName(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME)
//        assertThat(Configurations.getNames(configuration.extendsFrom, false), equalTo(toSet(JavaPlugin.COMPILE_CONFIGURATION_NAME)))
//        assertFalse(configuration.visible)
//        assertTrue(configuration.transitive)
//
//        configuration = project.configurations.getByName(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME)
//        assertThat(Configurations.getNames(configuration.extendsFrom, false), equalTo(toSet(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME, JavaPlugin.RUNTIME_CONFIGURATION_NAME)))
//        assertFalse(configuration.visible)
//        assertTrue(configuration.transitive)
//
//        configuration = project.configurations.getByName(Dependency.DEFAULT_CONFIGURATION)
//        assertThat(Configurations.getNames(configuration.extendsFrom, false), equalTo(toSet(Dependency.ARCHIVES_CONFIGURATION, JavaPlugin.RUNTIME_CONFIGURATION_NAME)))
//    }
//
//    @Test public void createsStandardSourceSetsAndAppliesMappings() {
//        javaPlugin.apply(project)
//
//        def set = project.sourceSets[SourceSet.MAIN_SOURCE_SET_NAME]
//        assertThat(set.java.srcDirs, equalTo(toLinkedSet(project.file('src/main/java'))))
//    }
}
