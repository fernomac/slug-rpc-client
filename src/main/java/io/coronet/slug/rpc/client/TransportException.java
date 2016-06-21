package io.coronet.slug.rpc.client;

/**
 * A {@code RpcClientException} that specifically indicates an error even
 * communicating with the remote server (versus successful communication
 * resulting in an error response).
 */
public class TransportException extends RpcClientException {

    private static final long serialVersionUID = 1L;
    private static final int INTERNAL_JSONRPC_ERROR = -32603;

    public TransportException(String message) {
        super(INTERNAL_JSONRPC_ERROR, message, null);
    }

    public TransportException(String message, Throwable cause) {
        super(INTERNAL_JSONRPC_ERROR, message, null, cause);
    }
}
