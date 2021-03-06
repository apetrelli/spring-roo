package org.springframework.roo.classpath.customdata.taggers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.jvnet.inflector.Noun;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsBuilder;
import org.springframework.roo.model.CustomDataAccessor;
import org.springframework.roo.support.util.Assert;

/**
 * An implementation of {@link CustomDataKeyDecorator}.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
@Component
@Service
public class CustomDataKeyDecoratorImpl implements CustomDataKeyDecorator {

    // Fields
    private final Map<String, Matcher<? extends CustomDataAccessor>> taggerMap = new HashMap<String, Matcher<? extends CustomDataAccessor>>();
    private final Map<String, String> pluralMap = new HashMap<String, String>();

    public MemberDetails decorate(final String requestingClass,
            final MemberDetails memberDetails) {
        MemberDetailsBuilder memberDetailsBuilder = new MemberDetailsBuilder(
                memberDetails);

        for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails
                .getDetails()) {
            if (memberHoldingTypeDetails instanceof ClassOrInterfaceTypeDetails) {
                if (!pluralMap.containsKey(memberHoldingTypeDetails
                        .getDeclaredByMetadataId())) {
                    pluralMap.put(
                            memberHoldingTypeDetails.getDeclaredByMetadataId(),
                            getInflectorPlural(memberHoldingTypeDetails
                                    .getName().getSimpleTypeName(),
                                    Locale.ENGLISH));
                }
            }
        }

        // Locate any requests that we add custom data to identifiable java
        // structures
        for (FieldMatcher fieldTagger : getFieldTaggers()) {
            for (FieldMetadata field : fieldTagger.matches(memberDetails
                    .getDetails())) {
                memberDetailsBuilder.tag(field, fieldTagger.getCustomDataKey(),
                        fieldTagger.getTagValue(field));
            }
        }

        for (MethodMatcher methodTagger : getMethodTaggers()) {
            for (MethodMetadata method : methodTagger.matches(
                    memberDetails.getDetails(), pluralMap)) {
                memberDetailsBuilder.tag(method,
                        methodTagger.getCustomDataKey(),
                        methodTagger.getTagValue(method));
            }
        }

        for (ConstructorMatcher constructorTagger : getConstructorTaggers()) {
            for (ConstructorMetadata constructor : constructorTagger
                    .matches(memberDetails.getDetails())) {
                memberDetailsBuilder.tag(constructor,
                        constructorTagger.getCustomDataKey(),
                        constructorTagger.getTagValue(constructor));
            }
        }

        for (TypeMatcher typeTagger : getTypeTaggers()) {
            for (MemberHoldingTypeDetails typeDetails : typeTagger
                    .matches(memberDetails.getDetails())) {
                memberDetailsBuilder.tag(typeDetails,
                        typeTagger.getCustomDataKey(),
                        typeTagger.getTagValue(typeDetails));
            }
        }

        return memberDetailsBuilder.build();
    }

    public MemberDetails decorateTypes(final String requestingClass,
            final MemberDetails memberDetails) {
        MemberDetailsBuilder memberDetailsBuilder = new MemberDetailsBuilder(
                memberDetails);
        for (TypeMatcher typeTagger : getTypeTaggers()) {
            for (MemberHoldingTypeDetails typeDetails : typeTagger
                    .matches(memberDetails.getDetails())) {
                memberDetailsBuilder.tag(typeDetails,
                        typeTagger.getCustomDataKey(),
                        typeTagger.getTagValue(typeDetails));
            }
        }
        return memberDetailsBuilder.build();
    }

    /**
     * This method returns the plural term as per inflector. ATTENTION: this
     * method does NOT take @RooPlural into account. Use getPlural(..) instead!
     * 
     * @param term The term to be pluralized
     * @param locale Locale
     * @return pluralized term
     */
    public String getInflectorPlural(final String term, final Locale locale) {
        try {
            return Noun.pluralOf(term, locale);
        }
        catch (RuntimeException re) {
            // Inflector failed (see for example ROO-305), so don't pluralize it
            return term;
        }
    }

    public void registerMatcher(final String addingClass,
            final Matcher<? extends CustomDataAccessor> matcher) {
        Assert.notNull(addingClass, "The calling class must be specified");
        Assert.notNull(matcher, "The matcher must be specified");
        taggerMap.put(addingClass + matcher.getCustomDataKey(), matcher);
    }

    public void registerMatchers(final Class<?> addingClass,
            final Matcher<? extends CustomDataAccessor>... matchers) {
        if (addingClass != null) {
            for (final Matcher<? extends CustomDataAccessor> matcher : matchers) {
                // We don't keep a reference to the class, as OSGi might unload
                // it later
                registerMatcher(addingClass.getName(), matcher);
            }
        }
    }

    public void unregisterMatchers(final String addingClass) {
        Set<String> toRemove = new HashSet<String>();
        for (String taggerKey : taggerMap.keySet()) {
            if (taggerKey.startsWith(addingClass)) {
                toRemove.add(taggerKey);
            }
        }
        for (String taggerKey : toRemove) {
            taggerMap.remove(taggerKey);
        }
    }

    public void unregisterMatchers(final Class<?> addingClass) {
        unregisterMatchers(addingClass.getName());
    }

    public List<MethodMatcher> getMethodTaggers() {
        List<MethodMatcher> methodTaggers = new ArrayList<MethodMatcher>();
        for (Matcher<? extends CustomDataAccessor> matcher : taggerMap.values()) {
            if (matcher instanceof MethodMatcher) {
                methodTaggers.add((MethodMatcher) matcher);
            }
        }
        return methodTaggers;
    }

    public List<FieldMatcher> getFieldTaggers() {
        List<FieldMatcher> fieldTaggers = new ArrayList<FieldMatcher>();
        for (Matcher<? extends CustomDataAccessor> matcher : taggerMap.values()) {
            if (matcher instanceof FieldMatcher) {
                fieldTaggers.add((FieldMatcher) matcher);
            }
        }
        return fieldTaggers;
    }

    public List<ConstructorMatcher> getConstructorTaggers() {
        List<ConstructorMatcher> constructorTaggers = new ArrayList<ConstructorMatcher>();
        for (Matcher<? extends CustomDataAccessor> matcher : taggerMap.values()) {
            if (matcher instanceof ConstructorMatcher) {
                constructorTaggers.add((ConstructorMatcher) matcher);
            }
        }
        return constructorTaggers;
    }

    public List<TypeMatcher> getTypeTaggers() {
        List<TypeMatcher> typeTaggers = new ArrayList<TypeMatcher>();
        for (Matcher<? extends CustomDataAccessor> matcher : taggerMap.values()) {
            if (matcher instanceof TypeMatcher) {
                typeTaggers.add((TypeMatcher) matcher);
            }
        }
        return typeTaggers;
    }
}
