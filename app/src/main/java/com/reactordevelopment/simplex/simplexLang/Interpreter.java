package com.reactordevelopment.simplex.simplexLang;

import java.util.*;

public class Interpreter {
    // key: funcName, value: [returnType, [argTypes], body: List<TreeNode>]
    /**Stores all the declared functions*/
    public static Map<String, Object[]> funcs = new HashMap<>();
    // key: varName, value: [type, value]
    /**Stores all the declared variables*/
    public static Map<String, Object[]> vars = new HashMap<>();
    /**Keeps track which scope the program is running in (main of function)*/
    public static Stack<String> stackFrame = new Stack<>();

    /**Processes the given block of code, but only parses it*/
    public static ArrayList<TreeNode> processParse(ArrayList<ArrayList<Object[]>> block) throws SimplexException {
        return processBlock(block, true);
    }
    /**Processes the given block of code in full*/
    public static ArrayList<TreeNode> processFull(ArrayList<ArrayList<Object[]>> block) throws SimplexException {
        return processBlock(block, false);
    }
    /**Converts a list of lists of tokens into a list of treeNodes
     * Checks if the line has balanced expressions
     * Forms the lists and arguments in each line
     * Parses each line
     * Converts each line into its action tree
     * Adds each line to the larger program*/
    public static ArrayList<TreeNode> processBlock(ArrayList<ArrayList<Object[]>> block, boolean justParse) throws SimplexException {
        ArrayList<TreeNode> tree = new ArrayList<>();
        if(block.size() == 0) return tree;
        //Loops through each line in the larger list
        for (int i = 0; i < block.size(); i++) {
            try {
                //If the last line does not have null data
                if (i > 0 && tree.get(tree.size() - 1).getData() != null)
                    //If the last line is a function assignment
                    if (tree.get(tree.size() - 1).getData()[0].equals("assignFunc")) {
                        //Number of '{' encountered
                        int opens = 1;
                        i++;
                        //Skips over body of the declared function
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
                //If the index is over the length of the line list
                if (i >= block.size()) return tree;
                //The line of tokens
                ArrayList<Object[]> lineArray = block.get(i);
                //The treeNode representing the line of tokens
                TreeNode branch = new TreeNode(null);
                //Return an empty treeNode if the line length is 0
                if (lineArray.size() == 0) {
                    tree.add(branch);
                    continue;
                }
                //Checks if the brackets '(', '[', '{' all have closing counterparts
                Parser.balancedExpression(lineArray, false);

                //Groups arguments together into their arrays
                Parser.groupLists(lineArray, 0, new String[]{"(", ")"});
                //Groups arrays together in arrays
                Parser.groupLists(lineArray, 0, new String[]{"[", "]"});
                //Turns the list of tokens into a treeNode
                branch = Parser.parse(lineArray);
                //Adds curly brackets to line list and continues
                if (lineArray.size() == 1 && (branch.getData()[1].equals("{") || branch.getData()[1].equals("}"))) {
                    tree.add(branch);
                    continue;
                }
                //Registers the declaration of functions or variables in a line
                if (lineArray.size() > 1)
                    Parser.registerAssignment(branch, block.subList(i + (lineArray.get(1)[0].equals("reserved") ? 1 : 0), block.size()));

                //Process the parsed tree into an action tree
                if(!justParse) Interpreter.actionTree(branch);

                //Adds the line tree into the list of lines
                tree.add(branch);
            } catch (SimplexException e) {
                e.printStackTrace();
                //If an error is thrown, throw the same error, but add the line number
                throw new SimplexException(e.getError()+" at line "+Lexer.lineMappings.get(i+1), e.getCode());
            }
        }
        return tree;
    }
    /**Processes given tree into an action tree
     * Types added to ids, operators
     * Ids converted to functions or variables*/
    public static void actionTree(TreeNode parsed) throws SimplexException {
        //If the tree is a function assignment
        boolean funcAssign = false;
        //If the line is a function assignment
        if(parsed.getData()[1].equals(":") && !parsed.getLeft().getData()[1].equals("return") && parsed.getRight().getRight() != null &&
                parsed.getLeft().getData()[1].equals("func"))
            //Collapses 'func: type: name' into 'name' if the function is registered
            if(funcs.containsKey(""+parsed.getRight().getRight().getData()[1])){
                TreeNode funcTree = parsed.getRight().getRight();
                parsed.setData(funcTree.getData());
                parsed.setRight(funcTree.getRight());
                parsed.setLeft(funcTree.getLeft());
                //Sets marker
                funcAssign = true;
            }
        //Recursively calls actionTree() on each branch of the tree if they exist
        if (parsed.getLeft() != null)
            actionTree(parsed.getLeft());
        if (parsed.getRight() != null  && !funcAssign)
            actionTree(parsed.getRight());
        //Calls actionTree on every element in arguments or arrays
        if(parsed.getData()[0].equals("args") || parsed.getData()[0].equals("array")) {
            for (Object item : (Object[]) parsed.getData()[1])
                actionTree((TreeNode) item);

            //If the tree is an array
            if(parsed.getData()[0].equals("array")){
                boolean typeCheck = true;
                String type = "";
                //Checks if every element in the array matches
                for(Object item : (Object[]) parsed.getData()[1]){
                    String tmpType;
                    if(((TreeNode)item).getData()[0].equals("literal"))
                        tmpType = (String) ((TreeNode)item).getData()[1];
                    else
                        tmpType = (String) ((TreeNode)item).getData()[2];
                    if(!type.equals("") && !tmpType.equals(type)) {
                        typeCheck = false;
                        break;
                    }
                    type = tmpType;
                }
                //Set the type of the array if they all match
                if(typeCheck)
                    parsed.setData(new Object[]{"literal", type+"[]", parsed.getData()[1]});
                //Sets the type of the array to any[]
                else
                    parsed.setData(new Object[]{"literal", "any[]", parsed.getData()[1]});
            }
        }
        //If the tree is an id
        else if(parsed.getData()[0].equals("id")) {
            Object[] info;
            //If the tree is a function call
            if (funcs.get(""+parsed.getData()[1]) != null) {
                //Format data as function assignment or call
                info = funcs.get(""+parsed.getData()[1]);
                parsed.setData(new Object[]{(funcAssign ? "assignF" : "f")+"unc", info[0], new Object[]{"args", info[1]}, parsed.getData()[1]});
            }
            else{
                //The value of the references variable
                info = vars.get(stackFrame.peek()+"."+parsed.getData()[1]);
                //The tree references a variable
                if (info != null)
                    parsed.setData(new Object[]{"var", info[0], Interpreter.stackFrame.peek()+"."+parsed.getData()[1]});
                //Unknown variable or function
                else
                    throw new SimplexException("No such function or variable '"+parsed.getData()[1]+"'", 1);
            }
        }
        //The tree is an operator
        else if(parsed.getData()[0].equals("op") && parsed.getData().length == 2){
            String leftType;
            String rightType;
            //If left of tree is not an operator
            if(parsed.getLeft().getData().length > 2)
                leftType = (String) parsed.getLeft().getData()[1];
            //If the length is too short
            else
                throw new SimplexException("Unexpected token to the left of '"+parsed.getData()[1]+"'", 1);
            //If right of tree is not an operator
            if(parsed.getRight().getData().length > 2)
                rightType = (String) parsed.getRight().getData()[1];
            //If the length is too short
            else
                throw new SimplexException("Unexpected token to the right of '"+parsed.getData()[1]+"'", 1);
            //Give types to operator
            parsed.setData(new Object[]{"op", leftType, rightType, parsed.getData()[1]});
        }
        //If the token is a reserved term
        else if(parsed.getData()[0].equals("reserved"))
            parsed.setData(new Object[]{"literal", parsed.getData()[0], parsed.getData()[1]});

        //Formats as literal of no other type matches
        else if(!parsed.getData()[0].equals("literal") && !parsed.getData()[0].equals("var") && !parsed.getData()[0].equals("func") && !parsed.getData()[0].equals("op")){
            parsed.setData(new Object[]{"literal", parsed.getData()[0], parsed.getData()[1]});
        }
    }
}
