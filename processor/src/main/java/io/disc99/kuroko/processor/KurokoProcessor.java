package io.disc99.kuroko.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;

import com.github.jknack.handlebars.Template;

@SupportedAnnotationTypes(Constants.ANNOTATION)
public class KurokoProcessor extends AbstractProcessor {

    private ProcessorLogger logger;
    private static final Template TEMPLATE;
    static {
        try {

            TEMPLATE = new Handlebars()
                    .with(EscapingStrategy.NOOP)
                    .compile("kuroko-class");
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
//            Model model = evaluate(element);
            MetaObject object = evaluate2(element);
            JavaFileObject file = processingEnv.getFiler().createSourceFile(object.getFullQualifiedName(), element);
            try (BufferedWriter writer = new BufferedWriter(file.openWriter())) {
                writer.write(TEMPLATE.apply(object));
                writer.flush();
            }
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }

    private MetaObject evaluate2(TypeElement element) {
        return new MetaObject(element, processingEnv);
    }


    private BackupModel evaluate(TypeElement element) {

        BackupBeanMetaData bean = new BackupBeanMetaData(element, processingEnv);

        Map<String, BackupPropertyMetaData> properties = new LinkedHashMap<>();

        // collect public getters
        for (ExecutableElement e : ElementFilter.methodsIn(element.getEnclosedElements())) {
            Set<Modifier> modifiers = e.getModifiers();
            if (modifiers.contains(Modifier.STATIC)) {
                continue;
            }
            if (modifiers.contains(Modifier.PUBLIC) && e.getParameters().isEmpty()) {
                BackupPropertyMetaData metaData = new BackupPropertyMetaData(bean, e, false, processingEnv);
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
                BackupPropertyMetaData metaData = new BackupPropertyMetaData(bean, e, true, processingEnv);
                if (metaData.isSetter()) {
                    String propertyName = metaData.getName();
                    if (properties.containsKey(propertyName)) {
                        properties.put(propertyName, metaData); // replace if exists
                    }
                }
            }
        }

        return new BackupModel(element, bean, properties, processingEnv);
    }
}