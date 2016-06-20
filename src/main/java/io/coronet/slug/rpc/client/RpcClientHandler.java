package io.coronet.slug.rpc.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

final class RpcClientHandler implements InvocationHandler {

    private final URL endpoint;
    private final ObjectMapper mapper;

    public RpcClientHandler(RpcClientBuilder<?> builder) {
        this.endpoint = builder.getEndpoint();
        this.mapper = builder.getMapper();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(proxy, args);
        }

        if (args.length != 1) {
            throw new IllegalStateException(
                    "Method " + method + " does not take one argument");
        }

        try {
            return doInvoke(proxy, method, args[0]);
        } catch (IOException e) {
            throw new TransportException(e.getMessage(), e);
        }
    }

    private Object doInvoke(Object proxy, Method method, Object arg)
            throws IOException {

        byte[] payload = serialize(method.getName(), arg);

        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();

        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setFixedLengthStreamingMode(payload.length);

        // TODO: Include more client version information?
        conn.setRequestProperty("User-Agent", "Slug");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("X-Slug-Rpc-Version", "1");

        // TODO: Authentication.

        // TODO: Configurable timeouts.
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (OutputStream out = conn.getOutputStream()) {
            out.write(payload);
        }

        if (conn.getResponseCode() != 200) {
            // TODO: Better protocol for errors.
            throw new TransportException(conn.getResponseMessage());
        }

        byte[] response = buffer(conn.getInputStream());

        Type type = method.getGenericReturnType();
        TypeFactory tf = mapper.getTypeFactory();
        return mapper.readValue(response, tf.constructType(type));
    }

    private byte[] serialize(String target, Object arg) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonGenerator gen = mapper.getFactory().createGenerator(baos)) {
            gen.writeStartObject();

            gen.writeFieldName("target");
            gen.writeString(target);

            gen.writeFieldName("payload");
            mapper.writeValue(gen, arg);

            gen.writeEndObject();
        }

        return baos.toByteArray();
    }

    private byte[] buffer(InputStream stream) throws IOException {
        int offset = 0;
        byte[] buffer = new byte[4 * 1024];

        while (offset < buffer.length) {
            int read = stream.read(buffer, offset, buffer.length - offset);
            if (read < 0) {
                return Arrays.copyOf(buffer, offset);
            }
            offset += read;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(8 * 1024);
        baos.write(buffer);

        while (true) {
            int read = stream.read(buffer);
            if (read < 0) {
                return baos.toByteArray();
            }
            baos.write(buffer, 0, read);
        }
    }
}
