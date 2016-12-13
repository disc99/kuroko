package sample;

import org.junit.Test;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 *
 */
public class C {
    @Test
    public void a() throws Exception {
        PropertyDescriptor pd = new PropertyDescriptor("name", Person.class);
        Method getter = pd.getReadMethod();
        Object f = getter.invoke(new Person());
        System.out.println(f);
    }

    static class Person {
        private String name = "tom";
        private boolean sex = false;
    }
}
