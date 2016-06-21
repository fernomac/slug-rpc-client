package io.coronet.slug.rpc.client;

import java.nio.charset.StandardCharsets;

/**
 * An {@code RpcClientException} thrown when the server sends a response
 * but we can't make any sense of it.
 */
public class ProtocolException extends RpcClientException {

    private static final long serialVersionUID = 1L;
    private static final int PARSE_ERROR = -32700;

    public ProtocolException(String message, byte[] content) {
        super(PARSE_ERROR, message, stringify(content));
    }

    public ProtocolException(String message, byte[] content, Throwable cause) {
        super(PARSE_ERROR, message, stringify(content), cause);
    }

    private static String stringify(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }
}
