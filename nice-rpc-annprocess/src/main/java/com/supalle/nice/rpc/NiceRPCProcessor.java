package com.supalle.nice.rpc;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

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

                    out.println("import com.supalle.nice.rpc.*;");

                    out.print("public class ");
                    out.print(builderSimpleClassName);
                    out.print(" implements ");
                    out.print(className);
                    out.println(" {");
                    out.println();

                    out.print("    private final RpcEngine rpcEngine;");
                    out.println();
                    out.print("    public " + builderSimpleClassName + "(RpcEngine rpcEngine) {");
                    out.println();
                    out.print("        this.rpcEngine = rpcEngine;");
                    out.println();
                    out.print("    }");

                    out.println();

                    int i = 0;
                    for (Element enclosedElement : enclosedElements) {
                        if (enclosedElement instanceof ExecutableElement) {
                            ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                            Name methodName = enclosedElement.getSimpleName();
                            List<? extends TypeParameterElement> typeParameters = executableElement.getTypeParameters();
                            List<? extends VariableElement> parameters = executableElement.getParameters();
                            TypeMirror returnType = executableElement.getReturnType();
                            String rpcMethodRef = "__rpcMethod__" + i;
                            out.print("    private final RpcMethod ");
                            out.print(rpcMethodRef);
                            out.print(" = RpcMethodImpl.of(");
                            out.print(builderSimpleClassName);
                            out.print(".class, \"");
                            out.print(methodName);
                            out.print("\", ");
                            if (parameters == null || parameters.isEmpty()) {
                                out.print("void.class");
                            } else {
                                out.print(parameters.stream().map(parameter -> parameter.asType().toString() + ".class").collect(Collectors.joining(" ,")));
                            }
                            out.println(");");

                            out.print("    @Override");
                            out.print(" public " + returnType.toString() + " " + methodName + "(");
                            String args = "";
                            if (parameters != null && !parameters.isEmpty()) {
                                out.print(parameters.stream().map(parameter -> parameter.asType().toString() + " " + parameter.getSimpleName()).collect(Collectors.joining(" ,")));
                                args = parameters.stream().map(VariableElement::getSimpleName).collect(Collectors.joining(" ,"));
                            }
                            out.println(") {");
                            out.print("        return rpcEngine.call(");
                            out.println("new RpcExecutePointImpl(" + rpcMethodRef + ", this, new Object[]{" + args + "}, new RpcExecuteContextImpl()));");
                            out.println("    }");

                            i++;
                        }

                    }
                    out.println("}");
                    out.println();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            entry.setValue(Boolean.TRUE);
        }

    }
}