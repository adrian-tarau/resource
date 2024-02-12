package net.microfalx.resource;

/**
 * An interface which carries a credential used to access a resource.
 */
public interface Credential {

    /**
     * A credential which means "no credential available"
     */
    Credential NA = ResourceUtils.NULL_CREDENTIAL;
}
