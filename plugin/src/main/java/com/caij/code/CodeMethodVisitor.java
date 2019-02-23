package com.caij.code;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * CodeMethodVisitor.java
 * <p>
 * Created by lijiankun24 on 2018/7/29.
 */
class CodeMethodVisitor extends AdviceAdapter {

    public final static String MATRIX_TRACE_METHOD_BEAT_CLASS = "com/sample/systrace/TraceTag";

    private static final String COST_ANNOTATION_DESC = "Lcom/lijiankun24/koala/KoalaLog;";

    private boolean isInjected = false;

    private int startTimeId;

    private int methodId;

    private String className;

    private String methodName;

    private String desc;

    private boolean isStaticMethod;

    private Type[] argumentArrays;

    CodeMethodVisitor(int api, MethodVisitor mv, int access, String className, String methodName, String desc) {
        super(api, mv, access, methodName, desc);
        this.className = className;
        this.methodName = methodName;
        this.desc = desc;
        argumentArrays = Type.getArgumentTypes(desc);
        isStaticMethod = ((access & Opcodes.ACC_STATIC) != 0);
        isInjected = true;
    }

//    @Override
//    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
////        if (COST_ANNOTATION_DESC.equals(desc)) {
////            isInjected = true;
////        }
//
////        isInjected = true;
//        return super.visitAnnotation(desc, visible);
//    }

    @Override
    protected void onMethodEnter() {
        if (isInjected) {
            mv.visitMethodInsn(INVOKESTATIC, MATRIX_TRACE_METHOD_BEAT_CLASS, "o", "()V", false);
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (isInjected) {
            mv.visitMethodInsn(INVOKESTATIC, MATRIX_TRACE_METHOD_BEAT_CLASS, "o", "()V", false);
        }
    }
}
