package gov.cms.ab2d.fhir;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This class provides a lot of underlying methods for the other Util classes including:
 *   1. Defining supported versions
 *   2. Defining valid classes
 *   3. Creating enumerations of versions and mapping between FHIR context objects and those versions
 *   4. Package private methods to use reflection to instantiate classes, enums and to invoke set/get for those classes
 *   5. The ability to determine which version is being used based on the URL passed from the HttpRequest
 */
@Slf4j
@UtilityClass
public class Versions {
    /**
     * So we don't have to instantiate all Method objects, hold on to them so we can just invoke them when necessary
     * to save time
     */
    private static final Map<String, Method> NEEDED_METHODS = new HashMap<>();

    /**
     * So we don't have to use reflection to instantiate objects, keep a list of blank objects so that we can copy them
     * when needed
     */
    private static final Map<String, Object> NEEDED_OBJECTS = new HashMap<>();

    /**
     * Currently, the classes in the model directories that we can instantiate
     */
    private static final Set<String> SUPPORTED_CLASSES = Set.of(
            "ExplanationOfBenefit",
            "Patient",
            "Identifier",
            "Bundle",
            "Extension",
            "Coding",
            "Type",
            "OperationOutcome",
            "CodeableConcept",
            "OperationOutcome.OperationOutcomeIssueComponent",
            "OperationOutcome.IssueSeverity",
            "OperationOutcome.IssueType",
            "Enumerations.PublicationStatus",
            "DateTimeType",
            "ResourceType",
            "Period"
    );

    /**
     * Get an object of a type className with an argument if desired. If the object already has been instantiated,
     * get a copy of that object, otherwise use reflection to instantiate it and save the copy for the next caller who
     * needs it
     *
     * @param version - the FHIR version
     * @param className - the class name to instantiate without the package (i.e, ExplanationOfBenefit, Extension)
     * @param arg - any argument to pass the constructor
     * @param argClass - the class of the argument
     * @return the object
     */
    static Object getObject(FhirVersion version, String className, Object arg, Class argClass) {
        String fullClassName = version.getClassName(className);
        return getObject(fullClassName, argClass, arg);
    }

    /**
     * Return an empty object for the correct FHIR version given the class name
     *
     * @param version - the FHIR version
     * @param className - the class name to instantiate or retrieve from cache
     * @return the object
     */
    static Object getObject(FhirVersion version, String className) {
        String fullName = version.getClassName(className);
        Object object = NEEDED_OBJECTS.get(fullName);
            if (object == null) {
                Class clazz;
                try {
                    clazz = Class.forName(fullName);
                    object = clazz.getDeclaredConstructor(null).newInstance();
                } catch (Exception e) {
                    log.error("Unable to create class " + fullName);
                    return null;
                }
            }
        if (object == null) {
            return null;
        }
        NEEDED_OBJECTS.put(fullName, object);
        Method method = getMethod(object.getClass(), "copy");
        try {
            assert method != null;
            return method.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException | AssertionError e) {
            return null;
        }
    }

