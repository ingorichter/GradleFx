/*
 * Copyright (c) 2011 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradlefx

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact
import org.gradle.api.tasks.Delete
import org.gradlefx.conventions.GradleFxConvention
import org.gradlefx.tasks.CopyResources
import org.gradlefx.tasks.HtmlWrapper
import org.gradlefx.tasks.Publish
import org.gradlefx.tasks.factory.CompileTaskClassFactory
import org.gradlefx.tasks.factory.CompileTaskClassFactoryImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GradleFxPlugin implements Plugin<Project> {

    public static final String COMPILE_TASK_NAME = 'compile'
    public static final String BUILD_TASK_NAME = 'build'
    public static final String PUBLISH_TASK_NAME = 'publish'
    public static final String COPY_RESOURCES_TASK_NAME = 'copyresources'
    public static final String CLEAN_TASK_NAME = 'clean'
    public static final String CREATE_HTML_WRAPPER = 'createHtmlWrapper'

    // configurations
    public static final String DEFAULT_CONFIGURATION_NAME = 'default'
    public static final String INTERNAL_CONFIGURATION_NAME = 'internal'
    public static final String EXTERNAL_CONFIGURATION_NAME = 'external'
    public static final String MERGE_CONFIGURATION_NAME = 'merged'
    public static final String RSL_CONFIGURATION_NAME = 'rsl'
    public static final String TEST_CONFIGURATION_NAME = 'test'

    Logger log = LoggerFactory.getLogger('flex')

    private Project project

    public void apply(Project project) {
        this.project = project

        GradleFxConvention pluginConvention = new GradleFxConvention(project)
        project.convention.plugins.flex = pluginConvention

        addDefaultConfigurations()

        addBuild()
        addCopyResources()
        addClean()
        addPublish()

        //do these tasks in the afterEvaluate phase because they need property access
        project.afterEvaluate {
            configureAnt()
            addCompile(pluginConvention)
            addHtmlWrapper()
            addDependsOnOtherProjects()
            addDefaultArtifact()
        }

         project.repositories {
             // The fully open source SDK can be directly downloaded with no click-through 
             // Download URL convention for OSS SDK (which contains player, ant, lib):
             //  http://fpdownload.adobe.com/pub/flex/sdk/builds/flex4/flex_sdk_4.0.0.14159_mpl.zip
             ivy {
                 name = 'flexOSSSDKRepo'
                 //artifactPattern "http://repo.mycompany.com/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"
                 // module = "flex4"
                 // revision = "4.0.0.14159"
                 artifactPattern "http://fpdownload.adobe.com/pub/flex/sdk/builds/[module]/flex_sdk_[revision]_mpl.zip"
             }

             // Free but not open source SDK might use flat file layout repo
             // Free but not open source SDK would be user-downloaded as a ZIP after the click through license into a sdkrepo/ directory
             // that could contain more than one SDK zip in the original file name style.
             // flatDir {
             //     name = 'flexLocalSDKRepo'
             //     // module = "flex4"
             //     // revision = "4.0.0.14159"
             //     // Decided not to supply a default for now: dirs = 'flexsdkrepo'
             //     artifactPattern "${dirs}/[module]/flex_sdk_[revision].zip"
             //     // has no MPL (mozilla public license)
             // }
         }

         // TODO: Test if the SDK is already unzipped
         // TODO: Unzip the SDK to a temp folder beneath the project (perhaps build/.flexsdk)
    }



    private void configureAnt() {
      //If the repository vectors are given register a artifact and unzip it to the local
      //project directory and set project.flexHome to that unzipped subdirectory
      //unzip.exec() to either a permanent dot directory like _.flexsdk_ or a cleanable one like _build/flexsdk_
      // Be specific in extraction to not expand the documentation, (asdoc) 
      // ONLY extract frameworks/, ant/, lib/
      //  

        project.ant.property(name: 'FLEX_HOME', value: project.flexHome)
        project.ant.property(name: 'FLEX_LIB', value: '${FLEX_HOME}/frameworks/libs')
        project.ant.property(name: 'FLEX_ANT', value: '${FLEX_HOME}/ant')
        project.ant.property(name: 'FLEX_ANTLIB', value: '${FLEX_ANT}/lib')
        project.ant.property(name: 'FLEX_PLAYER_LIB', value: "\${FLEX_LIB}/player/${project.playerVersion}")

        project.ant.taskdef(resource: 'flexTasks.tasks') {
            classpath {
                fileset(dir: '${FLEX_ANTLIB}') {
                    include(name: 'flexTasks.jar')
                }
            }
        }
    }

    private void addDefaultConfigurations() {
        project.configurations.add(DEFAULT_CONFIGURATION_NAME)
        project.configurations.add(INTERNAL_CONFIGURATION_NAME)
        project.configurations.add(EXTERNAL_CONFIGURATION_NAME)
        project.configurations.add(MERGE_CONFIGURATION_NAME)
        project.configurations.add(RSL_CONFIGURATION_NAME)
        project.configurations.add(TEST_CONFIGURATION_NAME)
    }

    private void addBuild() {
        DefaultTask buildTask = project.tasks.add(BUILD_TASK_NAME, DefaultTask)
        buildTask.setDescription("Assembles and tests this project.")
        buildTask.dependsOn(COMPILE_TASK_NAME)
    }

    private void addCompile(GradleFxConvention pluginConvention) {
        CompileTaskClassFactory compileTaskClassFactory = new CompileTaskClassFactoryImpl()

        Class<Task> compileClass = compileTaskClassFactory.createCompileTaskClass(project.type)
        Task compile = project.tasks.add(COMPILE_TASK_NAME, compileClass)
        compile.dependsOn(COPY_RESOURCES_TASK_NAME)
    }

    private void addHtmlWrapper() {
        if (project.type == FlexType.swf) {
            project.tasks.add(CREATE_HTML_WRAPPER, HtmlWrapper)
        }
    }

    private void addCopyResources() {
        project.tasks.add(COPY_RESOURCES_TASK_NAME, CopyResources)
    }

    private void addClean() {
        Delete clean = project.tasks.add(CLEAN_TASK_NAME, Delete)
        clean.description = "Deletes the build directory."
        clean.delete { project.buildDir }
    }

    private void addPublish() {
        project.tasks.add(PUBLISH_TASK_NAME, Publish)
    }

    private void addDependsOnOtherProjects() {
        // dependencies need to be added as a closure as we don't have the information at the moment to wire them up
        project.tasks.compile.dependsOn {
            Set dependentTasks = new HashSet()
            project.configurations.each { Configuration configuration ->
                Set deps = project.configurations."${configuration.name}".getDependencies(ProjectDependency)
                deps.each { projectDependency ->
                    //def projectDependency = (ProjectDependency) dependency
                    println "path to dependency: ${projectDependency.dependencyProject.path}"
                    dependentTasks.add(projectDependency.dependencyProject.path + ':compile')
                }
            }
            dependentTasks
        }
    }

    /**
     * If this is an implementation project (compiles a swc of swf), it adds an artifact
     * of the given project to the default configuration.
     * @param project
     */
    private void addDefaultArtifact() {
        if (isImplementationProject()) {
            addProjectArtifactToDefaultConfiguration()
        }
    }

    /**
     * This project is an implementation project when it compiles to a swc or swf file.
     * @return
     */
    private Boolean isImplementationProject() {
        return project.type != null;
    }

    /**
     * Adds an artifact to the default configuration.
     * @param project
     */
    private void addProjectArtifactToDefaultConfiguration() {
        project.artifacts { ArtifactHandler artifactHandler ->
            File artifactFile = new File(project.buildDir.path + "/" + project.output + "." + project.type)
            def artifact = new DefaultPublishArtifact(project.name, project.type.toString(), project.type.toString(), null, new Date(), artifactFile)
            artifactHandler."${DEFAULT_CONFIGURATION_NAME}" artifact
        }
    }

}
