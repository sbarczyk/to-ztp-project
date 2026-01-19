package pl.edu.agh.to.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MCPServerE2ETest {

    private static final ObjectMapper om = new ObjectMapper();

    private Process app;
    private McpStdioClient mcp;

    @BeforeAll
    void startServerOnce() throws Exception {
        app = AppProcessFactory.startApp(Map.of(
                "SPRING_PROFILES_ACTIVE", "test"
        ));

        mcp = new McpStdioClient(app);

        JsonNode init = waitForMcpReady(mcp, Duration.ofSeconds(20));
        System.out.println("initialize: " + om.writerWithDefaultPrettyPrinter().writeValueAsString(init));

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
                Thread.sleep(200);
            }
        }

        throw new IllegalStateException("MCP server did not become ready within " + timeout + " (attempts=" + attempts + ")");
    }

    @AfterAll
    void stopServerOnce() {
        if (mcp != null) {
            try { mcp.close(); } catch (Exception ignored) {}
        }
        if (app != null) {
            app.destroy();
        }
    }

    @Test
    void toolsList_shouldContainListStops() throws Exception {
        JsonNode tools = mcp.toolsList(2);
        assertThat(tools.has("result")).isTrue();

        String json = tools.toString();
        assertThat(json).contains("listStops");
    }

    @Test
    void listStops_shouldReturnSomeStops() throws Exception {
        JsonNode call = mcp.toolsCall(2, "listStops", Map.of("limit", 5));

        assertThat(call.has("result")).isTrue();

        String json = call.toString();
        assertThat(json).contains("AKF / PK");
    }

    @Test
    void unknownTool_shouldReturnError() throws Exception {
        JsonNode call = mcp.toolsCall(3, "doesNotExist", Map.of());

        assertThat(call.has("error")).isTrue();
        assertThat(call.get("error").get("code").asInt()).isNotZero();
    }
}
