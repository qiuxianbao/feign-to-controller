package cn.thinkinjava.plugin.feigntocontroller;


import cn.thinkinjava.plugin.feigntocontroller.kit.Resources;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.lang.jvm.annotation.JvmAnnotationArrayValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.file.PsiDirectoryImpl;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FeignJavaLineMarkerProvider extends RelatedItemLineMarkerProvider {
//    private static final String REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";
//    private static final String GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping";
//    private static final String POST_MAPPING = "org.springframework.web.bind.annotation.PostMapping";
//    private static final String PUT_MAPPING = "org.springframework.web.bind.annotation.PutMapping";
//    private static final String DELETE_MAPPING = "org.springframework.web.bind.annotation.DeleteMapping";
//    private static final List<String> NO_CONTEXT_PATH_APP = Arrays.asList("micro-record");

    public FeignJavaLineMarkerProvider() {
    }

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {

        if (element instanceof PsiIdentifier) {
            PsiElement parent = element.getParent();
            if (null != parent) {
                PsiClass pisClazz = this.getInterface(parent);
                if (null != pisClazz) {
                    // feignClient注解
                    PsiAnnotation annotation = pisClazz.getAnnotation("org.springframework.cloud.netflix.feign.FeignClient");
                    if (null != annotation) {
                        String microServiceName = this.getAnnotationVal(annotation, "name");
                        String pathPrefix = this.getAnnotationVal(annotation, "path");
                        if (null != microServiceName) {
                            PsiMethod psiMethod = (PsiMethod)parent;
                            String methodPath = this.tryGetMappingPath(psiMethod);
                            if (methodPath != null) {
                                String effectPath = this.deduceControllerPath(pathPrefix, methodPath, microServiceName);
                                effectPath = effectPath.replaceAll("//", "/");
                                Project project = element.getProject();
                                Module[] modules = ModuleManager.getInstance(project).getModules();
                                if (null != modules) {
                                    int modulesLength = modules.length;

                                    for(int i = 0; i < modulesLength; ++i) {
                                        Module module = modules[i];
                                        if (module.getName().equalsIgnoreCase(microServiceName)) {
                                            GlobalSearchScope moduleScope = module.getModuleScope();
                                            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
                                            List<PsiFile> files = this.getModulePsiFileList(module, project);
                                            Iterator var20 = files.iterator();

                                            while(var20.hasNext()) {
                                                PsiFile psiFile = (PsiFile)var20.next();
                                                PsiElement[] children = new PsiElement[0];

                                                try {
                                                    children = psiFile.getChildren();
                                                } catch (Exception var35) {
                                                    var35.printStackTrace();
                                                }

                                                int childrenSize = children.length;

                                                for(int j = 0; j < childrenSize; ++j) {
                                                    PsiElement psiElement = children[j];
                                                    if (psiElement instanceof PsiClass) {
                                                        PsiClass psiClass = (PsiClass)psiElement;
                                                        String servicePath = null;
                                                        String medhodComparingPath = effectPath;
                                                        if (psiClass.getAnnotation("org.springframework.web.bind.annotation.RestController") != null || psiClass.getAnnotation("org.springframework.web.bind.annotation.Controller") != null) {
                                                            PsiAnnotation annotation1 = psiClass.getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
                                                            if (annotation1 != null) {
                                                                servicePath = this.getRequestMapingAnnoPath(annotation1);
                                                                if (servicePath != null) {
                                                                    if (!this.isServicePathMatchUri(effectPath, servicePath)) {
                                                                        continue;
                                                                    }

                                                                    medhodComparingPath = this.truncateServicePathFromUri(effectPath, servicePath);
                                                                }
                                                            }

                                                            PsiMethod[] serviceClassMethods = psiClass.getMethods();
                                                            int serviceSize = serviceClassMethods.length;

                                                            for(int var34 = 0; var34 < serviceSize; ++var34) {
                                                                PsiMethod serviceMethod = serviceClassMethods[var34];
                                                                String serviceMethodPath = this.tryGetMappingPath(serviceMethod);
                                                                if (serviceMethodPath != null) {
                                                                    serviceMethodPath = serviceMethodPath.replaceAll("//", "/");
                                                                    if (this.isMethodPathMatchUri(medhodComparingPath, serviceMethodPath)) {
                                                                        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Resources.JUMP_ARROW).setTarget(serviceMethod).setTooltipText("Navigate to controller");
                                                                        result.add(builder.createLineMarkerInfo(element));
                                                                        return;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private @Nullable PsiClass getInterface(@NotNull PsiElement parent) {

        if (parent instanceof PsiMethod) {
            PsiClass psiClass = ((PsiMethod)parent).getContainingClass();
            if (null != psiClass && psiClass.isInterface()) {
                return psiClass;
            }
        }

        return null;
    }

    private String getAnnotationVal(PsiAnnotation annotation, String attrName) {
        List<JvmAnnotationAttribute> attributes = annotation.getAttributes();
        if (null != attributes) {
            Iterator var4 = attributes.iterator();

            while(var4.hasNext()) {
                JvmAnnotationAttribute attribute = (JvmAnnotationAttribute)var4.next();
                if (attribute.getAttributeName().equalsIgnoreCase(attrName)) {
                    if (attribute.getAttributeValue() instanceof JvmAnnotationArrayValue) {
                        JvmAnnotationArrayValue jvmAnnotationArrayValue = (JvmAnnotationArrayValue)attribute.getAttributeValue();
                        List<JvmAnnotationAttributeValue> values = jvmAnnotationArrayValue.getValues();
                        String retStr = "";
                        Iterator var9 = values.iterator();

                        while(var9.hasNext()) {
                            JvmAnnotationAttributeValue val = (JvmAnnotationAttributeValue)var9.next();
                            if (retStr.length() > 0) {
                                retStr = retStr + ";";
                                retStr = retStr + ((JvmAnnotationConstantValue)val).getConstantValue().toString();
                            } else {
                                retStr = retStr + ((JvmAnnotationConstantValue)val).getConstantValue().toString();
                            }
                        }

                        return retStr;
                    }

                    return ((PsiNameValuePairImpl)attribute).getLiteralValue();
                }
            }
        }

        return null;
    }

    private String getRequestMapingAnnoPath(PsiAnnotation requestMapping) {
        String value = this.getAnnotationVal(requestMapping, "value");
        if (StringUtils.isNotBlank(value)) {
            return value;
        } else if (this.getAnnotationVal(requestMapping, "path") != null) {
            return this.getAnnotationVal(requestMapping, "path");
        } else {
            String name = this.getAnnotationVal(requestMapping, "name");
            return StringUtils.isNotBlank(name) ? name : null;
        }
    }

    private String deduceControllerPath(String prefix, String path, String serviceName) {
        if (prefix != null) {
            if (prefix.startsWith("$")) {
                prefix = prefix.replaceAll("\\$\\{.*\\.(.+)\\}", "$1");
            }

            if (!prefix.endsWith("/") && !path.startsWith("/")) {
                path = prefix + "/" + path;
            } else {
                path = prefix + path;
            }
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

//        if (!NO_CONTEXT_PATH_APP.contains(serviceName)) {
            int splitPos = serviceName.indexOf('-');
            serviceName = serviceName.substring(splitPos + 1);
//            if (serviceName.equalsIgnoreCase("upay")) {
//                serviceName = "pay";
//            }

//            if (path.startsWith("/" + serviceName)) {
                path = path.substring(serviceName.length() + 1);
//            } else if (path.startsWith(serviceName)) {
//                path = path.substring(serviceName.length());
//            }
//        }

        return path;
    }

    private String tryGetMappingPath(PsiMethod psiMethod) {
        String methodPath = null;
        PsiAnnotation requestMapping = null;

        try {
            requestMapping = psiMethod.getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
        } catch (Exception var5) {
            var5.printStackTrace();
        }

        if (requestMapping != null) {
            methodPath = this.getRequestMapingAnnoPath(requestMapping);
        } else {
            requestMapping = psiMethod.getAnnotation("org.springframework.web.bind.annotation.GetMapping");
            if (requestMapping != null) {
                methodPath = this.getRequestMapingAnnoPath(requestMapping);
            } else {
                requestMapping = psiMethod.getAnnotation("org.springframework.web.bind.annotation.PostMapping");
                if (requestMapping != null) {
                    methodPath = this.getRequestMapingAnnoPath(requestMapping);
                }
            }
        }

        if (methodPath == null || methodPath.isEmpty()) {
            requestMapping = psiMethod.getAnnotation("org.springframework.web.bind.annotation.PutMapping");
            if (requestMapping != null) {
                methodPath = this.getRequestMapingAnnoPath(requestMapping);
            }
        }

        if (methodPath == null || methodPath.isEmpty()) {
            requestMapping = psiMethod.getAnnotation("org.springframework.web.bind.annotation.DeleteMapping");
            if (requestMapping != null) {
                methodPath = this.getRequestMapingAnnoPath(requestMapping);
            }
        }

        return methodPath;
    }

    private boolean isServicePathMatchUri(String effectPath, String servicePath) {
        try {
            servicePath = servicePath.replaceAll("//", "/");
            if (effectPath.length() < servicePath.length() + 1) {
                return false;
            }

            if (servicePath.startsWith("/")) {
                if (servicePath.equalsIgnoreCase(effectPath.substring(0, servicePath.length()))) {
                    return true;
                }
            } else if (servicePath.equalsIgnoreCase(effectPath.substring(1, servicePath.length() + 1))) {
                return true;
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        return false;
    }

    private String truncateServicePathFromUri(String effectPath, String servicePath) {
        return servicePath.startsWith("/") ? effectPath.substring(servicePath.length()) : effectPath.substring(servicePath.length() + 1);
    }

    private boolean isMethodPathMatchUri(String effectPath, String methodPath) {
        if (methodPath.contains(";")) {
            String[] splits = methodPath.split(";");
            String[] var4 = splits;
            int var5 = splits.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String memberPath = var4[var6];
                if (this.isMethodPathMatchUri(effectPath, memberPath)) {
                    return true;
                }
            }

            return false;
        } else {
            return methodPath.startsWith("/") ? effectPath.equals(methodPath) : effectPath.substring(1).equalsIgnoreCase(methodPath);
        }
    }

    private List<PsiFile> getModulePsiFileList(Module module, Project project) {
        VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(false);
        List<PsiFile> fileList = new LinkedList();
        int size = sourceRoots.length;

        try {
            for(int i = 0; i < size; ++i) {
                VirtualFile sourceRoot = sourceRoots[i];
                if (sourceRoot.getName().contains("java")) {
                    VirtualFile canonicalFile = sourceRoot.getCanonicalFile();
                    PsiManagerImpl psiManager = new PsiManagerImpl(project);
                    PsiDirectory psiDirectory = new PsiDirectoryImpl(psiManager, canonicalFile);
                    fileList.addAll(this.getPsiFileListFromPsiDirectory(psiDirectory));
                }
            }

            return fileList;
        } catch (Exception var11) {
            var11.printStackTrace();
            throw var11;
        }
    }

    private List<PsiFile> getPsiFileListFromPsiDirectory(PsiDirectory psiDirectory) {
        List<PsiFile> fileList = new LinkedList();
        PsiFile[] files = psiDirectory.getFiles();
        int var6;
        if (files != null && files.length > 0) {
            PsiFile[] var4 = files;
            int var5 = files.length;

            for(var6 = 0; var6 < var5; ++var6) {
                PsiFile psiFile = var4[var6];
                fileList.add(psiFile);
            }
        }

        PsiDirectory[] subdirectories = psiDirectory.getSubdirectories();
        if (subdirectories != null && subdirectories.length > 0) {
            PsiDirectory[] var10 = subdirectories;
            var6 = subdirectories.length;

            for(int var11 = 0; var11 < var6; ++var11) {
                PsiDirectory psiDirectory1 = var10[var11];
                fileList.addAll(this.getPsiFileListFromPsiDirectory(psiDirectory1));
            }
        }

        return fileList;
    }
}
