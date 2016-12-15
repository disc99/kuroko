package io.disc99.kuroko.processor;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import org.junit.Test;

import com.google.testing.compile.JavaFileObjects;

public class ProcessorTest {

    @Test
    public void testProcess() throws Exception {

        assert_().about(javaSource())
                .that(JavaFileObjects.forResource(ProcessorTest.class.getResource("/Person.java")))
                .processedWith(new KurokoProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(JavaFileObjects.forSourceString("foo.bar._Person",
                        "package sample.processor.generated;\n"
                        + "\n"
                        + "import java.lang.String;\n"
                        + "import javax.annotation.Generated;\n"
                        + "\n"
                        + "@Generated({\"me.geso.sample.hello.MyProcessor\"})\n"
                        + "public class Fuga {\n"
                        + "  public String hello() {\n"
                        + "    return \"hello\";\n"
                        + "  }\n"
                        + "}"));
    }
}

