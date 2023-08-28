package com.armorauth.web;

import java.lang.annotation.*;

/**
 * Indicates that the annotated class is a mvn controller used whithin armorauth ui.
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ArmorAuthController {

}
