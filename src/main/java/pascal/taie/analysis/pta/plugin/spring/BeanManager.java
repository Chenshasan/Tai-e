package pascal.taie.analysis.pta.plugin.spring;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.VirtualPointer;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.JClassUtils;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * TODO: consider solve the Prototype pattern
 */
public class BeanManager {

    private static final Logger logger = LogManager.getLogger(BeanManager.class);

    /**
     * given a bean name <br>
     * returns the {@link Bean}
     */
    private final Map<String, Bean> name2Bean = Maps.newMap();

    /**
     *  given a bean class name <br>
     *  returns the {@link Bean}
     */
    private final MultiMap<JClass, Bean> class2Beans = Maps.newMultiMap(Sets::newHybridSet);

    /**
     * given a {@link Bean#getPointer()} <br>
     * returns the {@link Bean}
     */
    private final Map<Pointer, Bean> pointer2Bean = Maps.newMap();

    @Nullable
    public Bean getBean(String beanName) {
        return name2Bean.get(beanName);
    }

    @Nullable
    public Bean getBean(JClass beanClass) {
        Set<Bean> beans = class2Beans.get(beanClass);
        if (beans.size() == 1) {
            return beans.iterator().next();
        }
        Bean bean = beans.stream()
                         .filter(b -> beanClass.equals(b.getJClass()))
                         .findFirst()
                         .orElse(null);
        if (bean != null) {
            return bean;
        }
        if (!beans.isEmpty()) {
            logger.warn("Bean of class '{}' is not unique: {}", beanClass.getName(), beans);
        }
        return null;
    }

    @Nullable
    public Bean getBean(VirtualPointer vp) {
        return pointer2Bean.get(vp);
    }

    public Collection<Bean> getAllBeans() {
        return pointer2Bean.values();
    }

    public int size() {
        return pointer2Bean.size();
    }

    public Bean create(JClass beanClass,
                       BeanConstructorGetter constructorGetter,
                       Obj obj,
                       String beanName) throws AnalysisException {
        if (class2Beans.get(beanClass)
                       .stream()
                       .map(Bean::getJClass)
                       .anyMatch(Predicate.isEqual(beanClass))) {
            throw new AnalysisException("Bean already exists: " + beanClass.getName());
        }
        JMethod constructor = constructorGetter.apply(beanClass);
        Bean bean = new Bean(beanClass, constructor, obj);
        name2Bean.put(beanName, bean);
        pointer2Bean.put(bean.getPointer(), bean);
        addClassHierarchyTree(beanClass, bean);
        return bean;
    }

    public boolean createWithBeanNames(JClass beanClass,
                                       Collection<String> beanNames,
                                       Collection<Var> inVars) {
        for (String beanName : beanNames) {
            if (name2Bean.containsKey(beanName)) {
                logger.warn("Bean name '{}' of '{}' already exists", beanName, beanClass);
                return false;
            }
        }
        Bean bean = new Bean(beanClass, inVars);
        pointer2Bean.put(bean.getPointer(), bean);
        addClassHierarchyTree(beanClass, bean);
        for (String beanName : beanNames) {
            name2Bean.put(beanName, bean);
        }
        return true;
    }

    public boolean exist(JClass beanClass) {
        return !class2Beans.get(beanClass).isEmpty();
    }

    /**
     * TODO: more sound
     * traverse its inheritance tree
     */
    private void addClassHierarchyTree(JClass clz, Bean bean) {
        Queue<JClass> queue = new LinkedList<>();
        queue.offer(clz);
        while (!queue.isEmpty()) {
            clz = queue.poll();
            if (clz == null || ClassNames.OBJECT.equals(clz.getName())) {
                continue;
            }
            class2Beans.put(clz, bean);
            queue.offer(clz.getSuperClass());
            queue.addAll(clz.getInterfaces());
        }
    }

    public void injectVar(Var injectedVar,
                          @Nullable String beanName) {
        if (injectedVar.getType() instanceof ClassType classType) {
            JClass jClass = classType.getJClass();
            Bean bean;
            if (beanName != null) {
                bean = getBean(beanName);
            } else {
                String paramName = injectedVar.getName();
                if (paramName.length() >= 2
                    && paramName.charAt(0) == 'r'
                    && Character.isDigit(paramName.charAt(1))) {
                    // parameter name will not be kept without '-parameters' in compiler args
                    // so use the lower camel name of class as the parameter name
                    bean = getBean(JClassUtils.getLowerCamelSimpleName(jClass));
                } else {
                    bean = getBean(paramName);
                }
                if (bean == null) {
                    bean = getBean(jClass);
                }
            }
            if (bean == null) {
                logger.error("No bean of '{}' found in {}",
                    injectedVar.getType(), injectedVar.getMethod());
            } else {
                bean.addOutEdge(injectedVar);
            }
        }
    }

    public void connectPFGEdge(BiConsumer<Pointer, Var> connectConsumer,
                               BiConsumer<Var, Pointer> connectConsumer2) {
        for (Bean bean : pointer2Bean.values()) {
            for (Var in : bean.getIns()) {
                connectConsumer2.accept(in, bean.getPointer());
            }
            for (Var out : bean.getOuts()) {
                connectConsumer.accept(bean.getPointer(), out);
            }
        }
    }
}
