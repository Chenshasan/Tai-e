package pascal.taie.analysis.pta.plugin.spring;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface BeanConstructorGetter extends Function<JClass, JMethod> {

    /**
     * if a constructor without arguments is found, it is returned <br>
     * if only one constructor is found, it is returned <br>
     * else throws an {@link AnalysisException}
     */
    static JMethod emptyConstructorFirstGetter(JClass beanClass) throws AnalysisException {
        List<JMethod> constructors = beanClass.getDeclaredMethods()
                                              .stream()
                                              .filter(JMethod::isConstructor)
                                              .toList();
        JMethod emptyParamConstructor = constructors.stream()
                                                    .filter(m -> m.getParamCount() == 0)
                                                    .findFirst()
                                                    .orElse(null);
        if (emptyParamConstructor != null) {
            return emptyParamConstructor;
        }
        if (constructors.size() == 1) {
            return constructors.get(0);
        }
        throw new AnalysisException("No matching bean constructor found in: "
            + beanClass.getName());
    }

    /**
     * return null
     */
    @Nullable
    static JMethod nonConstructorGetter(JClass ignored) {
        return null;
    }
}
