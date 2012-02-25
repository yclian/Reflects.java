package my.jug.reflects;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static my.jug.reflects.Reflects.Predicates.*;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.*;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * <p>Reflection utility class, to traverse, filter and trasnform reflection objects.</p>
 *
 * @author yclian
 * @since 1.0.20111103
 * @version 1.0.20120225
 */
public abstract class Reflects {

    protected Reflects() {}

    private static interface HasMethods {

        OnMethods onMethods();

        OnMethods onMethods(boolean includeInherited, boolean includeNonPublic, boolean includeStatic, boolean includeInterfaces);
    }

    private static interface HasAnnotations {

        OnAnnotations onAnnotations();
        
        OnAnnotations onAnnotations(boolean includeInherited);
    }

    private static interface OnCollection<E> {

        /**
         * Get the target elements.
         *
         * @return
         */
        List<E> get();

        /**
         * Seek (for one, and first found) element given a {@link Predicate}.
         * @param predicate
         * @return
         */
        E seek(Predicate<? super E> predicate);

        /**
         * @see Collections2#filter(java.util.Collection, com.google.common.base.Predicate)
         * @param regex
         * @return
         */
        List<E> filter(final String regex);

        /**
         * @see Collections2#filter(java.util.Collection, com.google.common.base.Predicate)
         * @param predicate
         * @return
         */
        List<E> filter(Predicate<? super E> predicate);

        /**
         * @see Collections2#transform(java.util.Collection, com.google.common.base.Function)
         * @param f
         * @param <O>
         * @return
         */
        <O> List<O> transform(final Function<? super E, O> f);
    }

    private static interface AnnotatableCollection<E> extends OnCollection<E> {

        /**
         * Return a list of annotatable objects annotated with the specified annotation.
         *
         * @see Collections2#filter(java.util.Collection, com.google.common.base.Predicate)
         * @param a
         * @return
         */
        List<E> filter(final Class<? extends Annotation> a);
    }

    /**
     * TODO
     */
    public static class OnPackage {}

    /**
     * TODO
     */
    public static class OnPackages {}

    public static class OnClass implements HasMethods, HasAnnotations {

        private Class<?> c;

        OnClass(Class c) {
            this.c = c;
        }

        public OnClasses onClasses() {
            return onClasses(false, true);
        }

        /**
         * <p>Act on classes (and interfaces if specified) declared and inherited by the class.</p>
         *
         * @param includeInterfaces If {@code true}, interfaces are appended to the result in order.
         * @param includeSelf If {@code true}, this {@link Class} is included (and as the first element).
         * @return {@link OnClasses} with the {@link Class} elements arranged in hierarchical order from the class.
         */
        public OnClasses onClasses(final boolean includeInterfaces, final boolean includeSelf) {

            final List<Class<?>> r = new ArrayList<Class<?>>();

            if (!c.isInterface()) {
                if (includeSelf) {
                    r.add(c);
                }
                r.addAll(getSuperClasses());
            }
            if (includeInterfaces) {
                r.addAll(getInterfaces(true, includeSelf));
            }

            return Reflects.onClasses(r);
        }

        public OnClasses onInnerClasses() {
            throw new UnsupportedOperationException();
        }

        public OnClasses onInterfaces() {
            return onInterfaces(true, true);
        }

        public OnClasses onInterfaces(final boolean includeInherited, final boolean includeSelf) {
            return Reflects.onClasses(getInterfaces(includeInherited, includeSelf));
        }

        public OnClasses onSuperClasses() {
            return Reflects.onClasses(getSuperClasses());
        }

        public OnMethods onMethods() {
            return onMethods(true, false, false, false);
        }

        public OnMethods onMethods(boolean includeInherited, boolean includeNonPublic, boolean includeStatic, boolean includeInterfaces) {

            Set<Method> methods = new LinkedHashSet<Method>();

            if (!c.isInterface()) {
                if (includeInherited) {
                    for (Class<?> c: onClasses(false, true).get()) {
                        exportMethods(methods, c, includeNonPublic, includeStatic);
                    }
                } else {
                    exportMethods(methods, c, includeNonPublic, includeStatic);
                }
            }

            if (c.isInterface() || includeInterfaces) {

                if (includeInherited) {
                    for (Class<?> i: onInterfaces(includeInherited, true).get()) {
                        exportMethods(methods, i, includeNonPublic, includeStatic);
                    }
                } else {
                    exportMethods(methods, c, includeNonPublic, includeStatic);
                }
            }

            return Reflects.onMethods(new ArrayList<Method>(methods));
        }

