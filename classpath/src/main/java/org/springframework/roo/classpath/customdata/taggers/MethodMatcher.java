package org.springframework.roo.classpath.customdata.taggers;

import static org.springframework.roo.model.RooJavaType.ROO_PLURAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * {@link MethodMetadata} specific implementation of {@link Matcher}. Matches
 * are based on field name which is dynamically determined based on: the
 * {@link FieldMatcher}s presented; the type of method (accessor/mutator); the
 * default method name; the user specified method name obtained from a
 * particular Roo annotation; a plural/singular suffix of the referenced entity;
 * and, an additional suffix.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MethodMatcher implements Matcher<MethodMetadata> {

    // Fields
    private final CustomDataKey<MethodMetadata> customDataKey;
    private final List<FieldMatcher> fieldTaggers = new ArrayList<FieldMatcher>();

    private boolean isAccessor;
    private boolean suffixPlural;
    private boolean suffixSingular;
    private JavaSymbolName userDefinedNameAttribute;
    private JavaType catalystAnnotationType;
    private String additionalSuffix = "";
    private String defaultName;

    /**
     * Constructor
     * 
     * @param fieldTaggers can be <code>null</code> for none
     * @param customDataKey
     * @param isAccessor
     */
    public MethodMatcher(final Collection<? extends FieldMatcher> fieldTaggers,
            final CustomDataKey<MethodMetadata> customDataKey,
            final boolean isAccessor) {
        this.customDataKey = customDataKey;
        this.isAccessor = isAccessor;
        if (fieldTaggers != null) {
            this.fieldTaggers.addAll(fieldTaggers);
        }
    }

    /**
     * Constructor
     * 
     * @param customDataKey
     * @param catalystAnnotationType
     * @param userDefinedNameAttribute
     * @param defaultName
     */
    public MethodMatcher(final CustomDataKey<MethodMetadata> customDataKey,
            final JavaType catalystAnnotationType,
            final JavaSymbolName userDefinedNameAttribute,
            final String defaultName) {
        this.catalystAnnotationType = catalystAnnotationType;
        this.customDataKey = customDataKey;
        this.userDefinedNameAttribute = userDefinedNameAttribute;
        this.defaultName = defaultName;
    }

    /**
     * Constructor
     * 
     * @param customDataKey
     * @param catalystAnnotationType
     * @param userDefinedNameAttribute
     * @param defaultName
     * @param suffixPlural
     * @param suffixSingular
     */
    public MethodMatcher(final CustomDataKey<MethodMetadata> customDataKey,
            final JavaType catalystAnnotationType,
            final JavaSymbolName userDefinedNameAttribute,
            final String defaultName, final boolean suffixPlural,
            final boolean suffixSingular) {
        this(customDataKey, catalystAnnotationType, userDefinedNameAttribute,
                defaultName);
        this.suffixPlural = suffixPlural;
        this.suffixSingular = suffixSingular;
    }

    /**
     * Constructor
     * 
     * @param customDataKey
     * @param catalystAnnotationType
     * @param userDefinedNameAttribute
     * @param defaultName
     * @param suffixPlural
     * @param suffixSingular
     * @param additionalSuffix
     */
    public MethodMatcher(final CustomDataKey<MethodMetadata> customDataKey,
            final JavaType catalystAnnotationType,
            final JavaSymbolName userDefinedNameAttribute,
            final String defaultName, final boolean suffixPlural,
            final boolean suffixSingular, final String additionalSuffix) {
        this(customDataKey, catalystAnnotationType, userDefinedNameAttribute,
                defaultName, suffixPlural, suffixSingular);
        this.additionalSuffix = additionalSuffix;
    }

    public List<MethodMetadata> matches(
            final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
        return null; // TODO: This needs to be dealt with -JT
    }

    public CustomDataKey<MethodMetadata> getCustomDataKey() {
        return customDataKey;
    }

    public Object getTagValue(final MethodMetadata key) {
        return null;
    }

    public List<MethodMetadata> matches(
            final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList,
            final Map<String, String> pluralMap) {
        List<FieldMetadata> fields = getFieldsInterestedIn(memberHoldingTypeDetailsList);
        List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
        Set<JavaSymbolName> methodNames = new HashSet<JavaSymbolName>();
        JavaSymbolName userDefinedMethodName = getUserDefinedMethod(
                memberHoldingTypeDetailsList, pluralMap);
        if (userDefinedMethodName == null) {
            for (FieldMetadata field : fields) {
                methodNames.add(new JavaSymbolName(getPrefix()
                        + StringUtils.capitalize(field.getFieldName()
                                .getSymbolName())));
            }
        }
        else {
            methodNames.add(new JavaSymbolName(userDefinedMethodName
                    .getSymbolName() + additionalSuffix));
        }
        for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
            for (MethodMetadata method : memberHoldingTypeDetails
                    .getDeclaredMethods()) {
                if (methodNames.contains(method.getMethodName())) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private JavaSymbolName getUserDefinedMethod(
            final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList,
            final Map<String, String> pluralMap) {
        if (catalystAnnotationType == null || userDefinedNameAttribute == null) {
            return null;
        }
        String suffix = suffixPlural || suffixSingular ? getSuffix(
                memberHoldingTypeDetailsList, suffixSingular, pluralMap) : "";
        ClassOrInterfaceTypeDetails cid = getMostConcreteClassOrInterfaceTypeDetails(memberHoldingTypeDetailsList);
        for (AnnotationMetadata annotationMetadata : cid.getAnnotations()) {
            if (annotationMetadata.getAnnotationType()
                    .getFullyQualifiedTypeName()
                    .equals(catalystAnnotationType.getFullyQualifiedTypeName())) {
                AnnotationAttributeValue<?> annotationAttributeValue = annotationMetadata
                        .getAttribute(userDefinedNameAttribute);
                if (annotationAttributeValue != null
                        && StringUtils.hasText(annotationAttributeValue
                                .getValue().toString())) {
                    return new JavaSymbolName(annotationAttributeValue
                            .getValue().toString() + suffix);
                }
                break;
            }
        }
        return defaultName == null ? null : new JavaSymbolName(defaultName
                + suffix);
    }

    private ClassOrInterfaceTypeDetails getMostConcreteClassOrInterfaceTypeDetails(
            final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
        ClassOrInterfaceTypeDetails cid = null;
        // The last ClassOrInterfaceTypeDetails is the most concrete as dictated
        // by the logic in MemberDetailsScannerImpl
        for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
            if (memberHoldingTypeDetails instanceof ClassOrInterfaceTypeDetails) {
                cid = (ClassOrInterfaceTypeDetails) memberHoldingTypeDetails;
            }
        }
        Assert.notNull(cid, "No concrete type found; cannot continue");
        return cid;
    }

    private String getSuffix(
            final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList,
            final boolean singular, final Map<String, String> pluralMap) {
        ClassOrInterfaceTypeDetails cid = getMostConcreteClassOrInterfaceTypeDetails(memberHoldingTypeDetailsList);
        if (singular) {
            return cid.getName().getSimpleTypeName();
        }
        String plural = pluralMap.get(cid.getDeclaredByMetadataId());
        for (AnnotationMetadata annotationMetadata : cid.getAnnotations()) {
            if (annotationMetadata.getAnnotationType()
                    .getFullyQualifiedTypeName()
                    .equals(ROO_PLURAL.getFullyQualifiedTypeName())) {
                AnnotationAttributeValue<?> annotationAttributeValue = annotationMetadata
                        .getAttribute(new JavaSymbolName("value"));
                if (annotationAttributeValue != null) {
                    plural = annotationAttributeValue.getValue().toString();
                }
                break;
            }
        }
        if (StringUtils.hasText(plural)) {
            plural = StringUtils.capitalize(plural);
        }
        return plural;
    }

    private List<FieldMetadata> getFieldsInterestedIn(
            final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
        List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
        for (FieldMatcher fieldTagger : fieldTaggers) {
            fields.addAll(fieldTagger.matches(memberHoldingTypeDetailsList));
        }
        return fields;
    }

    private String getPrefix() {
        return isAccessor ? "get" : "set";
    }
}
