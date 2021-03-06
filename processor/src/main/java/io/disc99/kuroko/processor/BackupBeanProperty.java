package io.disc99.kuroko.processor;


/**
 * Bean property meta data
 * 
 * @param <T> the type of the bean which this property belongs to
 * @param <V> the type of this property
 */
public interface BackupBeanProperty<T, V> extends BackupBeanPropertyAccessor<T, V> {

    /**
     * Apply the value to the bean
     * 
     * @param bean the bean
     * @param value the value to set
     * @return the bean which is applied the value
     */
    T apply(T bean, V value);

}
