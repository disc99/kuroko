package io.disc99.kuroko.processor;


import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

import io.disc99.kuroko.processor.util.Strings;

public class BackupPropertyMetaData {

    final BackupBeanMetaData bean;
    final ExecutableElement element;
    final boolean writable;
    final ProcessingEnvironment processingEnv;

    BackupPropertyMetaData(BackupBeanMetaData bean, ExecutableElement element, boolean writable, ProcessingEnvironment processingEnv) {
        this.bean = bean;
        this.element = element;
        this.writable = writable;
        this.processingEnv = processingEnv;
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

    public BackupBeanMetaData getBean() {
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