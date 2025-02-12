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
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;
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
        for (Map.Entry<Element, Boolean> entry : candidates.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                continue;
            }
            Element element = entry.getKey();

            TypeMirror elementType = element.asType();

            String className = elementType.toString();

            List<? extends Element> enclosedElements = element.getEnclosedElements();

            try {

                String packageName = null;
                int lastDot = className.lastIndexOf('.');
                if (lastDot > 0) {
                    packageName = className.substring(0, lastDot);
                }
                String simpleClassName = className.substring(lastDot + 1);
                String builderClassName = className + "NiceRpcImpl";
                String builderSimpleClassName = builderClassName
                        .substring(lastDot + 1);
                String rpcImplClassName = "NiceRpc_" + className + "Impl";

                JavaFileObject rpcImplFile = processingEnv.getFiler().createSourceFile(builderClassName);
                try (PrintWriter out = new PrintWriter(rpcImplFile.openWriter())) {

                    if (packageName != null) {
                        out.print("package ");
                        out.print(packageName);
                        out.println(";");
                        out.println();
                    }

                    out.print("public class ");
                    out.print(builderSimpleClassName);
                    out.println(" {");
                    out.println();

                    out.print("    private ");
                    out.print(simpleClassName);
                    out.print(" object = new ");
                    out.print(simpleClassName);
                    out.println("();");
                    out.println();

                    out.print("    public ");
                    out.print(simpleClassName);
                    out.println(" build() {");
                    out.println("        return object;");
                    out.println("    }");
                    out.println();


                    for (Element enclosedElement : enclosedElements) {
                        // String methodName = setter.getKey();
                        // String argumentType = setter.getValue();
                        //
                        // out.print("    public ");
                        // out.print(builderSimpleClassName);
                        // out.print(" ");
                        // out.print(methodName);
                        //
                        // out.print("(");
                        //
                        // out.print(argumentType);
                        // out.println(" value) {");
                        // out.print("        object.");
                        // out.print(methodName);
                        // out.println("(value);");
                        // out.println("        return this;");
                        // out.println("    }");
                        // out.println();
                    }
                    out.println("    }");
                    out.println();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            entry.setValue(Boolean.TRUE);
        }

    }
}