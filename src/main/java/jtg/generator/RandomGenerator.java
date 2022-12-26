package jtg.generator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.internal.parser.JSONParser;
import jtg.generator.util.RandomUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RandomGenerator {
    private String clsPath;
    private String clsName;
    private String mtdName;
    public RandomGenerator(String classPath, String className, String methodName) {
        clsPath = classPath;
        clsName = className;
        mtdName = methodName;
    }

    public void generate() throws Exception {
        Class<?> aClass = Class.forName(clsName);
        Method method = null;
        for (Method m : aClass.getMethods()) { //不知道方法的参数，只知道方法名，就暂时这样处理
            if (m.getName().equals(mtdName)) {
                method = m;
            }
        }
        assert method != null;
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            arguments[i] = genOneParam(parameterType);
        }
        System.out.println(JSON.toJSONString(arguments));
    }

    public Object genOneParam(Class<?> parameterType) throws Exception {
        Object result = handleBasicType(parameterType);
        if (result != null) {
            return result;
        } else if (parameterType.isArray()) {
            return handleArrType(parameterType);
        }else{
            return handleComplexType(parameterType);
        }
    }

    private Object handleBasicType(Class<?> parameterType) {
        Object argument = null;
        if (parameterType == int.class) {
            argument = RandomUtil.randInt();
        } else if (parameterType == double.class) {
            argument = RandomUtil.randDouble();
        } else if (parameterType == char.class) {
            argument = RandomUtil.randChar();
        } else if (parameterType == boolean.class) {
            argument = RandomUtil.randBool();
        } else if (parameterType == String.class) {
            argument = RandomUtil.randStr();
        } else if (parameterType.isEnum()) {
            Object[] enumConstants = parameterType.getEnumConstants();
            argument = enumConstants[RandomUtil.randInt(enumConstants.length)];
        }
        return argument;
    }

    private Object handleArrType(Class<?> parameterType) throws Exception {
        Class<?> componentType = parameterType.getComponentType();
        Object[] objects = new Object[RandomUtil.randInt(10)];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = handleBasicType(componentType);
            if (objects[i] == null) {
                objects[i] = handleComplexType(componentType);
            }
        }
        return objects;
    }

    private Object handleComplexType(Class<?> parameterType) throws Exception {
        Object obj = parameterType.newInstance();
        Field[] fields = parameterType.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            Class<?> type = field.getType();
            field.set(obj, handleBasicType(type));
        }
        return obj;
    }

}
