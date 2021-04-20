package com.reactordevelopment.simplex.simplexLang;

import java.util.ArrayList;
import java.util.List;

public class Executor {
    public static TreeNode<Object[]> resolve(TreeNode<Object[]> block) throws SimplexException {
        if(block.getData()[0].equals("op")){
            if(block.getLeft() != null) resolve(block.getLeft());
            if(block.getRight() != null) resolve(block.getRight());

            if(Lexer.mathOperators.contains(block.getData()[1])){
                Object obj1 = 0, obj2 = 0;
                String type1 = "", type2 = "";
                if(block.getLeft().getData()[0].equals("literal")) {
                    obj1 = block.getLeft().getData()[2];
                    type1 = (String) block.getLeft().getData()[1];
                }
                else if(block.getLeft().getData()[0].equals("var")) {
                    obj1 = ((Object[]) Interpreter.vars.get(Interpreter.stackFrame.peek() + "." + block.getLeft().getData()[1]))[2];
                    type1 = (String) block.getLeft().getData()[2];
                }
                if(block.getRight().getData()[0].equals("literal")) {
                    obj2 = block.getRight().getData()[2];
                    //type2 = (String) block.getRight().getData()[1];
                }
                else if(block.getRight().getData()[0].equals("var")) {
                    obj2 = ((Object[]) Interpreter.vars.get(Interpreter.stackFrame.peek() + "." + block.getRight().getData()[1]))[2];
                    //type2 = (String) block.getRight().getData()[2];
                }

                if(block.getData()[1].equals("+")) {
                    switch (type1) {
                        case "int":
                            block.setData(new Object[]{"literal", block.getData()[2], (int) obj1 + (int) obj2});
                            break;
                        case "double":
                            block.setData(new Object[]{"literal", block.getData()[2], (double) obj1 + (double) obj2});
                            break;
                        case "str":
                            block.setData(new Object[]{"literal", block.getData()[2], obj1 + (String) obj2});
                            break;
                    }
                }

                else if(block.getData()[1].equals("-")) {
                    switch (type1) {
                        case "int":
                            block.setData(new Object[]{"literal", block.getData()[2], (int) obj1 - (int) obj2});
                            break;
                        case "double":
                            block.setData(new Object[]{"literal", block.getData()[2], (double) obj1 - (double) obj2});
                            break;
                    }
                }

                else if(block.getData()[1].equals("*")) {
                    switch (type1) {
                        case "int":
                            block.setData(new Object[]{"literal", block.getData()[2], (int) obj1 * (int) obj2});
                            break;
                        case "double":
                            block.setData(new Object[]{"literal", block.getData()[2], (double) obj1 * (double) obj2});
                            break;
                    }
                }

                else if(block.getData()[1].equals("/")) {
                    switch (type1) {
                        case "int":
                            block.setData(new Object[]{"literal", block.getData()[2], (int) obj1 / (int) obj2});
                            break;
                        case "double":
                            block.setData(new Object[]{"literal", block.getData()[2], (double) obj1 / (double) obj2});
                            break;
                    }
                }

                else if(block.getData()[1].equals("&&") && type1.equals("bool"))
                    block.setData(new Object[]{"literal", block.getData()[2], (boolean) obj1 && (boolean) obj2});

                else if(block.getData()[1].equals("||") && type1.equals("bool"))
                    block.setData(new Object[]{"literal", block.getData()[2], (boolean) obj1 || (boolean) obj2});

                else if(block.getData()[1].equals("<")) {
                    switch (type1) {
                        case "int":
                            block.setData(new Object[]{"literal", "bool", (int) obj1 < (int) obj2});
                            break;
                        case "double":
                            block.setData(new Object[]{"literal", "bool", (double) obj1 < (double) obj2});
                            break;
                    }
                }

                else if(block.getData()[1].equals(">")) {
                    switch (type1) {
                        case "int":
                            block.setData(new Object[]{"literal", "bool", (int) obj1 > (int) obj2});
                            break;
                        case "double":
                            block.setData(new Object[]{"literal", "bool", (double) obj1 > (double) obj2});
                            break;
                    }
                }

                else if(block.getData()[1].equals("<=")) {
                    switch (type1) {
                        case "int":
                            block.setData(new Object[]{"literal", "bool", (int) obj1 <= (int) obj2});
                            break;
                        case "double":
                            block.setData(new Object[]{"literal", "bool", (double) obj1 <= (double) obj2});
                            break;
                    }
                }

                else if(block.getData()[1].equals(">=")) {
                    switch (type1) {
                        case "int":
                            block.setData(new Object[]{"literal", "bool", (int) obj1 >= (int) obj2});
                            break;
                        case "double":
                            block.setData(new Object[]{"literal", "bool", (double) obj1 >= (double) obj2});
                            break;
                    }
                }

                else if(block.getData()[1].equals("=="))
                    block.setData(new Object[]{"literal", block.getData()[2], obj1.equals(obj2)});



                block.setLeft(null);
                block.setRight(null);

            }
        }

        if(block.getData()[0].equals("var")){
            String varName = (String) block.getData()[1];
            if(block.getRight() != null){
                if((""+block.getData()[2]).charAt((""+block.getData()[2]).length()-1) == ']'){
                    Integer index = (int) resolve(block.getRight()).getData()[2];
                    TreeNode<Object[]> recur = (TreeNode<Object[]>) ((Object[])Interpreter.vars.get(Interpreter.stackFrame.peek()+"."+varName)[2])[index];
                    TreeNode<Object[]> indexTree = block.getRight().getRight();
                    Object value = null;
                    while (indexTree != null){
                        index = (int) resolve(block.getRight()).getData()[2];
                        recur = resolve((TreeNode<Object[]>) ((Object[])recur.getData()[2])[index]);
                        indexTree = indexTree.getRight();
                    }
                    value = recur.getData()[2];
                    //block.setData(((TreeNode<Object[]>)((Object[])((Object[])Interpreter.vars.get(Interpreter.stackFrame.peek()+"."+varName))[1])).getData());
                    block.setRight(null);
                    String baseType = (""+block.getData()[2]).substring(0, (""+block.getData()[2]).indexOf('['));
                    block.setData(new Object[]{"literal", baseType, value});
                }
            }
            //else block.setData(new Object[]{block.getData()[0], varName, block.getData()[2], Interpreter.vars.get(Interpreter.stackFrame.peek()+"."+varName)[1]});
            else
                block.setData(new Object[]{"literal", Interpreter.vars.get(Interpreter.stackFrame.peek()+"."+varName)[1], Interpreter.vars.get(Interpreter.stackFrame.peek()+"."+varName)[2]});
        }
        if(block.getData()[0].equals("func")){
            String funcName = (String) block.getData()[1];
            Interpreter.stackFrame.push(funcName);
            ArrayList<TreeNode<Object[]>> funcLine = new ArrayList<>();
            funcLine.add(block);
            block.setData(new Object[]{"literal", block.getData()[2], /*execute((List<TreeNode<Object[]>>) funcs.get(funcName)[2])*/ execute(funcLine)});
            Interpreter.stackFrame.pop();
        }


        return block;
    }

