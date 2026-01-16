package pl.edu.agh.to;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import pl.edu.agh.to.service.AIToolsService;

@SpringBootApplication
public class ToZtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToZtpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider aiTools(AIToolsService aiToolsService) {
        return MethodToolCallbackProvider.builder().toolObjects(aiToolsService).build();
    }
}