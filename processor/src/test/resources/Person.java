package foo.bar;

import io.disc99.kuroko.annotation.Kuroko;
import foo.bar.value.Name;

import java.io.File;
import java.util.List;

@Kuroko
public class Person {
    private static final int ID = 1;
    private int age;
    private File book;
    private Name name;
    private List<String> items;
    String str1;
    protected String str2;
    public String str3;
    private int num1;
    private Integer num2;
}
