package com.reactordevelopment.simplex.simplexLang;

import java.util.*;

public class Interpreter {
    // key: funcName, value: [returnType, [argTypes], scope: List<TreeNode<>>]
    public static Map<String, Object[]> funcs = new HashMap<>();
            //Arrays.asList(new Object[]{"print", "null", new String[]{"any"}}, new Object[]{"yes", "int", new String[]{"int", "int"}});
    // key: varName, value: [type, value]
    public static Map<String, Object[]> vars = new HashMap<>();
            //Arrays.asList(new Object[]{"one", "double", 7.0}, new Object[]{});

    public static Stack<String> stackFrame = new Stack<>();

    public static void actionTree(TreeNode<Object[]> parsed) throws SimplexException {
        boolean funcAssign = false;
        if(parsed.getData()[1].equals(":") && !parsed.getLeft().getData()[1].equals("return"))
            //Collapses ':' tree
            if(funcs.containsKey(parsed.getRight().getRight().getData()[1])){
                TreeNode<Object[]> funcTree = parsed.getRight().getRight();
                parsed.setData(funcTree.getData());
                parsed.setRight(funcTree.getRight());
                parsed.setLeft(funcTree.getLeft());
                funcAssign = true;
            }
        if (parsed.getLeft() != null)
            actionTree(parsed.getLeft());
        if (parsed.getRight() != null  && !funcAssign)
            actionTree(parsed.getRight());

        if(parsed.getData()[0].equals("args") || parsed.getData()[0].equals("array")) {
            for (Object item : (Object[]) parsed.getData()[1])
                actionTree((TreeNode<Object[]>) item);

            if(parsed.getData()[0].equals("array")){
                boolean typeCheck = true;
                String type = "";
                for(Object item : (Object[]) parsed.getData()[1]){
                    String tmpType;
                    if(((TreeNode<Object[]>)item).getData()[0].equals("literal"))
                        tmpType = (String) ((TreeNode<Object[]>)item).getData()[1];
                    else
                        tmpType = (String) ((TreeNode<Object[]>)item).getData()[2];
                    if(!type.equals("") && !tmpType.equals(type)) {
                        typeCheck = false;
                        break;
                    }
                    type = tmpType;
                }
                if(typeCheck)
                    parsed.setData(new Object[]{"literal", type+"[]", parsed.getData()[1]});
                else throw new ClassCastException("Types not correct");

            }
        }
        else if(parsed.getData()[0].equals("id")) {
            Object[] info;
            if (funcs.get(parsed.getData()[1]) != null) {
                info = funcs.get(parsed.getData()[1]);
                parsed.setData(new Object[]{(funcAssign ? "assignF" : "f")+"unc", parsed.getData()[1], info[0], info[1]});
            }
            else{
                info = vars.get(stackFrame.peek()+"."+parsed.getData()[1]);
                if (info != null)
                    parsed.setData(new Object[]{"var", parsed.getData()[1], info[0], info[1]});
            }
        }
        else if(parsed.getData()[0].equals("op")){
            String leftType;
            String rightType;

            if(parsed.getLeft().getData()[0].equals("literal"))
                leftType = (String) parsed.getLeft().getData()[1];
            else if(Lexer.contextMathOps.containsKey(parsed.getLeft().getData()[1])) {
                leftType = Lexer.contextMathOps.get(parsed.getLeft().getData()[1])[1];
                if (leftType.equals("input"))
                    leftType = (String) parsed.getLeft().getData()[2];
            }
            else{
                if(parsed.getLeft().getData().length > 2)
                    leftType = (String) parsed.getLeft().getData()[2];
                else
                    throw new SimplexException("Unexpected token to the left of '"+parsed.getData()[1]+"'", 1);
            }

            if(parsed.getRight().getData()[0].equals("literal"))
                rightType = (String) parsed.getRight().getData()[1];
            else if(Lexer.contextMathOps.containsKey(parsed.getRight().getData()[1])) {
                rightType = Lexer.contextMathOps.get(parsed.getRight().getData()[1])[1];
                if (rightType.equals("input"))
                    rightType = (String) parsed.getRight().getData()[2];
            }
            else {
                if(parsed.getRight().getData().length > 2)
                    rightType = (String) parsed.getRight().getData()[2];
                else
                    throw new SimplexException("Unexpected token to the right of '"+parsed.getData()[1]+"'", 1);
            }

            if(Lexer.contextMathOps.containsKey(parsed.getData()[1]))
                if(!leftType.equals(rightType) && Lexer.contextMathOps.get(parsed.getData()[1])[0].contains(leftType))
                    throw new SimplexException("Type mismatch, '"+parsed.getData()[1]+"' does not accept type '"+leftType+"' with type '"+rightType+"'", 1);

            parsed.setData(new Object[]{"op", parsed.getData()[1], leftType, rightType});
        }
        else if(parsed.getData()[0].equals("reserved")){
            if(parsed.getData()[1].equals("if") || parsed.getData()[1].equals("else")) parsed.setData(new Object[]{"branch", parsed.getData()[1]});
            else parsed.setData(new Object[]{"literal", parsed.getData()[0], parsed.getData()[1]});
        }
        else if(!parsed.getData()[0].equals("literal") && !parsed.getData()[0].equals("var") && !parsed.getData()[0].equals("func")){
            parsed.setData(new Object[]{"literal", parsed.getData()[0], parsed.getData()[1]});
        }
    }

