package net.microfalx.resource;

/**
 * A credential which has a user name.
 */
public interface UserAwareCredential extends Credential {

    /**
     * Returns the user name.
     *
     * @return a non-null instance
     */
    String getUserName();
}
