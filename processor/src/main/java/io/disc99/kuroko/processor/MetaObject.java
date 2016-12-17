package io.disc99.kuroko.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MetaObject {

    final TypeElement element;
    final ProcessingEnvironment processingEnv;
    final List<MetaProperty> properties;

    MetaObject(TypeElement element, ProcessingEnvironment processingEnv) {
        this.element = element;
        this.processingEnv = processingEnv;
        this.properties = ElementFilter.fieldsIn(element.getEnclosedElements()).stream()
                .map(MetaProperty::new)
                .collect(toList());
    }


    public String getBaseClassName() {
        return element.getSimpleName().toString();
    }

    public List<MetaProperty> getProperties() {
        return properties;
    }

    public String getPackageName() {
        return processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }

    public String getClassName() {
        return "_" + getBaseClassName();
    }


    public String getFullQualifiedName() {
        return String.format("%s.%s", getPackageName(), getClassName());
    }
}
