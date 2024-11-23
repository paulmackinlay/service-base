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
    <version>0.0.5</version>
</dependency>
```

or this dependency in gradle

```groovy
implementation 'com.webotech:service-base:0.0.5'
```

**Please use the latest version available in maven central - the version in this page may be old.**

## Quick start

- Create a service class that extends `AbstractAppService`
- Create a context class that extends `AbstractAppContext`
- The constructor of the service will need the context, the `PropSubsystem` and the
  `SupportSubsystem`
- The service needs a main method that uses `ServiceUtil` to start the service

### Example App

```java
public class ExampleApp extends AbstractAppService<AppContext> {

  ExampleApp(String[] args) {
    super(new AppContext("ExampleApp", args)
        .withSubsystems(List.of(new PropSubsystem<>(), new SupportSubsystem<>())));
  }

  public static void main(String[] args) {
    ServiceUtil.startService(new ExampleApp(args));
  }

  public static class AppContext extends AbstractAppContext<AppContext> {

    protected AppContext(String appName, String[] initArgs) {
      super(appName, initArgs);
    }
  }
}
```