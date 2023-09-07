package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import pascal.taie.frontend.newfrontend.BuildContext;
import pascal.taie.frontend.newfrontend.JavaMethodSource;
import pascal.taie.frontend.newfrontend.JavaSource;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaClassBuilder implements JClassBuilder  {

    private final JClass jClass;

    private final JavaSource sourceFile;

    private Set<Modifier> modifiers;

    private String simpleName;

    private JClass superClass;

    private List<JClass> interfaces;

    private JClass outerClass;

    private final List<JField> fields;

    private final List<JMethod> methods;

    public JavaClassBuilder(JavaSource sourceFile, JClass jClass) {
        this.sourceFile = sourceFile;
        this.jClass = jClass;
        fields = new ArrayList<>();
        methods = new ArrayList<>();
    }


    // TODO: handle annotation
    public void build() {
        String outerClassBinaryName = sourceFile.getOuterClass();
        if (outerClassBinaryName != null) {
            outerClass = BuildContext.get().getClassByName(outerClassBinaryName);
            assert outerClass != null;
        } else {
            outerClass = null;
        }
        ASTNode typeDeclaration = sourceFile.getTypeDeclaration();
        CompilationUnit cu = sourceFile.getUnit();
        AtomicBoolean meetCtor = new AtomicBoolean(false);
        ITypeBinding binding = ClassExtractor.getBinding(typeDeclaration);
        modifiers = TypeUtils.computeModifier(binding);
        simpleName = binding.getName();
        interfaces = Arrays.stream(binding.getInterfaces())
                .map(t -> (ClassType) TypeUtils.JDTTypeToTaieType(t))
                .map(ClassType::getJClass)
                .toList();
        superClass = TypeUtils.getSuperClass(binding);
        @Nullable InnerClassDescriptor descriptor = InnerClassManager.get()
                .getInnerClassDesc(binding);
        List<Type> synParaTypes = descriptor == null ? List.of() :
                TypeUtils.fromJDTTypeList(descriptor.synParaTypes().stream());

        typeDeclaration.accept(new ASTVisitor() {

            @Override
            public boolean visit(Initializer node) {
                int modifiers = node.getModifiers();
                if (! TypeUtils.isStatic(modifiers)) {
                    sourceFile.addNewInit(new BlockInit(node));
                }
                return false;
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                Set<Modifier> current = TypeUtils.fromJDTModifier(node.getModifiers());
                IMethodBinding binding = node.resolveBinding();
                String name;
                if (binding.isConstructor()) {
                    meetCtor.set(true);
                    name = MethodNames.INIT;
                } else {
                    name = binding.getName();
                }
                MethodType methodType = TypeUtils.getMethodType(binding);
                List<SingleVariableDeclaration> svd_s = node.parameters();
                List<String> paraNames = new ArrayList<>();
                for (SingleVariableDeclaration svd : svd_s) {
                    String paraName = svd.getName().getIdentifier();
                    paraNames.add(paraName);
                }
                List<Type> paraTypes = methodType.getParamTypes();
                if (binding.isConstructor() && descriptor != null) {
                    paraTypes = TypeUtils.addList(synParaTypes, paraTypes);
                    paraNames = TypeUtils.addList(descriptor.synParaNames(), paraNames);
                }
                JMethod method = new JMethod(jClass, name, current, paraTypes,
                        methodType.getReturnType(), TypeUtils.toClassTypes(binding.getExceptionTypes()),
                        null, null, paraNames,
                        new JavaMethodSource(cu, node, sourceFile));
                methods.add(method);
                return false;
            }

            @Override
            public boolean visit(TypeDeclaration node) {
                return node.resolveBinding() == binding;
            }

            @Override
            public boolean visit(AnonymousClassDeclaration node) {
                return node.resolveBinding() == binding;
            }

            @Override
            public boolean visit(FieldDeclaration node) {
                Set<Modifier> current = TypeUtils.fromJDTModifier(node.getModifiers());
                List<VariableDeclarationFragment> l = node.fragments();
                for (VariableDeclarationFragment fragment : l) {
                    SimpleName name = fragment.getName();
                    IVariableBinding binding = fragment.resolveBinding();
                    Type type = TypeUtils.JDTTypeToTaieType(binding.getType());
                    JField field = new JField(jClass, name.getIdentifier(),
                            current, type, null);
                    fields.add(field);

                    Expression init = fragment.getInitializer();
                    if (init != null) {
                        sourceFile.addNewInit(new FieldInit(field, init));
                    }
                }
                return false;
            }
        });

        if (! meetCtor.get()) {
            // add a default non-arg ctor
            List<Type> paraTypes = descriptor == null ? List.of() : synParaTypes;
            List<String> paraNames = descriptor == null ? List.of() : descriptor.synParaNames();
            JMethod method = new JMethod(jClass, MethodNames.INIT,
                    TypeUtils.copyVisuality(getModifiers()),
                    paraTypes,
                    VoidType.VOID,
                    List.of(),
                    null,
                    List.of(),
                    paraNames,
                    new JavaMethodSource(cu, null, sourceFile));
            methods.add(method);
        }

        if (descriptor != null) {
            for (int i = 0; i < synParaTypes.size(); ++i) {
                Type type = synParaTypes.get(i);
                String name = descriptor.synParaNames().get(i);
                JField f = new JField(jClass, name,
                            Set.of(Modifier.FINAL, Modifier.SYNTHETIC), type, null);
                if (name.startsWith("this$")) {
                    InnerClassManager.get().noticeOuterClassRef(jClass, f);
                }
                fields.add(f);
            }
        }
    }

    @Override
    public void build(JClass jclass) {
        build();
        assert jclass == this.jClass;
        jClass.build(this);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public ClassType getClassType() {
        return BuildContext.get().getTypeSystem()
                .getClassType(sourceFile.getClassName());
    }

    @Override
    public JClass getSuperClass() {
        return superClass;
    }

    @Override
    public Collection<JClass> getInterfaces() {
        return interfaces;
    }

    @Override
    public JClass getOuterClass() {
        return outerClass;
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        return fields;
    }

    @Override
    public Collection<JMethod> getDeclaredMethods() {
        return methods;
    }

    @Override
    public AnnotationHolder getAnnotationHolder() {
        return null;
    }

    @Override
    public boolean isApplication() {
        return sourceFile.isApplication();
    }

    @Override
    public boolean isPhantom() {
        return false;
    }
}