        private void exportMethods(Collection<Method> methods, Class<?> c, boolean includeNonPublic, boolean includeStatic) {

            Predicate<Method> p = null;

            if (!includeNonPublic) {
                p = publicMethod();
            }
            if (includeStatic) {
                if (null == p) {
                    p = staticMethod();
                } else {
                    p = and(p, staticMethod());
                }
            } else {
                if (null == p) {
                    p = not(staticMethod());
                } else {
                    p = and(p, not(staticMethod()));
                }
            }

            if (null == p) {
                exportElements(methods, c.getDeclaredMethods());
            } else {
                methods.addAll(Reflects.onMethods(c.getDeclaredMethods()).filter(p));
            }
        }

        private List<Class<?>> getInterfaces(boolean includeInherited, boolean includeSelf) {

            final Set<Class<?>> r = new LinkedHashSet<Class<?>>();

            if (includeSelf && c.isInterface()) {
                r.add(c);
            }
            if (includeInherited) {
                exportInterfaces(r, c);
            } else {
                exportElements(r, c.getInterfaces());
            }

            return new ArrayList<Class<?>>(r);
        }

        private List<Class<?>> getSuperClasses() {

            final List<Class<?>> r = new ArrayList<Class<?>>();

            Class<?> parent = c.getSuperclass();
            while (parent != null) {
                r.add(parent);
                parent = parent.getSuperclass();
            }

            return r;
        }

        private void exportInterfaces(Collection<Class<?>> out, Class<?> c) {
            for (Class<?> i : c.getInterfaces()) {
                if (out.add(i)) {
                    exportInterfaces(out, i);
                }
            }
        }

        @Override
        public OnAnnotations onAnnotations() {
            return onAnnotations(true);
        }

        @Override
        public OnAnnotations onAnnotations(boolean includeInherited) {

            List<Annotation> annotations = new ArrayList<Annotation>();

            exportElements(annotations, c.getDeclaredAnnotations());
            if (includeInherited) {
                for (Class<?> c: onClasses(true, false).get()) {
                    exportElements(annotations, c.getDeclaredAnnotations());
                }
            }

            return Reflects.onAnnotations(annotations);
        }
    }

    public static class OnClasses implements AnnotatableCollection<Class<?>>, HasMethods, HasAnnotations {

        private List<Class<?>> classes;

        OnClasses(List<Class<?>> classes) {
            this.classes = classes;
        }

        @Override
        public OnMethods onMethods() {
            return onMethods(true, false, false, false);
        }

        public OnMethods onMethods(boolean includeInherited, boolean includeNonPublic, boolean includeStatic, boolean includeInterfaces) {
            List<Method> r = new ArrayList<Method>();
            for (Class<?> c: classes) {
                r.addAll(Reflects.onClass(c).onMethods(includeInherited, includeNonPublic, includeStatic, includeInterfaces).get());
            }
            return Reflects.onMethods(r);
        }

        @Override
        public OnAnnotations onAnnotations() {
            return onAnnotations(true);
        }

        @Override
        public OnAnnotations onAnnotations(boolean includeInherited) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Class<?>> get() {
            return classes;
        }

        @Override
        public Class<?> seek(Predicate<? super Class<?>> predicate) {
            return seekElement(classes, predicate);
        }

        @Override
        public List<Class<?>> filter(final Class<? extends Annotation> a) {
            return filterAsList(classes, a);
        }

        @Override
        public List<Class<?>> filter(final String regex) {
            return filter(new Predicate<Class<?>>() { @Override public boolean apply(@Nullable Class<?> input) {
                return input != null && input.getName().matches(regex);
            }});
        }

        @Override
        public List<Class<?>> filter(Predicate<? super Class<?>> predicate) {
            return filterAsList(classes, predicate);
        }

        @Override
        public <O> List<O> transform(Function<? super Class<?>, O> f) {
            return transformAsList(classes, f);
        }
    }

    public static class OnMethod implements HasAnnotations {

        Method method;

        OnMethod(Method method) {
            this.method = method;
        }

        @Override
        public OnAnnotations onAnnotations() {
            return onAnnotations(true);
        }

