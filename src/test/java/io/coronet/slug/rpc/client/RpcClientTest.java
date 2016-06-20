package io.coronet.slug.rpc.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.junit.Test;

public class RpcClientTest {

    @Test
    public void testSuccess() throws Exception {
        ServerSocket srv = new ServerSocket();
        srv.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

        new Thread(() -> {
            try (Socket sock = srv.accept();
                    Reader r = new InputStreamReader(sock.getInputStream());
                    BufferedReader reader = new BufferedReader(r)) {

                while (true) {
                    String line = reader.readLine();
                    if (line == null || line.isEmpty()) {
                        break;
                    }
                    System.err.println(line);
                }

                PrintStream writer = new PrintStream(sock.getOutputStream());
                writer.print("HTTP/1.1 200 OK\r\n");
                writer.print("Content-Type: application/json\r\n");
                writer.print("Connection: close\r\n");
                writer.print("\r\n");
                writer.print("{\"tags\":[\"abc\",\"def\"]}");
                sock.shutdownOutput();

                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    System.err.println(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        String uri = "http://localhost:" + srv.getLocalPort() + "/test";
        TestService service = new RpcClientBuilder<>(TestService.class, uri)
                .build();

        TestOutput output = service.test(new TestInput()
                .setName("David")
                .setAge(12345));

        System.out.println(output.getTags());
        srv.close();
    }

    public static interface TestService {
        TestOutput test(TestInput input);
    }

    public static class TestInput {

        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public TestInput setName(String value) {
            name = value;
            return this;
        }

        public Integer getAge() {
            return age;
        }

        public TestInput setAge(Integer value) {
            age = value;
            return this;
        }
    }

    public static class TestOutput {

        private List<String> tags;

        public List<String> getTags() {
            return tags;
        }

        public TestOutput setTags(List<String> value) {
            tags = value;
            return this;
        }
    }
}
