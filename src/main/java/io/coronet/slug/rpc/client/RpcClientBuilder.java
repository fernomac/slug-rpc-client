package io.coronet.slug.rpc.client;

import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A builder for RPC clients.
 */
public class RpcClientBuilder<T> {

    private final ObjectMapper mapper = new ObjectMapper();

    private final Class<T> iface;
    private final URL endpoint;

    /**
     * @param iface the service interface type
     * @param endpoint the endpoint where the service is running
     */
    public RpcClientBuilder(Class<T> iface, String endpoint) {
        if (iface == null) {
            throw new NullPointerException("iface");
        }
        if (endpoint == null) {
            throw new NullPointerException("endpoint");
        }

        this.iface = iface;
        try {
            this.endpoint = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                    "Invalid endpoint '" + endpoint + "'",
                    e);
        }
    }

    /**
     * Gets the service endpoint URL.
     */
    URL getEndpoint() {
        return endpoint;
    }

    /**
     * Gets the object mapper to use.
     */
    ObjectMapper getMapper() {
        return mapper;
    }

    // TODO: Other configuration options.

    /**
     * Builds a client implementing the given service interface with the
     * given configuration.
     */
    public T build() {
        @SuppressWarnings("unchecked")
        T t = (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class[] { iface },
                new RpcClientHandler(this));
        return t;
    }
}
