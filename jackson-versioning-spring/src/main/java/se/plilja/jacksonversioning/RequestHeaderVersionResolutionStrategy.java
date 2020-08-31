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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

class RequestHeaderVersionResolutionStrategy<V extends Comparable<V>> implements VersionResolutionStrategy<V> {
    private final String headerName;
    private final VersionsDescription<V> versionsDescription;

    RequestHeaderVersionResolutionStrategy(String headerName, VersionsDescription<V> versionsDescription) {
        this.headerName = headerName;
        this.versionsDescription = versionsDescription;
    }

    @Override
    public V getSerializeToVersion(ObjectNode object) {
        return getParameter();
    }

    @Override
    public V getDeserializeToVersion(ObjectNode object) {
        return getParameter();
    }

    private V getParameter() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            String header = request.getHeader(headerName);
            if (header == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Missing API version request parameter %s", request));
            }
            V result = versionsDescription.fromString(header);
            if (result == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Unknown API version %s", header));
            }
            return result;
        } else {
            return null;
        }
    }
}
