package io.disc99.kuroko.processor;


import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

public class BackupBeanMetaData {

    final TypeElement element;
    final ProcessingEnvironment processingEnv;

    BackupBeanMetaData(TypeElement element, ProcessingEnvironment processingEnv) {
        this.element = element;
        this.processingEnv = processingEnv;
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