    public static void execute(String codeFile){
        try{
            ArrayList<TreeNode<Object[]>> programTree = build(codeFile);
            if(programTree != null)
                Executor.execute(programTree);
        }catch (SimplexException e){
            e.printStackTrace();
            Util.executeUtil("print", new Object[]{e.getError()+"\nError Code: "+e.getCode()});
        }

    }

    public static ArrayList<TreeNode<Object[]>> build(String codeFile) throws SimplexException {
        Util.initUtil();
        Interpreter.stackFrame.push("main");
        ArrayList<Object[]> lexed = Lexer.lexer(codeFile);

        ArrayList<ArrayList<Object[]>> program = new ArrayList<>();

        prepareLines(lexed, program);

        return Interpreter.processBlock(program);
    }

    public static void prepareLines(ArrayList<Object[]> lexed, ArrayList<ArrayList<Object[]>> program){
        ArrayList<Object[]> line = new ArrayList<>();
        for (Object[] objects : lexed) {
            if (objects[0].equals("newline")) {
                if(line.size() > 0)
                    program.add(line);
                line = new ArrayList<>();
            }
            else if (objects[1].equals("{") || objects[1].equals("}")) {
                program.add(line);
                line = new ArrayList<>();
                line.add(objects);
                program.add(line);
                line = new ArrayList<>();
            } else line.add(objects);
        }
    }

