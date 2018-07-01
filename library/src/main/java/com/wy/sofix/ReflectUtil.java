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
package com.wy.sofix;

import android.util.SparseArray;

import java.lang.reflect.Field;

/**
 *
 * @author cantalou
 * @date 2018-06-30 16:08
 */
public class ReflectUtil {

    private static SparseArray<Field> cache = new SparseArray<>();

    public static void setFieldValue(Object instance, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(instance, fieldName);
        field.set(instance, value);
    }

    public static Object getFieldValue(Object instance, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(instance, fieldName);
        return field.get(instance);
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
            Field field = cache.get(key);
            if (field != null) {
                return field;
            }
            try {
                field = clazz.getDeclaredField(name);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                cache.put(key, field);
                return field;
            } catch (NoSuchFieldException e) {
                // ignore and search next
            }
        }
        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass() + " and super class");
    }
}