        @Override
        public OnAnnotations onAnnotations(boolean includeInherited) {
            if (includeInherited) {
                List<Annotation> r = new ArrayList<Annotation>();
                for (Method m: Reflects.onClass(method.getDeclaringClass()).onMethods(true, !isPublic(method.getModifiers()), isStatic(method.getModifiers()), true).get()) {
                    exportElements(r, m.getDeclaredAnnotations());
                }
                return Reflects.onAnnotations(r);
            } else {
                return Reflects.onAnnotations(method.getDeclaredAnnotations());
            }
        }
    }

    public static class OnMethods implements AnnotatableCollection<Method>, HasAnnotations {

        List<Method> methods;

        OnMethods(List<Method> methods) {
            this.methods = methods;
        }

        @Override
        public OnAnnotations onAnnotations() {
            return onAnnotations(true);
        }

        @Override
        public OnAnnotations onAnnotations(boolean includeInherited) {
            Set<Annotation> r = new LinkedHashSet<Annotation>();
            for (Method m: methods) {
                r.addAll(Reflects.onMethod(m).onAnnotations(includeInherited).get());
            }
            return Reflects.onAnnotations(new ArrayList<Annotation>(r));
        }

        public <O> List<O> transform(Function<? super Method, O> f) {
            return transformAsList(methods, f);
        }

        public List<Method> get() {
            return methods;
        }

        @Override
        public Method seek(Predicate<? super Method> predicate) {
            return seekElement(methods, predicate);
        }

        public List<Method> filter(final Class<? extends Annotation> a) {
            return filterAsList(methods, a);
        }

        public List<Method> filter(final String regex) {
            return filter(new Predicate<Method>() { @Override public boolean apply(@Nullable Method input) {
                return input != null && input.getName().matches(regex);
            }});
        }

        public List<Method> filter(Predicate<? super Method> predicate) {
            return filterAsList(methods, predicate);
        }
    }

    public static class OnAnnotations implements OnCollection<Annotation> {

        private List<Annotation> annotations;

        OnAnnotations(List<Annotation> annotations) {
            this.annotations = annotations;
        }

        @Override
        public List<Annotation> get() {
            return annotations;
        }

        @Override
        public Annotation seek(Predicate<? super Annotation> predicate) {
            return seekElement(annotations, predicate);
        }

        @Override
        public List<Annotation> filter(final String regex) {
            return filter(new Predicate<Annotation>() { @Override public boolean apply(@Nullable Annotation a) {
                return a != null && a.annotationType().getName().matches(regex);
            }});
        }

        @Override
        public List<Annotation> filter(Predicate<? super Annotation> predicate) {
            return filterAsList(annotations, predicate);
        }

        @Override
        public <O> List<O> transform(Function<? super Annotation, O> f) {
            return transformAsList(annotations, f);
        }
    }

    /**
     * Factory for common {@link Predicate}.
     */
    public static final class Predicates {

