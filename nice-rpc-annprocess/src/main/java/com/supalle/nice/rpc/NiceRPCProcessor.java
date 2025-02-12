package com.supalle.nice.rpc;

import com.google.auto.service.AutoService;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AutoService(Processor.class)
public class NiceRPCProcessor extends AbstractProcessor {
    private ProcessingEnvironment processingEnv;
    private JavacElements elementUtils;
    private TreeMaker treeMaker;

    // 候选集合 ElementsAnnotated、JCTree
    private final ConcurrentMap<Element, Pair<JCTree, JCTree.JCCompilationUnit>> candidates = new ConcurrentHashMap<>();

    // 已处理集合
    private final ConcurrentMap<String, Boolean> processedSymbols = new ConcurrentHashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
        this.elementUtils = (JavacElements) processingEnv.getElementUtils();
        this.treeMaker = TreeMaker.instance(Objects.requireNonNull(getContext(processingEnv), "不支持的javac编译环境，无法获取编译上下文"));
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
        Set<? extends Element> rootElements = roundEnv.getRootElements();
        for (Element rootElement : rootElements) {
            candidates.put(rootElement, elementUtils.getTreeAndTopLevel(rootElement, null, null));
        }
        if (roundEnv.processingOver() || roundEnv.errorRaised() || LombokState.isLombokInvoked()) {
            finishRemaining();
            return true;
        }
        return false;
    }


    private void finishRemaining() {

    }

    private String getPackageName(JCTree.JCCompilationUnit jcCompilationUnit) {
        try {
            ExpressionTree packageName = jcCompilationUnit.getPackageName();
            if (packageName != null) {
                return packageName.toString();
            }
        } catch (Exception e) {
            if (e instanceof NoSuchMethodException) {
                try {
                    Method method = JCTree.JCCompilationUnit.class.getMethod("getPackage");
                    method.setAccessible(true);
                    return ((JCTree.JCPackageDecl) method.invoke(jcCompilationUnit)).getPackageName().toString();
                } catch (Exception ignored) {
                }
            }
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

}