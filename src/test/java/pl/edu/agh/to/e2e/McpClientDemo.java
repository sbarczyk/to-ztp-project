package pl.edu.agh.to.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class McpClientDemo {

    // TUTAJ TYLKO SPRAWDZAŁEM CZY DZIAŁA, DO USUNIĘCIA PO STWORZENIU TESTOW E2E !!!
    public static void main(String[] args) throws Exception {
        Process app = AppProcessFactory.startApp(Map.of(
                "SPRING_PROFILES_ACTIVE", "test"
        ));

        Thread.sleep(3000);

        try (McpStdioClient mcp = new McpStdioClient(app)) {
            ObjectMapper om = new ObjectMapper();

            JsonNode init = mcp.initialize(1);
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
}