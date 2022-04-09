package net.microfalx.resource;

/**
 * A resource which keeps a state (like a connection to a remote resource) and it needs to be released after use.
 */
public interface StatefulResource extends Resource, AutoCloseable {

    @Override
    void close();
}
