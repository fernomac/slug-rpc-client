/**
 * The client side of a little framework for doing RPC over HTTP.
 * <p>
 * For the moment uses the JVM's built-in {@code HttpURLConnection} to keep
 * it simple. That's probably a really bad idea in the long term and I should
 * fix that.
 *
 * @author David Murray &lt;fernomac@coronet.io&gt;
 */
package io.coronet.slug.rpc.client;