package pl.edu.agh.to.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class McpStdioClient implements Closeable {

    private final Process process;
    private final BufferedWriter stdin;
    private final BufferedReader stdout;
    private final ObjectMapper mapper;

    public McpStdioClient(Process process) {
        this.process = process;
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.mapper = new ObjectMapper();
    }

    public JsonNode initialize(int id) throws IOException {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", Map.of());

        Map<String, Object> msg = jsonrpcRequest(id, "initialize", Map.of("params", params));
        return sendAndAwait(id, msg, Duration.ofSeconds(10));
    }

    public void initializedNotification() throws IOException {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("jsonrpc", "2.0");
        msg.put("method", "notifications/initialized");
        send(msg);
    }

    public JsonNode toolsList(int id) throws IOException {
        Map<String, Object> msg = jsonrpcRequest(id, "tools/list", Map.of("params", Map.of()));
        return sendAndAwait(id, msg, Duration.ofSeconds(10));
    }

    public JsonNode toolsCall(int id, String toolName, Map<String, Object> arguments) throws IOException {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments == null ? Map.of() : arguments);

        Map<String, Object> msg = jsonrpcRequest(id, "tools/call", Map.of("params", params));
        return sendAndAwait(id, msg, Duration.ofSeconds(20));
    }

    // ---- Internals ----

    private Map<String, Object> jsonrpcRequest(int id, String method, Map<String, Object> extra) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("jsonrpc", "2.0");
        msg.put("id", id);
        msg.put("method", method);
        msg.putAll(extra);
        return msg;
    }

    private void send(Object msg) throws IOException {
        String line = mapper.writeValueAsString(msg);
        stdin.write(line);
        stdin.newLine();
        stdin.flush();
    }

    private JsonNode sendAndAwait(int expectedId, Object msg, Duration timeout) throws IOException {
        send(msg);
        return readResponseById(expectedId, timeout);
    }

    private JsonNode readResponseById(int expectedId, Duration timeout) throws IOException {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            if (!stdout.ready()) {
                sleep(10);
                continue;
            }

            String line = stdout.readLine();
            if (line == null) {
                throw new IOException("MCP server closed stdout unexpectedly");
            }

            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.charAt(0) != '{') {
                continue;
            }

            JsonNode json;
            try {
                json = mapper.readTree(line);
            } catch (Exception ignored) {
                continue;
            }

            JsonNode idNode = json.get("id");
            if (idNode != null && idNode.isInt() && idNode.asInt() == expectedId) {
                return json;
            }
        }

        throw new IOException("Timeout waiting for MCP response with id=" + expectedId);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        try { stdin.close(); } catch (Exception ignored) { }
        try { stdout.close(); } catch (Exception ignored) { }
        process.destroy();
    }
}