    private static Object execute(List<TreeNode<Object[]>> block) throws SimplexException {
        ArrayList<String> scopeVars = new ArrayList<>();
        for(int i=0; i<block.size(); i++) {
            try {
                TreeNode<Object[]> line = block.get(i);

                if (line.getData() == null) continue;
            /*if(line.getData()[0].equals("assignFunc")){
                int blockEnd;
                for(blockEnd= i +2; blockEnd<block.size(); blockEnd++)
                    if(block.get(blockEnd).getData() != null)
                        if(block.get(blockEnd).getData()[1].equals("}")) break;
                i=blockEnd;
                continue;
            }*/
                if (line.getData()[1].equals("if")) {
                    if (line.getRight().getData()[0].equals("args")) {
                        Object[] args = (Object[]) line.getRight().getData()[1];
                        for (Object arg : args)
                            resolve((TreeNode<Object[]>) arg);

                    }

                    int blockEnd;
                    int ifStart = i;
                    for (blockEnd = ifStart + 2; blockEnd < block.size(); blockEnd++)
                        if (block.get(blockEnd).getData() != null)
                            if (block.get(blockEnd).getData()[1].equals("}")) break;

                    int ifEnd = blockEnd;
                    int elseBegin = blockEnd + 3;
                    if (blockEnd < block.size() - 1) {
                        if (block.get(blockEnd + 1).getData()[1].equals("else")) {
                            for (blockEnd = blockEnd + 2; blockEnd < block.size(); blockEnd++)
                                if (block.get(blockEnd).getData() != null)
                                    if (block.get(blockEnd).getData()[1].equals("}")) break;

                        }
                    }
                    i = blockEnd;
                    if ((boolean) ((TreeNode<Object[]>) ((Object[]) line.getRight().getData()[1])[0]).getData()[2])
                        //if block
                        execute(block.subList(ifStart + 2, ifEnd));
                    else if (blockEnd > elseBegin)
                        //else block
                        execute(block.subList(elseBegin, blockEnd));

                } else if (line.getData()[1].equals("=")) {
                    TreeNode<Object[]> leftResolved = resolve(line.getLeft().clone());
                    if ((leftResolved.getData()[1].equals(line.getRight().getData()[1]) && line.getRight().getData()[0].equals("literal")) ||
                            (leftResolved.getData()[1].equals(line.getRight().getData()[2]) && !line.getRight().getData()[0].equals("literal"))) {
                        if (("" + Interpreter.vars.get(Interpreter.stackFrame.peek() + "." + line.getLeft().getData()[1])[1]).contains("["))
                            retrieveArray((Object[]) Interpreter.vars.get(Interpreter.stackFrame.peek() + "." + line.getLeft().getData()[1])[2], line.getLeft()).copy(resolve(line.getRight()));
                        else
                            Interpreter.vars.get(Interpreter.stackFrame.peek() + "." + line.getLeft().getData()[1])[2] = resolve(line.getRight()).getData()[2];
                    }
                } else if (line.getData()[0].equals("func")) {
                    ArrayList<TreeNode<Object[]>> body = (ArrayList<TreeNode<Object[]>>) Interpreter.funcs.get(line.getData()[1])[2];
                    Object[] args = (Object[]) line.getRight().getData()[1];
                    for (Object arg : args) {
                        Interpreter.actionTree((TreeNode<Object[]>) arg);
                        resolve((TreeNode<Object[]>) arg);
                    }
                    if (body.get(0).getData()[0].equals("Java")) {
                        //passes values of Interpreter.vars instead of Interpreter.vars
                        Object[] argCopy = args.clone();
                        for (int j = 0; j < argCopy.length; j++) {
                        /*if(((TreeNode<Object[]>) argCopy[j]).getData()[0].equals("var")) {
                            resolve((TreeNode<Object[]>)argCopy[j]);
                            //args[j] = ((Object[])((TreeNode<Object[]>) args[j]).getData()[3])[2];
                            argCopy[j] = ((TreeNode<Object[]>) argCopy[j]).getData()[3];
                            argCopy[j] = argCopy[j];
                        }

                        else if(((TreeNode<Object[]>) argCopy[j]).getData()[0].equals("literal"))*/
                            argCopy[j] = ((TreeNode<Object[]>) argCopy[j]).getData()[2];
                        }
                        Util.executeUtil((String) body.get(0).getData()[1], argCopy);
                    } else {
                        Object[] savedArgArray = (Object[]) ((Object[]) line.getData()[3])[1];
                        Object[] givenArgArray = (Object[]) line.getRight().getData()[1];
                        Interpreter.stackFrame.push((String) line.getData()[1]);
                        for (int j = 0; j < savedArgArray.length; j++) {
                            TreeNode<Object[]> savedArgTree = (TreeNode<Object[]>) savedArgArray[j];
                            TreeNode<Object[]> givenArgTree = (TreeNode<Object[]>) givenArgArray[j];
                            Interpreter.vars.put(Interpreter.stackFrame.peek() + "." + savedArgTree.getData()[1], givenArgTree.getData());
                        }
                        Object ret = execute(body);
                        Interpreter.stackFrame.pop();
                        return ret;
                    }
                } else if (line.getData()[1].equals(":")) {
                    // Return
                    if (line.getLeft().getData()[2].equals("return")) {
                        resolve(line.getRight());
                        if (line.getRight().getData()[0].equals("var"))
                            return ((Object[]) line.getRight().getData()[3])[2];
                        return line.getRight().getData()[2];
                    }
                    //Assignment
                    if (Lexer.primitives.contains("" + line.getData()[3]) || Lexer.primitives.contains(("" + line.getData()[3]).substring(0, ("" + line.getData()[3]).indexOf("[")))) {
                        String varName = Interpreter.stackFrame.peek() + "." + line.getRight().getLeft().getData()[1];
                        Interpreter.vars.put(varName, new Object[]{"literal", Interpreter.vars.get(varName)[0], resolve((TreeNode<Object[]>) Interpreter.vars.get(varName)[1]).getData()[2]});
                    }
                }
            } catch (SimplexException e) {
                throw new SimplexException(e.getError()+" at line "+(i+1), e.getCode());
            }
        }

        return null;
    }

    public static TreeNode<Object[]> retrieveArray(Object[] array, TreeNode<Object[]> indexes) throws SimplexException {
        TreeNode<Object[]> indexRecur = indexes.getRight();
        TreeNode<Object[]> recur = (TreeNode<Object[]>) array[(int) resolve(indexRecur).getData()[2]];
        while (!indexRecur.isLeaf()){
            indexRecur = indexes.getRight();
            recur = (TreeNode<Object[]>) array[(int) resolve(indexRecur).getData()[2]];
        }
        return recur;
    }
}
