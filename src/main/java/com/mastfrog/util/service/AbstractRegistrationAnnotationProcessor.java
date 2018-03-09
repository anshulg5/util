/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.util.service;

import com.mastfrog.util.Strings;
import static com.mastfrog.util.collections.CollectionUtils.setOf;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Base class for annotation processors that generate a registration file somewhere under META-INF.
 * Has support for writing lines to the file, deriving class names from annotation fields of type Class,
 * which requires some complexity with AnnotationMirror.  Simply override getOrder() and handleOne() and
 * call addLine().
 *
 * @author Tim Boudreau
 */
public abstract class AbstractRegistrationAnnotationProcessor<T extends Annotation> extends IndexGeneratingProcessor {

    protected final Set<String> legalOn;
    protected final Class<T> annotationType;
    Set<Element> elements = new HashSet<>();
    private final List<String> deferred = new LinkedList<>();

    public AbstractRegistrationAnnotationProcessor(
            Class<T> annotationType,
            String... annotationLegalOnFqns) {
        super(true);
        this.annotationType = annotationType;
        this.legalOn = setOf(annotationLegalOnFqns);
    }

    protected abstract int getOrder(T anno);

    protected boolean isSubtypeOf(Element e, String typeName) {
        Types types = processingEnv.getTypeUtils();
        Elements elementUtils = processingEnv.getElementUtils();
        TypeElement pageType = elementUtils.getTypeElement( typeName );
        if ( pageType == null ) {
            return false;
        }
        return types.isSubtype( e.asType(), pageType.asType() );
    }

    protected boolean isLegalForAnnotation(Element e) {
        if ( legalOn.isEmpty() ) {
            return true;
        }
        for ( String type : legalOn ) {
            if ( isSubtypeOf( e, type ) ) {
                return true;
            }
        }
        return false;
    }

    protected AnnotationMirror findMirror(Element el,
                                          Class<? extends Annotation> annoType) {
        for ( AnnotationMirror mir : el.getAnnotationMirrors() ) {
            TypeMirror type = mir.getAnnotationType().asElement().asType();
            if ( annoType.getName().equals( type.toString() ) ) {
                return mir;
            }
        }
        return null;
    }

    protected String canonicalize(TypeMirror tm, Types types) {
        TypeElement e = (TypeElement) types.asElement( tm );
        StringBuilder nm = new StringBuilder( e.getQualifiedName().toString() );
        Element enc = e.getEnclosingElement();
        while ( enc != null && enc.getKind() != ElementKind.PACKAGE ) {
            int ix = nm.lastIndexOf( "." );
            if ( ix > 0 ) {
                nm.setCharAt( ix, '$' );
            }
            enc = enc.getEnclosingElement();
        }
        return nm.toString();
    }

    protected void warn(String warning) {
        processingEnv.getMessager().printMessage( Diagnostic.Kind.WARNING, warning );
    }

    protected static String types(Object o) { //debug stuff
        List<String> s = new ArrayList<>();
        Class<?> x = o.getClass();
        while ( x != Object.class ) {
            s.add( x.getName() );
            for ( Class<?> c : x.getInterfaces() ) {
                s.add( c.getName() );
            }
            x = x.getSuperclass();
        }
        StringBuilder sb = new StringBuilder();
        for ( String ss : s ) {
            sb.append( ss );
            sb.append( ", " );
        }
        return sb.toString();
    }

