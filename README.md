The Slug RPC Client is a little framework for talking to (some)
[JSON-RPC](http://www.jsonrpc.org/) over HTTP services.

## Defining a Service Interface

To begin, define an interface representing the service. Each method on the
interface should accept zero or one arguments and return zero or one
results. Parameters and results SHOULD be simple POJOs (allow me to suggest the
[slug-maven-plugin](https://github.com/coronet/slug-maven-plugin)), although
technically anything that [Jackson](http://wiki.fasterxml.com/JacksonHome)
can figure out will work.

```java
public interface MySweetService {

    /**
     * Does some sweet stuff.
     */
    DoSweetStuffResult doSweetStuff(DoSweetStuffRequest request);
}
```

## Creating a Client

Next use the `RpcClientBuilder` to create an instance of your interface
hooked up to a particular endpoint.

```java
String uri = "https://my.sweet.service/my/sweet/service";
MySweetService service = new RpcClientBuilder(MySweetService.class, uri)
        // Other optional configuration here.
        .build();
```

Calling methods on the returned `service` object will issue HTTP POST requests
to the provided endpoint with a JSON-RPC 2.0 request object in the payload
and attempt to parse a JSON-RPC 2.0 response object from the response payload.

## Some Details

- The name of the interface method is taken to be the name of the remote
procedure to call. I might look into an annotation to override that, but
keeping it simple for now.
- Only one argument is allowed, which is serialized to a JSON object and sent
in the `params` field of the request. No support for positional parameters is
planned.
- There is currently no support for notifications; `void` interface methods
will always wait for a response. I could be convinced, but it's not a high
priority.
- Ids are generated randomly on requests, but ignored on response since HTTP
provides its own (albeit inferior) mechanisms for request/response correlation.
- No support for batch requests yet. Potentially interesting in the future
maybe?

