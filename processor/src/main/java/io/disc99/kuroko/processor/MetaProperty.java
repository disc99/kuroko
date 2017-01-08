package io.disc99.kuroko.processor;


import javax.lang.model.element.VariableElement;
import java.util.*;


public class MetaProperty {
    final ProcessorContext context;
    final VariableElement element;
    final List<AbstractGetter> getters;
    final List<AbstractGetter> kurokoGetters;
    final List<AbstractSetter> setters;

    public MetaProperty(ProcessorContext context, VariableElement element) {
        this.context = context;
        this.element = element;
        this.getters = Arrays.asList(new JavaBeansGetter(), new PropertyNameGetter());
        this.kurokoGetters = context.containsTarget(getType())
                ? Arrays.asList(new JavaBeanKurokoGetter(), new PropertyNameKurokoGetter())
                : Collections.emptyList();
        this.setters = Arrays.asList(new JavaBeansSetter(), new PropertyNameSetter());
    }

    public String getPropertyName() {
        return element.getSimpleName().toString();
    }

    public List<AbstractGetter> getGetters() {
        return getters;
    }

    public AbstractGetter getGetter() {
        return getters.get(0);
    }

    public List<AbstractGetter> getKurokoGetters() {
        return kurokoGetters;
    }

    public List<AbstractSetter> getSetters() {
        return setters;
    }



    private String getType() {
        return element.asType().toString();
    }

    interface Method {
        String getMethodName();
    }
    abstract class AbstractGetter implements Method {
        abstract String getReturnType();

        String convertMethodName(String name) {
            String prefix = "boolean".equals(getType()) ? "is" : "get";
            return prefix + name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        }
    }
    abstract class AbstractSetter implements Method {
        abstract String getParameterType();

        String convertMethodName(String name) {
            return "set" + name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        }
    }
    interface KurokoProperty {

        default String convertKurokoName(String name) {
            return "_" + name;
        }
        // com.example.Name => com.example._Name
        default String convertKurokoType(String name) {
            String[] names = name.split("\\.");
            int lastIndex = names.length - 1;
            names[lastIndex] = convertKurokoName(names[lastIndex]);
            return String.join(".", names);
        }

    }

    public class JavaBeansGetter extends AbstractGetter {
        @Override
        public String getMethodName() {
            return convertMethodName(getPropertyName());
        }
        @Override
        public String getReturnType() {
            return getType();
        }
    }

    public class JavaBeanKurokoGetter
            extends AbstractGetter implements KurokoProperty {
        @Override
        public String getMethodName() {
            return convertMethodName(convertKurokoName(getPropertyName()));
        }
        @Override
        public String getReturnType() {
            return convertKurokoType(getType());
        }
    }

    public class PropertyNameGetter extends AbstractGetter {
        @Override
        public String getMethodName() {
            return getPropertyName();
        }
        @Override
        public String getReturnType() {
            return getType();
        }
    }

    public class PropertyNameKurokoGetter
            extends AbstractGetter implements KurokoProperty {
        @Override
        public String getMethodName() {
            return convertKurokoName(getPropertyName());
        }
        @Override
        public String getReturnType() {
            return convertKurokoType(getType());
        }
    }

    public class JavaBeansSetter extends AbstractSetter {
        @Override
        public String getMethodName() {
            return convertMethodName(getPropertyName());
        }
        @Override
        public String getParameterType() {
            return getType();
        }
    }

    public class PropertyNameSetter extends AbstractSetter {
        @Override
        public String getMethodName() {
            return getPropertyName();
        }
        @Override
        public String getParameterType() {
            return getType();
        }
    }
}


