package org.walkmod.javalang.walkers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.Test;
import org.walkmod.conf.entities.TransformationConfig;
import org.walkmod.conf.entities.impl.ChainConfigImpl;
import org.walkmod.conf.entities.impl.TransformationConfigImpl;
import org.walkmod.conf.entities.impl.WalkerConfigImpl;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.compiler.symbols.RequiresSemanticAnalysis;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;
import org.walkmod.javalang.writers.StringWriter;
import org.walkmod.walkers.VisitorContext;

import static org.junit.Assert.fail;

public class DefaultJavaWalkerTest {

    @Test
    public void when_compilationunit_is_under_a_reader_path_then_subfolder_can_be_resolved() throws Exception {
        DefaultJavaWalker walker = new DefaultJavaWalker(){
            @Override
            protected String getReaderPath() {
                return "src/test/resources/test1";
            }
        };
        File sampleDir = initFolder("src/test/resources/test1/subfolder");
        File fooClass = createJavaFile("Foo", sampleDir);
          
        walker.resolveSourceSubdirs(fooClass, new DefaultJavaParser().parse(fooClass));
        
        Assert.assertEquals("subfolder"+File.separator, walker.getSourceSubdirectories());
    }

    @Test(expected = InvalidSourceDirectoryException.class)
    public void when_compilationunit_contains_invalid_package_then_exception_is_thrown() throws Exception {
        DefaultJavaWalker walker = new DefaultJavaWalker(){
            @Override
            protected String getReaderPath() {
                return ".";
            }
        };
        File sampleDir = new File(".");
        File fooClass = createInvalidJavaFile("Foo", sampleDir);

        walker.resolveSourceSubdirs(fooClass, new DefaultJavaParser().parse(fooClass));
        fooClass.delete();
        walker.getSourceSubdirectories();
    }
    
    @Test
    public void when_compilationunit_is_in_root_dir_then_subfolder_is_empty() throws Exception {
        DefaultJavaWalker walker = new DefaultJavaWalker(){
            @Override
            protected String getReaderPath() {
                return ".";
            }
        };
        File sampleDir = new File(".");
        File fooClass = createJavaFile("Foo", sampleDir);
          
        walker.resolveSourceSubdirs(fooClass, new DefaultJavaParser().parse(fooClass));
        fooClass.delete();
        
        Assert.assertEquals("", walker.getSourceSubdirectories());
       
    }
    
    @Test
    public void when_compilationunit_is_under_a_subfolder_of_root_dir_then_subfolder_is_empty() throws Exception {
        DefaultJavaWalker walker = new DefaultJavaWalker(){
            @Override
            protected String getReaderPath() {
                return ".";
            }
        };
        File sampleDir = initFolder("./src/test/resources/test1/subfolder");
        File fooClass = createJavaFile("Foo", sampleDir);
          
        walker.resolveSourceSubdirs(fooClass, new DefaultJavaParser().parse(fooClass));
        fooClass.delete();
        
        Assert.assertEquals("src/test/resources/test1/subfolder"+File.separator, walker.getSourceSubdirectories());
       
    }
    
    @Test
    public void when_there_are_subfolders_then_outputfile_with_same_reader_path_contains_them() throws Exception{
        DefaultJavaWalker walker = new DefaultJavaWalker(){
            @Override
            protected String getReaderPath() {
                return "src/test/resources/test1";
            }
            @Override
            protected String getWriterPath() {
                return "src/test/resources/test1";
            }
        };
        File sampleDir = initFolder("src/test/resources/test1/subfolder");
        File fooClass = createJavaFile("Foo", sampleDir);
        
        CompilationUnit cu = new DefaultJavaParser().parse(fooClass);
          
        walker.resolveSourceSubdirs(fooClass, cu);
      
        File file = walker.resolveFile(cu);
        
        Assert.assertEquals(fooClass.getCanonicalPath(), file.getPath());
    }
    
    
    @Test
    public void when_there_are_subfolders_then_outputfile_with_different_reader_path_contains_them() throws Exception{
        DefaultJavaWalker walker = new DefaultJavaWalker(){
            @Override
            protected String getReaderPath() {
                return "src/test/resources/test1";
            }
            @Override
            protected String getWriterPath() {
                return "src/test/resources/test2";
            }
        };
        File sampleDir = initFolder("src/test/resources/test1/subfolder");
        File sampleDir2 = initFolder("src/test/resources/test2");
        File fooClass = createJavaFile("Foo", sampleDir);
        
        CompilationUnit cu = new DefaultJavaParser().parse(fooClass);
          
        walker.resolveSourceSubdirs(fooClass, cu);
      
        File file = walker.resolveFile(cu);
        
        Assert.assertEquals(new File(new File(sampleDir2, "subfolder"), "Foo.java").getCanonicalPath(), file.getPath());
    }

    private File initFolder(String folder) throws IOException {
        File sampleDir = new File(folder);
        if (sampleDir.exists()) {
            FileUtils.deleteDirectory(sampleDir);
        }
        sampleDir.mkdirs();
        return sampleDir;
    }

    private File createJavaFile(String name, File sampleDir) throws IOException {

        File fooClass = new File(sampleDir, name + ".java");
        fooClass.createNewFile();
        FileUtils.write(fooClass, "public class " + name + " {}");
        return fooClass;
    }

    private File createInvalidJavaFile(String name, File sampleDir) throws IOException {
        File fooClass = new File(sampleDir, name + ".java");
        fooClass.createNewFile();
        FileUtils.write(fooClass, "package B; public class " + name + " {}");
        return fooClass;
    }

