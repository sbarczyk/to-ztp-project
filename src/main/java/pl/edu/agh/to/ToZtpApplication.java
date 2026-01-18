package pl.edu.agh.to;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import pl.edu.agh.to.mcp.McpToolsController;

@SpringBootApplication
public class ToZtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToZtpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider aiTools(McpToolsController mcpToolsController) {
        return MethodToolCallbackProvider.builder().toolObjects(mcpToolsController).build();
    }
}