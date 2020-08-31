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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Import({TestApplication.class, RequestHeaderTest.TestConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RequestHeaderTest extends RequestTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper requestHeaderObjectMapper(ApplicationContext applicationContext) {
            VersioningModule versioningModule = SpringVersioningModuleBuilder.withEnumVersions(ApiVersion.class)
                    .withVersionDeterminedByRequestHeader("API_VERSION")
                    .withConvertersFromApplicationContext(applicationContext)
                    .build();
            return new ObjectMapper().registerModule(versioningModule);
        }
    }

    @Override
    protected <T> T get(String url, String apiVersion, Class<T> returnType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("API_VERSION", apiVersion);
        HttpEntity<?> requestEntity = new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(url, HttpMethod.GET, requestEntity, returnType).getBody();
    }

    @Override
    protected <T> ResponseEntity<T> post(String url, Object body, String apiVersion, Class<T> returnType) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("API_VERSION", apiVersion);
        HttpEntity<?> requestEntity = new HttpEntity<>(body, httpHeaders);
        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, returnType);
    }
}