        private static final Predicate<Method> PUBLIC_METHOD = new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
            return null != m && isPublic(m.getModifiers());
        }};
        private static final Predicate<Method> OBJECT_METHOD = new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
            return null != m && m.getDeclaringClass().equals(Object.class);
        }};
        private static final Predicate<Method> INSTANCE_METHOD = new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
            return null != m && !isStatic(m.getModifiers());
        }};
        private static final Predicate<Method> STATIC_METHOD = new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
            return null != m && isStatic(m.getModifiers());
        }};

        public static Predicate<Class<?>> classAnnotatedWith(final Class<? extends Annotation> annotation) {
            return new Predicate<Class<?>>() { @Override public boolean apply(@Nullable Class<?> c) {
                return null != c && c.isAnnotationPresent(annotation);
            }};
        }

        public static Predicate<Class<?>> classOfName(final String regex) {
            return new Predicate<Class<?>>() { @Override public boolean apply(@Nullable Class<?> c) {
                return null != c && c.getName().matches(regex);
            }};
        }

        public static Predicate<Method> methodAnnotatedWith(final Class<? extends Annotation> annotation) {
            return new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
                return null != m && m.isAnnotationPresent(annotation);
            }};
        }

        public static Predicate<Method> methodOfName(final String regex) {
            return new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
                return null != m && m.getName().matches(regex);
            }};
        }

        public static Predicate<Method> methodOfSignature(final Method m) {
            return methodOfSignature(m.getName(), m.getReturnType(), m.getParameterTypes());
        }

        public static Predicate<Method> methodOfSignature(final String name, final Class<?> returnType, final Class... parameterTypes) {
            return new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
                if (null != m && name.equals(m.getName()) && returnType.equals(m.getReturnType()) && parameterTypes.length == m.getParameterTypes().length) {
                    final Class<?>[] p = m.getParameterTypes();
                    for (int i = 0; i < p.length; i++) {
                        if (!parameterTypes[i].equals(p[i])) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }};
        }

        public static Predicate<Method> instanceMethod() {
            return INSTANCE_METHOD;
        }

        public static Predicate<Method> publicMethod() {
            return PUBLIC_METHOD;
        }

        public static Predicate<Method> objectMethod() {
            return OBJECT_METHOD;
        }

        public static Predicate<Method> staticMethod() {
            return STATIC_METHOD;
        }
    }

    public static final class Functions {

        private static final Function<Class<?>, String> classToName = new Function<Class<?>, String>() { @Override public String apply(@Nullable Class<?> c) {
            return c.getName();
        }};
        private static final Function<Annotation, Class<? extends Annotation>> annotationToClass = new Function<Annotation, Class<? extends Annotation>>() { @Override public Class apply(@Nullable Annotation a) {
            return a.annotationType();
        }};

        public static Function<Class<?>, String> classToName() {
            return classToName;
        }

        public static Function<Annotation, Class<? extends Annotation>> annotationToClass() {
            return annotationToClass;
        }
    }

    /**
     * @see OnClass
     * @param c
     * @return
     * @throws ClassNotFoundException
     */
    public static OnClass onClass(String c) throws ClassNotFoundException {
        return onClass(Class.forName(c));
    }

    /**
     * @see OnClass
     * @param o
     * @return
     */
    public static OnClass onObject(Object o) {
        return onClass(o.getClass());
    }

    /**
     * @see OnClass
     * @param c
     * @return
     */
    public static OnClass onClass(Class<?> c) {
        return new OnClass(c);
    }

    /**
     * @see OnClasses
     * @param classes
     * @return
     */
    public static OnClasses onClasses(Class<?>... classes) {
        return Reflects.onClasses(asList(classes));
    }

    /**
     * @see OnClasses
     * @param classes
     * @return
     */
    public static OnClasses onClasses(List<Class<?>> classes) {
        return new OnClasses(classes);
    }

    public static OnMethod onMethod(Method method) {
        return new OnMethod(method);
    }

    public static OnMethods onMethods(Method... methods) {
        return onMethods(asList(methods));
    }

    public static OnMethods onMethods(List<Method> methods) {
        return new OnMethods(methods);
    }

    public static OnAnnotations onAnnotations(final Annotation... annotations) {
        return onAnnotations(asList(annotations));
    }

    public static OnAnnotations onAnnotations(final List<Annotation> annotations) {
        return new OnAnnotations(annotations);
    }

    public static OnPackage onPackage(String pck) { throw new UnsupportedOperationException(); }

    public static OnPackages onPackages(String... pkgs) { throw new UnsupportedOperationException(); }

    private static <E> boolean exportElements(Collection<? super E> c, E... elements) {
        return Collections.addAll(c, elements);
    }

    private static <I, O> List<O> transformAsList(List<I> l, final Function<? super I, O> f) {
        java.util.Collection r = Collections2.transform(l, f);
        return r instanceof List ? (List<O>) r : new ArrayList<O>(r);
    }
    
    private static <E> List<E> filterAsList(final List<E> l, final Class<? extends Annotation> a) {
        return filterAsList(l, new Predicate<E>() {  public boolean apply(@Nullable E input) {
            return input != null && input instanceof AccessibleObject && ((AccessibleObject) input).isAnnotationPresent(a);
        }});
    }

    private static <E> List<E> filterAsList(final List<E> l, final Predicate<? super E> p) {
        java.util.Collection r = Collections2.filter(l, p);
        return r instanceof List ? (List<E>) r : new ArrayList<E>(r);
    }

    private static <E> E seekElement(final Collection<E> c, Predicate<? super E> p) {
        for (E e: c) {
            if (p.apply(e)) {
                return e;
            }
        }
        return null;
    }
}
