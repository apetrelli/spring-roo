package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JpaJavaType.TEMPORAL;
import static org.springframework.roo.model.JpaJavaType.TEMPORAL_TYPE;
import static org.springframework.roo.model.Jsr303JavaType.FUTURE;
import static org.springframework.roo.model.Jsr303JavaType.PAST;
import static org.springframework.roo.model.SpringJavaType.DATE_TIME_FORMAT;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.DateTime;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * This field can optionally provide the mandatory JSR 220 temporal annotation.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DateField extends FieldDetails {

    /** Whether the JSR 220 @Temporal annotation will be added */
    private DateFieldPersistenceType persistenceType;

    /** Whether the JSR 303 @Past annotation will be added */
    private boolean past;

    /** Whether the JSR 303 @Future annotation will be added */
    private boolean future;

    private DateTime dateFormat;

    private DateTime timeFormat;

    /**
     * Custom date formatting through a DateTime pattern such as yyyy/mm/dd
     * h:mm:ss a.
     */
    private String pattern;

    public DateField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName) {
        super(physicalTypeIdentifier, fieldType, fieldName);
    }

    @Override
    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        super.decorateAnnotationsList(annotations);
        if (past) {
            annotations.add(new AnnotationMetadataBuilder(PAST));
        }
        if (future) {
            annotations.add(new AnnotationMetadataBuilder(FUTURE));
        }
        if (persistenceType != null) {
            // Add JSR 220 @Temporal annotation
            String value = null;
            if (persistenceType == DateFieldPersistenceType.JPA_DATE) {
                value = "DATE";
            }
            else if (persistenceType == DateFieldPersistenceType.JPA_TIME) {
                value = "TIME";
            }
            else if (persistenceType == DateFieldPersistenceType.JPA_TIMESTAMP) {
                value = "TIMESTAMP";
            }
            List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            attrs.add(new EnumAttributeValue(new JavaSymbolName("value"),
                    new EnumDetails(TEMPORAL_TYPE, new JavaSymbolName(value))));
            annotations.add(new AnnotationMetadataBuilder(TEMPORAL, attrs));
        }
        // Always add a DateTimeFormat annotation
        List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
        if (pattern != null) {
            attributes.add(new StringAttributeValue(new JavaSymbolName(
                    "pattern"), pattern));
        }
        else {
            String dateStyle = null != dateFormat ? String.valueOf(dateFormat
                    .getShortKey()) : "S";
            String timeStyle = null != timeFormat ? String.valueOf(timeFormat
                    .getShortKey()) : "-";
            attributes.add(new StringAttributeValue(
                    new JavaSymbolName("style"), dateStyle + timeStyle));
        }
        annotations.add(new AnnotationMetadataBuilder(DATE_TIME_FORMAT,
                attributes));
    }

    public boolean isPast() {
        return past;
    }

    public void setPast(final boolean past) {
        this.past = past;
    }

    public boolean isFuture() {
        return future;
    }

    public void setFuture(final boolean future) {
        this.future = future;
    }

    public DateFieldPersistenceType getPersistenceType() {
        return persistenceType;
    }

    public void setPersistenceType(
            final DateFieldPersistenceType persistenceType) {
        this.persistenceType = persistenceType;
    }

    public DateTime getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(final DateTime dateFormat) {
        this.dateFormat = dateFormat;
    }

    public DateTime getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(final DateTime timeFormat) {
        this.timeFormat = timeFormat;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }
}
