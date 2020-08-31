/*
 * The MIT License
 * Copyright © 2020 Patrik Lilja
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.plilja.jacksonversioning;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

abstract class RequestTest {

    private TypeReference<HashMap<String, Object>> mapTypeReference = new TypeReference<HashMap<String, Object>>() {
    };

    @LocalServerPort
    int port;

    @Autowired
    private CarController carController;
    @Autowired
    private ObjectMapper objectMapper;

    RestTemplate restTemplate = new RestTemplate();

    @AfterEach
    void tearDown() {
        carController.reset();
    }

    private static Stream<Arguments> getHistoricApiVersion() {
        return Stream.of(
                Arguments.of(ApiVersion.V1, "{\"id\":1,\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"firstName\":\"Sten\",\"lastName\":\"Frisk\"},\"company\":\"Toyota\"}"),
                Arguments.of(ApiVersion.V2, "{\"id\":1,\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"socialSecurityNumber\":\"1234567890\",\"firstName\":\"Sten\",\"lastName\":\"Frisk\"},\"company\":\"Toyota\"}"),
                Arguments.of(ApiVersion.V3, "{\"id\":1,\"make\":\"Toyota\",\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"socialSecurityNumber\":\"1234567890\",\"firstName\":\"Sten\",\"lastName\":\"Frisk\"}}")
        );
    }

    protected abstract <T> T get(String url, String apiVersion, Class<T> returnType);

    protected abstract <T> ResponseEntity<T> post(String url, Object body, String apiVersion, Class<T> returnType);

    @ParameterizedTest
    @MethodSource
    void getHistoricApiVersion(ApiVersion version, String expectedMapString) throws Exception {
        Map<String, Object> actual = get(String.format("http://localhost:%d/cars/1", port), version.toString(), Map.class);
        Map<String, Object> expected = objectMapper.readValue(expectedMapString, mapTypeReference);
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> postHistoricApiVersion() {
        return Stream.of(
                Arguments.of(ApiVersion.V1, "{\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"firstName\":\"Sten\",\"lastName\":\"Frisk\"},\"company\":\"Toyota\"}"),
                Arguments.of(ApiVersion.V2, "{\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"socialSecurityNumber\":\"1234567890\",\"firstName\":\"Sten\",\"lastName\":\"Frisk\"},\"company\":\"Toyota\"}"),
                Arguments.of(ApiVersion.V3, "{\"make\":\"Toyota\",\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"socialSecurityNumber\":\"1234567890\",\"firstName\":\"Sten\",\"lastName\":\"Frisk\"}}")
        );
    }

    @ParameterizedTest
    @MethodSource
    void postHistoricApiVersion(ApiVersion version, String requestMapString) throws Exception {
        String expectedMapString = "{\"make\":\"Toyota\",\"model\":\"Camry\",\"yearMade\":2020,\"owner\":{\"socialSecurityNumber\":\"1234567890\",\"firstName\":\"Sten\",\"lastName\":\"Frisk\"}}";
        Map<String, Object> request = objectMapper.readValue(requestMapString, mapTypeReference);
        ResponseEntity<Map> postResponse = post(String.format("http://localhost:%d/cars", port), request, version.toString(), Map.class);

        Object id = postResponse.getBody().remove("id");
        assertEquals(request, postResponse.getBody());

        Map<String, Object> getResponse = get(String.format("http://localhost:%d/cars/%s", port, id), ApiVersion.V3.toString(), Map.class);
        Map<String, Object> expected = objectMapper.readValue(expectedMapString, mapTypeReference);
        expected.put("id", id);

        assertEquals(expected, getResponse);
    }

    @Test
    void getIllegalApiVersion() {
        HttpStatusCodeException ex = assertThrows(HttpStatusCodeException.class, () -> {
            get(String.format("http://localhost:%d/cars", port), "UNKNOWN", Car[].class);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}