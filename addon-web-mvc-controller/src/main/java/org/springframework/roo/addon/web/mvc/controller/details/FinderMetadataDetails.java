package org.springframework.roo.addon.web.mvc.controller.details;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Aggregates metadata for a given Roo finder which is scaffolded by Web
 * add-ons.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public class FinderMetadataDetails implements Comparable<FinderMetadataDetails> {

    // Fields
    private final String finderName;
    private final MethodMetadata finderMethodMetadata;
    private final List<FieldMetadata> finderMethodParamFields;

    public FinderMetadataDetails(final String finderName,
            final MethodMetadata finderMethodMetadata,
            final List<FieldMetadata> finderMethodParamFields) {
        Assert.hasText(finderName, "Finder name required");
        Assert.notNull(finderMethodMetadata, "Finder method metadata required");
        this.finderName = finderName;
        this.finderMethodMetadata = finderMethodMetadata;
        this.finderMethodParamFields = finderMethodParamFields;
    }

    public String getFinderName() {
        return finderName;
    }

    public MethodMetadata getFinderMethodMetadata() {
        return finderMethodMetadata;
    }

    public List<FieldMetadata> getFinderMethodParamFields() {
        return finderMethodParamFields;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((finderName == null) ? 0 : finderName.hashCode());
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        // NB: Not using the normal convention of delegating to compareTo (for
        // efficiency reasons)
        return obj instanceof FinderMetadataDetails
                && finderName.equals(((FinderMetadataDetails) obj).finderName);
    }

    public final int compareTo(final FinderMetadataDetails o) {
        // NB: If adding more fields to this class ensure the equals(Object)
        // method is updated accordingly
        if (o == null)
            return -1;
        int cmp = finderName.compareTo(o.finderName);
        return cmp;
    }
}
