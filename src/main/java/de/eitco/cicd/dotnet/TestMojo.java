package de.eitco.cicd.dotnet;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;


/**
 * This goal calls {@code dotnet test} to execute test. It will add the command line parameter {@code --no-build},
 * since the {@code compile} phase will already be called before the {@code test} phase. It will always configure
 * the {@code trx} logger and transform the results to a valid {@code junit} description - enabling ci servers to
 * collect the test results in the default format for maven builds. The goal will honour the reactors failure behaviour.
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST)
public class TestMojo extends AbstractDotnetMojo {

    public static final String REACTOR_FAILURE_BEHAVIOR_FAIL_NEVER = "FAIL_NEVER";
    public static final String TEST_RESULT_EXTENSION = "trx";
    public static final String XSL_TRANSFORMATION = "xunit-to-junit.xsl";

    /**
     * This parameter specifies whether to skip the tests. Note that its property is the maven default property to
     * skip tests.
     */
    @Parameter(defaultValue = "false", property = "skipTests")
    private boolean skipTests;

    /**
     * This parameter specifies the directory where to write the test results to
     */
    @Parameter(defaultValue = "target/test-results")
    private File testResultDirectory;


    @Component
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skipTests) {

            getLog().info("Skipping tests");
            return;
        }

        int result = newExecutor(true).test(TEST_RESULT_EXTENSION, testResultDirectory.getPath());

        transformResultFiles();

        if (REACTOR_FAILURE_BEHAVIOR_FAIL_NEVER.equals(session.getReactorFailureBehavior())) {

            return;
        }

        if (result == 0) {

            return;
        }

        throw new MojoFailureException("c# test failed");
    }

    private void transformResultFiles() throws MojoExecutionException {

        File[] files = testResultDirectory.listFiles(file -> file.getName().endsWith("." + TEST_RESULT_EXTENSION));

        if (files != null) {

            try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(XSL_TRANSFORMATION)) {

                byte[] transformationBytes = Objects.requireNonNull(resourceAsStream, () -> "resource " + XSL_TRANSFORMATION + " not found").readAllBytes();

                for (File file : files) {

                    StreamSource xsltSource = new StreamSource(new ByteArrayInputStream(transformationBytes));
                    transformToJUnit(file, xsltSource);
                }

            } catch (IOException e) {
                throw new MojoExecutionException(e);
            }
        }
    }

    private void transformToJUnit(File file, StreamSource xsltSource) throws MojoExecutionException {

        try {

            TransformerFactory transformerFactory = TransformerFactory.newInstance();

            Templates templates = transformerFactory.newTemplates(xsltSource);
            Transformer transformer = templates.newTransformer();

            Source xmlSource = new StreamSource(file);
            Result result = new StreamResult(new File(file.getAbsolutePath() + ".xml"));

            transformer.transform(xmlSource, result);

        } catch (TransformerException e) {
            throw new MojoExecutionException(e);
        }

    }
}
