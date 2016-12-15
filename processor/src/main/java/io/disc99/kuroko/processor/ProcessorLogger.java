package io.disc99.kuroko.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

class ProcessorLogger {
    private ProcessingEnvironment processingEnv;

    ProcessorLogger(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }


    void error(String format, Object... args) {
        error(null, format, args);
    }

    private void error(Element element, String format, Object... args) {
        log(Diagnostic.Kind.ERROR, element, String.format(format, args));
    }

    private void log(Diagnostic.Kind kind, Element element, String message) {
        processingEnv.getMessager().printMessage(kind, message, element);
    }

}
