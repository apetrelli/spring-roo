package org.springframework.roo.addon.jpa.identifier;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_TYPE;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;
import static org.springframework.roo.model.JpaJavaType.TEMPORAL;
import static org.springframework.roo.model.JpaJavaType.TEMPORAL_TYPE;
import static org.springframework.roo.model.JpaJavaType.TRANSIENT;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooIdentifier}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class IdentifierMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    // Constants
    private static final String PROVIDES_TYPE_STRING = IdentifierMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

    // Fields
    private boolean publicNoArgConstructor;
    // See {@link IdentifierService} for further information (populated via
    // {@link IdentifierMetadataProviderImpl}); may be null
    private List<Identifier> identifierServiceResult;

    public IdentifierMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final IdentifierAnnotationValues annotationValues,
            final List<Identifier> identifierServiceResult) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Assert.isTrue(isValid(identifier), "Metadata identification string '"
                + identifier + "' does not appear to be a valid");
        Assert.notNull(annotationValues, "Annotation values required");

        if (!isValid()) {
            return;
        }

        this.identifierServiceResult = identifierServiceResult;

        // Add @Embeddable annotation
        builder.addAnnotation(getEmbeddableAnnotation());

        // Add declared fields and accessors and mutators
        List<FieldMetadataBuilder> fields = getFieldBuilders();
        for (FieldMetadataBuilder field : fields) {
            builder.addField(field);
        }

        // Obtain a parameterised constructor
        builder.addConstructor(getParameterizedConstructor(fields));

        // Obtain a no-arg constructor, if one is appropriate to provide
        if (annotationValues.isNoArgConstructor()) {
            builder.addConstructor(getNoArgConstructor());
        }

        if (annotationValues.isGettersByDefault()) {
            for (MethodMetadataBuilder accessor : getAccessors(fields)) {
                builder.addMethod(accessor);
            }
        }
        if (annotationValues.isSettersByDefault()) {
            for (MethodMetadataBuilder mutator : getMutators(fields)) {
                builder.addMethod(mutator);
            }
        }

        // Add custom data tag for Roo Identifier type
        builder.putCustomData(IDENTIFIER_TYPE, null);

        // Create a representation of the desired output ITD
        buildItd();
    }

    private AnnotationMetadata getEmbeddableAnnotation() {
        if (governorTypeDetails.getAnnotation(EMBEDDABLE) != null) {
            return null;
        }
        AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                EMBEDDABLE);
        return annotationBuilder.build();
    }

    /**
     * Locates declared fields.
     * <p>
     * If no parent is defined, one will be located or created. All declared
     * fields will be returned.
     * 
     * @return fields (never returns null)
     */
    private List<FieldMetadataBuilder> getFieldBuilders() {
        // Locate all declared fields
        List<? extends FieldMetadata> declaredFields = governorTypeDetails
                .getDeclaredFields();

        // Add fields to ITD from annotation
        List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
        if (identifierServiceResult != null) {
            for (Identifier identifier : identifierServiceResult) {
                List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
                annotations.add(getColumnBuilder(identifier));
                if (identifier.getFieldType().equals(DATE)) {
                    setDateAnnotations(identifier.getColumnDefinition(),
                            annotations);
                }

                FieldMetadata idField = new FieldMetadataBuilder(getId(),
                        Modifier.PRIVATE, annotations,
                        identifier.getFieldName(), identifier.getFieldType())
                        .build();

                // Only add field to ITD if not declared on governor
                if (!hasField(declaredFields, idField)) {
                    fields.add(idField);
                }
            }
        }

        fields.addAll(declaredFields);

        // Remove fields with static and transient modifiers
        for (Iterator<FieldMetadata> iter = fields.iterator(); iter.hasNext();) {
            FieldMetadata field = iter.next();
            if (Modifier.isStatic(field.getModifier())
                    || Modifier.isTransient(field.getModifier())) {
                iter.remove();
            }
        }

        // Remove fields with the @Transient annotation
        List<FieldMetadata> transientAnnotatedFields = governorTypeDetails
                .getFieldsWithAnnotation(TRANSIENT);
        if (fields.containsAll(transientAnnotatedFields)) {
            fields.removeAll(transientAnnotatedFields);
        }

        List<FieldMetadataBuilder> fieldBuilders = new ArrayList<FieldMetadataBuilder>();
        if (!fields.isEmpty()) {
            for (FieldMetadata field : fields) {
                fieldBuilders.add(new FieldMetadataBuilder(field));
            }
            return fieldBuilders;
        }

        // We need to create a default identifier field
        List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

        // Compute the column name, as required
        AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(
                COLUMN);
        columnBuilder.addStringAttribute("name", "id");
        columnBuilder.addBooleanAttribute("nullable", false);
        annotations.add(columnBuilder);

        fieldBuilders.add(new FieldMetadataBuilder(getId(), Modifier.PRIVATE,
                annotations, new JavaSymbolName("id"), LONG_OBJECT));

        return fieldBuilders;
    }

    private AnnotationMetadataBuilder getColumnBuilder(
            final Identifier identifier) {
        AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(
                COLUMN);
        columnBuilder.addStringAttribute("name", identifier.getColumnName());
        if (StringUtils.hasText(identifier.getColumnDefinition())) {
            columnBuilder.addStringAttribute("columnDefinition",
                    identifier.getColumnDefinition());
        }
        columnBuilder.addBooleanAttribute("nullable", false);

        // Add length attribute for Strings
        if (identifier.getColumnSize() < 4000
                && identifier.getFieldType().equals(JavaType.STRING)) {
            columnBuilder.addIntegerAttribute("length",
                    identifier.getColumnSize());
        }

        // Add precision and scale attributes for numeric fields
        if (identifier.getScale() > 0
                && (identifier.getFieldType().equals(JavaType.DOUBLE_OBJECT)
                        || identifier.getFieldType().equals(
                                JavaType.DOUBLE_PRIMITIVE) || identifier
                        .getFieldType().equals(BIG_DECIMAL))) {
            columnBuilder.addIntegerAttribute("precision",
                    identifier.getColumnSize());
            columnBuilder.addIntegerAttribute("scale", identifier.getScale());
        }

        return columnBuilder;
    }

    private void setDateAnnotations(final String columnDefinition,
            final List<AnnotationMetadataBuilder> annotations) {
        // Add JSR 220 @Temporal annotation to date fields
        String temporalType = StringUtils.defaultIfEmpty(
                StringUtils.toUpperCase(columnDefinition), "DATE");
        if ("DATETIME".equals(temporalType)) {
            temporalType = "TIMESTAMP"; // ROO-2606
        }
        AnnotationMetadataBuilder temporalBuilder = new AnnotationMetadataBuilder(
                TEMPORAL);
        temporalBuilder.addEnumAttribute("value", new EnumDetails(
                TEMPORAL_TYPE, new JavaSymbolName(temporalType)));
        annotations.add(temporalBuilder);

        AnnotationMetadataBuilder dateTimeFormatBuilder = new AnnotationMetadataBuilder(
                DATE_TIME_FORMAT);
        dateTimeFormatBuilder.addStringAttribute("style", "M-");
        annotations.add(dateTimeFormatBuilder);
    }

    private boolean hasField(
            final List<? extends FieldMetadata> declaredFields,
            final FieldMetadata idField) {
        for (FieldMetadata declaredField : declaredFields) {
            if (declaredField.getFieldName().equals(idField.getFieldName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Locates the accessor methods.
     * <p>
     * If {@link #getFieldBuilders()} returns fields created by this ITD, public
     * accessors will automatically be produced in the declaring class.
     * 
     * @param fields
     * @return the accessors (never returns null)
     */
    private List<MethodMetadataBuilder> getAccessors(
            List<FieldMetadataBuilder> fields) {
        List<MethodMetadataBuilder> accessors = new ArrayList<MethodMetadataBuilder>();

        // Compute the names of the accessors that will be produced
        for (FieldMetadataBuilder field : fields) {
            JavaSymbolName requiredAccessorName = BeanInfoUtils
                    .getAccessorMethodName(field.getFieldName(),
                            field.getFieldType());
            MethodMetadata accessor = getGovernorMethod(requiredAccessorName);
            if (accessor == null) {
                accessors.add(getAccessorMethod(field.getFieldName(),
                        field.getFieldType()));
            }
            else {
                Assert.isTrue(
                        Modifier.isPublic(accessor.getModifier()),
                        "User provided field but failed to provide a public '"
                                + requiredAccessorName.getSymbolName()
                                + "()' method in '"
                                + destination.getFullyQualifiedTypeName() + "'");
                accessors.add(new MethodMetadataBuilder(accessor));
            }
        }
        return accessors;
    }

    /**
     * Locates the mutator methods.
     * <p>
     * If {@link #getFieldBuilders()} returns fields created by this ITD, public
     * mutators will automatically be produced in the declaring class.
     * 
     * @param fields
     * @return the mutators (never returns null)
     */
    private List<MethodMetadataBuilder> getMutators(
            List<FieldMetadataBuilder> fields) {
        List<MethodMetadataBuilder> mutators = new ArrayList<MethodMetadataBuilder>();

        // Compute the names of the mutators that will be produced
        for (FieldMetadataBuilder field : fields) {
            JavaSymbolName requiredMutatorName = BeanInfoUtils
                    .getMutatorMethodName(field.getFieldName());
            final JavaType parameterType = field.getFieldType();
            MethodMetadata mutator = getGovernorMethod(requiredMutatorName,
                    parameterType);
            if (mutator == null) {
                mutators.add(getMutatorMethod(field.getFieldName(),
                        field.getFieldType()));
            }
            else {
                Assert.isTrue(
                        Modifier.isPublic(mutator.getModifier()),
                        "User provided field but failed to provide a public '"
                                + requiredMutatorName + "("
                                + field.getFieldName().getSymbolName()
                                + ")' method in '"
                                + destination.getFullyQualifiedTypeName() + "'");
                mutators.add(new MethodMetadataBuilder(mutator));
            }
        }
        return mutators;
    }

    /**
     * Locates the parameterised constructor consisting of the id fields for
     * this class.
     * 
     * @param fields
     * @return the constructor, never null.
     */
    private ConstructorMetadataBuilder getParameterizedConstructor(
            List<FieldMetadataBuilder> fields) {
        // Search for an existing constructor
        List<JavaType> parameterTypes = new ArrayList<JavaType>();
        for (FieldMetadataBuilder field : fields) {
            parameterTypes.add(field.getFieldType());
        }

        ConstructorMetadata result = governorTypeDetails
                .getDeclaredConstructor(parameterTypes);
        if (result != null) {
            // Found an existing parameterised constructor on this class
            publicNoArgConstructor = true;
            return null;
        }

        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("super();");
        for (FieldMetadataBuilder field : fields) {
            String fieldName = field.getFieldName().getSymbolName();
            bodyBuilder.appendFormalLine("this." + fieldName + " = "
                    + fieldName + ";");
            parameterNames.add(field.getFieldName());
        }

        // Create the constructor
        ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(
                getId());
        constructorBuilder.setModifier(Modifier.PUBLIC);
        constructorBuilder.setParameterTypes(AnnotatedJavaType
                .convertFromJavaTypes(parameterTypes));
        constructorBuilder.setParameterNames(parameterNames);
        constructorBuilder.setBodyBuilder(bodyBuilder);
        return constructorBuilder;
    }

    /**
     * Locates the no-arg constructor for this class, if available.
     * <p>
     * If a class defines a no-arg constructor, it is returned (irrespective of
     * access modifiers).
     * <p>
     * If a class does not define a no-arg constructor, one might be created. It
     * will only be created if the {@link RooIdentifier#noArgConstructor} is
     * true AND there is at least one other constructor declared in the source
     * file. If a constructor is created, it will have a private access
     * modifier.
     * 
     * @return the constructor (may return null if no constructor is to be
     *         produced)
     */
    private ConstructorMetadataBuilder getNoArgConstructor() {
        // Search for an existing constructor
        List<JavaType> parameterTypes = new ArrayList<JavaType>();
        ConstructorMetadata result = governorTypeDetails
                .getDeclaredConstructor(parameterTypes);
        if (result != null) {
            // Found an existing no-arg constructor on this class
            return null;
        }

        // Create the constructor
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("super();");

        ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(
                getId());
        constructorBuilder.setModifier(publicNoArgConstructor ? Modifier.PUBLIC
                : Modifier.PRIVATE);
        constructorBuilder.setParameterTypes(AnnotatedJavaType
                .convertFromJavaTypes(parameterTypes));
        constructorBuilder.setBodyBuilder(bodyBuilder);
        return constructorBuilder;
    }

    public static String getMetadataIdentifierType() {
        return PROVIDES_TYPE;
    }

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }
}
