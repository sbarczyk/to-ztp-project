package pl.edu.agh.to.exceptions;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldHandleBadRequestException() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Bad request message")))
                .andExpect(jsonPath("$.path", is("/test/bad-request")));
    }

    @Test
    void shouldHandleNotFoundException() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void shouldHandleNoResourceFoundException() throws Exception {
        // Testujemy specyficzny przypadek z handlera dla NoResourceFoundException
        mockMvc.perform(get("/test/no-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Not found")))
                .andExpect(jsonPath("$.path", is("/test/no-resource")));
    }

    @Test
    void shouldHandleInvalidProtocolBufferExceptionAsInternalServerError() throws Exception {
        mockMvc.perform(get("/test/protobuf-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Unexpected server error")));
    }

    @Test
    void shouldHandleGeneralExceptionAsInternalServerError() throws Exception {
        mockMvc.perform(get("/test/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.message", is("Unexpected server error")));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/bad-request")
        public void throwBadRequest() {
            throw new BadRequestException("Bad request message");
        }

        @GetMapping("/test/not-found")
        public void throwNotFound() {
            throw new NotFoundException("Not found message");
        }

        @GetMapping("/test/no-resource")
        public void throwNoResource() throws NoResourceFoundException {
            // NoResourceFoundException wymaga podania metody HTTP i ścieżki
            throw new NoResourceFoundException(HttpMethod.GET, "/test/no-resource");
        }

        @GetMapping("/test/protobuf-error")
        public void throwProtobuf() throws InvalidProtocolBufferException {
            throw new InvalidProtocolBufferException("Protobuf error");
        }

        @GetMapping("/test/internal-error")
        public void throwGeneral() {
            throw new RuntimeException("Secret technical detail");
        }
    }
}