# Jackson Versioning Module
Jackson 2.x module for handling versioning of models.

## The Problem
Let's say we create an API that accepts the following Car data JSON:
```json
{
  "model": "civic",
  "year": 2016,
  "new": true
}
```

Later, we decide to add the `make` attribute. But we don't want to make the new attribute nullable as every car should have a make.

```json
{
  "make": "honda",
  "model": "civic",
  "year": 2016,
  "new": true
}
```

Then we decide that `new` should be actually be renamed to `used` and inverted.
```json
{
  "make": "honda",
  "model": "civic",
  "year": 2016,
  "used": false
}
```

By this point, we have three formats of the Car model that clients might be sending to 
or requesting from our API. 

This isn't a new problem, a common solution is to create new classes each time a breaking
change needs to occur. Then manually in the controller implement the conversion between new
and old data formats. 

This works well if breaking changes are rare. But if you make changes like this often 
this will quickly get out of hand. Generally you don't want to be in a spot where it's 
expensive to make changes.

Another problematic scenario is if your model classes are nested (maybe you have a 
CarManufacturer class that holds a lists of Cars).  

These are problems that this project attempts to solve.

```
POST /api/car/v1/     <-  CarV1
GET  /api/car/v1/     ->  List<CarV1>
GET  /api/car/v1/{id} ->  CarV1

POST /api/car/v2/     <-  CarV2
GET  /api/car/v2/     ->  List<CarV2>
GET  /api/car/v2/{id} ->  CarV2
...
```

## Examples

#### Basic Usage

**Declare your versions. The simplest way is to use an Enum.**

```java
public enum ApiVersion {
    V1, V2, V3;
}
```

**Create a model for the newest version of the data in the example above. Annotate the 
model as versioned and specify which class to use for conversions.**

```java
@JsonVersioned(converterClass = CarConverter.class)
public class Car {
    private String make;
    private String model;
    private int year;
    private boolean used;
    private ApiVersion version; // Holds the api version

    // getters and setters left out for brevity...
}
```

**Create the converter class. The simplest way is to extends the AbstractVersionConverter base class.**
```java
public class CarConverter extends AbstractVersionConverter<ApiVersion> {
    public CarConverter() {
        super(Car.class);
        attributeAdded(ApiVersion.V1, ApiVersion.V2, "make", (data) -> {
            JsonNode model = data.get("model");
            return new CarService().getMakeFromModel(model.asText());
        });
        attributeModified(ApiVersion.V2, ApiVersion.V3, "new", (data, field) -> !field.asBoolean(), (data, field) -> !field.asBoolean());
        attributeRenamed(ApiVersion.V2, ApiVersion.V3, "new", "used");
    }
}
```

**Determine how versions are resolved. This can for example be a request parameter or an attribute on the model class.**

```java
public class AttributeVersionResolutionStrategy implements VersionResolutionStrategy<ApiVersion> {
    public ApiVersion getSerializeToVersion(ObjectNode object) {
        return ApiVersion.valueOf(object.get("version").asText());
    }

    public ApiVersion getDeserializeToVersion(ObjectNode object) {
        return ApiVersion.valueOf(object.get("version").asText());
    }
}
```

**Configure the Jackson ObjectMapper with the module and test it out.**
```java
class ExampleProgram {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new VersioningModule(new EnumVersionsDescription<>(ApiVersion.class), new AttributeVersionResolutionStrategy()));

        // version 1 JSON -> POJO current version
        Car hondaCivic = mapper.readValue(
                "{\"model\":\"civic\",\"year\":2016,\"version\":\"V1\",\"new\":true}",
                Car.class
        );

        // POJO current version -> version 1 JSON
        System.out.println(mapper.writeValueAsString(hondaCivic));
        // prints '{"model":"civic","year":2016,"version":"V1","new":true}'

        hondaCivic.setVersion(ApiVersion.V3);
        System.out.println(mapper.writeValueAsString(hondaCivic));
        // prints '{"make":"Honda","model":"civic","year":2016,"used":false,"version":"V3"}'
    }
}
```

## Compatibility
* Requires Java 8 or higher
* Requires Jackson 2.2 or higher

## Also check out

This project is originally forked: 
* [jackson-module-model-versioning](https://github.com/jonpeterson/jackson-module-model-versioning)

For Spring bindings please check out:
* [jackson-versioning-spring](https://github.com/plilja/jackson-versioning-spring)
