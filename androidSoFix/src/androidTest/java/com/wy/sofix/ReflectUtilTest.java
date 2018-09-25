package com.wy.sofix;

import android.support.test.runner.AndroidJUnit4;

import com.wy.sofix.utils.ReflectUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author cantalou
 * @date 2018-07-08 11:31
 */
@RunWith(AndroidJUnit4.class)
public class ReflectUtilTest {

    SuperBean bean;

    static class SuperBean {
        private static int sf1 = 1;
        private static String sf2 = "2";

        private int sf3 = 3;
        private String sf4 = "4";

        private static int sf1() {
            return 1;
        }

        private static String sf2(String value) {
            return "2";
        }

        private int sf3() {
            return 3;
        }

        private String sf4(String value) {
            return "4";
        }

        private String fuzzyMethod(List<String> list) {
            return "fuzzyMethod" + (list != null && !list.isEmpty() ? list.get(0) : "");
        }

    }

    static class Bean extends SuperBean {
        private static int f1 = 11;
        private static String f2 = "22";

        private int f3 = 33;
        private String f4 = "44";

        private static int f1() {
            return 11;
        }

        private static String f2(String value) {
            return "22";
        }

        private int f3() {
            return 33;
        }

        private String f4(String value) {
            return "44";
        }
    }

    @Before
    public void setUp() {
        bean = new Bean();
    }

    @Test
    public void setFieldValue() throws Exception {

        //super
        ReflectUtil.setFieldValue(bean, "sf1", 11);
        assertEquals(11, ReflectUtil.getFieldValue(bean, "f1"));

        ReflectUtil.setFieldValue(bean, "sf2", "22");
        assertEquals("22", ReflectUtil.getFieldValue(bean, "f2"));

        ReflectUtil.setFieldValue(bean, "sf3", 33);
        assertEquals(33, ReflectUtil.getFieldValue(bean, "f3"));

        ReflectUtil.setFieldValue(bean, "sf4", "44");
        assertEquals("44", ReflectUtil.getFieldValue(bean, "f4"));

        //subclass
        ReflectUtil.setFieldValue(bean, "f1", 111);
        assertEquals(111, ReflectUtil.getFieldValue(bean, "f1"));

        ReflectUtil.setFieldValue(bean, "f2", "222");
        assertEquals("222", ReflectUtil.getFieldValue(bean, "f2"));

        ReflectUtil.setFieldValue(bean, "f3", 333);
        assertEquals(333, ReflectUtil.getFieldValue(bean, "f3"));

        ReflectUtil.setFieldValue(bean, "f4", "444");
        assertEquals("444", ReflectUtil.getFieldValue(bean, "f4"));
    }

    @Test
    public void getFieldValue() throws Exception {

        //super
        assertEquals(1, ReflectUtil.getFieldValue(bean, "sf1"));
        assertEquals("2", ReflectUtil.getFieldValue(bean, "sf2"));
        assertEquals(3, ReflectUtil.getFieldValue(bean, "sf3"));
        assertEquals("4", ReflectUtil.getFieldValue(bean, "sf4"));

        //subclass
        assertEquals(11, ReflectUtil.getFieldValue(bean, "f1"));
        assertEquals("22", ReflectUtil.getFieldValue(bean, "f2"));
        assertEquals(33, ReflectUtil.getFieldValue(bean, "f3"));
        assertEquals("44", ReflectUtil.getFieldValue(bean, "f4"));
    }

    @Test
    public void invoke() throws Exception {
        //super
        assertEquals(1, ReflectUtil.invoke(bean, "sf1", new Class[]{}, new Object[]{}));
        assertEquals("2", ReflectUtil.invoke(bean, "sf2", new Class[]{String.class}, new Object[]{""}));
        assertEquals(3, ReflectUtil.invoke(bean, "sf3", new Class[]{}, new Object[]{}));
        assertEquals("4", ReflectUtil.invoke(bean, "sf4", new Class[]{String.class}, new Object[]{""}));

        ArrayList arrayList = new ArrayList();
        arrayList.add("ArrayList");
        assertEquals("fuzzyMethodArrayList", ReflectUtil.invoke(bean, "fuzzyMethod", null, arrayList));

        LinkedList linkedList = new LinkedList();
        linkedList.add("LinkedList");
        assertEquals("fuzzyMethodLinkedList", ReflectUtil.invoke(bean, "fuzzyMethod", null, linkedList));


        //subclass
        assertEquals(11, ReflectUtil.invoke(bean, "f1", new Class[]{}, new Object[]{}));
        assertEquals("22", ReflectUtil.invoke(bean, "f2", new Class[]{String.class}, new Object[]{""}));
        assertEquals(33, ReflectUtil.invoke(bean, "f3", new Class[]{}, new Object[]{}));
        assertEquals("44", ReflectUtil.invoke(bean, "f4", new Class[]{String.class}, new Object[]{""}));
    }

    @Test
    public void findField() throws Exception {

        //super
        assertNotNull(ReflectUtil.findField(bean, "sf1"));
        assertNotNull(ReflectUtil.findField(bean, "sf2"));
        assertNotNull(ReflectUtil.findField(bean, "sf3"));
        assertNotNull(ReflectUtil.findField(bean, "sf4"));

        //subclass
        assertNotNull(ReflectUtil.findField(bean, "f1"));
        assertNotNull(ReflectUtil.findField(bean, "f2"));
        assertNotNull(ReflectUtil.findField(bean, "f3"));
        assertNotNull(ReflectUtil.findField(bean, "f4"));
    }
}