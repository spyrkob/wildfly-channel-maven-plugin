package org.wildfly.channelplugin.manipulation;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.assertj.core.api.Assertions;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.codehaus.stax2.XMLInputFactory2;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.io.PomIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.channelplugin.utils.DependencyModel;

public class PomManipulatorTestCase {

    private StringBuilder content;
    private ModifiedPomXMLEventReader eventReader;

    @BeforeEach
    public void before() throws XMLStreamException, URISyntaxException, IOException {
        URL pomUrl = getClass().getResource("pom.xml");
        content = PomHelper.readXmlFile(new File(pomUrl.toURI()));

        XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
        inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
        eventReader = new ModifiedPomXMLEventReader(content, inputFactory, pomUrl.getPath());
    }

    @Test
    public void testInsertManagedDependency()
            throws IOException, XMLStreamException, ManipulationException {
        ArtifactRef dep = new SimpleArtifactRef("org.aesh", "aesh", "2.4.0", "jar", null);

        DependencyModel model = readDependencyModel();
        Assertions.assertThat(model.getDependency(dep.getGroupId(), dep.getArtifactId(), dep.getType(), null))
                .isEmpty();

        PomManipulator.injectManagedDependency(eventReader, dep, Collections.emptyList());

        model = readDependencyModel();
        Assertions.assertThat(model.getDependency(dep.getGroupId(), dep.getArtifactId(), dep.getType(), null))
                .satisfies(d -> {
                    Assertions.assertThat(d).isPresent();
                    Assertions.assertThat(d.get().getVersion()).isEqualTo("2.4.0");
                });
    }

    private DependencyModel readDependencyModel() throws IOException, ManipulationException {
        Path pomFile = Files.createTempFile("pom", "xml");
        Files.write(pomFile, content.toString().getBytes());

        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject(pomFile.toFile());
        Assertions.assertThat(projects.size()).isEqualTo(1);
        return new DependencyModel(projects.get(0).getModel());
    }
}
