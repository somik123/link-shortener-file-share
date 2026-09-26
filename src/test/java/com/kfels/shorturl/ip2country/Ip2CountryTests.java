package com.kfels.shorturl.ip2country;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class Ip2CountryTests {

    @Test
    void fetchesPagesFromLocalHttpServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/country", exchange -> {
            byte[] body = "{\"country\":\"US\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            assertEquals("{\"country\":\"US\"}",
                    Ip2Country.getPage("http://127.0.0.1:" + server.getAddress().getPort() + "/country"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void invalidPageUrlReturnsNullAndUnknownCountryIsAllowed() {
        assertNull(Ip2Country.getPage("not a url"));
        assertTrue(Ip2Country.isCountryAllowed("unknown"));
        assertTrue(Ip2Country.isCountryAllowed("-"));
    }
}
