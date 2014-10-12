package com.github.disc99.kuroko.annotation.processor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.github.disc99.template.engine.Template;
import com.github.disc99.template.util.Strings;

@SupportedAnnotationTypes(Constants.ANNOTATION)
public class KurokoProcessor extends AbstractProcessor {

    private static final Template TEMPLATE;
    static {
        try {
        	StringBuffer sb = new StringBuffer();
			BufferedReader br = new BufferedReader(new InputStreamReader(KurokoProcessor.class.getResourceAsStream("meta-class.template"), "UTF-8"));
			for (int c = br.read(); c != -1; c = br.read()){
				sb.append((char)c);
			}

			TEMPLATE = new Template(sb.toString());   
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    /** {@inheritDoc} */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }
        for (TypeElement annotation : annotations) {
            for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
                if (hasAnnotation(element)) {
                    generateMetaClass(element, roundEnv);
                }
            }
        }
        return true;
    }

    private boolean hasAnnotation(TypeElement element) {
        List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
        if (mirrors == null || mirrors.isEmpty()) {
            return false;
        }
        for (AnnotationMirror mirror : mirrors) {
            if (mirror.getAnnotationType().toString().equals(Constants.ANNOTATION)) {
                return true;
            }
        }
        return false;
    }

    private void generateMetaClass(TypeElement element, RoundEnvironment roundEnv) {
        try {
            Model model = evaluate(element);
            JavaFileObject file = processingEnv.getFiler().createSourceFile(model.getFullQualifiedName(), element);
            try (BufferedWriter writer = new BufferedWriter(file.openWriter())) {
                writer.write(TEMPLATE.render(model));
                writer.flush();
            }
        } catch (IOException e) {
            error(e.toString());
        }
    }

    private Model evaluate(TypeElement element) {

        BeanMetaData bean = new BeanMetaData(element);

        Map<String, PropertyMetaData> properties = new LinkedHashMap<>();

        // collect public getters
        for (ExecutableElement e : ElementFilter.methodsIn(element.getEnclosedElements())) {
            Set<Modifier> modifiers = e.getModifiers();
            if (modifiers.contains(Modifier.STATIC)) {
                continue;
            }
            if (modifiers.contains(Modifier.PUBLIC) && e.getParameters().isEmpty()) {
                PropertyMetaData metaData = new PropertyMetaData(bean, e, false);
                if (metaData.isGetter()) {
                    String propertyName = metaData.getName();
                    if (properties.containsKey(propertyName)) {
                        continue;
                    }
                    properties.put(propertyName, metaData);
                }
            }
        }

        for (ExecutableElement e : ElementFilter.methodsIn(element.getEnclosedElements())) {
            Set<Modifier> modifiers = e.getModifiers();
            if (modifiers.contains(Modifier.STATIC)) {
                continue;
            }
            if (modifiers.contains(Modifier.PUBLIC) && e.getParameters().size() == 1) {
                PropertyMetaData metaData = new PropertyMetaData(bean, e, true);
                if (metaData.isSetter()) {
                    String propertyName = metaData.getName();
                    if (properties.containsKey(propertyName)) {
                        properties.put(propertyName, metaData); // replace if exists
                    }
                }
            }
        }

        return new Model(element, bean, properties);

    }

    public class Model {

        final TypeElement element;

        final BeanMetaData bean;

        final Map<String, PropertyMetaData> properties;

        Model(TypeElement element, BeanMetaData bean, Map<String, PropertyMetaData> properties) {
            this.element = element;
            this.bean = bean;
            this.properties = properties;
        }

        public BeanMetaData getBean() {
            return bean;
        }

        public String getFullQualifiedName() {
            return String.format("%s.%s", getPackageName(), getClassName());
        }

        public String getPackageName() {
            return toMetaPackageName(bean.getPackageName());
        }

        public String getClassName() {
            return toMetaClassName(bean.getClassName());
        }

        public List<PropertyMetaData> getProperties() {
        	return properties.values().stream()
        				.filter(property -> property.isWritable())
        				.collect(Collectors.toList());
        }

        public List<PropertyMetaData> getPropertyAccessors() {
        	return properties.values().stream()
        				.filter(property -> !property.isWritable())
        				.collect(Collectors.toList());
        }

        private String toMetaPackageName(String baseName) {
            String packageName = getOption(Options.PACKAGE, baseName);
            String packageSuffix = getOption(Options.PACKAGE_SUFFIX, null);
            if (packageSuffix != null) {
                packageName = packageName == null || packageName.trim().length() == 0 ? packageSuffix : String.format("%s.%s", packageName, packageSuffix);
            }
            return packageName;
        }

        private String toMetaClassName(String baseName) {
            String prefix = getOption(Options.CLASS_PREFIX, null);
            String suffix = getOption(Options.CLASS_SUFFIX, null);
            if (prefix == null && suffix == null) {
                suffix = "__";
            }
            String className = baseName;
            if (prefix != null) {
                className = String.format("%s%s", prefix, className);
            }
            if (suffix != null) {
                className = String.format("%s%s", className, suffix);
            }
            return className;
        }

        private String getOption(String key, String defaultValue) {
            final String value = processingEnv.getOptions().get(key);
            return value == null ? defaultValue : value;
        }

    }

    public class BeanMetaData {

        final TypeElement element;

        BeanMetaData(TypeElement element) {
            this.element = element;
        }
        
        public String getFullQualifiedName() {
            return element.getQualifiedName().toString();
        }

        public String getPackageName() {
            return processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
        }

        public String getClassName() {
            return element.getSimpleName().toString();
        }

    }

    public class PropertyMetaData {

        final BeanMetaData bean;

        final ExecutableElement element;

        final boolean writable;

        PropertyMetaData(BeanMetaData bean, ExecutableElement element, boolean writable) {
            this.bean = bean;
            this.element = element;
            this.writable = writable;
        }

        public String getName() {
            String methodName = getMethodName();
            return Strings.uncapitalize(methodName.replaceAll("^(set|get|is)", ""));
        }

        public String getWrappedType() {
            TypeMirror mirror = getTypeMirror();
            if (mirror.getKind().isPrimitive()) {
                return processingEnv.getTypeUtils().boxedClass((PrimitiveType) mirror).getQualifiedName().toString();
            }
            return mirror.toString();
        }

        public String getType() {
            return getTypeMirror().toString();
        }

        public BeanMetaData getBean() {
            return bean;
        }

        public String getReadMethodName() {
            String template = "boolean".equals(getType()) ? "is%s" : "get%s";
            return String.format(template, Strings.capitalize(getName()));
        }

        public String getWriteMethodName() {
            return String.format("set%s", Strings.capitalize(getName()));
        }

        public boolean isGetter() {
            return getMethodName().startsWith("get") || getMethodName().startsWith("is");
        }

        public boolean isSetter() {
            return getMethodName().startsWith("set");
        }

        public String getMethodName() {
            return element.getSimpleName().toString();
        }

        boolean isWritable() {
            return writable;
        }

        private TypeMirror getTypeMirror() {
            String methodName = getMethodName();
            if (methodName.startsWith("get") || methodName.startsWith("is")) {
                List<? extends VariableElement> params = element.getParameters();
                if (params.isEmpty()) {
                    return element.getReturnType();
                }
            } else if (methodName.startsWith("set")) {
                List<? extends VariableElement> params = element.getParameters();
                if (params.size() == 1) {
                    VariableElement param = params.iterator().next();
                    return param.asType();
                }
            }
            throw new IllegalStateException();
        }

    }

    private void error(String format, Object... args) {
        error(null, format, args);
    }

    private void error(Element element, String format, Object... args) {
        log(Kind.ERROR, element, String.format(format, args));
    }

    private void log(Kind kind, Element element, String message) {
        processingEnv.getMessager().printMessage(kind, message, element);
    }

}