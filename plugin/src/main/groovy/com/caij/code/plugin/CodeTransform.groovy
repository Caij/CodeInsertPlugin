package com.caij.code.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.caij.code.CodeClassVisitor
import org.gradle.api.Project
import org.apache.commons.codec.digest.DigestUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.apache.commons.io.FileUtils
import com.caij.code.Util

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * CodeTransform
 * <p>
 */
class CodeTransform extends Transform {

    Project project

    CodeTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "CodeTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->
                        def name = file.name
                        if (name.endsWith(".class") && !(name == ("R.class"))
                                && !name.startsWith("R\$") && !(name == ("BuildConfig.class"))) {

                            ClassReader reader = new ClassReader(file.bytes)
                            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
                            ClassVisitor visitor = new CodeClassVisitor(writer)
                            reader.accept(visitor, ClassReader.EXPAND_FRAMES)

                            byte[] code = writer.toByteArray()
                            def classPath = file.parentFile.absolutePath + File.separator + name
                            FileOutputStream fos = new FileOutputStream(classPath)
                            fos.write(code)
                            fos.close()
                        }
                    }
                }

                def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)


                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            final File rootOutput = new File(project.buildDir, "classes/${getName()}/")
            input.jarInputs.each { JarInput jarInput ->
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                boolean success = false;
                File output;
                if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                    output = new File(rootOutput, jarName + "_trace.jar");
                if (!output.getParentFile().exists()) {
                    output.getParentFile().mkdirs();
                }
                ZipOutputStream zipOutputStream;
                ZipFile zipFile;
                    try {
                        zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
                        zipFile = new ZipFile(jarInput.file);
                        println(jarInput.file.getAbsolutePath())
                        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                        while (enumeration.hasMoreElements()) {
                            ZipEntry zipEntry = enumeration.nextElement();
                            String zipEntryName = zipEntry.getName();
                            println(zipEntryName)
                            if (zipEntryName.endsWith(".class") && !zipEntryName.contains("R\$") &&
                                    !zipEntryName.contains("R.class") && !zipEntryName.contains("BuildConfig.class")) {
                                InputStream inputStream = zipFile.getInputStream(zipEntry);
                                ClassReader classReader = new ClassReader(inputStream);
                                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                                ClassVisitor classVisitor = new CodeClassVisitor(classWriter);
                                classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                                byte[] data = classWriter.toByteArray();
                                InputStream byteArrayInputStream = new ByteArrayInputStream(data);
                                ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                                Util.addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream);
                            } else {
                                InputStream inputStream = zipFile.getInputStream(zipEntry);
                                ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                                Util.addZipEntry(zipOutputStream, newZipEntry, inputStream);
                            }
                        }
                        success = true;
                    } catch (Exception e) {
                        println("error " + e.toString())
                    } finally {
                        try {
                            if (zipOutputStream != null) {
                                zipOutputStream.finish();
                                zipOutputStream.flush();
                                zipOutputStream.close();
                            }
                            if (zipFile != null) {
                                zipFile.close();
                            }
                        } catch (Exception e) {
                            println(e.toString())
                        }
                    }
                }

                def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                if (success && output != null) {
                    println("success set jar " + output.getAbsolutePath())
                    FileUtils.copyFile(output, dest)
                } else {
                    println("fail set jar " + jarInput.file.getAbsolutePath())
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
        }
    }
}