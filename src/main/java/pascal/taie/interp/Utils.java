package pascal.taie.interp;

import pascal.taie.World;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class Utils {

    public static int INT_TRUE = 1;

    public static int INT_FALSE = 0;

    public static String GET_CLASS = "getClass";

    public static String CLONE = "clone";

    public static String EQUALS = "equals";

    public static List<String> PRIMITIVE_TYPES = List.of(
            ClassNames.BOOLEAN, ClassNames.BYTE, ClassNames.CHARACTER,
            ClassNames.SHORT, ClassNames.INTEGER, ClassNames.FLOAT,
            ClassNames.LONG, ClassNames.DOUBLE);

    public static boolean isGetClass(MethodRef ref) {
        return ref.getName().equals(GET_CLASS) && ref.getParameterTypes().isEmpty();
    }

    public static boolean isClone(MethodRef ref) {
        return ref.getName().equals(CLONE) && ref.getParameterTypes().isEmpty();
    }

    public static boolean isEquals(MethodRef ref) {
        return ref.getName().equals(EQUALS);
    }

    public static Class<?> toJVMType(Type t) {
        if (t instanceof PrimitiveType primitiveType) {
            return switch (primitiveType) {
                case BOOLEAN -> boolean.class;
                case BYTE -> byte.class;
                case CHAR -> char.class;
                case SHORT -> short.class;
                case INT -> int.class;
                case LONG -> long.class;
                case FLOAT -> float.class;
                case DOUBLE -> double.class;
            };
        } else if (t instanceof ClassType classType) {
            try {
                return Class.forName(classType.getName());
            } catch (ClassNotFoundException e) {
                throw new InterpreterException(e);
            }
        } else if (t instanceof ArrayType arrayType) {
            Class<?> res = toJVMType(arrayType.baseType());
            for (int i = 0; i < arrayType.dimensions(); ++i) {
                res = res.arrayType();
            }
            return res;
        } else if (t == VoidType.VOID) {
            return void.class;
        } else {
            throw new InterpreterException();
        }
    }

    public static Class<?>[] toJVMTypeList(List<Type> typeList) {
        return typeList.stream().map(Utils::toJVMType)
                .toArray(Class[]::new);
    }

    public static Object[] toJVMObjects(List<JValue> values, List<Type> targetType) {
        Object[] res = new Object[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            res[i] = typedToJVMObj(values.get(i), targetType.get(i));
        }
        return res;
    }

    public static Object typedToJVMObj(JValue value, Type type) {
        assert value != null;
        if (value instanceof JPrimitive primitive) {
            if (primitive.toJVMObj() instanceof Integer i) {
                assert type instanceof PrimitiveType;
                return downCastInt(i, (PrimitiveType) type);
            }
        }
        assert World.get().getTypeSystem().isSubtype(type, value.getType());
        return value.toJVMObj();
    }

    public static Object downCastInt(Integer i, PrimitiveType p) {
        return switch (p) {
            case BOOLEAN -> toBoolean(i.byteValue());
            case BYTE -> i.byteValue();
            case CHAR -> (char) i.intValue();
            case SHORT -> i.shortValue();
            case INT -> i;
            default -> throw new InterpreterException();
        };
    }

    public static int getIntValue(Object o) {
        if (o instanceof Boolean b) {
            return toInt(b);
        } else if (o instanceof Character c) {
            return c;
        } else if (o instanceof Short s) {
            return s;
        } else if (o instanceof Byte b) {
            return b;
        } else if (o instanceof Integer i) {
            return i;
        } else {
            throw new InterpreterException();
        }
    }

    public static Method toJVMMethod(JMethod method) {
        try {
            Method mtd = toJVMType(method.getDeclaringClass().getType())
                    .getDeclaredMethod(method.getName(), Utils.toJVMTypeList(method.getParamTypes()));
            mtd.setAccessible(true);
            return mtd;
        } catch (NoSuchMethodException e) {
            throw new InterpreterException(e);
        }
    }

    public static Field toJVMField(JField field) {
        Class<?> klass = Utils.toJVMType(field.getDeclaringClass().getType());
        try {
            Field f = klass.getDeclaredField(field.getName());
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new InterpreterException(e);
        }
    }

    public static Class<?>[] toJVMTypeListFromValue(List<JValue> values) {
        return values.stream().map(JValue::getType)
                .map(Utils::toJVMType)
                .toArray(Class[]::new);
    }

    public static boolean isJVMClass(Type t) {
        if (t instanceof ClassType ct) {
            JClass klass = ct.getJClass();
            assert klass != null;
            return klass.getName().startsWith("java.") ||
                    klass.getName().startsWith("org.junit.") ||
                    klass.getName().startsWith("org.gradle") ||
                    isBoxedType(ct);
        } else if (t instanceof ArrayType at) {
            return isJVMClass(at.baseType());
        } else {
            return true;
        }
    }

    public static boolean isBoxedType(ClassType t) {
        return PRIMITIVE_TYPES.contains(t.getName());
    }

    public static ClassType fromJVMClass(Class<?> klass) {
        return World.get().getTypeSystem().getClassType(klass.getName());
    }

    public static Type fromJVMType(Class<?> klass) {
        assert klass != null;
        if (klass == boolean.class) {
            return PrimitiveType.BOOLEAN;
        } else if (klass == char.class) {
            return PrimitiveType.CHAR;
        } else if (klass == byte.class) {
            return PrimitiveType.BYTE;
        } else if (klass == short.class) {
            return PrimitiveType.SHORT;
        } else if (klass == int.class) {
            return PrimitiveType.INT;
        } else if (klass == long.class) {
            return PrimitiveType.LONG;
        } else if (klass == float.class) {
            return PrimitiveType.FLOAT;
        } else if (klass == double.class) {
            return PrimitiveType.DOUBLE;
        } else if (klass.isArray()) {
            Class<?> arr = klass;
            int dimensionCount = 0;
            while (arr.isArray()) {
                arr = arr.getComponentType();
                dimensionCount++;
            }
            return World.get().getTypeSystem().getArrayType(fromJVMType(arr), dimensionCount);
        } else {
            return fromJVMClass(klass);
        }
    }

    public static JValue fromPrimitiveJVMObject(VM vm, Object o, Type t) {
        if (t instanceof PrimitiveType) {
            if (o instanceof Boolean b) {
                return JPrimitive.get(toInt(b));
            } else if (o instanceof Character c) {
                return JPrimitive.get(Integer.valueOf(c));
            } else if (o instanceof Byte b) {
                return JPrimitive.get(Integer.valueOf(b));
            } else if (o instanceof Short s) {
                return JPrimitive.get(Integer.valueOf(s));
            } else if (o instanceof Integer i) {
                return JPrimitive.get(i);
            } else if (o instanceof Float f) {
                return JPrimitive.get(f);
            } else if (o instanceof Long l) {
                return JPrimitive.get(l);
            } else if (o instanceof Double d) {
                return JPrimitive.get(d);
            }
        } else if (t instanceof ClassType) {
            return new JVMObject((JVMClassObject)
                    vm.loadClass(fromJVMClass(o.getClass())), o);
        }
        throw new InterpreterException();
    }

    public static JValue fromJVMObject(VM vm, Object o, Type t) {
        if (o == null) {
            return JNull.NULL;
        } else if (PRIMITIVE_TYPES.contains(o.getClass().getName())) {
            return fromPrimitiveJVMObject(vm, o, t);
        } else if (o instanceof JObject jObject) {
            return jObject;
        } else if (o.getClass().isArray()) {
            ArrayType at = (ArrayType) fromJVMType(o.getClass());
            int count = Array.getLength(o);
            JValue[] arr = new JValue[count];
            for (int i = 0; i < count; ++i) {
                Object oi = Array.get(o, i);
                arr[i] = fromJVMObject(vm, oi, at.elementType());
            }
            return new JArray(arr, at.baseType(), at.dimensions());
        } else if (t instanceof ClassType ct) {
            Class<?> klass = o.getClass();
            Class<?> klassDecl = toJVMType(ct);
            assert klassDecl.isAssignableFrom(klass);
            // TODO: fix this, check if jvm class
            // TODO: use correct type
            JClass jClass = World.get().getClassHierarchy().getClass(klass.getName());
            JClassObject klassObj;
            if (jClass != null) {
                klassObj = vm.loadJVMClass(klass);
            } else {
                klassObj = vm.loadClass(ct);
            }
            return new JVMObject((JVMClassObject) klassObj, o);
        }  else {
            throw new InterpreterException();
        }
    }

    public static MethodType toJVMMethodType(pascal.taie.ir.exp.MethodType methodType) {
        return java.lang.invoke.MethodType.methodType(
                Utils.toJVMType(methodType.getReturnType()),
                Utils.toJVMTypeList(methodType.getParamTypes()));
    }

    public static boolean toBoolean(byte b) {
        return (b & INT_TRUE) == INT_TRUE;
    }

    public static int toInt(Boolean b) {
        return b ? INT_TRUE : INT_FALSE;
    }
}
