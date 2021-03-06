package org.springframework.roo.felix;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Represents a Bundle Symbolic Name.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class BundleSymbolicName implements Comparable<BundleSymbolicName> {

    // Fields
    private final String key;

    public BundleSymbolicName(final String key) {
        Assert.hasText(key, "Key required");
        this.key = key;
    }

    /**
     * Locates the bundle ID for this BundleSymbolicName, if available.
     * 
     * @param context to search (required)
     * @return the ID (or null if cannot be found)
     */
    public Long findBundleIdWithoutFail(final BundleContext context) {
        Assert.notNull(context, "Bundle context is unavailable");
        Bundle[] bundles = context.getBundles();
        if (bundles == null) {
            throw new IllegalStateException(
                    "Bundle IDs cannot be retrieved as BundleContext unavailable");
        }
        for (Bundle b : bundles) {
            if (getKey().equals(b.getSymbolicName())) {
                return b.getBundleId();
            }
        }
        throw new IllegalStateException("Bundle symbolic name '" + getKey()
                + "' has no local bundle ID at this time");
    }

    public String getKey() {
        return key;
    }

    @Override
    public final int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        return obj instanceof BundleSymbolicName
                && this.compareTo((BundleSymbolicName) obj) == 0;
    }

    public final int compareTo(final BundleSymbolicName o) {
        if (o == null)
            return -1;
        return this.key.compareTo(o.key);
    }

    @Override
    public String toString() {
        ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("key", key);
        return tsc.toString();
    }
}
