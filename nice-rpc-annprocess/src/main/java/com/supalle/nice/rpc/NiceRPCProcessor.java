package com.supalle.nice.rpc;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            if (Boolean.TRUE.equals(entry.setValue(Boolean.TRUE))) {
                continue;
            }
            Element key = entry.getKey();
            if (!(key instanceof TypeElement)) {
                continue;
            }
            TypeElement typeElement = (TypeElement) key;
            String classFullName = typeElement.getQualifiedName().toString();
            String classSimpleName = typeElement.getSimpleName().toString();
            String packageName = typeElement.getEnclosingElement().getSimpleName().contentEquals("") ? null : typeElement.getEnclosingElement().toString();
            List<? extends TypeParameterElement> classTypeParameters = typeElement.getTypeParameters();
            List<? extends Element> enclosedElements = typeElement.getEnclosedElements();

            String rpcImplClassSimpleName = classSimpleName + "__NiceRpcConsumerImpl";
            String rpcImplClassFullName = classFullName + "__NiceRpcConsumerImpl";

            try {
                JavaFileObject rpcImplFile = processingEnv.getFiler().createSourceFile(rpcImplClassFullName);
                StringBuilder code = new StringBuilder(4096);
                if (packageName != null) {
                    code.append("package ").append(packageName).append(';').append(System.lineSeparator());
                }
                code.append(System.lineSeparator());

                code.append("import com.supalle.nice.rpc.*;").append(System.lineSeparator()).append(System.lineSeparator());

                code.append("public class ").append(rpcImplClassSimpleName).append(" implements ").append(classFullName).append(" {").append(System.lineSeparator());

                code.append(System.lineSeparator());
                code.append("    private final RpcEngine rpcEngine;").append(System.lineSeparator());
                code.append("    public ").append(rpcImplClassSimpleName).append("(RpcEngine rpcEngine) {").append(System.lineSeparator());
                code.append("        this.rpcEngine = rpcEngine;").append(System.lineSeparator());
                code.append("    }").append(System.lineSeparator());
                code.append(System.lineSeparator());

                int methodIndex = 0;
                for (Element enclosedElement : enclosedElements) {
                    if (enclosedElement instanceof ExecutableElement) {
                        ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                        Name methodName = enclosedElement.getSimpleName();
                        List<? extends TypeParameterElement> typeParameters = executableElement.getTypeParameters();
                        List<? extends VariableElement> parameters = executableElement.getParameters();
                        TypeMirror returnType = executableElement.getReturnType();

                        String rpcMethodRef = "__rpcMethod__" + methodIndex;
                        code.append("    private static final RpcMethod ").append("__rpcMethod__").append(methodIndex)
                                .append(" = RpcMethodImpl.of(").append(rpcImplClassSimpleName).append(".class, \"").append(methodName).append('\"');
                        if (parameters != null && !parameters.isEmpty()) {
                            code.append(", ").append(parameters.stream().map(parameter -> {
                                String typeString = parameter.asType().toString();
                                int index = typeString.indexOf('<');
                                if (index != -1) {
                                    typeString = typeString.substring(0, index);
                                }
                                return typeString + ".class";
                            }).collect(Collectors.joining(" ,")));
                        }
                        code.append(");").append(System.lineSeparator());

                        code.append("    @Override public ").append(returnType.toString()).append(" ").append(methodName).append("(");
                        String args = "";
                        if (parameters != null && !parameters.isEmpty()) {
                            code.append(parameters.stream().map(parameter -> parameter.asType().toString() + " " + parameter.getSimpleName()).collect(Collectors.joining(" ,")));
                            args = parameters.stream().map(VariableElement::getSimpleName).collect(Collectors.joining(", "));
                        }
                        code.append(") {").append(System.lineSeparator());
                        code.append("        return rpcEngine.call(");
                        code.append("new RpcExecutePointImpl(").append(rpcMethodRef).append(", this, new Object[]{").append(args).append("}, new RpcExecuteContextImpl()));").append(System.lineSeparator());
                        code.append("    }").append(System.lineSeparator());

                        code.append(System.lineSeparator());
                        methodIndex++;
                    }

                }

                code.append('}').append(System.lineSeparator());

                try (Writer writer = rpcImplFile.openWriter()) {
                    writer.write(code.toString());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}