package com.arts.jvm;


import org.objectweb.asm.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import static jdk.internal.org.objectweb.asm.Opcodes.GETSTATIC;

/**
 * @author yusheng
 */
public class AsmDemo {
    public static void main(String[] args) throws Exception {
        ClassReader cr = new ClassReader("com.arts.jvm.Singleton");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new MyClassVisitor(Opcodes.ASM6, cw);
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        Files.write(Paths.get("D:\\work\\sourcework\\ARTS\\615-No2\\src\\main\\java\\com\\arts\\jvm\\Singleton.class"), cw.toByteArray());
    }

    static class MyMethodVisitor extends MethodVisitor {
        private MethodVisitor mv;
        public MyMethodVisitor(int i, MethodVisitor methodVisitor) {
            super(i, null);
            this.mv = methodVisitor;
        }

        @Override
        public void visitCode() {
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("Hello, World!");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
    }

    static class MyClassVisitor extends ClassVisitor{

        public MyClassVisitor(int i, ClassVisitor classVisitor) {
            super(i, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
            MethodVisitor methodVisitor = super.visitMethod(i, s, s1, s2, strings);
            if("main".equals(s)){
                return new MyMethodVisitor(Opcodes.ASM6, methodVisitor);
            }
            return methodVisitor;
        }
    }
}
