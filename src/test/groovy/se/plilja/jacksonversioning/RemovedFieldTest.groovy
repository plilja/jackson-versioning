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

class RemovedFieldTest extends Specification {

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
        // int yearMade; removed version 2
        // String registrationPlate; // removed version 3
        Person owner
    }

    @JsonVersioned(converterClass = PersonConverter)
    static class Person {
        String firstName
        String lastName
        // String socialSecurityNumber // Removed version 2
    }

    static class CarConverter extends AbstractVersionConverter<Vs> {
        CarConverter() {
            super(Car.class)
            attributeRemoved(Vs.V1, Vs.V2, "yearMade", 2020)
            attributeRemoved(Vs.V2, Vs.V3, "registrationPlate", "ABC-123")
        }
    }

    static class PersonConverter extends AbstractVersionConverter<Vs> {
        PersonConverter() {
            super(Person.class)
            attributeRemoved(Vs.V1, Vs.V2, "socialSecurityNumber", "1234567890")
        }
    }

    @Unroll
    def 'to past version'() {
        when:
        versionStrategy.setVersion(Vs.V3)
        def car = mapper.readValue('{"model":"camry","make":"toyota","owner":{"firstName":"Per","lastName":"Persson"}}', Car)
        versionStrategy.setVersion(toVersion)
        def actual = mapper.readValue(mapper.writeValueAsString(car), Map)

        then:
        actual == expected

        where:
        toVersion | expected
        Vs.V1     | [make: 'toyota', model: 'camry', 'yearMade': 2020, 'registrationPlate': 'ABC-123', 'owner': [firstName: 'Per', 'lastName': 'Persson', 'socialSecurityNumber': '1234567890']]
        Vs.V2     | [make: 'toyota', model: 'camry', 'registrationPlate': 'ABC-123', 'owner': [firstName: 'Per', 'lastName': 'Persson']]
        Vs.V3     | [make: 'toyota', model: 'camry', 'owner': [firstName: 'Per', 'lastName': 'Persson']]
    }

    def 'to current version'() {
        when:
        versionStrategy.setVersion(Vs.V1)
        def carV1 = mapper.readValue('{"model":"camry","make":"toyota","owner":{"firstName":"Per","lastName":"Persson","socialSecurityNumber":"1234567890"},"registrationPlate":"ABC-123","yearMade":2020}', Car)
        versionStrategy.setVersion(Vs.V3)
        def carV3 = mapper.readValue('{"model":"camry","make":"toyota","owner":{"firstName":"Per","lastName":"Persson"}}', Car)
        def actual = mapper.readValue(mapper.writeValueAsString(carV1), Map)
        def expected = mapper.readValue(mapper.writeValueAsString(carV3), Map)

        then:
        actual == expected
    }
}
