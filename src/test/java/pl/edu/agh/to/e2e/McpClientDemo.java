package pl.edu.agh.to.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class McpClientDemo {

    // Manual check only. Remove after E2E tests are in place.
    public static void main(String[] args) throws Exception {
        Process app = AppProcessFactory.startApp(Map.of(
                "SPRING_PROFILES_ACTIVE", "test"
        ));

        try (McpStdioClient mcp = new McpStdioClient(app)) {
            ObjectMapper om = new ObjectMapper();

            JsonNode init = waitForMcpReady(mcp, Duration.ofSeconds(20));
            System.out.println("=== initialize ===");
            System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(init));

            mcp.initializedNotification();
            System.out.println("=== notifications/initialized sent ===");

            JsonNode tools = mcp.toolsList(2);
            System.out.println("=== tools/list ===");
            System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(tools));

            JsonNode call = mcp.toolsCall(3, "listStops", Map.of("limit", 5));
            System.out.println("=== tools/call listStops ===");
            System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(call));
        } finally {
            app.destroy();
        }
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
}