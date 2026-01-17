# service-base

This repo contains base functionality for
the [service API](https://github.com/paulmackinlay/state-machine). It is a collection of fundamental
subsystems and tools to be used as the basis of any app or microservice. It includes:

- A subsystem that bootstraps application configuration based on properties
- A subsystem that prepares and logs fundamental information useful for 3rd line support
- Utilities to help with command line arguments, properties and starting a process
- Small dependency footprint consisting of
  a [service API](https://github.com/paulmackinlay/state-machine) and a logging API
- Extensively tested

In the [docs](docs/01-intro.md) you will find examples of how to use this library.

## Minimum Java Version

This project uses a Java language specification version 17, it is compatible with Java 17 and
higher.

## Use service-base with maven or gradle

This project is in [maven central](https://central.sonatype.com/artifact/com.webotech/service-base),
to start using it just add this dependency to your POM.

```xml

<dependency>
  <groupId>com.webotech</groupId>
  <artifactId>service-base</artifactId>
  <version>1.2.1</version>
</dependency>
```

or this dependency in gradle

```groovy
implementation 'com.webotech:service-base:1.2.1'
```

**Please use the latest version available in maven central - the version in this page may be old.**

## Quick start

- Create a service class that extends `AbstractAppService<BasicAppContext>`
- Use `ServiceUtil` to pre-emptively initialize application properties before the app is constructed
  and to provide a `BasicAppContext` equipped with recommended Subsystems
- The service needs a main method that uses `ServiceUtil` to start it

### Example App

```java
import com.webotech.util.ServiceUtil;

public class ExampleApp extends AbstractAppService<BasicAppContext> {

  ExampleApp(String[] args) {
    super(ServiceUtil.preemptAppProps(args).equipBasicContext("ExampleApp", args));
  }

  public static void main(String[] args) {
    ServiceUtil.startService(new ExampleApp(args));
  }
}
```