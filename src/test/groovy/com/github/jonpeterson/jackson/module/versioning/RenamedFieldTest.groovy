package com.github.jonpeterson.jackson.module.versioning

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.Unroll


class RenamedFieldTest extends Specification {

    def mapper = new ObjectMapper().registerModule(new VersioningModule(Vs))

    enum Vs {
        V1, V2, V3
    }

    @JsonVersioned(converterClass = CarConverter)
    static class Car {
        String makeV3
        Person ownerV3
        @JsonVersionAttribute
        String _version
        @JsonVersionToAttribute
        String _toVersion
    }

    @JsonVersioned(converterClass = PersonConverter)
    static class Person {
        String firstNameV3
        String lastNameV3
        @JsonVersionAttribute
        String _version
        @JsonVersionToAttribute
        String _toVersion
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
        def car = mapper.readValue('{"makeV3":"toyota","ownerV3":{"firstNameV3":"Per","lastNameV3":"Persson","_version":"V3"},"_version":"V3"}', Car)
        car._toVersion = toVersion
        car.ownerV3._toVersion = toVersion
        def actual = mapper.readValue(mapper.writeValueAsString(car), Map)

        then:
        actual == expected

        where:
        toVersion | expected
        Vs.V1     | [_version: 'V1', makeV1: 'toyota', 'ownerV1': [_version: 'V1', firstNameV1: 'Per', 'lastNameV1': 'Persson']]
        Vs.V2     | [_version: 'V2', makeV2: 'toyota', 'ownerV2': [_version: 'V2', firstNameV2: 'Per', 'lastNameV2': 'Persson']]
        Vs.V3     | [_version: 'V3', makeV3: 'toyota', 'ownerV3': [_version: 'V3', firstNameV3: 'Per', 'lastNameV3': 'Persson']]
    }

    def 'to current version'() {
        when:
        def carV1 = mapper.readValue('{"makeV1":"toyota","ownerV1":{"firstNameV1":"Per","lastNameV1":"Persson","_version":"V1"},"_version":"V1"}', Car)
        def carV3 = mapper.readValue('{"makeV3":"toyota","ownerV3":{"firstNameV3":"Per","lastNameV3":"Persson","_version":"V3"},"_version":"V3"}', Car)

        then:
        mapper.readValue(mapper.writeValueAsString(carV1), Map) == mapper.readValue(mapper.writeValueAsString(carV3), Map)
    }
}

