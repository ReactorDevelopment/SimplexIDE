package com.reactordevelopment.simplex.simplexLang;

import java.util.*;

public class Parser {

    /**Taking in a string expression, the number and location of '(' and ')' characters are recorded
     * this is usedto determine if the expression if balanced
     * @param lexed the lexed expression
     * @return if the expression is balanced*/
    public static boolean balancedExpression(ArrayList<Object[]> lexed, boolean doBrackets) throws SimplexException {
        if(lexed.size() == 0) return false;

        Stack<String> brackets = new Stack<>();
        boolean balanced = true;

        //counts open and closed parentheses
        for (Object[] objects : lexed) {
            Object data = objects[1];
            if (data.equals("(") || (data.equals("{") && doBrackets) || data.equals("["))
                brackets.push((String) data);
            String open = " ";

            if (!data.equals(")") && !(data.equals("}") && doBrackets) && !data.equals("]")) continue;
            switch ((String) data) {
                case ")":
                    try {
                        open = brackets.pop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!open.equals("(")) balanced = false;
                    break;
                case "}":
                    if(doBrackets) {
                        try {
                            open = brackets.pop();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (!open.equals("{")) balanced = false;
                    }
                    break;
                case "]":
                    try {
                        open = brackets.pop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!open.equals("[")) balanced = false;
                    break;
            }
            //System.out.println(brackets);
        }

        if(balanced && brackets.isEmpty())
            return true;
        else
            throw new SimplexException("Unbalanced operations", 1);
    }

    /**Calculate the result of the mathematical expression.
     * Add each number in the expression to a numbers stack, and every operation
     * to an operation stack.  When a closed parentheses is hit by then cycling through the character,
     * the contents of each stack is evaluated and collapsed, multiplication first, then addition.
     * This turns each instance of '(x*y+z...) into a single term.  This continues until the entire expression
     * is collapsed into a single term.  The program then returns this term.
     * @param lexed the inputted expression
     * @return the evaluated expression*/
    public static TreeNode<Object[]> parse(List<Object[]> lexed) throws SimplexException {
        try {
            if (lexed.size() == 1)
                return new TreeNode<>(lexed.get(0), null, null);

            lexed.add(0, new Object[]{"op", "("});
            lexed.add(lexed.size(), new Object[]{"op", ")"});

            Stack<TreeNode<Object[]>> nodeStack = new Stack<>();
            Stack<String> operations = new Stack<>();
            ArrayList<Object[]> controllers = new ArrayList<>();
            for (int i = 0, lexedSize = lexed.size(); i < lexedSize; i++) {
                Object[] element = lexed.get(i);
                if (i < lexedSize - 1)
                    if ((element[0].equals("id") || element[0].equals("reserved")) && (lexed.get(i + 1)[1].equals("(") || lexed.get(i + 1)[1].equals("["))) {
                        controllers.add(new Object[]{element, operations.size()});
                        continue;
                    }
                if (!element[0].equals("op"))
                    nodeStack.add(new TreeNode<>(element, null, null));
                if (element[1].equals(")") /*|| element[1].equals("}")*/ || element[1].equals("]")) {
                    String beginBracket = " ";
                    if (element[1].equals(")")) beginBracket = "(";
                    /*if (element[1].equals("}")) beginBracket = "{";*/
                    if (element[1].equals("]")) beginBracket = "[";

                    //two loops to properly calculate order of operations
                    Stack<TreeNode<Object[]>> tmpTrees = new Stack<>();
                    Stack<String> tmpOps = new Stack<>();
                    //calculate multiplication operations
                    while (!operations.peek().equals(beginBracket)) {
                        TreeNode<Object[]> tree2 = nodeStack.pop();
                        TreeNode<Object[]> tree1 = nodeStack.pop();
                        String operation = operations.pop();

                        if (operation.equals("*") || operation.equals("/")) {
                            nodeStack.push(new TreeNode<>(new Object[]{"op", operation}, tree1, tree2));
                        } else {
                            tmpOps.push(operation);
                            tmpTrees.push(tree2);
                            nodeStack.push(tree1);
                        }
                    }
                    //Empty storage of addition/subtraction
                    while (tmpOps.size() > 0)
                        operations.push(tmpOps.pop());
                    while (tmpTrees.size() > 0)
                        nodeStack.push(tmpTrees.pop());

                    //calculate addition operations
                    while (!operations.peek().equals(beginBracket)) {
                        TreeNode<Object[]> tree2 = nodeStack.pop();
                        TreeNode<Object[]> tree1 = nodeStack.pop();
                        String operation = operations.pop();

                        nodeStack.push(new TreeNode<>(new Object[]{"op", operation}, tree1, tree2));
                    }
                    //remove initial '(', '{', '[' from operation stack
                    operations.pop();

                } else if (element[0].equals("op") && !element[1].equals("{"))
                    operations.add((String) element[1]);

                // Gives children of functions/arrays
                for (int j = 0; j < controllers.size(); j++) {
                    Object[] control = controllers.get(j);
                    if ((int) control[1] == operations.size()) {
                        //keeps adding for multiple [0][0]...[0]
                        boolean chained = false;
                        if (nodeStack.size() > 1) {
                            TreeNode<Object[]> tmp = nodeStack.pop();
                            if (nodeStack.peek().getData().equals(control[0])) {
                                nodeStack.peek().getRightmost().setRight(tmp);
                                chained = true;
                            } else nodeStack.push(tmp);
                        }
                        if (!chained)
                            nodeStack.push(new TreeNode<>((Object[]) control[0], null, nodeStack.pop()));
                        if (!lexed.get(i + 1)[1].equals("[")) {
                            controllers.remove(control);
                            j--;
                        }
                    }
                }
                //Useful to evaluate what is happening inside
                //System.out.println("NumStack: "+nodeStack.toString()+", OpStack: "+operations.toString());
            }

            return nodeStack.pop();
        }catch (Exception e){
            throw new SimplexException("Syntax Error", 1);
        }
    }

    public static int groupLists(ArrayList<Object[]> lexed, int start, String[] brackets) throws SimplexException{
        for(int i=start; i<lexed.size(); i++){
            if(lexed.get(i)[1].equals(brackets[0]) && i > 0){
                ArrayList<TreeNode<Object[]>> contents = new ArrayList<>();
                //condense declarations (int: num)
                if(lexed.get(0)[1].equals("func"))
                    for(int j=i; j<lexed.size(); j++){
                        if(lexed.get(j)[1].equals(":")){
                            TreeNode<Object[]> argTree = new TreeNode<>(new Object[]{"var", lexed.get(j+1)[1], lexed.get(j-1)[1], "null"}, null, null);
                            contents.add(argTree);
                            lexed.remove(j-1);
                            lexed.remove(j-1);
                            lexed.remove(j-1);
                            j -= 2;
                        }
                        else if(lexed.get(j)[1].equals(",")){
                            lexed.remove(j);
                            j--;
                        }
                    }
                //Handles nested functions
                for(int j=i+1; j<lexed.size(); j++){
                    if(lexed.get(j)[1].equals("(")){
                        TreeNode<Object[]> argTree = new TreeNode<>(new Object[]{"func", lexed.get(j-1)[1], Interpreter.funcs.get(lexed.get(j-1)[1])[0], Interpreter.funcs.get(lexed.get(j-1)[1])[1]}, null, null);
                        groupLists(lexed, j, new String[]{"(", ")"});
                        lexed.remove(j-1);
                        for(Object arg : (Object[])lexed.get(j)[1])
                            Interpreter.actionTree((TreeNode<Object[]>) arg);
                        argTree.setRight(new TreeNode<>(lexed.get(j), null, null));
                        contents.add(argTree);
                        lexed.remove(j-1);
                        lexed.remove(j-1);
                        lexed.remove(j-1);
                        /*

                        lexed.remove(j-1);

                        int extra = 0;
                        while (lexed.get(j)[1].equals(")")) {
                            lexed.remove(j - 1);
                            extra ++;
                            if(j == lexed.size()) break;
                        }
                        lexed.remove(j-1);
                        j -= extra;*/
                    }
                }

                if(contents.size() > 0){
                    lexed.add(i + 1, new Object[]{brackets[0].equals("(") ? "args" : "array", contents.toArray()});
                    break;
                }
                //empty agrs
                if(i < lexed.size() - 1)
                    if(lexed.get(i + 1)[1].equals(brackets[1])) {
                        lexed.add(i + 1, new Object[]{brackets[0].equals("(") ? "args" : "array", contents.toArray()});
                        i += 2;
                    }
                /*if(i < lexed.size() - 1)
                    if(lexed.get(i+1)[1].equals("["))
                        groupLists(lexed, i+1, brackets);*/
                //one arg
                /*if(i < lexed.size() - 2)
                    if(lexed.get(i + 2)[1].equals(brackets[1])) {
                        contents.add(parse(lexed.subList(i + 1, i + 2)));
                        lexed.add(i + 1, new Object[]{brackets[0].equals("(") ? "args" : "array", contents.toArray()});
                        lexed.remove(lexed.get(i + 2));
                        i += 3;
                        lexed.add(i + 1, new Object[]{brackets[0].equals("(") ? "args" : "array", contents.toArray()});
                        continue;
                    }*/
                if(i > lexed.size() - 3) break;
                //several args
                ArrayList<Object[]> items = new ArrayList<>();
                if(i < lexed.size() - 1)
                    if((lexed.get(i - 1)[0].equals("id") && brackets[0].equals("[")) || lexed.get(i - 1)[0].equals("]"))
                        break;
                while (!lexed.get(i + 1)[1].equals(brackets[1])){
                    if(lexed.get(i + 1)[1].equals(brackets[0])) {
                        groupLists(lexed, i+1, brackets);
                        items.add(lexed.get(i + 2));
                        lexed.remove(i + 1);
                        lexed.remove(i + 1);
                        lexed.remove(i + 1);
                        if(lexed.get(i + 1)[1].equals(brackets[1]))
                            break;
                        lexed.remove(i + 1);
                        continue;
                    }
                    items.add(lexed.get(i + 1));
                    lexed.remove(i + 1);
                    if (lexed.get(i + 1)[1].equals(",")){
                        lexed.remove(i + 1);
                        contents.add(parse(items));
                        if(contents.get(contents.size()-1) == null)
                            return 1;

                        items = new ArrayList<>();
                    }
                }
                if(items.size() > 0) {
                    for(Object[] item : items) {
                        if(item[0].equals("array")) {
                            contents.add(new TreeNode<>(item, null, null));
                        }
                        else {
                            contents.add(parse(items));
                            if(contents.get(contents.size()-1) == null)
                                return 1;
                            break;
                        }
                    }
                    //lexed.add(i + 1, new Object[]{"op", ")"});
                    lexed.add(i + 1, new Object[]{brackets[0].equals("(") ? "args" : "array", contents.toArray()});
                }
                break;
            }
            else if(lexed.get(i)[1].equals(brackets[1]))
                return 0;
        }
        return 0;
    }

    public static void registerAssignment(TreeNode<Object[]> parsed, List<ArrayList<Object[]>> program) throws SimplexException {
        if(parsed.getData()[1].equals(":")){
            if(parsed.getLeft().getData()[0].equals("primitive")) {
                String type = (String) parsed.getLeft().getData()[1];
                String name = (String) parsed.getRight().getLeft().getData()[1];
                TreeNode<Object[]> value = parsed.getRight().getRight();

                Interpreter.vars.put(Interpreter.stackFrame.peek()+"."+name, new Object[]{type, value});

                //Object[] data = parsed.getRight().getData();

                //parsed.setLeft(parsed.getRight().getLeft());
                //parsed.setRight(parsed.getRight().getRight());

                //parsed.setData(data);

                return;
            }
            if(parsed.getLeft().getData()[1].equals("func")) {
                String type = (String) parsed.getRight().getLeft().getData()[1];
                String name = (String) parsed.getRight().getRight().getData()[1];
                Object[] args = parsed.getRight().getRight().getRight().getData();
                ArrayList<ArrayList<Object[]>> scope = new ArrayList<>();
                Interpreter.stackFrame.push(name);
                for(Object arg : (Object[])args[1]){
                    String argType = (String)((TreeNode<Object[]>)arg).getData()[2];
                    String argName = (String)((TreeNode<Object[]>)arg).getData()[1];
                    Object argValue = ((TreeNode<Object[]>)arg).getData()[3];

                    Interpreter.vars.put(Interpreter.stackFrame.peek()+"."+argName, new Object[]{argType, argValue});
                }
                int opens = 0;
                for(ArrayList<Object[]> line : program){
                    if(line.size() == 0) continue;

                    if(line.get(0)[1].equals("{")) {
                        opens++;
                        scope.add(line);
                    }

                    else if(line.get(0)[1].equals("}")) {
                        opens --;
                        scope.add(line);
                        if (opens == 0)
                            break;
                    }

                    else scope.add(line);
                }

                Interpreter.funcs.put(name, new Object[]{type, args, null});
                ArrayList<TreeNode<Object[]>> scopeTree = Interpreter.processBlock(scope);

                Interpreter.funcs.put(name, new Object[]{type, args, scopeTree});
                Interpreter.stackFrame.pop();
            }
        }
    }

}
