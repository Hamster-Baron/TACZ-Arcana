
package org.spongepowered.asm.gradle.plugins;

import org.gradle.api.GradleException;

import javax.annotation.Nullable;


public class MixinGradleException extends GradleException {

    private static final long serialVersionUID = 1L;

    public MixinGradleException(String message) {
        super(message);
    }

    public MixinGradleException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }

}