    protected List<String> typeList(AnnotationMirror mirror, String param, String... failIfNotSubclassesOf) {
        List<String> result = new ArrayList<>();
        if ( mirror != null ) {
            for ( Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> x : mirror.getElementValues()
                    .entrySet() ) {
                String annoParam = x.getKey().getSimpleName().toString();
                if ( param.equals( annoParam ) ) {
                    if ( x.getValue().getValue() instanceof List<?> ) {
                        List<?> list = (List<?>) x.getValue().getValue();
                        for ( Object o : list ) {
                            if ( o instanceof AnnotationValue ) {
                                AnnotationValue av = (AnnotationValue) o;
                                if ( av.getValue() instanceof DeclaredType ) {
                                    DeclaredType dt = (DeclaredType) av.getValue();
                                    if ( failIfNotSubclassesOf.length > 0 ) {
                                        boolean found = false;
                                        for ( String f : failIfNotSubclassesOf ) {
                                            if ( !isSubtypeOf( dt.asElement(), f ) ) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if ( !found ) {
                                            fail( "Not a " + Strings.join( '/', failIfNotSubclassesOf ) + " subtype: "
                                                  + av );
                                            continue;
                                        }
                                    }
                                    // Convert e.g. mypackage.Foo.Bar.Baz to mypackage.Foo$Bar$Baz
                                    String canonical = canonicalize( dt.asElement().asType(),
                                                                     processingEnv.getTypeUtils() );
                                    result.add( canonical );
                                } else {
                                    // Unresolvable type?
                                    warn( "Not a declared type: " + av );
                                }
                            } else {
                                // Probable invalid source
                                warn( "Annotation value for value() is not an AnnotationValue " + types( o ) );
                            }
                        }
                    } else if ( x.getValue().getValue() instanceof DeclaredType ) {
                        DeclaredType dt = (DeclaredType) x.getValue().getValue();
                        if ( failIfNotSubclassesOf.length > 0 ) {
                            boolean found = false;
                            for ( String f : failIfNotSubclassesOf ) {
                                if ( !isSubtypeOf( dt.asElement(), f ) ) {
                                    found = true;
                                    break;
                                }
                            }
                            if ( !found ) {
                                fail( "Not a " + Strings.join( '/', failIfNotSubclassesOf ) + " subtype: " + dt );
                                continue;
                            }
                        }
                        // Convert e.g. mypackage.Foo.Bar.Baz to mypackage.Foo$Bar$Baz
                        String canonical = canonicalize( dt.asElement().asType(), processingEnv.getTypeUtils() );
                        result.add( canonical );
                    } else {
                        warn( "Annotation value for is not a List of types or a DeclaredType on " + mirror + " - "
                              + types( x.getValue().getValue() ) + " - invalid source?" );
                    }
                }
            }
        }
        return result;
    }

    protected List<String> classNames(Element el,
                                      Class<? extends Annotation> annotationType, String memberName,
                                      String... mustBeSubtypesOf) {
        AnnotationMirror mirror = findMirror( el, annotationType );
        List<String> result = typeList( mirror, memberName, mustBeSubtypesOf );
        return result;
    }

    protected void fail(String msg, Element el) {
        processingEnv.getMessager().printMessage( Diagnostic.Kind.ERROR, msg, el );
    }

    protected void fail(String msg) {
        processingEnv.getMessager().printMessage( Diagnostic.Kind.ERROR, msg );
    }

    @Override
    public boolean handleProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<Element> all = new HashSet<>( roundEnv.getElementsAnnotatedWith( annotationType ) );
        List<String> failed = new LinkedList<>();

        // Add in any types that could not be generated on a previous round because
        // they relied on a generated time (i.e. @InjectRequestBodyAs can't be copied
        // correctly into a generated page subclass if the type of its value will be
        // generated by Numble in a later round of processing)
        for ( String type : deferred ) {
            TypeElement retry = processingEnv.getElementUtils().getTypeElement( type );
            all.add( retry );
        }
        deferred.clear();
        try {
            for ( Element e : all ) {
                Annotation anno = e.getAnnotation( annotationType );
                if ( anno == null || !annotationType.isInstance( anno ) ) {
                    continue;
                }
                T a = annotationType.cast(anno);
                int order = getOrder( a );
                boolean isLegalForAnnotation = isLegalForAnnotation( e );
                if ( !isLegalForAnnotation ) {
                    fail( "Not a subclass of " + Strings.join( '/', legalOn ) + ": " + e.asType(), e );
                }
                elements.add( e );
                if ( isLegalForAnnotation ) {
                    handleOne( e, a, order );
                }
            }
        } catch ( Exception ex ) {
            Logger.getLogger(AbstractRegistrationAnnotationProcessor.class.getName() ).log( Level.SEVERE, null, ex );
            return false;
        }
        deferred.addAll( failed );
        return failed.isEmpty();
    }

    protected abstract void handleOne(Element e, T anno, int order);
}
