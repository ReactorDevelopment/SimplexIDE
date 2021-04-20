package com.reactordevelopment.simplex.simplexLang;

import java.util.*;

public class Util {

    //public static Map<String, Object[]> funcs = new HashMap<>();
    private static String console;

    public static void initUtil(){
        TreeNode<Object[]> tree = new TreeNode<>(new Object[]{"Java", "print"}, null, null);
        ArrayList<TreeNode<Object[]>> list = new ArrayList<>();
        list.add(tree);
        Interpreter.funcs.put("print", new Object[]{"null", new String[]{"any"}, list});
        console = "";
    }


    public static void executeUtil(String func, Object[] args){
        if(func.equals("print"))
               console += args[0].toString()+"\n";
    }

    public static String console(){
        return console;
    }

    public static int stringOccurrences(String str, String search){
        int count = 0, fromIndex = 0;

        while ((fromIndex = str.indexOf(search, fromIndex)) != -1 ){
            count++;
            fromIndex++;
        }
        return count;
    }
}
