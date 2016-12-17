package io.disc99.kuroko.processor;

import javax.lang.model.element.VariableElement;

public class MetaProperty {
    final VariableElement element;

    public MetaProperty(VariableElement element) {
        this.element = element;
    }

    public String getPropertyName() {
        return element.getSimpleName().toString();
    }
    public String getGetterMethodName() {
        return getPropertyName();
    }
    public String getGetterReturnType() {
        return getType();
    }
    public String getSetterMethodName() {
        return getPropertyName();
    }
    public String getSetterParameterType() {
        return getType();
    }

    private String getType() {
        return element.asType().toString();
    }
}