    /**
     * Instantiates an object of a class name with an object as a parameter. This cannot
     * store and save empty object instances since the parameter means it's not empty.
     *
     * @param className - the class name to instantiate without the package
     * @param classArg - the Class value of the parameter
     * @param arg - the value of the parameter
     * @return the object
     */
    static Object getObject(String className, Class classArg, Object arg) {
        try {
            Class clazz = Class.forName(className);
            return clazz.getDeclaredConstructor(classArg).newInstance(arg);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Given a resource and the method name, return the result of calling the method
     *
     * @param resource - the resource object
     * @param methodName - the method name
     * @return the result of calling the method
     */
    static Object invokeGetMethod(Object resource, String methodName) {
        try {
            Method method = resource.getClass().getMethod(methodName);
            return method.invoke(resource);
        } catch (Exception ex) {
            log.error("Unable to invoke get method " + methodName + " on " + resource.getClass().getName());
            return null;
        }
    }

    /**
     * Given a resource, the method name and an argument, return the result of calling the method with a parameter
     *
     * @param resource - the resource
     * @param methodName - the method name
     * @param arg - the argument
     * @param clazz - the argument class
     * @return the result of calling the method
     */
    static Object invokeGetMethodWithArg(Object resource, String methodName, Object arg, Class clazz) {
        try {
            Method method = resource.getClass().getMethod(methodName, clazz);
            return method.invoke(resource, arg);
        } catch (Exception ex) {
            log.error("Unable to invoke get method " + methodName + " on " + resource.getClass().getName() + " with " + clazz.getName() + " argument", ex);
            return null;
        }
    }

    /**
     * Given a resource, method name and an argument, set a value (or call a method with a void return type)
     *
     * @param resource - the resource
     * @param methodName - the method name
     * @param val - the value of the parameter
     * @param paramType - the class of the parameter
     */
    static void invokeSetMethod(Object resource, String methodName, Object val, Class paramType) {
        try {
            Method method = getMethod(resource.getClass(), methodName, paramType);
            assert method != null;
            method.invoke(resource, val);
        } catch (Exception ex) {
            log.error("Unable to invoke set method " + methodName + " on " + resource.getClass().getName() + " with " + val + " argument", ex);
        }
    }

    private static Method getMethod(Class clazz, String methodName) {
        return getMethod(clazz, methodName, null);
    }

    private static Method getMethod(Class clazz, String methodName, Class paramType) {
        String fullMethodName = null;
        if (paramType != null) {
            fullMethodName = getMethodName(clazz.getName(), methodName, paramType.getName());
        } else {
            fullMethodName = getMethodName(clazz.getName(), methodName);
        }
        Method method = NEEDED_METHODS.get(fullMethodName);
        if (method != null) {
            return method;
        }
        Method methodObj;
        try {
            if (paramType != null) {
                methodObj = clazz.getMethod(methodName, paramType);
            } else {
                methodObj = clazz.getMethod(methodName);
            }
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
        NEEDED_METHODS.put(fullMethodName, methodObj);
        return methodObj;
    }

    private static String getMethodName(String className, String methodName) {
        return getMethodName(className, methodName, null);
    }

    private static String getMethodName(String className, String methodName, String argClass) {
        String value = className + "#" + methodName;
        if (argClass != null && !argClass.isEmpty()) {
            value += "$" + argClass;
        }
        return value;
    }
    /**
     * Instantiate enum from the version, enum name and value of the enum
     *
     * @param version - the FHIR version
     * @param cName - the enum name
     * @param value - the enum value
     * @return the enum
     */
    static Object instantiateEnum(FhirVersion version, String cName, String value) {
        try {
            String topClassName = version.getClassName(cName);
            Class clazz = Class.forName(topClassName);
            Method valueOf = getMethod(clazz, "valueOf", String.class);
            assert valueOf != null;
            return  valueOf.invoke(null, value);
        } catch (Exception ex) {
            log.error("Unable to instantiate enum " + cName + " with value " + value, ex);
            return null;
        }
    }

    /**
     * Instantiate enum  the version, enum name (class and internal enum name) and value of the enum
     *
     * @param version - the FHIR version
     * @param topLevel - the top level class
     * @param lowerLevel - the internal enum
     * @param value - the enum value
     * @return the enum
     */
    static Object instantiateEnum(FhirVersion version, String topLevel, String lowerLevel, String value) {
        try {
            String topClassName = version.getClassName(topLevel);
            Class top = Class.forName(topClassName);
            Class[] classes = top.getClasses();
            for (Class c : classes) {
                if (c.getName().endsWith("$" + lowerLevel)) {
                    Method valueOf = getMethod(c, "valueOf", String.class);
                    assert valueOf != null;
                    return valueOf.invoke(null, value);
                }
            }
            return null;
        } catch (Exception ex) {
            log.error("Unable to instantiate enum " + topLevel + "." + lowerLevel + " with value " + value, ex);
            return null;
        }
    }

    /**
     * Instantiate an internal class from a version and class names
     *
     * @param version - the FHIR version
     * @param topLevel - the class name
     * @param lowerLevel - the internal class name
     * @return the object of that type
     */
    static Object instantiateClass(FhirVersion version, String topLevel, String lowerLevel) {
        try {
            String name = version.getClassName(topLevel);
            Class<?> clazz = Class.forName(name);
            String className = topLevel + "." + lowerLevel;
            if (!SUPPORTED_CLASSES.contains(className)) {
                throw new RuntimeException("Class " + className + " is not supported");
            }
            Class<?>[] classes = clazz.getClasses();
            for (Class<?> c : classes) {
                if (c.getName().endsWith("$" + lowerLevel)) {
                    return c.getDeclaredConstructor(null).newInstance();
                }
            }
            return null;
        } catch (Exception ex) {
            log.error("Unable to instantiate " + topLevel + "." + lowerLevel, ex);
            return null;
        }
    }
}
