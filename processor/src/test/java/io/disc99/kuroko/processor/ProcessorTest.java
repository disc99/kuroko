package io.disc99.kuroko.processor;

import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;


public class ProcessorTest {

    @Test
    public void testProcess() throws Exception {

        assert_().about(javaSource())
                .that(JavaFileObjects.forResource(ProcessorTest.class.getResource("/Person.java")))
                .processedWith(new KurokoProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(JavaFileObjects.forSourceString("foo.bar._Person",
                        "package foo.bar;\n"
                        + "\n"
                        + "import java.lang.String;\n"
                        + "import javax.annotation.Generated;\n"
                        + "\n"
                        + "@Generated({\"me.geso.sample.hello.MyProcessor\"})\n"
                        + "public class _Person {\n"
                        + "  public String hello() {\n"
                        + "    return \"hello\";\n"
                        + "  }\n"
                        + "}"));
    }
}

