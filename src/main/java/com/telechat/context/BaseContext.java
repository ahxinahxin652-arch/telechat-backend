package com.telechat.context;

/**
 * 基于ThreadLocal（用户的一个请求到响应过程中为tomcat服务器的一个线程，在这个线程内可以定义线程内共享变量）封装工具类，用户保存和获取当前登录用户id
 */
public class BaseContext {

    public static ThreadLocal<Integer> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Integer id) {
        threadLocal.set(id);
    }

    public static Integer getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