    @Test
    public void testExceptionsMustDefineTheAffectedSourceFile() throws Exception {
        DefaultJavaWalker walker = new DefaultJavaWalker(){
            @Override
            protected String getReaderPath() {
                return "src/test/resources/test1";
            }
            @Override
            protected String getWriterPath() {
                return "src/test/resources/test2";
            }
        };
        File sampleDir = initFolder("src/test/resources/test1");
        File fooClass= createJavaFile("Foo", sampleDir);
        List<Object> visitors = new LinkedList<Object>();
        VisitorWithException instance = new VisitorWithException();
        visitors.add(instance);
        walker.setVisitors(visitors);
        walker.setParser(new DefaultJavaParser());

        ChainConfigImpl cfg = new ChainConfigImpl();
        WalkerConfigImpl walkerCfg = new WalkerConfigImpl();

        List<TransformationConfig> transformations = new LinkedList<TransformationConfig>();
        TransformationConfigImpl tcfg = new TransformationConfigImpl();
        tcfg.setVisitorInstance(instance);
        transformations.add(tcfg);
        walkerCfg.setTransformations(transformations);

        cfg.setWalkerConfig(walkerCfg);
        walker.setChainConfig(cfg);
        try {
            walker.accept(fooClass);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message.contains("Error processing [" + fooClass.getCanonicalPath() + "]"));

        } finally {
            fooClass.delete();
            FileUtils.deleteDirectory(sampleDir);
        }
    }

    @Test
    public void testExceptionsOnAnalysisSemantic() throws Exception {
        
        DefaultJavaWalker walker = new DefaultJavaWalker(){
            @Override
            protected String getReaderPath() {
                return "src/test/resources/test1";
            }
            @Override
            protected String getWriterPath() {
                return "src/test/resources/test2";
            }
        };
        File sampleDir = initFolder("src/test/resources/test1");

        File fooClass = createJavaFile("Foo", sampleDir);
     
        FileUtils.write(fooClass, "import bar.InvalidClass; public class Foo {}");
        List<Object> visitors = new LinkedList<Object>();
        EmptySemanticVisitor instance = new EmptySemanticVisitor();
        visitors.add(instance);
        walker.setVisitors(visitors);
        walker.setParser(new DefaultJavaParser());
        walker.setClassLoader(this.getClass().getClassLoader());

        ChainConfigImpl cfg = new ChainConfigImpl();
        WalkerConfigImpl walkerCfg = new WalkerConfigImpl();

        List<TransformationConfig> transformations = new LinkedList<TransformationConfig>();
        TransformationConfigImpl tcfg = new TransformationConfigImpl();
        tcfg.setVisitorInstance(instance);
        transformations.add(tcfg);
        walkerCfg.setTransformations(transformations);

        cfg.setWalkerConfig(walkerCfg);
        walker.setChainConfig(cfg);
        try {
            walker.accept(fooClass);
        } catch (Exception e) {
            String message = e.getMessage();
            Assert.assertTrue(message.contains("Error processing the analysis of [Foo]"));

        } finally {
            fooClass.delete();
            FileUtils.deleteDirectory(sampleDir);
        }
    }

    @Test
    public void testHaha() throws Exception {
        BasicConfigurator.configure();

        URL resource = this.getClass().getClassLoader().getResource("Hello.java");

        File tempFile = new File(resource.toURI());

        Process exec = Runtime.getRuntime().exec("javac " + tempFile.getAbsolutePath());
        exec.waitFor();
        if (exec.exitValue() == 0) {
            System.out.println("Compiled successfully");
            new File("target/classes/Hello.class").deleteOnExit();
            Class<?> hello = this.getClass().getClassLoader().loadClass("Hello");
            System.out.println(hello);
        }

        DefaultJavaWalker walker = new DefaultJavaWalker(){
            @Override
            protected String getReaderPath() {
                return "/tmp";
            }
            @Override
            protected String getWriterPath() {
                return "/tmp";
            }
        };
        List<Object> visitors = new LinkedList<Object>();
        MyVisitor instance = new MyVisitor();
        visitors.add(instance);
        walker.setVisitors(visitors);
        walker.setParser(new DefaultJavaParser());
        walker.setClassLoader(this.getClass().getClassLoader());

        ChainConfigImpl cfg = new ChainConfigImpl();
        WalkerConfigImpl walkerCfg = new WalkerConfigImpl();

        List<TransformationConfig> transformations = new LinkedList<TransformationConfig>();
        TransformationConfigImpl tcfg = new TransformationConfigImpl();
        tcfg.setVisitorInstance(instance);
        transformations.add(tcfg);
        walkerCfg.setTransformations(transformations);

        cfg.setWalkerConfig(walkerCfg);
        walker.setChainConfig(cfg);
        StringWriter writer = new StringWriter();
        walker.setWriter(writer);

        walker.accept(tempFile);

    }

    public class VisitorWithException extends VoidVisitorAdapter<VisitorContext> {
        @Override
        public void visit(CompilationUnit cu, VisitorContext vc) {
            throw new RuntimeException("Hello");
        }
    }

    @RequiresSemanticAnalysis
    public class EmptySemanticVisitor extends VoidVisitorAdapter<VisitorContext> {

    }

    @RequiresSemanticAnalysis
    public class MyVisitor extends VoidVisitorAdapter<VisitorContext> {
        @Override
        public void visit(final CompilationUnit n, final VisitorContext arg) {
            System.out.println(n.toString());
        }
    }
}
