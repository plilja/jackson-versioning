/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jon Peterson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package se.plilja.jacksonversioning

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.Unroll


class RenamedFieldTest extends Specification {

    def mapper = createMapper()
    def versionStrategy

    def createMapper() {
        versionStrategy = new FixedVersionStrategy<Vs>();
        def versionsDescription = new EnumVersionsDescription<>(Vs.class)
        return new ObjectMapper().registerModule(new VersioningModule(versionsDescription, versionStrategy))
    }

    @JsonVersioned(converterClass = CarConverter)
    static class Car {
        String makeV3
        Person ownerV3
    }

    @JsonVersioned(converterClass = PersonConverter)
    static class Person {
        String firstNameV3
        String lastNameV3
    }

    static class CarConverter extends AbstractVersionConverter<Vs> {
        CarConverter() {
            super(Car.class)
            attributeRenamed(Vs.V1, Vs.V2, "makeV1", "makeV2")
            attributeRenamed(Vs.V2, Vs.V3, "makeV2", "makeV3")
            attributeRenamed(Vs.V1, Vs.V2, "ownerV1", "ownerV2")
            attributeRenamed(Vs.V2, Vs.V3, "ownerV2", "ownerV3")
        }
    }

    static class PersonConverter extends AbstractVersionConverter<Vs> {
        PersonConverter() {
            super(Person.class)
            attributeRenamed(Vs.V1, Vs.V2, "firstNameV1", "firstNameV2")
            attributeRenamed(Vs.V2, Vs.V3, "firstNameV2", "firstNameV3")
            attributeRenamed(Vs.V1, Vs.V2, "lastNameV1", "lastNameV2")
            attributeRenamed(Vs.V2, Vs.V3, "lastNameV2", "lastNameV3")
        }
    }

    @Unroll
    def 'to past version'() {
        when:
        versionStrategy.setVersion(Vs.V3)
        def car = mapper.readValue('{"makeV3":"toyota","ownerV3":{"firstNameV3":"Per","lastNameV3":"Persson"}}', Car)
        versionStrategy.setVersion(toVersion)
        def actual = mapper.readValue(mapper.writeValueAsString(car), Map)

        then:
        actual == expected

        where:
        toVersion | expected
        Vs.V1     | [makeV1: 'toyota', 'ownerV1': [firstNameV1: 'Per', 'lastNameV1': 'Persson']]
        Vs.V2     | [makeV2: 'toyota', 'ownerV2': [firstNameV2: 'Per', 'lastNameV2': 'Persson']]
        Vs.V3     | [makeV3: 'toyota', 'ownerV3': [firstNameV3: 'Per', 'lastNameV3': 'Persson']]
    }

    def 'to current version'() {
        when:
        versionStrategy.setVersion(Vs.V1)
        def carV1 = mapper.readValue('{"makeV1":"toyota","ownerV1":{"firstNameV1":"Per","lastNameV1":"Persson"}}', Car)
        versionStrategy.setVersion(Vs.V3)
        def carV3 = mapper.readValue('{"makeV3":"toyota","ownerV3":{"firstNameV3":"Per","lastNameV3":"Persson"}}', Car)

        then:
        mapper.readValue(mapper.writeValueAsString(carV1), Map) == mapper.readValue(mapper.writeValueAsString(carV3), Map)
    }
}

