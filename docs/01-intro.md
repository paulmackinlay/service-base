# Introduction

The [state-machine](https://github.com/paulmackinlay/state-machine) has a Service API which provides
a simple and robust, state machine based framework for creating a standalone app or microservice.
The Service API is shipped with the state-machine library since it is closely coupled to it's
implementation and it is only a lightweight layer on top of it.

[Documentation for the Service API can be found in the service section](https://github.com/paulmackinlay/state-machine/tree/main/docs).
Here is a quick recap:

- the Service API requires you to create single responsibility Subsystems (like one responsible for
  _"bootstrapping properties"_)
- Subsystems are stored in a List with an implicit order
- when the app (service) is started, it starts each Subsystem in order
- when the app is stopped, each Subsystem is stopped in reverse order
- the service API provides an AppContext where app-level state can be stored

The [service-base](https://github.com/paulmackinlay/service-base) library builds upon the service
API, it contains essential and general functionality which forms the basis of an app. The library is
a collection of fundamental Subsystems and tools that make creating a standalone app quick and easy.
The design of the Service API is flexible, a 'first cut' app that uses service-base can be extended
and enhanced to any desired level as the codebase progresses. In short, it's a quick and robust way
of building anything from a proof of concept to a full-blown enterprise system.

## Subsystems and utilities

The following Subsystems and utilities are available.

### [PropSubsystem](../src/main/java/com/webotech/service/PropSubsystem.java) - bootstraps properties

The PropSubsystem initializes Property based configuration that can be used throughout an app.
Properties can be loaded before the Subsystem is constructed, at construction time or when it
starts. When loaded at start time, they will also be unloaded when the Subsystem stops. Properties
can be accessed using [PropertyUtil](../src/main/java/com/webotech/util/PropertyUtil.java) and there
are mechanisms for logging, overriding, defaulting and converting properties into useful java
constructs.

### [SupportSubsystem](../src/main/java/com/webotech/service/SupportSubsystem.java) - logs process fundamentals

The SupportSubsystem logs fundamental information about the process, host and JVM. It has a deadlock
detection mechanism that logs thread information when deadlocked. Logging this information is useful
for 3rd line support. [SupportData](../src/main/java/com/webotech/service/data/SupportData.java) is
also made available statically so it can be used by other parts of an application.

### Utilities

Here is a list of the utilities

- [PropertyUtil](../src/main/java/com/webotech/util/PropertyUtil.java) - for interacting with
  Property based configuration
- [ArgUtil](../src/main/java/com/webotech/util/ArgUtil.java) - helps with parsing process arguments
- [ServiceUtil](../src/main/java/com/webotech/util/ServiceUtil.java) - utilities for simplifying how
  an app is bootstrapped
