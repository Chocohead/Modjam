package com.chocohead.sm.loader;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the annotated type is loaded on the pre-Mixin phase of Knot
 * <br>
 * <b>BE VERY CAREFUL WHAT IS LOADED FROM THIS TYPE</b>
 *
 * @author Chocohead
 *
 * @since 0.3
 */
@Documented
@Retention(SOURCE)
@Target({TYPE, TYPE_PARAMETER, TYPE_USE})
public @interface PreMixinClassloaded {
}