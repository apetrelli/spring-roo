package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link FieldMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultFieldMetadata extends
        AbstractIdentifiableAnnotatedJavaStructureProvider implements
        FieldMetadata {

    // Fields
    private final String fieldInitializer;
    private final JavaSymbolName fieldName;
    private final JavaType fieldType;

    // Package protected to mandate the use of FieldMetadataBuilder
    DefaultFieldMetadata(final CustomData customData,
            final String declaredByMetadataId, final int modifier,
            final List<AnnotationMetadata> annotations,
            final JavaSymbolName fieldName, final JavaType fieldType,
            final String fieldInitializer) {
        super(customData, declaredByMetadataId, modifier, annotations);
        Assert.hasText(declaredByMetadataId, "Declared by metadata ID required");
        Assert.notNull(fieldName, "Field name required");
        Assert.notNull(fieldType, "Field type required");
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.fieldInitializer = fieldInitializer;
    }

    public String getFieldInitializer() {
        return fieldInitializer;
    }

    public JavaSymbolName getFieldName() {
        return fieldName;
    }

    public JavaType getFieldType() {
        return fieldType;
    }

    @Override
    public String toString() {
        ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
        tsc.append("modifier", Modifier.toString(getModifier()));
        tsc.append("fieldType", fieldType);
        tsc.append("fieldName", fieldName);
        tsc.append("fieldInitializer", fieldInitializer);
        tsc.append("annotations", getAnnotations());
        tsc.append("customData", getCustomData());
        return tsc.toString();
    }
}
