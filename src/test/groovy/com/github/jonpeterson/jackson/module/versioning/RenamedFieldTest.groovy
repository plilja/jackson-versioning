package com.github.jonpeterson.jackson.module.versioning

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.Unroll


class RenamedFieldTest extends Specification {

    def mapper = new ObjectMapper().registerModule(new VersioningModule())

    @JsonVersionedModel(currentVersion = '3',
            defaultDeserializeToVersion = "3",
            toCurrentConverterClass = CarConverter,
            toPastConverterClass = CarConverter,
            propertyName = '_version'
    )
    static class Car {
        String makeV3
        Person ownerV3
        String _version
        @JsonSerializeToVersion
        String _toVersion
    }

    @JsonVersionedModel(currentVersion = '3',
            defaultDeserializeToVersion = "3",
            toCurrentConverterClass = PersonConverter,
            toPastConverterClass = PersonConverter,
            propertyName = '_version'
    )
    static class Person {
        String firstNameV3
        String lastNameV3
        String _version
        @JsonSerializeToVersion
        String _toVersion
    }

    static class CarConverter extends AbstractUpAndDownVersionedModelConverter {
        CarConverter() {
            super(Car.class)
            attributeRenamed("1", "2", "makeV1", "makeV2")
            attributeRenamed("2", "3", "makeV2", "makeV3")
            attributeRenamed("1", "2", "ownerV1", "ownerV2")
            attributeRenamed("2", "3", "ownerV2", "ownerV3")
        }
    }

    static class PersonConverter extends AbstractUpAndDownVersionedModelConverter {
        PersonConverter() {
            super(Person.class)
            attributeRenamed("1", "2", "firstNameV1", "firstNameV2")
            attributeRenamed("2", "3", "firstNameV2", "firstNameV3")
            attributeRenamed("1", "2", "lastNameV1", "lastNameV2")
            attributeRenamed("2", "3", "lastNameV2", "lastNameV3")
        }
    }

    @Unroll
    def 'to past version'() {
        when:
        def car = mapper.readValue('{"makeV3":"toyota","ownerV3":{"firstNameV3":"Per","lastNameV3":"Persson","_version":"3"},"_version":"3"}', Car)
        car._toVersion = toVersion
        car.ownerV3._toVersion = toVersion
        def actual = mapper.readValue(mapper.writeValueAsString(car), Map)

        then:
        actual == expected

        where:
        toVersion | expected
        1         | [_version: '1', makeV1: 'toyota', 'ownerV1': [_version: '1', firstNameV1: 'Per', 'lastNameV1': 'Persson']]
        2         | [_version: '2', makeV2: 'toyota', 'ownerV2': [_version: '2', firstNameV2: 'Per', 'lastNameV2': 'Persson']]
        3         | [_version: '3', makeV3: 'toyota', 'ownerV3': [_version: '3', firstNameV3: 'Per', 'lastNameV3': 'Persson']]
    }

    @Unroll
    def 'to current version'() {
        when:
        def carV1 = mapper.readValue('{"makeV1":"toyota","ownerV1":{"firstNameV1":"Per","lastNameV1":"Persson","_version":"1"},"_version":"1"}', Car)
        def carV3 = mapper.readValue('{"makeV3":"toyota","ownerV3":{"firstNameV3":"Per","lastNameV3":"Persson","_version":"3"},"_version":"3"}', Car)

        then:
        mapper.readValue(mapper.writeValueAsString(carV1), Map) == mapper.readValue(mapper.writeValueAsString(carV3), Map)
    }
}

