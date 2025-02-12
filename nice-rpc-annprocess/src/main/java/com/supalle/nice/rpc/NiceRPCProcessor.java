package com.supalle.nice.rpc;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AutoService(Processor.class)
public class NiceRPCProcessor extends AbstractProcessor {
    private ProcessingEnvironment processingEnv;

    // 候选集合
    private final ConcurrentMap<Element, Boolean> candidates = new ConcurrentHashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
    }

    private Context getContext(ProcessingEnvironment processingEnv) {
        Elements elements = processingEnv.getElementUtils();
        JavaCompiler javaCompiler = getFieldValue("javaCompiler", elements);
        return getFieldValue("context", javaCompiler);
    }

    private <T> T getFieldValue(String fieldName, Object instance) {
        if (instance == null || fieldName == null) {
            return null;
        }
        try {
            Optional<Field> opt = Arrays.stream(instance.getClass().getDeclaredFields())
                    .filter(field -> fieldName.equals(field.getName()))
                    .findFirst();
            if (opt.isPresent()) {
                Field field = opt.get();
                field.setAccessible(true);
                return (T) field.get(instance);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(NiceRPC.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(NiceRPC.class);
        if (elements != null && !elements.isEmpty()) {
            for (Element element : elements) {
                candidates.put(element, Boolean.FALSE);
            }
        }
        if (roundEnv.processingOver() || roundEnv.errorRaised() || LombokState.isLombokInvoked()) {
            finishRemaining();
            return true;
        }
        return false;
    }

    private void finishRemaining() {
        ConcurrentMap<Element, Boolean> candidates = this.candidates;
        System.out.println(candidates);
    }

}