package pascal.taie.util;

import pascal.taie.analysis.pta.plugin.spring.MicroserviceHolder;
import pascal.taie.language.classes.JClass;

public class JClassUtils {

    /**
     * cast a jClass name to a lower camel format
     */
    public static String getLowerCamelSimpleName(JClass jClass) {
        return getLowerCamelSimpleName(jClass.getSimpleName());
    }

    public static String getLowerCamelSimpleName(String className) {
        int i = className.lastIndexOf('.');
        if (i != -1) {
            className = className.substring(i + 1);
        }
        if (className.length() == 1) {
            return className.toLowerCase();
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * TODO: isApplication, isLibrary, isJdk in Tai-e but Microservices
     */
    public static boolean isApplicationInMicroservices(JClass jClass) {
        String className = jClass.getName();
        for (MicroserviceHolder ms : MicroserviceHolder.getAllHolders()) {
            if (ms.getAppClasses().contains(className)) {
                return true;
            }
        }
        return false;
    }
}
