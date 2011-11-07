package my.jug.reflect;

import static java.util.Arrays.asList;
import static my.jug.reflect.Reflects.Predicates.publicMethod;
import static my.jug.reflect.Reflects.Predicates.staticMethod;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * <p>Reflection utility class, to traverse, filter and trasnform reflection objects.</p>
 *
 * @author yclian
 * @since 1.0.20111103
 * @version 1.0.20111107
 */
public abstract class Reflects {

    protected Reflects() {}

    private static interface OnCollection<E> {

        /**
         * Get the target elements.
         *
         * @return
         */
        List<E> get();

        /**
         * Seek (for one, and first found) element given a {@link Predicate}.
         *
         * @param predicate
         * @return
         */
        E seek(Predicate<E> predicate);

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
        List<E> filter(Predicate<E> predicate);

        /**
         * @see Collections2#transform(java.util.Collection, com.google.common.base.Function)
         * @param f
         * @param <O>
         * @return
         */
        <O> List<O> transform(final Function<E, O> f);
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

    public static class OnClass {

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
                p = null == p ? staticMethod() : com.google.common.base.Predicates.<Method>and(p, staticMethod());
            }

            if (null == p) {
                exportElements(methods, c.getDeclaredMethods());
            } else {
                methods.addAll(Reflects.onMethods(c.getDeclaredMethods()).filter(p));
            }
        }

        public <A extends Annotation> OnAnnotations<A> onAnnotations () {
            return onAnnotations(true);
        }

        public OnAnnotations onAnnotations (boolean includeInherited) {

            List<Annotation> annotations = new ArrayList<Annotation>();

            exportElements(annotations, c.getDeclaredAnnotations());
            if (includeInherited) {
                for (Class<?> c: onClasses(true, false).get()) {
                    exportElements(annotations, c.getDeclaredAnnotations());
                }
            }

            return Reflects.onAnnotations(annotations);
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
    }

    public static class OnClasses implements AnnotatableCollection<Class<?>> {

        private List<Class<?>> classes;

        OnClasses(List<Class<?>> classes) {
            this.classes = classes;
        }

        public OnMethods onMethods(boolean includeInherited, boolean includeNonPublic) {
            List<Method> r = new ArrayList<Method>();
            for (Class<?> c: classes) {
                r.addAll(Reflects.onClass(c).onMethods(includeInherited, includeNonPublic, false, false).get());
            }
            return Reflects.onMethods(r);
        }

        @Override
        public List<Class<?>> get() {
            return classes;
        }

        @Override
        public Class<?> seek(Predicate predicate) {
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
        public List<Class<?>> filter(Predicate predicate) {
            return filterAsList(classes, predicate);
        }

        @Override
        public <O> List<O> transform(Function<Class<?>, O> f) {
            return transformAsList(classes, f);
        }
    }

    public static class OnMethods implements AnnotatableCollection<Method> {

        List<Method> methods;

        OnMethods(List<Method> methods) {
            this.methods = methods;
        }

        public <O> List<O> transform(Function<Method, O> f) {
            return transformAsList(methods, f);
        }

        public List<Method> get() {
            return methods;
        }

        @Override
        public Method seek(Predicate predicate) {
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

        public List<Method> filter(Predicate predicate) {
            return filterAsList(methods, predicate);
        }
    }

    public static class OnAnnotations<A extends Annotation> implements OnCollection<A> {

        private List<A> annotations;

        OnAnnotations(List<A> annotations) {
            this.annotations = annotations;
        }

        @Override
        public List<A> get() {
            return annotations;
        }

        @Override
        public A seek(Predicate<A> predicate) {
            return seekElement(annotations, predicate);
        }

        @Override
        public List<A> filter(final String regex) {
            return filter(new Predicate<Annotation>() { @Override public boolean apply(@Nullable Annotation a) {
                return a != null && a.annotationType().getName().matches(regex);
            }});
        }

        @Override
        public List<A> filter(Predicate predicate) {
            return filterAsList(annotations, predicate);
        }

        @Override
        public <O> List<O> transform(Function<A, O> f) {
            return transformAsList(annotations, f);
        }
    }

    public static final class Predicates {

        private static final Predicate<Method> PUBLIC_METHOD = new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
            return null != m && Modifier.isPublic(m.getModifiers());
        }};
        private static final Predicate<Method> OBJECT_METHOD = new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
            return null != m && m.getDeclaringClass().equals(Object.class);
        }};
        private static final Predicate<Method> INSTANCE_METHOD = new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
            return null != m && !Modifier.isStatic(m.getModifiers());
        }};
        private static final Predicate<Method> STATIC_METHOD = new Predicate<Method>() { @Override public boolean apply(@Nullable Method m) {
            return null != m && Modifier.isStatic(m.getModifiers());
        }};

        public static Predicate<Method> instanceMethod() { return INSTANCE_METHOD; }
        public static Predicate<Method> publicMethod() { return PUBLIC_METHOD; }
        public static Predicate<Method> objectMethod() { return OBJECT_METHOD; }
        public static Predicate<Method> staticMethod() { return STATIC_METHOD; }
    }

    public static final class Functions {

        private static final Function<Class<?>, String> classToName = new Function<Class<?>, String>() { @Override public String apply(@Nullable Class<?> c) {
            return c.getName();
        }};
        private static final Function<Annotation, Class<? extends Annotation>> annotationToClass = new Function<Annotation, Class<? extends Annotation>>() { @Override public Class apply(@Nullable Annotation a) {
            return a.annotationType();
        }};

        public static Function<Class<?>, String> classToName() { return classToName; }
        public static Function<Annotation, Class<? extends Annotation>> annotationToClass() { return annotationToClass; }
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

    public static OnMethods onMethod(Method method) { throw new UnsupportedOperationException(); }

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

    private static <E> boolean exportElements(Collection<E> c, E... elements) {
        return Collections.addAll(c, elements);
    }

    private static <I, O> List<O> transformAsList(List<I> l, final Function<I, O> f) {
        java.util.Collection r = Collections2.transform(l, f);
        return r instanceof List ? (List<O>) r : new ArrayList<O>(r);
    }
    
    private static <E> List<E> filterAsList(final List<E> l, final Class<? extends Annotation> a) {
        return filterAsList(l, new Predicate<E>() {  public boolean apply(@Nullable E input) {
            return input != null && input instanceof AccessibleObject && ((AccessibleObject) input).isAnnotationPresent(a);
        }});
    }

    private static <E> List<E> filterAsList(final List<E> l, final Predicate<E> p) {
        java.util.Collection r = Collections2.filter(l, p);
        return r instanceof List ? (List<E>) r : new ArrayList<E>(r);
    }

    private static <E> E seekElement(final Collection<E> c, Predicate<E> p) {
        for (E e: c) {
            if (p.apply(e)) {
                return e;
            }
        }
        return null;
    }
}
