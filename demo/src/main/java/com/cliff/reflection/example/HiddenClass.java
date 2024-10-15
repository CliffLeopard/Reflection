package com.cliff.reflection.example;

import androidx.annotation.NonNull;

import com.orhanobut.logger.Logger;

/**
 * @Author CliffLeopard
 * @Email precipiceleopard@gmail.com
 * @Time 2024/10/6 18:50
 */
public class HiddenClass {
    public String name;
    public int age;

    public HiddenClass(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public void print() {
        Logger.i("print:name=" + name + " ;age=" + age);
    }

    @NonNull
    public String toStr() {
        return "HiddenClass:name=" + name + " ;age=" + age;
    }
}
