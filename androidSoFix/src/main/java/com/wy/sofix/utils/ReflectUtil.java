/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.wy.sofix.utils;

import android.util.SparseArray;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author cantalou
 * @date 2018-06-30 16:08
 */
public class ReflectUtil {

    private static SparseArray<Field> fieldCache = new SparseArray<>();

    private static SparseArray<Method> methodCache = new SparseArray<>();

    public static void setFieldValue(Object instance, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(instance, fieldName);
        field.set(instance, value);
    }

    public static Object getFieldValue(Object instance, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(instance, fieldName);
        return field.get(instance);
    }

    public static Object invoke(Object instance, String name, Class<?>[] paramTypes, Object... param) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            int key = clazz.hashCode() ^ name.hashCode();
            for (Class<?> paramType : paramTypes) {
                key ^= paramType.hashCode();
            }
            Method method = methodCache.get(key);
            if (method != null && name.equals(method.getName())) {
                return method.invoke(instance, param);
            }

            try {
                method = clazz.getDeclaredMethod(name, paramTypes);
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                methodCache.put(key, method);
                return method.invoke(instance, param);
            } catch (NoSuchMethodException e) {
                // ignore and search next
            }

            try {
                method = clazz.getMethod(name, paramTypes);
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                methodCache.put(key, method);
                return method.invoke(instance, param);
            } catch (NoSuchMethodException e) {
                // ignore and search next
            }
        }
        throw new NoSuchMethodException("Method " + name + " not found in " + instance.getClass() + " and super class");

    }

    /**
     * Locates a given field anywhere in the class inheritance hierarchy.
     *
     * @param instance an object to search the field into.
     * @param name     field name
     * @return a field object
     * @throws NoSuchFieldException if the field cannot be located
     */
    public static Field findField(Object instance, String name) throws NoSuchFieldException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            int key = clazz.hashCode() ^ name.hashCode();
            Field field = fieldCache.get(key);
            if (field != null && name.equals(field.getName())) {
                return field;
            }

            try {
                field = clazz.getDeclaredField(name);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                fieldCache.put(key, field);
                return field;
            } catch (NoSuchFieldException e) {
                // ignore and search next
            }

            try {
                field = clazz.getField(name);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                fieldCache.put(key, field);
                return field;
            } catch (NoSuchFieldException e) {
                // ignore and search next
            }
        }
        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass() + " and super class");
    }
}
