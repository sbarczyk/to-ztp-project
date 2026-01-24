package pl.edu.agh.to.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MCPServerE2ETest {

    private Process app;
    private McpStdioClient mcp;
    private JsonNode init;

    @BeforeAll
    void startServerOnce() throws Exception {
        app = AppProcessFactory.startApp(Map.of(
                "SPRING_PROFILES_ACTIVE", "test"
        ));

        mcp = new McpStdioClient(app);
        init = waitForMcpReady(mcp, Duration.ofSeconds(20));
        mcp.initializedNotification();
    }

    private static JsonNode waitForMcpReady(McpStdioClient mcp, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        int attempts = 0;

        while (Instant.now().isBefore(deadline)) {
            attempts++;
            try {
                return mcp.initialize(1);
            } catch (Exception ex) {
                await().atMost(Duration.ofMillis(200)).until(() -> Instant.now().isAfter(deadline));
            }
        }

        throw new IllegalStateException("MCP server did not become ready within " + timeout + " (attempts=" + attempts + ")");
    }

    @AfterAll
    void stopServerOnce() {
        if (mcp != null) {
            mcp.close();
        }
        if (app != null) {
            app.destroy();
        }
    }

    @Test
    void initialize_shouldContainExpectedProtocolVersion() {
        assertThat(init.has("result")).isTrue();
        assertThat(init.get("result").has("protocolVersion")).isTrue();
        String protocolVersion = init.get("result").get("protocolVersion").asText();
        assertThat(protocolVersion).isEqualTo("2024-11-05");
    }

    @Test
    void toolsList_shouldContainAllTools() throws IOException {
        JsonNode tools = mcp.toolsList(2);
        assertThat(tools.has("result")).isTrue();
        assertThat(tools.get("result").has("tools")).isTrue();
        assertThat(tools.get("result").withArrayProperty("tools").valueStream()
                .allMatch(tool ->
                    switch (tool.get("name").asText()) {
                        case "listStops", "findFastestConnections", "listNextDepartures" -> true;
                        default -> false;
                    }
                )).isTrue();
    }

    @Test
    void listStops_shouldReturnCorrectData() throws IOException {
        JsonNode call = mcp.toolsCall(2, "listStops", Map.of("limit", 5, "lastStopName", "AKF / PK"));

        assertThat(call.has("result")).isTrue();
        assertThat(call.get("result").get("isError").asBoolean()).isFalse();
        assertThat(call.toString())
                .contains("Aleja Róż")
                .contains("\\\"lastStopName\\\":\\\"Aleja Waszyngtona\\\"")
                .contains("\\\"hasNext\\\":true");
    }

    @Test
    void findFastestConnections_shouldReturnCorrectData() throws IOException {
        JsonNode call = mcp.toolsCall(2, "findFastestConnections", Map.of("fromStop", "Kawiory", "toStop", "Czarnowiejska"));

        assertThat(call.has("result")).isTrue();
        assertThat(call.get("result").get("isError").asBoolean()).isFalse();
        assertThat(call.get("result").withArrayProperty("content").get(0).get("text").toString()).contains(",\\\"connections\\\":[{\\\"lineNumber\\\":");
    }

    @Test
    void listNextDepartures_shouldReturnCorrectData() throws IOException {
        JsonNode call = mcp.toolsCall(2, "listNextDepartures", Map.of("stopName", "Teatr Bagatela", "lineNumber", "4"));

        assertThat(call.has("result")).isTrue();
        assertThat(call.get("result").get("isError").asBoolean()).isFalse();
        assertThat(call.get("result").withArrayProperty("content").get(0).get("text").toString()).contains(",\\\"departures\\\":[{\\\"scheduledDeparture\\\":");
    }

    @Test
    void unknownTool_shouldReturnError() throws IOException {
        JsonNode call = mcp.toolsCall(3, "doesNotExist", Map.of());

        assertThat(call.has("result")).isFalse();
        assertThat(call.has("error")).isTrue();
        assertThat(call.get("error").get("code").asInt()).isNotZero();
    }
}
