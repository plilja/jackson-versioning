/**
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
package se.plilja.jacksonversioning

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.Unroll

class ModifiedFieldTest extends Specification {

    def mapper = createMapper()
    def versionStrategy

    def createMapper() {
        versionStrategy = new FixedVersionStrategy<Vs>();
        def versionsDescription = new EnumVersionsDescription<>(Vs.class)
        return new ObjectMapper().registerModule(new VersioningModule(versionsDescription, versionStrategy))
    }

    @JsonVersioned(converterClass = CarConverter)
    static class Car {
        String model
        String make
        Boolean used // Changed from !new version 2
        Person owner
    }

    @JsonVersioned(converterClass = PersonConverter)
    static class Person {
        String firstName // Split from name = firstName + " " + lastName version 3
        String lastName
    }

    static class CarConverter extends AbstractVersionConverter<Vs> {
        CarConverter() {
            super(Car.class)
            attributeRenamed(Vs.V2, "new", "used")
            attributeModified(Vs.V2, "used", { data, attribute -> !attribute.asBoolean() }, { data, attribute -> !attribute.asBoolean() })
        }
    }

    static class PersonConverter extends AbstractVersionConverter<Vs> {
        PersonConverter() {
            super(Person.class)
            attributeAdded(Vs.V3, "lastName", { data ->
                return data.get("name").asText().split(" ")[1]
            })
            attributeModified(Vs.V3, "name",
                    { data, attribute ->
                        def lastName = data.get("lastName").asText()
                        return attribute.asText() + " " + lastName
                    },
                    { data, attribute ->
                        return data.get("name").asText().split(" ")[0]
                    }
            );
            attributeRenamed(Vs.V3, "name", "firstName")
        }
    }

    @Unroll
    def 'to past version'() {
        when:
        versionStrategy.setVersion(Vs.V3)
        def car = mapper.readValue('{"model":"camry","make":"toyota","used":true,"owner":{"firstName":"Per","lastName":"Persson"}}', Car)
        versionStrategy.setVersion(toVersion)
        def actual = mapper.readValue(mapper.writeValueAsString(car), Map)

        then:
        actual == expected

        where:
        toVersion | expected
        Vs.V1     | [make: 'toyota', model: 'camry', new: false, 'owner': [name: 'Per Persson']]
        Vs.V2     | [make: 'toyota', model: 'camry', used: true, 'owner': [name: 'Per Persson']]
        Vs.V3     | [make: 'toyota', model: 'camry', used: true, 'owner': [firstName: 'Per', 'lastName': 'Persson']]
    }

    def 'to current version'() {
        when:
        versionStrategy.setVersion(Vs.V1)
        def carV1 = mapper.readValue('{"model":"camry","make":"toyota","new":true,"owner":{"name":"Per Persson"}}', Car)
        versionStrategy.setVersion(Vs.V3)
        def carV3 = mapper.readValue('{"model":"camry","make":"toyota","used":false,"owner":{"firstName":"Per","lastName":"Persson"}}', Car)
        def actual = mapper.readValue(mapper.writeValueAsString(carV1), Map)
        def expected = mapper.readValue(mapper.writeValueAsString(carV3), Map)

        then:
        actual == expected
    }
}
