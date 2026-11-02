package com.flansmodultimate.mcpdev;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Loopback-only mapped UI endpoint. Never included in release artifacts. */
final class McpDevServer
{
    private final int port;
    private final McpUi ui = new McpUi();

    McpDevServer(int port)
    {
        this.port = port;
    }

    void start()
    {
        try
        {
            HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            http.createContext("/api/status", exchange -> respond(exchange, identity()));
            http.createContext("/api/cmd", this::command);
            http.setExecutor(Executors.newSingleThreadExecutor(task ->
            {
                Thread thread = new Thread(task, "Flans MCP mapped UI");
                thread.setDaemon(true);
                return thread;
            }));
            http.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> http.stop(0), "Flans MCP shutdown"));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot bind mapped MCP UI port " + port, e);
        }
    }

    private JsonObject identity()
    {
        JsonObject result = new JsonObject();
        result.addProperty("ok", true);
        result.addProperty("type", "flans-mcp-dev");
        result.addProperty("protocol", 1);
        result.addProperty("minecraft", "1.20.1");
        result.addProperty("loader", "forge");
        result.addProperty("pid", ProcessHandle.current().pid());
        result.addProperty("port", port);
        return result;
    }

    private void command(HttpExchange exchange) throws IOException
    {
        JsonObject result;
        try
        {
            if (!exchange.getRequestMethod().equals("POST"))
                throw new IllegalArgumentException("Use POST");
            byte[] body = exchange.getRequestBody().readNBytes(65537);
            if (body.length > 65536)
                throw new IllegalArgumentException("Request too large");
            JsonObject request = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
            if (request.get("cmd").getAsString().equals("fixture_editor"))
            {
                respond(exchange, McpFixtures.editor());
                return;
            }
            FutureTask<JsonObject> action = new FutureTask<>(() -> switch (request.get("cmd").getAsString())
            {
                case "ui_state" -> ui.state();
                case "ui_click" -> ui.click(request);
                case "ui_close" -> ui.close(request);
                case "ui_capture" -> ui.capture(request);
                case "fixture_world" -> McpFixtures.world();
                default -> throw new IllegalArgumentException("Unknown mapped UI command");
            });
            Minecraft.getInstance().execute(action);
            try
            {
                result = action.get(10, TimeUnit.SECONDS);
            }
            finally
            {
                action.cancel(false);
            }
        }
        catch (Exception e)
        {
            if (e instanceof InterruptedException)
                Thread.currentThread().interrupt();
            result = new JsonObject();
            result.addProperty("error", e.getCause() == null ? e.toString() : e.getCause().toString());
        }
        respond(exchange, result);
    }

    private static void respond(HttpExchange exchange, JsonObject result) throws IOException
    {
        byte[] bytes = result.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody())
        {
            output.write(bytes);
        }
        finally
        {
            exchange.close();
        }
    }
}
