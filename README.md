A record-to-bean generator and mapper.

Java has records, but also an ecosystem that fundamentally opposes their use in many respects: that is, many libraries and applications are "bean"-oriented. Beans are mutable and have different accessor methods than records.
_beanerator_ works to bridge this gap by autogenerating bean classes for your records - this allows the use of records and their features while providing interoperability with other libraries still stuck in the dark ages.

## installation and dependencies

No external runtime dependencies.

Recommended installation method is to use maven. Check the latest package on github.

### known limitations

#### nesting classes

_Beanerator_ will process and generate classes for annotated records that are nested in other classes/records, but depending on usage the `.fromRecord()`/`.toRecord()` methods may not end up with the right argument types.

The workaround is to simply not define record component types as child record types.
This could be solved if there's any interest in supporting this usage, but for now, I don't see it as a priority.

#### providing annotations and interfaces

You can tell _Beanerator_ to add any desired annotations and/or interfaces to the generated classes. However, due to limitations of the java language, you must provide these qualified classnames as strings, and it's up to you to ensure you provide annotations to `annotations` and interfaces to `interfaces`, else you'll get compile errors.

As above, I don't see value in handling this for the developer, but if there is interest I can look into adding those guardrails.

## usage

Simply add the `@Beanerate` annotation to your record classes, and a basic "bean" class will be generated based on its components:
```java
@Beanerate
public record Coffee(Variety variety, Flavor flavor, ...) {}
```

This will generate a class similar to:
```java
public class CoffeeBean {

  public static CoffeeBean fromRecord(Coffee record) { ... }

  public CoffeeBean() {}
  public CoffeeBean(Variety variety, Flavor flavor, ...) { ... }

  private Variety variety = null;
  public Variety getVariety() { ... }
  public CoffeeBean setVariety(Variety variety) { ... }

  private Flavor flavor = null;
  public Flavor getFlavor() { ... }
  public CoffeeBean setFlavor(Flavor flavor) { ... }

  . . .

  public Coffee toRecord() { ... }
}
```

The `@Beanerate` annotation also allows you to pass annotations and interfaces* to be applied to the generated bean. For example,
```java
@Beanerate(annotations={"your.Annotation"}, interfaces={"your.one.Interface", "your.other.Interface"})
record Foo ( ... ) {}
```
...would result in something like
```java
@your.Annotation
class FooBean implements your.one.Interface, your.other.Interface {
  ...
}
```
_* see ### known limitations: annotations and interfaces_

## tests

Tests are still being added. you can run the test suite via maven:
```
mvn test
```

## contributing or getting help

I'm on IRC at [libera#__adrian](https://web.libera.chat/#__adrian), or open an issue on github. Feedback is welcomed as well!
