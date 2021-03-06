package org.springframework.roo.addon.web.mvc.controller.json;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Provides operations for Web MVC Json functionality.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public interface WebJsonOperations {

    boolean isWebJsonInstallationPossible();

    boolean isWebJsonCommandAvailable();

    void setup();

    void annotateType(JavaType type, JavaType jsonType);

    void annotateAll(JavaPackage javaPackage);
}
