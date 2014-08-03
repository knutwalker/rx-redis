RxRedis
========

Reactive Extensions for Redis.

## Motivation
###### Or why yet another redis client

RxRedis is a client for [redis](http://redis.io).
It is different from other clients, in that it's using [Reactive Extensions](http://reactivex.io/) for its API.

Using RxJava allows for some nice abstractions. So is, for example, the return type of `GET` and `MGET` identical; It's just, that the Observable of `GET` completes after the first value.

## Overview

RxRedis is written for Scala 2.11 and Java 8 and has different language specific bindings for each of them. It uses Netty under the hood for network transportation and RxJava for its API. RxRedis itself is written in Scala.

RxRedis comes with a small type tree for the Redis Serialization Protocol (RESP), a netty pipeline, that can send and receive such RESP types, and a client, that hides this pipeline behind RxJava.


## Installation

RxRedis can be obtained from [Maven Central](http://search.maven.org/#search|ga|1|g%3A%22de.knutwalker%22%20AND%20a%3Arx-redis-*_2.11%20AND%20v%3A%220.3.0%22).

The main artifacts are `rx-redis-java_2.11` for the Java binding and `rx-redis-scala_2.11` for the Scala binding. The current version is 0.3.0.

For maven:

    <dependency>
        <groupId>de.knutwalker</groupId>
        <artifactId>rx-redis-java_2.11</artifactId>
        <version>0.3.0</version>
    </dependency>


For sbt:

    libraryDependencies += "de.knutwalker" %% "rx-redis-scala" % "0.3.0"


RxRedis has a dependency on Netty and RxJava. The Scala binding also depend on the Scala bindings of RxJava.
As it is written in Scala, RxRedis also depends on scala-library and the scala-reflect module, though I'm trying to get the last one out of the runtime classpath. Here's the full graph (except for scala-library):

<a href="https://raw.githubusercontent.com/knutwalker/rx-redis/v0.3.0/dependency-graph.png"><img src="https://raw.githubusercontent.com/knutwalker/rx-redis/v0.3.0/dependency-graph.png" alt="dependency-graph" width="640" height="171"></a>


`rx-redis-parent` is the umbrella artifact and should not be used.

`rx-redis-client` is a raw client, that supports the complete API, but is more or less inconvenient to use from your target language.

`rx-redis-pipeline` contains the netty pipeline. It could also be used for a different client that uses Netty.

`rx-redis-core` contains the RESP types and all type class definitions. Additional support libraries or modules (e.g. for Jackson Support) only need to depend on this artifact.



## Usage

RxRedis only exposes its API with RxJava, thus it only supports asynchronous execution and has no built-in synchronous client. That being said, there is a `Observable::toBlocking()` method, to get effectively blocking behaviour.


### Scala


```scala

import rx.redis.api.RxRedis

val client = RxRedis("localhost", 6379)
client.set("foo", "bar")
client.get("foo", "bar").foreach(println)

client.del("foo").doOnCompleted(client.shutdown()).subscribe()
RxRedis.await(client)

```


### Java


```java

import rx.redis.japi.RxRedis;
import rx.redis.japi.Client;

...

final Client client = RxRedis.connect("localhost", 6379);
client.set("foo", "bar");
client.get("foo", "bar").forEach(System.out::println);

client.del("foo").doOnCompleted(client::shutdown).subscribe();
RxRedis.await(client);

```


The examples are very similar and probably not much suprising.
Two things to point out:

1. The call to `client.xxx.doOnCompleted(client.shutdown()).subscribe()`.
    
    doOnComplete notifies the client, that it should shutdown after this command has completed.
    The `subscribe` is necessary, so that the callback actually fires. Without subscription, the onCompleted message would be lost.
2. The call to `RxRedis.await(client)`.

    Since the call to any command is asynchronous, you can't just close the client at the end of your script, since the commands might be fliying around the network. `RxRedis.await` is a helper, that blocks until the last response from the server was delivered to the client. This requires, that you call `client.shutdown()` at some point, otherwise `await` would block forever.


An instance of this Client is thread-safe, almost all operations are executed on nettys event loop.


### Custom types

You can send arbitrary types (POJOs) with RxRedis. All you have to do is implement an instance of `rs.redis.serialization.BytesFormat` for your type. For Scala: this is a type class and should be made implicitly available. Commands that support custom types end on `As` and are parameterized in their type.

#### Scala


```scala

// given implicit evidence of BytesFormat[Person]
client.setAs("danger", Person("Heisenberg"))
client.getAs[Person]("danger")

```

#### Java


```java

BytesFormat<Person> personFormat = ...;
client.setAs("danger", new Person("Heisenberg"), personFormat);
client.getAs("danger", personFormat);

```

The trait/interface `BytesFormat` has two simple methods: `bytes` and `value` for transforming your type into an byte array and back.


For Scala, instances for `String`, `Array[Byte]`, `Long`, `Duration`, and `Deadline` are provided.

For Java, these instances are available in `rx.redis.japi.DefaultBytes.*`.


### Arbitrary commands

At the moment, very few commands are actually implemented. However, you still have the possibility to send anything you want. You just pass a string that you would have entered in the redis-cli, for example.

#### Scala


```scala

import rx.redis._

client.command(cmd"INFO SERVER")
val part = "memory"
client.command(cmd"INFO $part")

```


#### Java


```java

client.command(RxRedis.command("INFO SERVER"));
String part = "memeory";
client.command(RxRedis.command("INFO " + part));

```


In addition to that, there is a type class `rx.redis.serialization.Writes`, that is used to determin how a type `A` gets transformed into the Redis Serialzation Protocol (RESP).
For every command, there exists a case class and a macro generates the implementation of `Writes` for this case class.
The client APIs then just send a new instance of some case class to the netty pipeline.

You can also send any `A` for which you can provide an instance of `Writes[A]`.


## Working with the Source

RxRedis is built with sbt and comes with a launch script, you only need a JDK installed. After downloading/cloning, run `./sbt` to drop into the sbt shell. Some things to can do there, besides `compile` and `test`:

- `example/run` to run the Scala examples
- `java-example/run` to run the Java examples
- `it:test` to tun integration tests, need a Redis instance running **DB 0 will be deleted!**
- `reg:test` to run regression tests (codec tests)
- `publishM2` to install the snapshot version locally


## Future

Plans for future versions (in no particular order or timeline):

- Support all commands in the API
- Support for transactions (MULTI/EXEC)
- Support for Redis Cluster
- Connection pooling/client-side cluster
- Possible support for pipelining (maybe transparent to transactions)
- Support for backpressure, reconnecting clients, etc.
- Cross compile for Scala 2.10, maybe also for Java 7
- BytesFormat implementation for typical data formats, such as Json (e.g. Jackson, Gson, various Scala ones), Protobuf, etc...
- Language binding for Clojure (Groovy, Kotlin, ...? Basically, anything that is supported by RxJava)
- Abstract backend implementation, build on reactive-streams am make backend pluggable (e.g. RxNetty, Akka streams, ...)
- ...
