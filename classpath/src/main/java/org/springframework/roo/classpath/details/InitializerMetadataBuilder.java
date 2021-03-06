package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;

import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;

/**
 * Builder for {@link InitializerMetadata}.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
public class InitializerMetadataBuilder extends
        AbstractIdentifiableJavaStructureBuilder<InitializerMetadata> {

    // Fields
    private boolean isStatic;
    private InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    public InitializerMetadataBuilder(final String declaredbyMetadataId) {
        super(declaredbyMetadataId);
    }

    public InitializerMetadataBuilder(final InitializerMetadata existing) {
        super(existing);
        this.isStatic = existing.getModifier() == Modifier.STATIC
                || existing.isStatic();
        this.bodyBuilder.append(existing.getBody());
    }

    public InitializerMetadataBuilder(final String declaredbyMetadataId,
            final int modifier, boolean isStatic,
            final InvocableMemberBodyBuilder bodyBuilder) {
        this(declaredbyMetadataId);
        setModifier(modifier);
        if (modifier == Modifier.STATIC) {
            isStatic = true;
        }
        this.isStatic = isStatic;
        this.bodyBuilder = bodyBuilder;
    }

    public InitializerMetadata build() {
        return new DefaultInitializerMetadata(getCustomData().build(),
                getDeclaredByMetadataId(), getModifier(), isStatic,
                getBodyBuilder().getOutput());
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(final boolean isStatic) {
        this.isStatic = isStatic;
    }

    public String getBody() {
        if (bodyBuilder != null) {
            return bodyBuilder.getOutput();
        }
        return null;
    }

    public InvocableMemberBodyBuilder getBodyBuilder() {
        return bodyBuilder;
    }

    public void setBodyBuilder(final InvocableMemberBodyBuilder bodyBuilder) {
        this.bodyBuilder = bodyBuilder;
    }
}
