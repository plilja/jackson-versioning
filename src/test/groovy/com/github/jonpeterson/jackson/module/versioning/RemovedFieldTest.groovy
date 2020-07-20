package com.github.jonpeterson.jackson.module.versioning

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.Unroll

class RemovedFieldTest extends Specification {

    def mapper = new ObjectMapper().registerModule(new VersioningModule())

    @JsonVersionedModel(currentVersion = '3',
            defaultDeserializeToVersion = "3",
            toCurrentConverterClass = CarConverter,
            toPastConverterClass = CarConverter,
            propertyName = '_version'
    )
    static class Car {
        String model
        String make
        // int yearMade; removed version 2
        // String registrationPlate; // removed version 3
        Person owner
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
        String firstName
        String lastName
        String _version
        @JsonSerializeToVersion
        String _toVersion
        // String socialSecurityNumber // Removed version 2
    }

    static class CarConverter extends AbstractUpAndDownVersionedModelConverter {
        CarConverter() {
            super(Car.class)
            attributeRemoved("1", "2", "yearMade", 2020)
            attributeRemoved("2", "3", "registrationPlate", "ABC-123")
        }
    }

    static class PersonConverter extends AbstractUpAndDownVersionedModelConverter {
        PersonConverter() {
            super(Person.class)
            attributeRemoved("1", "2", "socialSecurityNumber", "1234567890")
        }
    }

    @Unroll
    def 'to past version'() {
        when:
        def car = mapper.readValue('{"model":"camry","make":"toyota","owner":{"firstName":"Per","lastName":"Persson","_version":"3"},"_version":"3"}', Car)
        car._toVersion = toVersion
        car.owner._toVersion = toVersion
        def actual = mapper.readValue(mapper.writeValueAsString(car), Map)

        then:
        actual == expected

        where:
        toVersion | expected
        1         | [_version: '1', make: 'toyota', model: 'camry', 'yearMade': 2020, 'registrationPlate': 'ABC-123', 'owner': [_version: '1', firstName: 'Per', 'lastName': 'Persson', 'socialSecurityNumber': '1234567890']]
        2         | [_version: '2', make: 'toyota', model: 'camry', 'registrationPlate': 'ABC-123', 'owner': [_version: '2', firstName: 'Per', 'lastName': 'Persson']]
        3         | [_version: '3', make: 'toyota', model: 'camry', 'owner': [_version: '3', firstName: 'Per', 'lastName': 'Persson']]
    }

    @Unroll
    def 'to current version'() {
        when:
        def carV1 = mapper.readValue('{"model":"camry","make":"toyota","owner":{"firstName":"Per","lastName":"Persson","socialSecurityNumber":"1234567890","_version":"1"},"registrationPlate":"ABC-123","yearMade":2020,"_version":"1"}', Car)
        def carV3 = mapper.readValue('{"model":"camry","make":"toyota","owner":{"firstName":"Per","lastName":"Persson","_version":"3"},"_version":"3"}', Car)
        def actual = mapper.readValue(mapper.writeValueAsString(carV1), Map)
        def expected = mapper.readValue(mapper.writeValueAsString(carV3), Map)

        then:
        actual == expected
    }
}
