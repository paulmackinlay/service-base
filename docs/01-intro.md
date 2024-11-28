# Introduction

The [state-machine](https://github.com/paulmackinlay/state-machine) has a Service API which provides
a simple, robust, state machine based framework for creating a standalone app or microservice. The
Service API is shipped with the state-machine library since it is closely coupled to the
state-machine implementation, and it is only a lightweight layer built on top of it.

[Documentation for the Service API can be found in the service section](https://github.com/paulmackinlay/state-machine/tree/main/docs).
Here is a quick recap:

- the Service API requires you to create single responsibility Subsystems (like one responsible for
  _"bootstrapping properties"_)
- Subsystems are stored in a List with an implicit order
- when the app (service) is started it starts each SubSystem in order
- when the app is stopped each Subsystem is stopped in reverse order
- the service API has `start` and `stop` methods for starting and stopping the app

The [service-base](https://github.com/paulmackinlay/service-base) library is a collection of
fundamental Subsystems and tools that make creating a standalone app quick and easy. The aim is to
provide a solid foundation for building an app.

## Subsystems and utilities

The following Subsystems and utilities are avialable.

### [PropSubsystem](../src/main/java/com/webotech/service/PropSubsystem.java) - bootstraps properties

The Subsystem initializes Property based configuration that can be used throughout an app. Properies
are loaded when the Subsystem starts and unloaded when it stops. Properties can be accessed
using [PropertyUtil](../src/main/java/com/webotech/util/PropertyUtil.java). There are mechanisms for
overriding, defaulting and converting properties into useful java constructs.

### [SupportSubsystem](../src/main/java/com/webotech/service/SupportSubsystem.java) - logs process fundamentals

The Subsystem logs fundamental information about the process, host and JVM. This information is
useful for 3rd line support. It also has a deadlock detection mechanism (that can be disabled) to
help resolve deadlock problems related to
threads. [SupportData](../src/main/java/com/webotech/service/data/SupportData.java) is made
available statically so it can be used by other parts of an application.

### Utilities

Here is a list of the utilities

- [PropertyUtil](../src/main/java/com/webotech/util/PropertyUtil.java) - for interacting with
  Property based configuration
- [ArgUtil](../src/main/java/com/webotech/util/ArgUtil.java) - helps with parsing process arguments
- [ServiceUtil](../src/main/java/com/webotech/util/ServiceUtil.java) - utilities for simplifying how
  an app is bootstrapped

TODO