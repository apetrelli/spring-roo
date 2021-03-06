package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Represents a {@link Class} annotation attribute value.
 * <p>
 * Source code parsers should treat any non-quoted string ending in ".class" as
 * a class name, and then use normal package resolution techniques to determine
 * the fully-qualified class.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class ClassAttributeValue extends
        AbstractAnnotationAttributeValue<JavaType> {

    // Fields
    private final JavaType value;

    /**
     * Constructor
     * 
     * @param name the attribute name (required)
     * @param value the value (required)
     */
    public ClassAttributeValue(final JavaSymbolName name, final JavaType value) {
        super(name);
        Assert.notNull(value, "Value required");
        this.value = value;
    }

    public JavaType getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + " -> " + value.getNameIncludingTypeParameters();
    }
}
