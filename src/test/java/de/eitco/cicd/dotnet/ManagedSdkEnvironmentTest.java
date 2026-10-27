package de.eitco.cicd.dotnet;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ManagedSdkEnvironmentTest {

    @Test
    void managedSdkAddsOptOutVariables() {
        Map<String, String> result = AbstractDotnetMojo.effectiveEnvironment(true, Map.of());

        assertEquals("1", result.get("DOTNET_NOLOGO"));
        assertEquals("1", result.get("DOTNET_SKIP_FIRST_TIME_EXPERIENCE"));
        assertEquals("false", result.get("DOTNET_GENERATE_ASPNET_CERTIFICATE"));
        assertEquals("false", result.get("DOTNET_ADD_GLOBAL_TOOLS_TO_PATH"));
        assertEquals("1", result.get("DOTNET_CLI_WORKLOAD_UPDATE_NOTIFY_DISABLE"));
    }

    @Test
    void systemDotnetKeepsUserEnvironmentUnchanged() {
        Map<String, String> userEnv = Map.of("FOO", "bar");

        assertEquals(userEnv, AbstractDotnetMojo.effectiveEnvironment(false, userEnv));
    }

    @Test
    void userEnvironmentOverridesOptOutVariables() {
        Map<String, String> result = AbstractDotnetMojo.effectiveEnvironment(true,
                Map.of("DOTNET_GENERATE_ASPNET_CERTIFICATE", "true", "FOO", "bar"));

        assertEquals("true", result.get("DOTNET_GENERATE_ASPNET_CERTIFICATE"));
        assertEquals("bar", result.get("FOO"));
        assertEquals("1", result.get("DOTNET_NOLOGO"));
    }
}
