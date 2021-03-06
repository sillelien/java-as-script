/*
 *    Copyright (c) 2014-2017 Neil Ellis
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sillelien.jas.impl.jproxy.core.clsmgr.comp;

import com.sillelien.jas.RelProxyException;
import com.sillelien.jas.impl.FileExt;
import com.sillelien.jas.impl.jproxy.core.clsmgr.FolderSourceList;
import com.sillelien.jas.impl.jproxy.core.clsmgr.JProxyEngineChangeDetectorAndCompiler;
import com.sillelien.jas.impl.jproxy.core.clsmgr.cldesc.ClassDescriptor;
import com.sillelien.jas.impl.jproxy.core.clsmgr.cldesc.ClassDescriptorInner;
import com.sillelien.jas.impl.jproxy.core.clsmgr.cldesc.ClassDescriptorSourceFileJava;
import com.sillelien.jas.impl.jproxy.core.clsmgr.cldesc.ClassDescriptorSourceFileRegistry;
import com.sillelien.jas.impl.jproxy.core.clsmgr.cldesc.ClassDescriptorSourceScript;
import com.sillelien.jas.impl.jproxy.core.clsmgr.cldesc.ClassDescriptorSourceUnit;
import com.sillelien.jas.impl.jproxy.core.clsmgr.comp.jfo.JavaFileObjectInputSourceInMemory;
import com.sillelien.jas.impl.jproxy.core.clsmgr.comp.jfo.JavaFileObjectOutputClass;
import com.sillelien.jas.jproxy.JProxyDiagnosticsListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author jmarranz
 */
public class JProxyCompilerInMemory {
    @NotNull
    private static final Logger log = LoggerFactory.getLogger(JProxyCompilerInMemory.class);
    @NotNull
    protected JProxyEngineChangeDetectorAndCompiler parent;
    @NotNull
    protected JavaCompiler compiler;
    @Nullable
    protected Iterable<String> compilationOptions;
    @Nullable
    protected JProxyDiagnosticsListener diagnosticsListener;

    public JProxyCompilerInMemory(@NotNull JProxyEngineChangeDetectorAndCompiler engine,
                                  @Nullable Iterable<String> compilationOptions,
                                  @Nullable JProxyDiagnosticsListener diagnosticsListener) {
        parent = engine;
        this.compilationOptions = compilationOptions;
        this.diagnosticsListener = diagnosticsListener;
        compiler = ToolProvider.getSystemJavaCompiler();
    }

    public void compileSourceFile(@NotNull ClassDescriptorSourceUnit sourceFileDesc,
                                  @NotNull JProxyCompilerContext context,
                                  @NotNull ClassLoader currentClassLoader,
                                  @Nullable ClassDescriptorSourceFileRegistry sourceRegistry) {
        //File sourceFile = sourceFileDesc.getSourceFile();
        LinkedList<JavaFileObjectOutputClass> outClassList = compile(sourceFileDesc, context, currentClassLoader, sourceRegistry);

        if (outClassList == null) {
            throw new JProxyCompilationException(sourceFileDesc);
        }

        String className = sourceFileDesc.getClassName();

        // Puede haber más de un resultado cuando hay inner classes y/o clase privada en el mismo archivo o bien simplemente clases dependientes
        for (JavaFileObjectOutputClass outClass : outClassList) {
            String currClassName = outClass.binaryName();
            byte[] classBytes = outClass.getBytes();
            assert classBytes != null;

            if (className.equals(currClassName)) {
                sourceFileDesc.setClassBytes(classBytes);
            } else {
                ClassDescriptorInner innerClass = sourceFileDesc.getInnerClassDescriptor(currClassName, true);
                if (innerClass != null) {
                    innerClass.setClassBytes(classBytes);
                } else {
                    // Lo mismo es un archivo dependiente e incluso una inner class pero de otra clase que está siendo usada en el archivo compilado
                    assert sourceRegistry != null;
                    ClassDescriptor dependentClass = sourceRegistry.getClassDescriptor(currClassName);
                    if (dependentClass != null) {
                        dependentClass.setClassBytes(classBytes);
                    } else {
                        // Seguramente es debido a que el archivo java tiene una clase privada autónoma declarada en el mismo archivo .java (las que se ponen después de la clase principal pública normal), no permitimos estas clases porque sólo podemos
                        // detectarlas cuando cambiamos el código fuente, pero no si el código fuente no se ha tocado, por ejemplo no tenemos
                        // forma de conseguir que se recarguen de forma determinista y si posteriormente se cargara via ClassLoader al usarse no podemos reconocer que es una clase
                        // "hot reloadable" (quizás a través del package respecto a las demás clases hot pero no es muy determinista pues nada impide la mezcla de hot y no hot en el mismo package)
                        // Es una limitación mínima.

                        // También puede ser un caso de clase excluida por el listener de exclusión, no debería ocurrir, tengo un caso de test en donde ocurre a posta
                        // (caso de JProxyExampleAuxIgnored cuando se cambia la JProxyExampleDocument que la usa) pero en programación normal no.

                        if (parent.getJProxyInputSourceFileExcludedListener() == null) {
                            throw new RelProxyException("Unexpected class when compiling: " + currClassName + " maybe it is an autonomous private class declared in the same java file of the principal class, this kind of classes are not supported in hot reload");
                        } else {
                            log.error(
                                    "Unexpected class when compiling: " + currClassName + " maybe it is an excluded class or is an autonomous private class declared in the same java file of the principal class, this kind of classes are not supported in hot reload");
                        }
                    }
                }
            }
        }
    }

