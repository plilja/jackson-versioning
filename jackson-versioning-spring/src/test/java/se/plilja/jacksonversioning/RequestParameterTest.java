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
import org.springframework.http.ResponseEntity;

@Import({TestApplication.class, RequestParameterTest.TestConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestParameterTest extends RequestTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper requestParameterObjectMapper(ApplicationContext applicationContext) {
            VersioningModule versioningModule = SpringVersioningModuleBuilder.withEnumVersions(ApiVersion.class)
                    .withVersionDeterminedByRequestParameter("API_VERSION")
                    .withConvertersFromApplicationContext(applicationContext)
                    .build();
            return new ObjectMapper().registerModule(versioningModule);
        }
    }

    @Override
    protected <T> T get(String url, String apiVersion, Class<T> returnType) {
        if (url.contains("?")) {
            return restTemplate.getForObject(url + "&API_VERSION=" + apiVersion, returnType);
        } else {
            return restTemplate.getForObject(url + "?API_VERSION=" + apiVersion, returnType);
        }
    }

    @Override
    protected <T> ResponseEntity<T> post(String url, Object body, String apiVersion, Class<T> returnType) {
        if (url.contains("?")) {
            return restTemplate.postForEntity(url + "&API_VERSION=" + apiVersion, body, returnType);
        } else {
            return restTemplate.postForEntity(url + "?API_VERSION=" + apiVersion, body, returnType);
        }
    }
}
