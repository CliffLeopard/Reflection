package com.cliff.reflection.example;

import com.orhanobut.logger.Logger;

import java.util.List;
import java.util.Map;

/**
 * @Author CliffLeopard
 * @Email precipiceleopard@gmail.com
 * @Time 2024/10/6 03:43
 */
public class TargetClass {

    private static String TAG = "TargetClass";
    private static HiddenClass hiddenField = new HiddenClass("HiddenClass",123);
    private static HiddenClass hiddenField2 = new HiddenClass("HiddenClass",123);
    private static TargetInsideClass hiddenInside = new TargetInsideClass();

    private static String sMethod1() {
        return "这里是sMethod1";
    }

    private String strField = "CliffLeopard";
    private int intField = 35;
    private long longField = 200000;

    private void print() {
        Logger.i("name=$name,age=$age,hair=$hair");
    }

    // 测试普通对象方法
    private int method1() {
        return intField * 2;
    }

    // 测试带餐对象方法
    private String method2(String name, int age) {
        String result = "method2:" + " name:" + name + " age:" + age;
        Logger.i(result);
        return result;
    }

    // 测试类型擦除的泛型
    private String method3(String name, int age, List<Map<String, String>> list) {
        StringBuilder result = new StringBuilder("method2:" + " name:" + name + " age:" + age);
        for (Map<String, String> item : list) {
            result.append("item:{");
            for (String key : item.keySet()) {
                result.append("key=").append(key).append("; value=").append(item.get(key));
            }
            result.append("};");
        }
        Logger.i(result.toString());
        return result.toString();
    }

    // 测试隐藏类，作为参数
    private String method4(String name, int age, HiddenClass hiddenClass) {
        String result = "method2:" + " name:" + name + " age:" + age;
        result += "hiddenClass:{" + hiddenClass.toStr() + "}";
        return result;
    }

    // 测试隐藏类，作为参数
    private String method42(String name, int age, HiddenClass hiddenClass) {
        String result = "method2:" + " name:" + name + " age:" + age;
        result += "hiddenClass:{" + hiddenClass.toStr() + "}";
        return result;
    }

    // 测试隐藏类，作为返回值
    private HiddenClass method5(String name, int age) {
        Logger.i("method4:{name:" + name + " age:" + age + "}");
        return new HiddenClass(name, age);
    }

    // 测试隐藏类，作为返回值
    private HiddenClass method6(String name, int age) {
        Logger.i("method6:{name:" + name + " age:" + age + "}");
        return new HiddenClass(name, age);
    }

    // 测试内部类作为隐藏类，作为返回值
    private TargetInsideClass method7() {
        TargetInsideClass insideClass = new TargetInsideClass();
        Logger.i(insideClass.toString());
        return insideClass;
    }

    public static class TargetInsideClass {
        private static String TAG = "TargetInsideClass";

        private static String sMethod1() {
            return "这里是sMethod1";
        }

        private String strField = "CliffLeopard";
        private int intField = 35;
        private long longField = 200000;

        private void print() {
            Logger.i("name=$name,age=$age,hair=$hair");
        }

        // 测试普通对象方法
        private int method1() {
            return intField * 2;
        }

        public void setStr(String str) {
            this.strField = str;
        }

        // 测试带餐对象方法
        private String method2(String name, int age) {
            String result = "method2:" + " name:" + name + " age:" + age;
            Logger.i(result);
            return result;
        }

        // 测试类型擦除的泛型
        private String method3(String name, int age, List<Map<String, String>> list) {
            StringBuilder result = new StringBuilder("method3:" + " name:" + name + " age:" + age);
            for (Map<String, String> item : list) {
                result.append("item:{");
                for (String key : item.keySet()) {
                    result.append("key=").append(key).append("; value=").append(item.get(key));
                }
                result.append("};");
            }
            Logger.i(result.toString());
            return result.toString();
        }

        // 测试隐藏类，作为参数
        private String method4(String name, int age, HiddenClass hiddenClass) {
            String result = "method4:" + " name:" + name + " age:" + age;
            result += "hiddenClass:{" + hiddenClass.toStr() + "}";
            return result;
        }

        // 测试隐藏类，作为参数
        private String method42(String name, int age, HiddenClass hiddenClass) {
            String result = "method42:" + " name:" + name + " age:" + age;
            result += "hiddenClass:{" + hiddenClass.toStr() + "}";
            return result;
        }

        // 测试隐藏类，作为返回值
        private HiddenClass method5(String name, int age) {
            Logger.i("method5:{name:" + name + " age:" + age + "}");
            return new HiddenClass(name, age);
        }

        // 测试隐藏类，作为返回值
        private HiddenClass method6(String name, int age) {
            Logger.i("method6:{name:" + name + " age:" + age + "}");
            return new HiddenClass(name, age);
        }
    }
}
