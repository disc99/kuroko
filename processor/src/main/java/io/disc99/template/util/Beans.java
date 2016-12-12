package io.disc99.template.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import io.disc99.template.exception.PropertyAccessRuntimeException;

/**
 * Bean utilities
 */
public final class Beans {

    private static Map<Class<?>, Map<String, PropertyDescriptor>> CACHE;
    static {
        CACHE = new ConcurrentHashMap<>();
    }

    private static final char PATH_SEPARATOR = '.';

    /**
     * Get the bean property 
     * 
     * @param bean bean
     * @param propertyName propertyName
     * @return property value
     */
    public static Object getProperty(Object bean, String propertyName) {

        int p = propertyName.indexOf(PATH_SEPARATOR);
        if (p > 0) {
            Object property = getProperty(bean, propertyName.substring(0, p));
            if (property == null) {
                return null;
            }
            return getProperty(property, propertyName.substring(p + 1));
        }

        Class<?> clazz = bean.getClass();
        PropertyDescriptor descriptor = getPropertyDescriptor(clazz, propertyName);
        Method method = descriptor.getReadMethod();
        if (method == null) {
            String message = String.format("ReadMethod not found: class=%s, property=%s", clazz.getName(), propertyName);
            throw new PropertyAccessRuntimeException(message);
        }
        return invoke(bean, method);

    }

    private static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName) {
        PropertyDescriptor propertyDescriptor = null;
        int p = propertyName.indexOf(PATH_SEPARATOR);
        if (p > 0) {
            String property = propertyName.substring(0, p);
            propertyDescriptor = getPropertyDescriptor(clazz, property);
            if (propertyDescriptor != null) {
                return getPropertyDescriptor(propertyDescriptor.getPropertyType(), propertyName.substring(p + 1));
            }
        } else {
            propertyDescriptor = getPropertyDescriptors(clazz).get(propertyName);
        }
        if (propertyDescriptor == null) {
            String message = String.format("Property not found: class=%s, property=%s", clazz.getName(), propertyName);
            throw new PropertyAccessRuntimeException(message);
        }
        return propertyDescriptor;
    }

    private static Map<String, PropertyDescriptor> getPropertyDescriptors(final Class<?> clazz) {
    	return CACHE.computeIfAbsent(clazz, key -> {
            Map<String, PropertyDescriptor> descriptors = new HashMap<>();
            collectBeanInfo(key).stream()
            	.flatMap(info -> Stream.of(info.getPropertyDescriptors()))
            	.forEach(propertyDescriptor -> {
					PropertyDescriptor existence = descriptors.get(propertyDescriptor.getName());
					Class<?> existenceType = existence == null ? null : existence.getPropertyType();
					if (existenceType == null || !propertyDescriptor.getPropertyType().isAssignableFrom(existenceType)) {
					    descriptors.put(propertyDescriptor.getName(), propertyDescriptor);
					}
            	});
            return descriptors;
    	});
    }

    private static List<BeanInfo> collectBeanInfo(Class<?> clazz) {
        List<BeanInfo> beanInfoList = new ArrayList<>();
        BeanInfo info = getBeanInfo(clazz);
        if (info != null) {
            beanInfoList.add(info);
        }
        Arrays.stream(clazz.getInterfaces())
        	.filter(i -> Modifier.isPublic(i.getModifiers()))
        	.forEach(i -> beanInfoList.addAll(collectBeanInfo(i)));
        return beanInfoList;
    }

    private static BeanInfo getBeanInfo(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        try {
            return Modifier.isPublic(clazz.getModifiers()) ? Introspector.getBeanInfo(clazz) : getBeanInfo(clazz.getSuperclass());
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invoke(Object o, Method method, Object... args) {
        try {
            return method.invoke(o, args);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Beans() {
    }

}
