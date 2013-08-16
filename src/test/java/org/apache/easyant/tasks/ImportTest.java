/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.easyant.tasks;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.Path;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ImportTest {
    private File cache;

    private Import importTask;

    @Before
    public void setUp() throws MalformedURLException, URISyntaxException {
        createCache();
        Project project = new Project();
        ProjectUtils.configureProjectHelper(project);
        project.setProperty("ivy.cache.dir", cache.getAbsolutePath());

        IvyConfigure configure = new IvyConfigure();
        configure.setProject(project);

        File f = new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml").toURI());
        configure.setFile(f);

        configure.setSettingsId(EasyAntMagicNames.EASYANT_IVY_INSTANCE);
        configure.execute();

        importTask = new Import();
        importTask.setProject(project);
        importTask.setOwningTarget(ProjectUtils.createTopLevelTarget());
        importTask.setLocation(new Location(ProjectUtils.emulateMainScript(project).getAbsolutePath()));
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cache);
        del.execute();
    }

    @Test(expected = BuildException.class)
    public void shouldFailIfNoMandatoryAttributeAreSet() {
        importTask.execute();
    }

    @Test
    public void shouldIncludeSimplePlugin() {
        importTask.setOrg("mycompany");
        importTask.setModule("simpleplugin");
        importTask.setRev("0.1");
        importTask.setMode("include");
        importTask.execute();

        Path pluginClasspath = importTask.getProject().getReference("mycompany#simpleplugin.classpath");
        Assert.assertNotNull(pluginClasspath);
        Assert.assertEquals(0, pluginClasspath.list().length);
    }

    @Test
    public void shouldComputeAsAttributeOnIncludeWithMRID() {
        importTask.setMrid("mycompany#simpleplugin;0.1");
        importTask.setMode("include");
        importTask.execute();

        // as attribute is preconfigured with module name
        Assert.assertNotNull(importTask.getAs());
        Assert.assertEquals("simpleplugin", importTask.getAs());
    }

    @Test
    public void shouldImportSimplePlugin() {
        importTask.setOrg("mycompany");
        importTask.setModule("simpleplugin");
        importTask.setRev("0.1");
        importTask.execute();

        Path pluginClasspath = importTask.getProject().getReference("mycompany#simpleplugin.classpath");
        Assert.assertNotNull(pluginClasspath);
        Assert.assertEquals(0, pluginClasspath.list().length);
    }

    @Test
    public void shouldImportSimplePluginWithProperties() {
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setRev("0.1");
        importTask.execute();

        verifySimplePluginWithPropertiesIsImported();
    }

    private void verifySimplePluginWithPropertiesIsImported() {
        Path pluginClasspath = importTask.getProject().getReference("mycompany#simplepluginwithproperties.classpath");
        Assert.assertNotNull(pluginClasspath);
        Assert.assertEquals(0, pluginClasspath.list().length);

        String propertiesFileLocation = importTask.getProject().getProperty(
                "mycompany#simplepluginwithproperties.properties.file");
        Assert.assertNotNull(propertiesFileLocation);

        String srcMainJavaProperty = importTask.getProject().getProperty("src.main.java");
        Assert.assertNotNull(srcMainJavaProperty);
        Assert.assertEquals(importTask.getProject().getBaseDir() + "/src/main/java", srcMainJavaProperty);

        // imported from property file
        String aProperty = importTask.getProject().getProperty("aproperty");
        Assert.assertNotNull(aProperty);
        Assert.assertEquals("value", aProperty);

        String anotherJavaProperty = importTask.getProject().getProperty("anotherproperty");
        Assert.assertNotNull(anotherJavaProperty);
        Assert.assertEquals("value", anotherJavaProperty);
    }

    @Test
    public void shouldSkipSimplePlugin() {
        importTask.getProject().setNewProperty("skip.mycompany#simplepluginwithproperties;0.1", "true");
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setRev("0.1");
        importTask.execute();

        verifySimplePluginWithPropertiesIsSkipped();

    }

    private void verifySimplePluginWithPropertiesIsSkipped() {
        Path pluginClasspath = importTask.getProject().getReference("mycompany#simplepluginwithproperties.classpath");
        Assert.assertNull(pluginClasspath);

        String propertiesFileLocation = importTask.getProject().getProperty(
                "mycompany#simplepluginwithproperties.properties.file");
        Assert.assertNull(propertiesFileLocation);

        String srcMainJavaProperty = importTask.getProject().getProperty("src.main.java");
        Assert.assertNull(srcMainJavaProperty);

        // imported from property file
        String aProperty = importTask.getProject().getProperty("aproperty");
        Assert.assertNull(aProperty);

        String anotherJavaProperty = importTask.getProject().getProperty("anotherproperty");
        Assert.assertNull(anotherJavaProperty);
    }

    @Test
    public void shouldSkipSimplePluginWithAsAttribute() {
        importTask.getProject().setNewProperty("skip.myalias", "true");
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setRev("0.1");
        importTask.setAs("myalias");
        importTask.execute();

        verifySimplePluginWithPropertiesIsSkipped();

    }

    @Test
    public void shouldNotSkipMandatoryPlugin() {
        importTask.getProject().setNewProperty("skip.mycompany#simplepluginwithproperties;0.1", "true");
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setRev("0.1");
        importTask.setMandatory(true);
        importTask.execute();

        verifySimplePluginWithPropertiesIsImported();
    }

    @Test
    public void shouldImportMandatoryPlugin() {
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setRev("0.1");
        importTask.setMandatory(true);
        importTask.execute();

        verifySimplePluginWithPropertiesIsImported();
    }

    @Test
    public void shouldComputeAsAttributeOnInclude() {
        importTask.setOrg("mycompany");
        importTask.setModule("simpleplugin");
        importTask.setRev("0.1");
        importTask.setMode("include");
        importTask.execute();

        // as attribute is preconfigured with module name
        Assert.assertNotNull(importTask.getAs());
        Assert.assertEquals("simpleplugin", importTask.getAs());
    }

    @Test(expected = BuildException.class)
    public void shouldFailBuildConfAreFound() {
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setRev("0.1");
        importTask.setBuildConfigurations("amissingConf");
        importTask.execute();
        //
    }

    @Test
    public void shouldNotImportIfBuildConfDoesntMatch() {
        importTask.getProject().setProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, "aBuildConfNotActive");
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setRev("0.1");
        importTask.setBuildConfigurations("aBuildConfNotActive");
        importTask.execute();

        verifySimplePluginWithPropertiesIsSkipped();
    }

    @Test
    public void shouldImportIfBuildConfMatch() {
        importTask.getProject().setProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, "aBuildConfActive");

        importTask.getProject().setProperty(EasyAntMagicNames.MAIN_CONFS, "aBuildConfActive");

        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setRev("0.1");
        importTask.setBuildConfigurations("aBuildConfActive");
        importTask.execute();

        verifySimplePluginWithPropertiesIsImported();
    }

}