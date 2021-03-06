package org.springframework.roo.addon.dbre.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.roo.support.util.Assert;

/**
 * Represents an index definition for a table which may be either unique or
 * non-unique.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class Index {

    // Fields
    private String name;
    private boolean unique;
    private final Set<IndexColumn> columns = new LinkedHashSet<IndexColumn>();

    /**
     * Constructor
     * 
     * @param name
     */
    Index(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(final boolean unique) {
        this.unique = unique;
    }

    public Set<IndexColumn> getColumns() {
        return columns;
    }

    public boolean addColumn(final IndexColumn indexColumn) {
        Assert.notNull(indexColumn, "Column required");
        return columns.add(indexColumn);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Index)) {
            return false;
        }
        Index other = (Index) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("Index [name=%s, unique=%s, columns=%s]", name,
                unique, columns);
    }
}
