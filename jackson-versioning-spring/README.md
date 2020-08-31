# jackson-versioning-spring
Spring MVC bindings for [jackson-versioning](https://github.com/plilja/jackson-versioning).

Simplifies making braking changes to your API. By telling
Jackson how to convert between you API versions you can
keep only the latest version of you code.

Supports versions determined by either a request header or 
a request parameter. 

Outgoing responses are converted to to a version compatible
with the caller.

Incoming request bodies are converted to the latest version
compatible with the application code.

## Usage
To use, register the versioning module on your object mapper:

```java
@Configuration
class YourConfiguration {
    @Bean
    ObjectMapper objectMapper(ApplicationContext applicationContext) {
            var versioningModule = SpringVersioningModuleBuilder.withEnumVersions(YourVersionEnum.class)
                    .withVersionDeterminedByRequestHeader("YOUR_HEADER_NAME") 
                    .withConvertersFromApplicationContext(applicationContext)
                    .build();
            return new ObjectMapper().registerModule(versioningModule);
    }
}
```

Your model classes that you want versioned should be annotated like this:

```java
@JsonVersioned(converterClass = PersonConverter.class)
class Person {
    // ...
}
```
Your converter class should look something like this:
```java
@Component
public class PersonConverter extends AbstractVersionConverter<YourVersionEnum> {
    public PersonConverter() {
        super(Person.class);
        // Your convertions goes here ...
        attributeAdded(YourVersionEnum.V1, YourVersionEnum.V2, "ssn", "1234567890");
        attributeRenamed(YourVersionEnum.V2, YourVersionEnum.V3, "ssn", "socialSecurityNumber");
    }
}
```