    @NotNull
    public JProxyCompilerContext createJProxyCompilerContext() {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
        assert standardFileManager != null;
        return new JProxyCompilerContext(standardFileManager, diagnostics, diagnosticsListener);
    }

    @Nullable
    private LinkedList<JavaFileObjectOutputClass> compile(@NotNull ClassDescriptorSourceUnit sourceFileDesc,
                                                          @NotNull JProxyCompilerContext context,
                                                          @NotNull ClassLoader currentClassLoader,
                                                          @Nullable ClassDescriptorSourceFileRegistry sourceRegistry) {
        // http://stackoverflow.com/questions/12173294/compiling-fully-in-memory-with-javax-tools-javacompiler
        // http://www.accordess.com/wpblog/an-overview-of-java-compilation-api-jsr-199/
        // http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/com/sun/tools/javac/util/JavacFileManager.java?av=h#JavacFileManager
        // http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7-b147/javax/tools/StandardLocation.java
        // http://books.brainysoftware.com/java6_sample/chapter2.pdf
        // http://atamur.blogspot.com.es/2009/10/using-built-in-javacompiler-with-custom.html
        // http://www.javablogging.com/dynamic-in-memory-compilation/ Si no queremos generar archivos
        // http://atamur.blogspot.com.es/2009/10/using-built-in-javacompiler-with-custom.html
        // http://stackoverflow.com/questions/264828/controlling-the-classpath-in-a-servlet?rq=1
        // http://stackoverflow.com/questions/1563909/how-to-set-classpath-when-i-use-javax-tools-javacompiler-compile-the-source
        // http://stackoverflow.com/questions/10767048/javacompiler-with-custom-classloader-and-filemanager


        StandardJavaFileManager standardFileManager = context.getStandardFileManager(); // recuerda que el StandardJavaFileManager puede reutilizarse entre varias compilaciones consecutivas mientras se cierre al final

        Iterable<? extends JavaFileObject> compilationUnits;

        if (sourceFileDesc instanceof ClassDescriptorSourceFileJava) {
            List<File> sourceFileList = new ArrayList<>();
            sourceFileList.add(((ClassDescriptorSourceFileJava) sourceFileDesc).getSourceFile().getFile());
            compilationUnits = standardFileManager.getJavaFileObjectsFromFiles(sourceFileList);
        } else if (sourceFileDesc instanceof ClassDescriptorSourceScript) {
            ClassDescriptorSourceScript sourceFileDescScript = (ClassDescriptorSourceScript) sourceFileDesc;
            LinkedList<JavaFileObject> compilationUnitsList = new LinkedList<>();
            String code = sourceFileDescScript.getSourceCode();
            compilationUnitsList.add(new JavaFileObjectInputSourceInMemory(sourceFileDescScript.getClassName(), code,
                                                                           sourceFileDescScript.getEncoding(),
                                                                           sourceFileDescScript.getTimestamp()));
            compilationUnits = compilationUnitsList;
        } else {
            throw new RelProxyException("Internal error");
        }

        assert sourceRegistry != null;
        JavaFileManagerInMemory fileManagerInMemory = new JavaFileManagerInMemory(standardFileManager, currentClassLoader,
                                                                                  sourceRegistry,
                                                                                  parent.getRequiredExtraJarPaths());

        assert compilationUnits != null;
        boolean success = compile(compilationUnits, fileManagerInMemory, context);
        if (!success) {
            return null;
        }

        LinkedList<JavaFileObjectOutputClass> classObj = fileManagerInMemory.getJavaFileObjectOutputClassList();
        return classObj;
    }

    private boolean compile(@NotNull Iterable<? extends JavaFileObject> compilationUnits,
                            @NotNull JavaFileManager fileManager,
                            @NotNull JProxyCompilerContext context) {
        /*
        String systemClassPath = System.getProperty("java.class.path");
        */

        LinkedList<String> finalCompilationOptions = new LinkedList<>();
        if (compilationOptions != null) {
            for (String option : compilationOptions) {
                finalCompilationOptions.add(option);
            }
        }

        FolderSourceList folderSourceList1 = parent.getFolderSourceList();
        if (folderSourceList1 != null) {
            FileExt[] folderSourceList = folderSourceList1.getArray();
            if (folderSourceList != null) {
                finalCompilationOptions.add("-classpath");
                StringBuilder classPath = new StringBuilder();
                for (int i = 0; i < folderSourceList.length; i++) {
                    FileExt folderSources = folderSourceList[i];
                    classPath.append(folderSources.getCanonicalPath());
                    if (i < (folderSourceList.length - 1)) {
                        classPath.append(File.pathSeparatorChar);
                    }
                }
                finalCompilationOptions.add(classPath.toString());
            }
        }
        DiagnosticCollector<JavaFileObject> diagnostics = context.getDiagnosticCollector();
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, finalCompilationOptions, null,
                                                             compilationUnits);
        boolean success = task.call();

        return success;
    }


}
