package org.springframework.roo.classpath.customdata.taggers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * A {@link TypeMatcher} that looks for any of a given set of annotations.
 * 
 * @author James Tyrrell
 */
public class AnnotatedTypeMatcher extends TypeMatcher {

    // Fields
    private final CustomDataKey<MemberHoldingTypeDetails> customDataKey;
    private final List<JavaType> annotationTypesToMatchOn;

    /**
     * Constructor
     * 
     * @param customDataKey the {@link CustomDataKey} to apply (required)
     * @param annotationTypesToMatchOn
     */
    public AnnotatedTypeMatcher(
            final CustomDataKey<MemberHoldingTypeDetails> customDataKey,
            final JavaType... annotationTypesToMatchOn) {
        Assert.notNull(customDataKey, "Custom data key required");
        this.annotationTypesToMatchOn = Arrays.asList(annotationTypesToMatchOn);
        this.customDataKey = customDataKey;
    }

    public List<MemberHoldingTypeDetails> matches(
            final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
        final Map<String, MemberHoldingTypeDetails> matched = new HashMap<String, MemberHoldingTypeDetails>();
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
            for (final AnnotationMetadata annotationMetadata : memberHoldingTypeDetails
                    .getAnnotations()) {
                for (final JavaType annotationTypeToMatchOn : annotationTypesToMatchOn) {
                    if (annotationMetadata.getAnnotationType().equals(
                            annotationTypeToMatchOn)) {
                        matched.put(memberHoldingTypeDetails
                                .getDeclaredByMetadataId(),
                                memberHoldingTypeDetails);
                    }
                }
            }
        }
        return new ArrayList<MemberHoldingTypeDetails>(matched.values());
    }

    public CustomDataKey<MemberHoldingTypeDetails> getCustomDataKey() {
        return customDataKey;
    }

    public Object getTagValue(final MemberHoldingTypeDetails key) {
        return null;
    }
}
