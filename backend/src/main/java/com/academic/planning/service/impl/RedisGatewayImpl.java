package com.academic.planning.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.academic.planning.service.RedisGateway;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class RedisGatewayImpl implements RedisGateway {

    private static final byte[] CRLF = new byte[]{'\r', '\n'};
    private final String host;
    private final int port;
    private final String password;

    public RedisGatewayImpl(
            @Value("${app.redis.host:127.0.0.1}") String host,
            @Value("${app.redis.port:6379}") int port,
            @Value("${app.redis.password:}") String password
    ) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public String get(String key) {
        Object response = execute("GET", key);
        return response == null ? null : response.toString();
    }

    public void set(String key, String value, Duration ttl) {
        execute("SETEX", key, String.valueOf(ttl.toSeconds()), value);
    }

    public void delete(String key) {
        execute("DEL", key);
    }

    public void expire(String key, Duration ttl) {
        execute("EXPIRE", key, String.valueOf(ttl.toSeconds()));
    }

    public long increment(String key, Duration ttl) {
        Object response = execute("EVAL",
                "local n = redis.call('incr', KEYS[1]); if n == 1 then redis.call('expire', KEYS[1], ARGV[1]) end; return n",
                "1", key, String.valueOf(ttl.toSeconds()));
        return ((Number) response).longValue();
    }

    public boolean acquireLock(String key, String owner, Duration ttl) {
        return "OK".equals(execute("SET", key, owner, "NX", "EX", String.valueOf(ttl.toSeconds())));
    }

    public void releaseLock(String key, String owner) {
        execute("EVAL", "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
                "1", key, owner);
    }

    public boolean setIfLockOwner(String lockKey, String owner, String key, String value, Duration ttl) {
        Object result = execute("EVAL",
                "if redis.call('get', KEYS[1]) ~= ARGV[1] then return 0 end; "
                        + "redis.call('setex', KEYS[2], ARGV[2], ARGV[3]); return 1",
                "2", lockKey, key, owner, String.valueOf(ttl.toSeconds()), value);
        return ((Number) result).longValue() == 1;
    }

    private Object execute(String... command) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2_000);
            socket.setSoTimeout(2_000);
            try (BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
                 BufferedInputStream input = new BufferedInputStream(socket.getInputStream())) {
                if (password != null && !password.isBlank()) {
                    writeCommand(output, "AUTH", password);
                    readResponse(input);
                }
                writeCommand(output, command);
                return readResponse(input);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Redis connection failed", exception);
        }
    }

    private void writeCommand(BufferedOutputStream output, String... arguments) throws IOException {
        output.write(("*" + arguments.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String argument : arguments) {
            byte[] bytes = argument.getBytes(StandardCharsets.UTF_8);
            output.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.write(CRLF);
        }
        output.flush();
    }

    private Object readResponse(BufferedInputStream input) throws IOException {
        int prefix = input.read();
        if (prefix == -1) {
            throw new EOFException("Redis closed the connection");
        }
        String line = readLine(input);
        if (prefix == '+') {
            return line;
        }
        if (prefix == '-') {
            throw new IllegalStateException("Redis error: " + line);
        }
        if (prefix == ':') {
            return Long.parseLong(line);
        }
        if (prefix == '$') {
            int length = Integer.parseInt(line);
            if (length == -1) {
                return null;
            }
            byte[] value = input.readNBytes(length);
            if (value.length != length || input.read() != '\r' || input.read() != '\n') {
                throw new EOFException("Incomplete Redis bulk response");
            }
            return new String(value, StandardCharsets.UTF_8);
        }
        throw new IllegalStateException("Unsupported Redis response prefix: " + (char) prefix);
    }

    private String readLine(BufferedInputStream input) throws IOException {
        StringBuilder builder = new StringBuilder();
        int previous = -1;
        int current;
        while ((current = input.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                builder.setLength(builder.length() - 1);
                return builder.toString();
            }
            builder.append((char) current);
            previous = current;
        }
        throw new EOFException("Incomplete Redis response line");
    }
}
