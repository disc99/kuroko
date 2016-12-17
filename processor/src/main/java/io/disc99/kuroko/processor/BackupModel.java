package io.disc99.kuroko.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class BackupModel {

    final TypeElement element;
    final BackupBeanMetaData bean;
    final Map<String, BackupPropertyMetaData> properties;
    final ProcessingEnvironment processingEnv;

    public BackupModel(TypeElement element, BackupBeanMetaData bean, Map<String, BackupPropertyMetaData> properties, ProcessingEnvironment processingEnv) {
        this.element = element;
        this.bean = bean;
        this.properties = properties;
        this.processingEnv = processingEnv;
    }

    public BackupBeanMetaData getBean() {
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

    public List<BackupPropertyMetaData> getProperties() {
        return properties.values().stream()
                .filter(BackupPropertyMetaData::isWritable)
                .collect(Collectors.toList());
    }

    public List<BackupPropertyMetaData> getPropertyAccessors() {
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