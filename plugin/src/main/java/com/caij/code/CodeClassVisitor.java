package com.caij.code;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * CodeClassVisitor.java
 * <p>
 * Created by lijiankun24 on 2018/7/29.
 */
public class CodeClassVisitor extends ClassVisitor {

    private String mClassName;

    public CodeClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM5, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.mClassName = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        methodVisitor = new CodeMethodVisitor(Opcodes.ASM5, methodVisitor, access, mClassName, name, desc);
        return methodVisitor;
    }
}
