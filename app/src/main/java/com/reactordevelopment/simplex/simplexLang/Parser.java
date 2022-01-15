package com.reactordevelopment.simplex.simplexLang;

import java.util.*;

public class Parser {

    /**Determines if the brackets '(', '[', '{' all have closing counterparts
     * this is used to determine if the expression is balanced
     * @param lexed the lexed expression
     * @return if the expression is balanced*/
    public static boolean balancedExpression(ArrayList<Object[]> lexed, boolean doBrackets) throws SimplexException {
        //Returns if the list of tokens is empty
        if(lexed.size() == 0) return false;
        //Keeps track of encountered brackets
        Stack<String> brackets = new Stack<>();
        boolean balanced = true;
        //counts open and closed brackets
        for (Object[] objects : lexed) {
            Object data = objects[1];
            if (data.equals("(") || (data.equals("{") && doBrackets) || data.equals("["))
                brackets.push((String) data);
            String open = " ";
            //Skips over non-bracket elements
            if (!data.equals(")") && !(data.equals("}") && doBrackets) && !data.equals("]")) continue;
            //Checks if the current closing bracket matches the last open bracket
            //If not, the line is unbalanced
            switch ((String) data) {
                case ")":
                    if(brackets.size() > 0)
                        open = brackets.pop();
                    if (!open.equals("(")) balanced = false;
                    break;
                case "}":
                    if(doBrackets) {
                        if(brackets.size() > 0)
                            open = brackets.pop();
                        if (!open.equals("{")) balanced = false;
                    }
                    break;
                case "]":
                    if(brackets.size() > 0)
                        open = brackets.pop();
                    if (!open.equals("[")) balanced = false;
                    break;
            }
        }
        //If each bracket had a matching counterpart
        if(balanced && brackets.isEmpty())
            return true;
        //If the line was unbalanced
        else
            throw new SimplexException("Unbalanced operations", 1);
    }
    /**Parses given list of tokens into a treeNode*/
    public static TreeNode parse(List<Object[]> lexed) throws SimplexException {
        return parse(lexed, new ArrayList<>(0));
    }
    /**Calculate the result of the mathematical expression.
     * Add each number in the expression to a numbers stack, and every operation
     * to an operation stack.  When a closed parentheses is hit by then cycling through the character,
     * the contents of each stack is evaluated and collapsed, multiplication first, then addition.
     * This turns each instance of '(x*y+z...)' into a single term.  This continues until the entire expression
     * is collapsed into a single term.  The program then returns this term.
     * If the stack of nodes is empty and there is still an operation waiting, pull node form inserts,
     * this accounts for tokens already being parsed as part of a list*/
    public static TreeNode parse(List<Object[]> lexed, ArrayList<TreeNode> inserts) throws SimplexException {
        try {
            //Returns tree containing only list item
            if (lexed.size() == 1) return new TreeNode(lexed.get(0));

            //Wrap whole line in parentheses
            lexed.add(0, new Object[]{"op", "("});
            lexed.add(lexed.size(), new Object[]{"op", ")"});

            //Stack of non-operators
            Stack<TreeNode> nodeStack = new Stack<>();
            //Stack of operators
            Stack<String> operations = new Stack<>();
            //List of tokens that are to be given subscripts ex. (data: function, right: [args])
            ArrayList<Object[]> controllers = new ArrayList<>();
            //Loops through list of tokens
            for (int i = 0, lexedSize = lexed.size(); i < lexedSize; i++) {
                //The current token
                Object[] element = lexed.get(i);
                //Adds token to controllers if it is an id, reserved, primitive or an array and
                // it is immediately followed by a '(' or a '['
                if (i < lexedSize - 1)
                    if ((element[0].equals("id") || element[0].equals("reserved") || Lexer.primitives.contains(""+element[0]) || element[0].equals("array"))
                            && (lexed.get(i + 1)[1].equals("(") || lexed.get(i + 1)[1].equals("["))) {
                        //The controller element, the depth of operations (tells when scope of controller ends, type of bracket to signal controller
                        controllers.add(new Object[]{element, operations.size(), lexed.get(i + 1)[1]});
                        continue;
                    }
                //Add token to nodeStack if it is not an operator
                if (!element[0].equals("op"))
                    nodeStack.push(new TreeNode(element));
                //If the element if a close bracket
                if (element[1].equals(")") || element[1].equals("]")) {
                    //Saves which type of bracket triggered this block
                    String beginBracket = " ";
                    if (element[1].equals(")")) beginBracket = "(";
                    if (element[1].equals("]")) beginBracket = "[";

                    //Saves tokens that are being added before multiplication level (*, /)
                    Stack<TreeNode> addTrees = new Stack<>();
                    //Saves addition level operations (+, -)
                    Stack<String> addOps = new Stack<>();
                    //calculate multiplication operations
                    while (!operations.peek().equals(beginBracket) && !(operations.peek().equals(":") && beginBracket.equals("["))) {
                        //The right operand
                        TreeNode tree2 = nodeStack.pop();
                        //The left operand
                        TreeNode tree1;
                        //If there is a node to pull from
                        if(nodeStack.size() != 0)
                            tree1 = nodeStack.pop();
                        //Pulls from inserts
                        else {
                            tree1 = inserts.get(0);
                            inserts.remove(0);
                        }
                        //The current operation being preformed
                        String operation = operations.pop();
                        //Parse as multiplication
                        if (operation.equals("*") || operation.equals("/"))
                            nodeStack.push(new TreeNode(new Object[]{"op", operation}, tree1, tree2));
                        //Save operation and operands to be added later
                        else{
                            addOps.push(operation);
                            addTrees.push(tree2);
                            nodeStack.push(tree1);
                        }
                    }
                    //Empty storage of addition/subtraction
                    while (addOps.size() > 0)
                        operations.push(addOps.pop());
                    while (addTrees.size() > 0)
                        nodeStack.push(addTrees.pop());

                    //Calculate addition operations
                    while (!operations.peek().equals(beginBracket)) {
                        String operation = operations.pop();
                        //If the most recent controller is the index of an array or string
                        if((controllers.size() > 0 && controllers.get(controllers.size()-1)[2].equals("[")
                                && operation.equals(":"))){
                            //Keeps track of ending of index modifier
                            int place = i;
                            //Number of opening brackets that have been encountered
                            int brackets = 0;
                            //Found an int, str, etc before ':'
                            boolean foundLeftNum = false;
                            //Found end index
                            boolean foundRightNum = false;
                            //Glides backwards until it finds the current colon
                            while (!lexed.get(place)[1].equals(":") && brackets == 0) {
                                place--;
                                Object[] current = lexed.get(place);
                                if(current[1].equals("]"))
                                    brackets++;
                                else if(current[1].equals("["))
                                    brackets--;
                                else if(!current[0].equals("op"))
                                    foundRightNum = true;
                            }
                            //Glides backwards until the opening of the index modifier is found
                            while (!lexed.get(place)[1].equals("[") && brackets == 0) {
                                place--;
                                Object[] current = lexed.get(place);
                                if(current[1].equals("]"))
                                    brackets++;
                                else if(current[1].equals("["))
                                    brackets--;
                                else if(!current[0].equals("op"))
                                    foundLeftNum = true;
                            }
                            //Both indices present -> '[start:end]'
                            if(foundLeftNum && foundRightNum) {
                                TreeNode tree2 = nodeStack.pop();
                                TreeNode tree1 = nodeStack.pop();
                                nodeStack.push(new TreeNode(new Object[]{"op", operation}, tree1, tree2));
                            }
                            //Left index present -> '[start:Infinity]'
                            else if(foundLeftNum)
                                nodeStack.push(new TreeNode(new Object[]{"op", operation}, nodeStack.pop(), new TreeNode(new Object[]{"literal", "int", Integer.MAX_VALUE})));
                            //Right index present -> '[0:end]'
                            else
                                nodeStack.push(new TreeNode(new Object[]{"op", operation}, new TreeNode(new Object[]{"literal", "int", 0}), nodeStack.pop()));
                        }
                        //Not in an index modifier -> do normal addition/subtraction
                        else {
                            TreeNode tree2 = nodeStack.pop();
                            TreeNode tree1 = nodeStack.pop();
                            nodeStack.push(new TreeNode(new Object[]{"op", operation}, tree1, tree2));
                        }
                    }
                    //remove initial '(', '{', '[' from operation stack
                    operations.pop();

                }
                //Add all other operations to operation stack
                else if (element[0].equals("op") && !element[1].equals("{")) {
                    //Add 0 to the left of any '-' without a left operand (negative number)
                    if(i > 0 && lexed.get(i-1)[0].equals("op")) {
                        if (element[1].equals("-"))
                            nodeStack.push(new TreeNode(new Object[]{"int", 0}));
                        if (element[1].equals("!"))
                            nodeStack.push(new TreeNode(new Object[]{"void", "null"}));
                    }
                    operations.add((String) element[1]);
                }

                //Gives children of functions/arrays
                for (int j = 0; j < controllers.size(); j++) {
                    //The current controller
                    Object[] control = controllers.get(j);
                    //If the operations size matches that saved in the controller
                    if ((int) control[1] == operations.size()) {
                        //Keeps adding for multiple [0][0]...[0]
                        boolean chained = false;
                        if (nodeStack.size() > 1) {
                            TreeNode tmp = nodeStack.pop();
                            //If the next node is the controller
                            if (nodeStack.peek().getData().equals(control[0])) {
                                //Sets the rightmost element to tmp (Puts multiple array indices under the controller)
                                nodeStack.peek().getRightmost().setRight(tmp);
                                chained = true;
                            } else nodeStack.push(tmp);
                        }
                        //If the array already has its indices under it
                        if (!chained)
                            //Add the controller with the correct sub-indices to the nodeStack
                            nodeStack.push(new TreeNode((Object[]) control[0], null, nodeStack.pop()));
                        //Remove controller if there are no more sub-indices
                        if (!lexed.get(i + 1)[1].equals("[")) {
                            controllers.remove(control);
                            j--;
                        }
                    }
                }
            }
            return nodeStack.pop();
        }catch (Exception e){
            e.printStackTrace();
            throw new SimplexException("Syntax Error", 1);
        }
    }
    /**Groups any arguments or arrays together in arrays depending on the type of bracket that is being worked on*/
    public static void groupLists(ArrayList<Object[]> lexed, int start, String[] brackets) throws SimplexException{
        //Loops through every token in the list
        for(int i=start; i<lexed.size(); i++){
            //If the current token matches the given opening bracket in brackets
            if(lexed.get(i)[1].equals(brackets[0]) && i > 0){
                //If the group is not for a function call ex. (4+(3*2))
                if(lexed.get(i)[1].equals("(") && !lexed.get(i-1)[0].equals("id"))
                    continue;
                //Contents of array or arguments
                ArrayList<TreeNode> contents = new ArrayList<>();
                //condense declarations (int: num)
                //If line is a function declaration
                if(lexed.get(0)[1].equals("func")) {
                    //Loop through declared args
                    for (int j = i; j < lexed.size(); j++) {
                        //If the current token is an parameter declaration ex. (int: num)
                        if (lexed.get(j)[1].equals(":")) {
                            //Add parameter to contents and remove form list
                            TreeNode argTree = new TreeNode(new Object[]{"var", lexed.get(j - 1)[1], lexed.get(4)[1]+"."+lexed.get(j + 1)[1]});
                            contents.add(argTree);
                            lexed.remove(j - 1);
                            lexed.remove(j - 1);
                            lexed.remove(j - 1);
                            j -= 2;
                        }
                        //If the current token is a comma
                        else if (lexed.get(j)[1].equals(",")) {
                            //Delete and skip
                            lexed.remove(j);
                            j--;
                        }
                    }
                }
                //Handles nested functions
                //Loops through tokens after i
                for(int j=i+1; j<lexed.size(); j++){
                    //If a function call is encountered
                    if(lexed.get(j)[1].equals("(") && lexed.get(j-1)[0].equals("id")){
                        //The name of the function being called
                        String funcName = (String) lexed.get(j-1)[1];
                        //Throw error if the function does not exist
                        if(Interpreter.funcs.get(funcName) == null)
                            throw new SimplexException("No such function or variable '"+funcName.substring(funcName.indexOf(".")+1)+"'", 1);
                        //Saves the function token as a tree with information from the 'funcs' table
                        TreeNode argTree = new TreeNode(new Object[]{"func", Interpreter.funcs.get(funcName)[0], new Object[]{"args", Interpreter.funcs.get(funcName)[1]}, funcName});
                        //Group the arguments that come after
                        groupLists(lexed, j, new String[]{"(", ")"});
                        lexed.remove(j-1);
                        //Process each argument to function as an action tree
                        for(Object arg : (Object[])lexed.get(j)[1])
                            Interpreter.actionTree((TreeNode) arg);
                        //Set the right tree of the function call to be the argument tree
                        argTree.setRight(new TreeNode(lexed.get(j)));
                        //Handles modifiers after function ex. foo()[0]
                        if(lexed.get(j+2)[1].equals("[")){
                            //Number of brackets encountered
                            int opens = 0;
                            //Saves the place of the first index
                            int place = j+2;
                            ArrayList<Object[]> modifiers = new ArrayList<>(0);
                            modifiers.add(new Object[]{"str", "dummy"});
                            //Gathers all of []
                            while ((!lexed.get(place)[1].equals("]") && opens == 0) || lexed.get(place+1)[1].equals("[")){
                                modifiers.add(lexed.get(place));
                                lexed.remove(place);
                                if(lexed.get(place)[1].equals("["))
                                    opens++;
                                if(lexed.get(place)[1].equals("]"))
                                    opens--;
                            }
                            modifiers.add(lexed.get(place));
                            lexed.remove(place);
                            //Parse the modifiers
                            TreeNode modTree = parse(modifiers).getRight();
                            //Adds to function
                            argTree.getRight().setRight(modTree);
                        }
                        //Adds the arguments with modifiers to contents
                        contents.add(argTree);
                        lexed.remove(j-1);
                        lexed.remove(j-1);
                        lexed.remove(j-1);
                        //Skip over commas
                        if(lexed.get(j-1)[1].equals(","))
                            lexed.remove(j-1);
                    }
                }
                //Empty arguments
                if(i < lexed.size() - 1)
                    if(lexed.get(i + 1)[1].equals(brackets[1])) {
                        //Add empty arguments or array
                        lexed.add(i + 1, new Object[]{brackets[0].equals("(") ? "args" : "array", contents.toArray()});
                        return;
                    }
                if(i > lexed.size() - 3) break;
                //several args
                ArrayList<Object[]> items = new ArrayList<>();
                if(i < lexed.size() - 1)
                    //Skips array making if previous token is an id or a str (seeks sub-indices as is)
                    if(((lexed.get(i - 1)[0].equals("id") || lexed.get(i - 1)[0].equals("str") || lexed.get(i - 1)[0].equals("array"))
                            && brackets[0].equals("[")))
                        break;
                //Stack of brackets encountered
                Stack<String> bracketStack = new Stack<>();
                //Add current bracket
                bracketStack.push((String) lexed.get(i)[1]);
                int opens = 1;
                //Loops while the closing bracket is not encountered
                while (i + 1 < lexed.size() && !lexed.get(i + 1)[1].equals(brackets[1]) || opens > 1){
                    //Start of inner list/function
                    if(lexed.get(i + 1)[1].equals("(") || lexed.get(i + 1)[1].equals("[")) {
                        if(lexed.get(i + 1)[1].equals(brackets[0])) opens++;
                        bracketStack.push((String) lexed.get(i + 1)[1]);
                    }
                    if(lexed.get(i + 1)[1].equals(")") || lexed.get(i + 1)[1].equals("]")) {
                        if(lexed.get(i + 1)[1].equals(brackets[1])) opens--;
                        bracketStack.pop();
                    }
                    if(lexed.get(i + 1)[1].equals(brackets[0]) && (brackets[0].equals("[") || ( brackets[0].equals("(") && items.size() > 0 && items.get(items.size()-1)[0].equals("id")))) {
                        //adjust bracket stack
                        if(lexed.get(i + 1)[1].equals(brackets[0])) opens--;
                        bracketStack.pop();
                        //Group inner list/args
                        groupLists(lexed, i+1, brackets);
                        //Glide to end of nested brackets
                        items.add(lexed.get(i + 1));
                        lexed.remove(i+1);
                    }
                    //If the current token is a coma inside of an inner array or function
                    else if (bracketStack.peek().equals(brackets[0]) && lexed.get(i + 1)[1].equals(",")){
                        //Add previous item to contents
                        contents.add(parse(items));
                        if(contents.get(contents.size()-1) == null)
                            return;
                        //Reset items
                        items = new ArrayList<>();
                        lexed.remove(i+1);
                    }else {
                        //Add token to items
                        items.add(lexed.get(i + 1));
                        lexed.remove(i+1);
                    }
                }
                //If there have been items entered
                if(items.size() > 0) {
                    for(Object[] item : items) {
                        //If the item is an array
                        if(item[0].equals("array"))
                            contents.add(new TreeNode(item));
                        else {
                            //If there is an array inside a function call
                            if(brackets[0].equals("(")) {
                                items.add(0, new Object[]{"op", "|"});
                                groupLists(items, 0, new String[]{"[", "]"});
                                items.remove(0);
                            }
                            //Add items to contents
                            contents.add(parse(items, contents));
                            if(contents.get(contents.size()-1) == null)
                                return;
                            break;
                        }
                    }
                    //Remove surrounding square brackets
                    if(brackets[0].equals("[")) lexed.remove(i+1);
                    //Add arguments or array to list
                    lexed.add(i + 1, new Object[]{brackets[0].equals("(") ? "args" : "array", contents.toArray()});
                    if(brackets[0].equals("[")) lexed.remove(i);
                }
            }
            else if(lexed.get(i)[1].equals(brackets[1])) return;
        }
    }
    /**Looks for variable or function declaration, and registers it in {@link Interpreter#vars} or {@link Interpreter#funcs}
     * If the declaration if a function, it searches for the body of the function in the given pgrmRemainder list*/
    public static void registerAssignment(TreeNode parsed, List<ArrayList<Object[]>> pgrmRemainder) throws SimplexException {
        //If parsed contains a declaration
        if(parsed.getData()[1].equals(":")){
            //If the declaration is for a variable
            if(parsed.getLeft().getData()[0].equals("primitive")) {
                String type = (String) parsed.getLeft().getData()[1];
                String name = (String) parsed.getRight().getLeft().getData()[1];
                //Puts a new variable in vars with the correct name and type and with a null value
                Interpreter.vars.put(Interpreter.stackFrame.peek()+"."+name, new Object[]{type, null});
                return;
            }
            //If the declaration is for a function
            if(parsed.getLeft().getData()[1].equals("func")) {
                String type = (String) parsed.getRight().getLeft().getData()[1];
                String name = (String) parsed.getRight().getRight().getData()[1];
                //The declared args for the function
                Object[] args = parsed.getRight().getRight().getRight().getData();
                //The body of the function
                ArrayList<ArrayList<Object[]>> scope = new ArrayList<>();
                //Adjust stackFrame
                Interpreter.stackFrame.push(name);
                //Adds each declared argument as a variable under the current function
                for(Object arg : (Object[])args[1]){
                    String argType = (String)((TreeNode)arg).getData()[1];
                    String argName = (String)((TreeNode)arg).getData()[2];
                    Interpreter.vars.put(argName, new Object[]{argType, null});
                }
                //Number of curly braces that have been encountered
                int opens = 0;
                //Loops through the lines in the remaining program and stops once the matching closing bracket has been reached
                for(ArrayList<Object[]> line : pgrmRemainder){
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
                    //Adds the current line to the scope
                    else scope.add(line);
                }
                //Puts the function with no body into funcs
                Interpreter.funcs.put(name, new Object[]{type, args[1], null});
                //Processes the body of the function
                ArrayList<TreeNode> scopeTree = Interpreter.processFull(scope);

                for(int i=0; i<scopeTree.size(); i++){
                    //Removes all opening brackets from body
                    if(scopeTree.get(i).getData()[1].equals("{")){
                       scopeTree.remove(i);
                       i--;
                       continue;
                    }
                    //Checks that the type of each return matches the declared return type
                    if (scopeTree.get(i).getLeft() != null && scopeTree.get(i).getLeft().getData()[2].equals("return")) {
                        if(!type.equals(scopeTree.get(i).getRight().getData()[1]))
                            throw new SimplexException("Return types must match", 1);
                    }
                }
                Interpreter.funcs.put(name, new Object[]{type, args[1], scopeTree});
                Interpreter.stackFrame.pop();
            }
        }
    }
}
