package io.coronet.slug.rpc.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@code InvocationHandler} for dynamically implementing service interfaces.
 */
final class RpcClientHandler implements InvocationHandler {

    private final CloseableHttpClient client;
    private final ObjectMapper mapper;
    private final URI endpoint;

    RpcClientHandler(RpcClient.Builder builder) {
        this.client = builder.getClient();
        this.mapper = builder.getMapper();
        this.endpoint = builder.getEndpoint();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        if (method.getDeclaringClass() == Object.class) {
            // Delegate Object methods to this object.
            return method.invoke(this, args);
        }

        if (args.length > 1) {
            throw new IllegalStateException(
                    "Method " + method + " takes more than one argument");
        }

        try {
            return doInvoke(proxy, method, args.length == 0 ? null : args[0]);
        } catch (IOException e) {
            throw new TransportException(e.getMessage(), e);
        }
    }

    /**
     * Closes the underlying HTTP client.
     */
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            throw new TransportException(e.getMessage(), e);
        }
    }

    private Object doInvoke(Object proxy, Method method, Object arg)
            throws IOException {

        byte[] payload = serialize(method.getName(), arg);

        HttpPost post = new HttpPost(endpoint);

        post.setHeader("User-Agent", "Slug-Rpc-Client");
        post.setHeader("Content-Type", "application/json-rpc");
        post.setHeader("Accept", "application/json-rpc");

        post.setEntity(new ByteArrayEntity(payload));

        // TODO: Authentication.

        // TODO: Configurable timeouts.
        post.setConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(5000)
                .setConnectTimeout(5000)
                .setSocketTimeout(5000)
                .build());

        try (CloseableHttpResponse resp = client.execute(post)) {
            byte[] content = buffer(resp.getEntity().getContent());
            JsonNode response = parse(content);

            JsonNode error = response.get("error");
            if (error != null) {
                if (!error.isObject()) {
                    throw new ProtocolException(
                            "Error parsing response: 'error' is not an object",
                            content);
                }

                int code = getCode(error);
                String message = getMessage(error, content);
                Object data = getData(code, error);

                throw new RpcClientException(code, message, data);
            }

            JsonNode result = response.get("result");
            if (result == null) {
                // Hopefully void method?
                return null;
            }

            Type type = method.getGenericReturnType();
            JavaType jtype = mapper.getTypeFactory().constructType(type);

            try {
                return mapper.readValue(result.traverse(), jtype);
            } catch (IOException e) {
                throw new ProtocolException(
                        "Error parsing result object " + result + " as a "
                                + type,
                        content,
                        e);
            }
        }
    }

    private byte[] serialize(String method, Object arg) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonGenerator gen = mapper.getFactory().createGenerator(baos)) {
            gen.writeStartObject();

            gen.writeFieldName("jsonrpc");
            gen.writeString("2.0");

            gen.writeFieldName("method");
            gen.writeString(method);

            if (arg != null) {
                gen.writeFieldName("params");
                mapper.writeValue(gen, arg);
            }

            gen.writeFieldName("id");
            gen.writeNumber(ThreadLocalRandom.current().nextInt());

            gen.writeEndObject();
        } catch (IOException e) {
            throw new IllegalStateException("Error serializing to JSON", e);
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

    private JsonNode parse(byte[] content) {
        try {

            return mapper.readTree(content);

        } catch (IOException e) {
            throw new ProtocolException(
                    "Error parsing response",
                    content,
                    e);
        }
    }

    private int getCode(JsonNode error) {
        JsonNode code = error.get("code");
        if (code == null || !code.isInt()) {
            return -32700;
        }
        return code.intValue();
    }

    private String getMessage(JsonNode error, byte[] content) {
        JsonNode message = error.get("message");
        if (message == null) {
            return "No error message present in response: "
                    + new String(content, StandardCharsets.UTF_8);
        }
        if (!message.isTextual()) {
            return "Error message in response is not a string: "
                    + new String(content, StandardCharsets.UTF_8);
        }
        return message.textValue();
    }

    private Object getData(int code, JsonNode error) {
        JsonNode data = error.get("data");
        if (data == null) {
            return null;
        }

        // TODO: Better strategy for registering expected data type based on
        // error code.

        try {
            return mapper.treeToValue(data, Object.class);
        } catch (JsonProcessingException e) {
            // Is this even possible?
            throw new IllegalStateException(e);
        }
    }
}
