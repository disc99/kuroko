package io.disc99.kuroko.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.processing.*;
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

import com.github.jknack.handlebars.Handlebars;

import com.github.jknack.handlebars.Template;
import io.disc99.kuroko.processor.util.Strings;

@SupportedAnnotationTypes(Constants.ANNOTATION)
public class KurokoProcessor extends AbstractProcessor {

    private ProcessorLogger logger;
    private static final Template TEMPLATE;
    static {
        try {
            TEMPLATE = new Handlebars().compile("meta-class");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    public KurokoProcessor() {
        super();
        logger = new ProcessorLogger(processingEnv);
    }

    /** {@inheritDoc} */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        String src =
                "package sample.processor.generated;\r\n"
                        + "public class Fuga {\r\n"
                        + "    public void hello() {\r\n"
                        + "        System.out.println(\"Hello World!!\");\r\n"
                        + "    }\r\n"
                        + "}\r\n"
                ;

        try {
            Messager messager = super.processingEnv.getMessager();

            Filer filer = super.processingEnv.getFiler();
            JavaFileObject javaFile = filer.createSourceFile("Fuga");

            try (Writer writer = javaFile.openWriter()) {
                writer.write(src);
            }

            messager.printMessage(Kind.NOTE, "generate source code!!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (roundEnv.processingOver()) {
            return true;
        }

        for (TypeElement annotation : annotations) {
            for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
                if (hasAnnotation(element)) {
                    generateMetaClass(element);
                }
            }
        }
        return true;
    }

    private boolean hasAnnotation(TypeElement element) {
        List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
        if (mirrors == null) {
            return false;
        }
        return mirrors.stream()
                .anyMatch(m -> m.getAnnotationType().toString().equals(Constants.ANNOTATION));
    }

    private void generateMetaClass(TypeElement element) {
        try {
            Model model = evaluate(element);
            JavaFileObject file = processingEnv.getFiler().createSourceFile(model.getFullQualifiedName(), element);
            try (BufferedWriter writer = new BufferedWriter(file.openWriter())) {
                writer.write(TEMPLATE.apply(model));
                writer.flush();
            }
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }


    private Model evaluate(TypeElement element) {

        BeanMetaData bean = new BeanMetaData(element, processingEnv);

        Map<String, PropertyMetaData> properties = new LinkedHashMap<>();

        // collect public getters
        for (ExecutableElement e : ElementFilter.methodsIn(element.getEnclosedElements())) {
            Set<Modifier> modifiers = e.getModifiers();
            if (modifiers.contains(Modifier.STATIC)) {
                continue;
            }
            if (modifiers.contains(Modifier.PUBLIC) && e.getParameters().isEmpty()) {
                PropertyMetaData metaData = new PropertyMetaData(bean, e, false, processingEnv);
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
                PropertyMetaData metaData = new PropertyMetaData(bean, e, true, processingEnv);
                if (metaData.isSetter()) {
                    String propertyName = metaData.getName();
                    if (properties.containsKey(propertyName)) {
                        properties.put(propertyName, metaData); // replace if exists
                    }
                }
            }
        }

        return new Model(element, bean, properties, processingEnv);
    }
}