    /*private static int deepSearch(List<Object[]> list, String id){
        for (int i = 0; i < list.size(); i++) {
            Object[] contents = list.get(i);
            if (contents[0].equals(id))
                return i;
        }
        return -1;
    }*/

    public static ArrayList<TreeNode<Object[]>> processBlock(ArrayList<ArrayList<Object[]>> block, boolean justParse) throws SimplexException {
        ArrayList<TreeNode<Object[]>> tree = new ArrayList<>();
        if(block.size() == 0) return tree;

        for (int i = 0; i < block.size(); i++) {
            try {
                if (i > 0 && tree.get(tree.size() - 1).getData() != null)
                    if (tree.get(tree.size() - 1).getData()[0].equals("assignFunc")) {
                        //int lines = ((ArrayList<TreeNode<Object[]>>)funcs.get(tree.get(i-1).getData()[1])[2]).size();
                        int opens = 1;
                        i++;
                        while (opens > 0) {
                            if (block.get(i).size() == 0) {
                                i++;
                                continue;
                            }
                            if (block.get(i).get(0)[1].equals("{")) opens++;
                            else if (block.get(i).get(0)[1].equals("}")) opens--;
                            i++;
                        }
                    }
                if (i >= block.size()) return tree;

                ArrayList<Object[]> lineArray = block.get(i);
                TreeNode<Object[]> branch = new TreeNode<>(null, null, null);
                if (lineArray.size() == 0) {
                    tree.add(branch);
                    continue;
                }
                Parser.balancedExpression(lineArray, false);

                //Temporary
                Parser.groupLists(lineArray, 0, new String[]{"(", ")"});
                Parser.groupLists(lineArray, 0, new String[]{"[", "]"});


                branch = Parser.parse(lineArray);

                System.out.println("Parsed");
                System.out.println(branch.toString());
                if (lineArray.size() == 1 && (branch.getData()[1].equals("{") || branch.getData()[1].equals("}"))) {
                    tree.add(branch);
                    continue;
                } else if (lineArray.size() > 1)
                    Parser.registerAssignment(branch, block.subList(i + (lineArray.get(1)[0].equals("reserved") ? 1 : 0), block.size()));

                if(!justParse) Interpreter.actionTree(branch);

                System.out.println("Action");
                System.out.println(branch.toString());

                tree.add(branch);
            } catch (SimplexException e) {
                e.printStackTrace();
                throw new SimplexException(e.getError()+" at line "+(i+1), e.getCode());
            }
        }
        return tree;
    }

    public static ArrayList<TreeNode<Object[]>> processBlock(ArrayList<ArrayList<Object[]>> block) throws SimplexException {
        return processBlock(block, false);
    }
}
