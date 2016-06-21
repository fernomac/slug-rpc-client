package io.coronet.slug.rpc.client;

/**
 * Exception thrown when something goes wrong while invoking the remote
 * procedure. Encompasses both transport-level errors and application-level
 * errors.
 */
public class RpcClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final Object data;

    public RpcClientException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    protected RpcClientException(
            int code,
            String message,
            Object data,
            Throwable cause) {

        super(message, cause);
        this.code = code;
        this.data = data;
    }

    /**
     * @return the error code from the response
     */
    public int getCode() {
        return code;
    }

    /**
     * @return the data (if any) from the response
     */
    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return super.toString() + " (code=" + code + ", data=" + data + ")";
    }
}
