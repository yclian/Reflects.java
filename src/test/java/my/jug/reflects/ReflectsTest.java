package my.jug.reflects;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static my.jug.reflects.Reflects.Functions.*;
import static my.jug.reflects.Reflects.Predicates.*;
import static my.jug.reflects.Reflects.onClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

/**
 * @author yclian
 * @since 1.0.20111104
 * @version 1.0.20120225
 */
public class ReflectsTest {

    @Retention(RetentionPolicy.RUNTIME)
    static @interface Annotation {
        String value() default "";
    }

    @SuppressWarnings("unused")
    static interface Interface extends Serializable {

        @Annotation("foo")
        void Method();
    }

    static class Class implements Interface {

        public static void StaticMethod() {}

        private static void PrivateStaticMethod() {}

        @Annotation("bar")
        @Deprecated
        @Override public void Method() {}
    }

    @Nullable
    @Deprecated
    @SuppressWarnings("foo")
    static class AnnotatedClass {}

    @Test
    public void testOnClass() throws Exception {
        assertTrue(onClass(LinkedHashMap.class).onClasses(true, true).get().contains(LinkedHashMap.class));
        assertFalse(onClass(LinkedHashMap.class).onClasses(true, false).get().contains(LinkedHashMap.class));
    }

    @Test
    public void testOnClassByName() throws ClassNotFoundException {
        onClass("my.jug.reflects.ReflectsTest$Interface");
    }

    @Test(expected = ClassNotFoundException.class)
    public void testOnClassByNameNotFound() throws ClassNotFoundException {
        onClass("Interface");
    }

    @Test
    public void testOnMethodOfName() {
        assertEquals(1, onClass(Class.class).onMethods().filter(methodOfName("Method")).size());
    }

    @Test
    public void testOnMethodAnnotatedWith() {
        assertEquals(1, onClass(Class.class).onMethods().filter(methodAnnotatedWith(Deprecated.class)).size());
    }

    @Test
    public void testOnMethodOfSignature() {
        assertEquals(1, onClass(Class.class).onMethods().filter(methodOfSignature("Method", void.class)).size());
    }

    @Test
    public void testOnPublicNonObjectInstanceMethods() {
        assertEquals(1, onClass(Class.class).onMethods().filter(and(publicMethod(), instanceMethod(), not(objectMethod()))).size());
    }

    @Test
    public void testOnStaticMethods() {

        assertEquals(2, onClass(Class.class).onMethods(false, true, true, false).filter(and(staticMethod())).size());

        assertEquals(1, onClass(Class.class).onMethods(false, true, true, true).filter(and(staticMethod(), not(publicMethod()))).size());
        assertEquals(1, onClass(Class.class).onMethods(false, true, true, false).filter(and(staticMethod(), not(publicMethod()))).size());
    }

    @Test
    @SuppressWarnings({"deprecation"})
    public void testOnAnnotations() throws Exception {

        assertTrue(onClass(AnnotatedClass.class).onAnnotations().transform(annotationToClass()).contains(Deprecated.class));
        assertTrue(onClass(AnnotatedClass.class).onAnnotations().transform(annotationToClass()).contains(Nullable.class));
        assertFalse("SuppressWarnings uses SOURCE retention policy and should not be detectable during runtime!", onClass(AnnotatedClass.class).onAnnotations().transform(annotationToClass()).contains(SuppressWarnings.class));
    }

    @Test
    public void testOnMethodsOnAnnotations() {
        assertEquals("bar", ((Annotation) onClass(Class.class).onMethods().onAnnotations(true).filter(".*\\$Annotation").get(0)).value());
        assertEquals("foo", ((Annotation) onClass(Class.class).onMethods().onAnnotations(true).filter(".*\\$Annotation").get(1)).value());
    }
}
