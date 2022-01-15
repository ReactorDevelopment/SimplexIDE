package com.reactordevelopment.simplex.simplexLang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Executor {
    /**Lexes the raw string then builds it into processed action tree*/
    public static ArrayList<TreeNode> build(String codeFile) throws SimplexException {
        //Initialize stackFrame to main
        Interpreter.stackFrame.push("main");
        //Lexes string into tokens
        ArrayList<Object[]> lexed = Lexer.lexer(codeFile);
        //Holds each line of the program
        ArrayList<ArrayList<Object[]>> program = new ArrayList<>();

        prepareLines(lexed, program);
        //Returns the built action trees
        return Interpreter.processFull(program);
    }
    /**Separates the list of tokens into a list of lines of tokens*/
    public static void prepareLines(ArrayList<Object[]> lexed, ArrayList<ArrayList<Object[]>> program){
        ArrayList<Object[]> line = new ArrayList<>();
        for (Object[] token : lexed) {
            //If a new line is encountered
            if (token[0].equals("newline")) {
                //Add the line to the program and reset the line
                if(line.size() > 0)
                    program.add(line);
                line = new ArrayList<>();
            }
            //If a curly bracket is encountered
            else if (token[1].equals("{") || token[1].equals("}")) {
                //Add the line to the program
                program.add(line);
                //Add the bracket to the program as its own line
                line = new ArrayList<>();
                line.add(token);
                program.add(line);
                //Reset the line
                line = new ArrayList<>();
            }
            //Add the token to the line
            else line.add(token);
        }
    }
    /**Builds the program, then executes it, catching and printing any error that it encounters*/
    public static void buildAndExecute(String codeFile) throws SimplexException {
        try{
            //Builds program
            ArrayList<TreeNode> programTree = build(codeFile);
            if(programTree != null)
                //Attempts to execute
                Executor.execute(programTree);
        }catch (SimplexException e){
            //e.printStackTrace();
            //Prints any error that is made
            Util.executeUtil("print", new String[]{"err"}, new Object[]{e.getError()+"\nError Code: "+e.getCode()});
        }

    }
    /**Converts the given treeNode into its simplest form
     * Executing functions and setting the node to the return type
     * Simplifying expressions
     * Resolving variable references*/
    public static TreeNode resolve(TreeNode block) throws SimplexException {
        //If the treeNode represents a mathematical expression
        if(block.getData()[0].equals("op")){
            //Resolve both sides
            if(block.getLeft() != null) resolve(block.getLeft());
            if(block.getRight() != null) resolve(block.getRight());
            //If the operator is contained in mathOperators
            if(Lexer.mathOperators.contains(""+block.getData()[3])){
                //The operands
                Object obj1, obj2;
                //The types of the operands
                String type1, type2;
                //saves right of right number (used un nested indices array[0:1][0])
                TreeNode saveRight;
                //Get left item and type
                obj1 = block.getLeft().getData()[2];
                type1 = (String) block.getLeft().getData()[1];
                //Get right item and type
                obj2 = block.getRight().getData()[2];
                saveRight = block.getRight().getRight();
                type2 = (String) block.getRight().getData()[1];
                //If the operand is of an array type
                boolean isMatrix = type1.contains("[");
                //If the array is formatted correctly for the given operation
                boolean matrixCheck = false;
                //If the array is properly formatted as a matrix
                if(isMatrix && block.getData().length > 3)
                    //Check that the matrix is of the correct dimensions
                    matrixCheck = matrixCheck(""+block.getData()[3], (Object[]) obj1, (Object[])obj2);
                //If the operand is a matrix and is not formatted correctly
                if(isMatrix && !matrixCheck && !(block.getData()[3].equals("+") && (type1.equals("str") || type2.equals("str"))))
                    throw new SimplexException("Incorrect matrix format for operation '"+block.getData()[3]+"'", 1);
                //Preform the mathematical operation
                block.copy(calculateOp((String) block.getData()[3], type1, type2, obj1, obj2));
                //Remove the left branch
                block.setLeft(null);
                //Restore the right branch
                block.setRight(saveRight);

            }
        }
        //If the block is an array
        if((""+block.getData()[1]).contains("[") && block.getData()[0].equals("literal"))
            for(Object tree : (Object[]) block.getData()[2])
                resolve((TreeNode) tree);

        //If the block is a literal string
        if(block.getData()[0].equals("literal") && block.getData()[1].equals("str"))
            //Convert the raw string into an array of characters ex. ("HI" -> ["H", "I"])
            convertStr(block);

        //If the block has a modifier
        if(((block.getData()[0].equals("var") || block.getData()[0].equals("literal")) && block.getRight() != null) ||
                (block.getData()[0].equals("func") && block.getRight().getRight() != null)){
            //If the block is an array
            if((""+block.getData()[1]).contains("[") || block.getData()[1].equals("str")){
                Object[] treeArray;
                //Get the value of the block
                if(block.getData()[0].equals("var"))
                    treeArray = (Object[]) Interpreter.vars.get((String) block.getData()[2])[1];
                else
                    treeArray = (Object[]) block.getData()[2];
                //The next modifier to the array ex. ([1, 4, 7]->[0]<-[0]...)
                TreeNode nextIndex = block.getRight().getRight() != null ? resolve(block.getRight().getRight()) : null;
                //If there is a sub-index and it contains a ':'
                if(nextIndex != null && block.getRight().getData().length > 3 && block.getRight().getData()[3].equals(":")) {
                    //The end index of the sub-array
                    int upperIndex = (int) nextIndex.getData()[2];
                    //The start index of the sub-array
                    int lowerIndex = (int) resolve(block.getRight().getLeft()).getData()[2];
                    //Account for negative indices
                    if (upperIndex < 0) upperIndex = treeArray.length + upperIndex;
                    if (lowerIndex < 0) lowerIndex = treeArray.length + lowerIndex;
                    //If the start comes after the end
                    if (((int) block.getRight().getLeft().getData()[2]) > upperIndex)
                        throw new SimplexException("Beginning index must not be greater than ending index", 1);
                    //Create the sub-array
                    treeArray = Arrays.copyOfRange(treeArray, lowerIndex, upperIndex == Integer.MAX_VALUE ? treeArray.length : upperIndex);
                    //Set the block to that sub-array
                    block.setData(new Object[]{"literal", block.getData()[1], treeArray});
                }
                //If the sub-index contains a single number
                else {
                    //Get the next index
                    nextIndex = block.getRight() != null ? resolve(block.getRight()) : null;
                    //Simplify and expressions in the index
                    int index = (int) resolve(block.getRight()).getData()[2];
                    //Handle negative indices
                    if (index < 0) index = treeArray.length + index;
                    String type = (String)block.getData()[1];
                    if(type.contains("["))
                        type = ((String) block.getData()[1]).substring(0, ((String) block.getData()[1]).lastIndexOf("["));
                    //Set the block to the array element
                    if(type.equals("str"))
                        block.setData(new Object[]{"literal", type, new String[]{""+treeArray[index]}});
                    else
                        block.setData(new Object[]{"literal", type, ((TreeNode) treeArray[index]).getData()[2]});
                }
                //Restore the next index
                block.setRight(nextIndex != null ? nextIndex.getRight() : null);
                //Resolve further if there is a next index
                if(block.getRight() != null)
                    block = resolve(block);
            }
            /*else if(block.getData()[1].equals("str")){
                String[] strArray;
                if(block.getData()[0].equals("var"))
                    strArray = (String[]) Interpreter.vars.get((String) block.getData()[2])[1];
                else
                    strArray = (String[]) block.getData()[2];
                TreeNode nextIndex = block.getRight().getRight() != null ? resolve(block.getRight().getRight()) : null;
                if(nextIndex != null && block.getRight().getData()[3].equals(":")) {
                    int upperIndex = (int) resolve(block.getRight().getRight()).getData()[2];
                    int lowerIndex = (int) resolve(block.getRight().getLeft()).getData()[2];
                    if (upperIndex < 0) upperIndex = strArray.length + upperIndex;
                    if (lowerIndex < 0) lowerIndex = strArray.length + lowerIndex;
                    if (((int) block.getRight().getLeft().getData()[2]) > upperIndex)
                        throw new SimplexException("Beginning index must not be greater than ending index", 1);
                    strArray = Arrays.copyOfRange(strArray, lowerIndex, upperIndex == Integer.MAX_VALUE ? strArray.length : upperIndex);
                    block.setData(new Object[]{"literal", "str", strArray});
                }
                else {
                    int index = (int) resolve(block.getRight()).getData()[2];
                    if (index < 0) index = strArray.length + index;
                    block.setData(new Object[]{"literal", block.getData()[1], new String[]{strArray[index]}});
                }
                block.setRight(null);
            }*/
        }
        /*if((""+block.getData()[1]).contains("[")){
                Object[] treeArray;
                //Get the value of the block
                if(block.getData()[0].equals("var"))
                    treeArray = (Object[]) Interpreter.vars.get((String) block.getData()[2])[1];
                else
                    treeArray = (Object[]) block.getData()[2];
                //The next modifier to the array ex. ([1, 4, 7]->[0]<-[0]...)
                TreeNode nextIndex = block.getRight().getRight() != null ? resolve(block.getRight().getRight()) : null;
                //If there is a sub-index and it contains a ':'
                if(nextIndex != null && block.getRight().getData().length > 3 && block.getRight().getData()[3].equals(":")) {
                    //The end index of the sub-array
                    int upperIndex = (int) nextIndex.getData()[2];
                    //The start index of the sub-array
                    int lowerIndex = (int) resolve(block.getRight().getLeft()).getData()[2];
                    //Account for negative indices
                    if (upperIndex < 0) upperIndex = treeArray.length + upperIndex;
                    if (lowerIndex < 0) lowerIndex = treeArray.length + lowerIndex;
                    //If the start comes after the end
                    if (((int) block.getRight().getLeft().getData()[2]) > upperIndex)
                        throw new SimplexException("Beginning index must not be greater than ending index", 1);
                    //Create the sub-array
                    treeArray = Arrays.copyOfRange(treeArray, lowerIndex, upperIndex == Integer.MAX_VALUE ? treeArray.length : upperIndex);
                    //Set the block to that sub-array
                    block.setData(new Object[]{"literal", block.getData()[1], treeArray});
                }
                //If the sub-index contains a single number
                else {
                    //Get the next index
                    nextIndex = block.getRight() != null ? resolve(block.getRight()) : null;
                    //Simplify and expressions in the idnex
                    int index = (int) resolve(block.getRight()).getData()[2];
                    //Handle negative indices
                    if (index < 0) index = treeArray.length + index;
                    //Set the block to the array element
                    block.setData(new Object[]{"literal", ((String) block.getData()[1]).substring(0, ((String) block.getData()[1]).lastIndexOf("[")),
                            ((TreeNode) treeArray[index]).getData()[2]});
                }
                //Restore the next index
                block.setRight(nextIndex != null ? nextIndex.getRight() : null);
                //Resolve further if there is a next index
                if(block.getRight() != null)
                    block = resolve(block);
            }*/
        //If the block references a variable
        else if(block.getData()[0].equals("var")){
            //Replaces block with value of referenced variable
            String varName = (String) block.getData()[2];
            block.setData(new Object[]{"literal", Interpreter.vars.get(varName)[0], Interpreter.vars.get(varName)[1]});
        }
        //If the block is a function call
        if(block.getData()[0].equals("func")){
            //The name if the called function
            String funcName = (String) block.getData()[3];
            //Include the function in the stackFrame
            Interpreter.stackFrame.push(funcName);
            //Temporary array to execute block
            ArrayList<TreeNode> funcLine = new ArrayList<>();
            funcLine.add(block);
            //Faves any modifier that come after the args
            TreeNode origRight = block.getRight().getRight();
            //Replace block with result of function call
            block.copy((TreeNode) execute(funcLine)[1]);
            //Restore any modifications
            block.setRightmost(origRight);
            //Resolve further if the block has modifiers
            if(block.getRight() != null)
                resolve(block);
            //Remove the function from the stackFrame
            Interpreter.stackFrame.pop();
        }

        return block;
    }
    /**Checks that the dimensions of a matrix are correct for the given operation*/
    public static boolean matrixCheck(String op, Object[] a, Object[] b) throws SimplexException {
        //If the operation is addition level
        if(op.equals("+") || op.equals("-") || op.equals("&&") || op.equals("||") || op.equals("!") || op.equals("~|")){
            //If outer arrays do not have the same length
            if(a.length != b.length) return false;
            //If the array is two deep
            if(((String)((TreeNode) a[0]).getData()[0]).contains("[")) {
                //Length of first sub array
                int childLength = ((TreeNode[]) ((TreeNode) a[0]).getData()[2]).length;
                for (int i = 0; i < a.length; i++) {
                    TreeNode element = (TreeNode) a[i];
                    //If the current sub array does not match the rest
                    if (((TreeNode[]) element.getData()[2]).length != childLength)
                        return false;
                    //If the recursive check for the sub array is false
                    if (!matrixCheck(op, (Object[]) element.getData()[2], (Object[]) ((TreeNode) b[i]).getData()[2]))
                        return false;
                }
            }
        }
        //If the operation if multiplication
        if(op.equals("*") || op.equals("~*")){
            //If the array is not two deep
            if(!((String) ((TreeNode) a[0]).getData()[1]).contains("[") ||
                    !((String) ((TreeNode) b[0]).getData()[1]).contains("["))
                throw new SimplexException("Arrays must have a depth of two to be multiplied", 1);
            //The length of a
            int aFirst = a.length;
            //The length of the sub arrays of a
            int aSecond = ((Object[]) ((TreeNode) a[0]).getData()[2]).length;
            //The length of b
            int bFirst = b.length;
            //The length of the sub arrays of b
            int bSecond = ((Object[]) ((TreeNode) b[0]).getData()[2]).length;
            //If the two dimensions are not equal and their opposites are not equal
            if(!(aSecond == bFirst && aFirst == bSecond) && !(aFirst == bFirst && aSecond == bSecond)) return false;
            if(op.equals("~*") && (aFirst != 1 || aSecond != 3))
                throw new SimplexException("Vectors must only be 3 dimensional for operation '~*'", 1);
            //Checks that all sub arrays of a and b are the same
            int len = ((Object[]) ((TreeNode) a[0]).getData()[2]).length;
            for(int i=1; i<a.length; i++)
                if(len != ((Object[]) ((TreeNode) a[i]).getData()[2]).length)
                    return false;
            len = ((Object[]) ((TreeNode) b[0]).getData()[2]).length;
            for(int i=1; i<b.length; i++)
                if(len != ((Object[]) ((TreeNode) b[i]).getData()[2]).length)
                    return false;

        }
        return true;
    }
    /**Preforms the given operation on the two given arrays*/
    public static TreeNode operateArrays(String op, Object[] a, Object[] b) throws SimplexException {
        //Initialize the result
        Object[] result = new Object[0];
        //The types of each of the first sub arrays
        String base1 = (String) ((TreeNode) a[0]).getData()[1];
        String base2 = (String) ((TreeNode) b[0]).getData()[1];
        //The brackets that are in the arrays ex. (int->[][][]<-)
        String brackets = base1.contains("[") ? base1.substring(base1.indexOf("[")) : "";
        //Set the array type to 'double' if either of the arrays are of doubles, else 'int'
        String finalType = base1.contains("double") || base2.contains("double") ? "double"+brackets : "int"+brackets;
        if(base1.contains("str") || base2.contains("str")) finalType = "str"+brackets;
        if(base2.contains("bool")) finalType = "bool"+brackets;
        //The base-type of each array ex. (int[] -> int)
        base1 = base1.contains("[") ? base1.substring(0, base1.indexOf("[")) : base1;
        base2 = base2.contains("[") ? base2.substring(0, base2.indexOf("[")) : base2;
        //Set the array base type to 'double' if either of the arrays are of doubles, else 'int'
        String finalBase = base1.equals("double") || base2.equals("double") ? "double" : "int";
        if(base1.equals("str") || base2.equals("str")) finalBase = "str";
        if(base2.contains("bool")) finalBase = "bool";
        //If the operation is addition or subtraction
        if(op.equals("+") || op.equals("-") || op.equals("&&") || op.equals("||") || op.equals("!") || op.equals("~|")){
            //Set the result length to that of a
            result = new Object[a.length];
            for(int i=0; i<a.length; i++){
                //Add or subtract each of the corresponding elements together using recursion if needed
                TreeNode element = (TreeNode) a[i];
                TreeNode element2 = (TreeNode) b[i];
                //The sum or difference
                result[i] = calculateOp(op, (String) element.getData()[1], (String) element2.getData()[1], element.getData()[2], element2.getData()[2]);
            }
        }
        //If the operation if multiplication
        if(op.equals("*") || op.equals("~*")) {
            //A dimensions = first x second
            int aFirst = a.length;
            int aSecond = ((Object[]) ((TreeNode) a[0]).getData()[2]).length;
            int bFirst = b.length;
            int bSecond = ((Object[]) ((TreeNode) b[0]).getData()[2]).length;
            boolean flippedDims = aFirst == bFirst;
            //If the first array is a constant and the operation is dot product
            if(aFirst == 1 && aSecond == 1 && op.equals("*")){
                //Set the result length to that of b
                result = new Object[bFirst];
                for (int i = 0; i < bFirst; i++) {
                    //Set the length of each sub array to that of the sub arrays of b
                    result[i] = new TreeNode(new Object[]{"literal", finalBase+"[]", new Object[bSecond]});
                    //For every element in the sub array
                    for (int j = 0; j < bSecond; j++) {
                        //The product of each element of b and the constant
                        double product = Double.parseDouble("" + ((TreeNode) ((Object[]) ((TreeNode) a[0]).getData()[2])[0]).getData()[2]) *
                                Double.parseDouble("" + resolve((TreeNode) ((Object[]) ((TreeNode) b[i]).getData()[2])[j]).getData()[2]);
                        //If both are of int type, set result ot int type
                        if (base1.equals("int") && base2.equals("int"))
                            ((Object[])((TreeNode) result[i]).getData()[2])[j] = new TreeNode(new Object[]{"literal", "int", (int) product});
                        //Set to double otherwise
                        else
                            ((Object[])((TreeNode) result[i]).getData()[2])[j] = new TreeNode(new Object[]{"literal", "double", product});
                    }
                }
            }
            //If a has greater dimensions than 1
            else if (op.equals("*")){
                //Set the result length to that of a
                result = new Object[aFirst];
                for (int i = 0; i < aFirst; i++) {
                    //Set the length of each sub array to that of the sub arrays of b
                    result[i] = new TreeNode(new Object[]{"literal", finalType, new Object[flippedDims ? bFirst : bSecond]});
                    for (int j = 0; j < (flippedDims ? bFirst : bSecond); j++) {
                        //The sum of the products of the particular array elements
                        double sum = 0;
                        //Add each group of products to the sum
                        for (int k = 0; k < (flippedDims ? bSecond : bFirst); k++)
                            sum += Double.parseDouble("" + resolve((TreeNode) ((Object[]) ((TreeNode) a[i]).getData()[2])[k]).getData()[2])
                                    * Double.parseDouble("" + resolve((TreeNode) ((Object[]) ((TreeNode) b[flippedDims ? j : k]).getData()[2])[flippedDims ? k : j]).getData()[2]);
                        //If both are of int type, set result ot int type
                        if (base1.equals("int") && base2.equals("int"))
                            ((Object[]) ((TreeNode) result[i]).getData()[2])[j] = new TreeNode(new Object[]{"literal", "int", (int) sum});
                            //Set to double otherwise
                        else
                            ((Object[]) ((TreeNode) result[i]).getData()[2])[j] = new TreeNode(new Object[]{"literal", "double", sum});
                    }
                }
            }
            if(op.equals("~*")) {
                //Set the result length to that of b
                result = new Object[aFirst];
                //Set the length of each sub array to that of the sub arrays of a
                result[0] = new TreeNode(new Object[]{"literal", finalBase+"[]", new Object[aSecond]});
                for (int k = 0; k < (flippedDims ? bSecond : bFirst); k++) {
                    //Returns the indices from arrays a and b that match the index k in result
                    int[] crossResult = crossIndex(k);
                    //THe product of a and b elements that produce the correct index for result
                    double product = Double.parseDouble("" + resolve((TreeNode) ((Object[]) ((TreeNode) a[0]).getData()[2])[crossResult[0]]).getData()[2])
                            * Double.parseDouble("" + resolve((TreeNode) ((Object[]) ((TreeNode) b[flippedDims ? 0 : crossResult[1]]).getData()[2])[flippedDims ? crossResult[1] : 0]).getData()[2]) -
                            Double.parseDouble("" + resolve((TreeNode) ((Object[]) ((TreeNode) a[0]).getData()[2])[crossResult[1]]).getData()[2])
                                    * Double.parseDouble("" + resolve((TreeNode) ((Object[]) ((TreeNode) b[flippedDims ? 0 : crossResult[0]]).getData()[2])[flippedDims ? crossResult[0] : 0]).getData()[2]);
                    //If both are of int type, set result ot int type
                    if (base1.equals("int") && base2.equals("int"))
                        ((Object[]) ((TreeNode) result[0]).getData()[2])[k] = new TreeNode(new Object[]{"literal", "int", (int) product});
                        //Set to double otherwise
                    else
                        ((Object[]) ((TreeNode) result[0]).getData()[2])[k] = new TreeNode(new Object[]{"literal", "double", product});
                }
            }
        }
        return new TreeNode(new Object[]{"literal", finalType+"[]", result});
    }
    /**Uses the given index to return the correct remaining indices*/
    public static int[] crossIndex(int index){
        if(index == 0) return new int[]{1, 2};
        if(index == 1) return new int[]{2, 0};
        if(index == 2) return new int[]{0, 1};
        return null;
    }
    /**Returns a treeNode representing the result of the given objects and the given operation*/
    public static TreeNode calculateOp(String op, String leftType, String rightType, Object left, Object right) throws SimplexException {
        //If the left operand is a number
        boolean leftIsNum = leftType.equals("int") || leftType.equals("double");
        //If the right operand is a number
        boolean rightIsNum = rightType.equals("int") || rightType.equals("double");
        //If the left operand is a number or an array of numbers
        boolean leftContainsNum = leftType.contains("int") || leftType.contains("double");
        //If the right operand is a number or an array of numbers
        boolean rightContainsNum = rightType.contains("int") || rightType.contains("double");
        //If the left operand is an array
        boolean leftIsArray = leftType.contains("[");
        //If the right operand is an array
        boolean rightIsArray = rightType.contains("[");
        //If the operation is addition
        if (op.equals("+")) {
            //If the operands are both numbers
            if (leftIsNum && rightIsNum)
                //If one of the numbers is a double
                if ((leftType.equals("double") || rightType.equals("double")))
                    //Return the operation as a double
                    return new TreeNode(new Object[]{"literal", "double", Double.parseDouble(""+left) + Double.parseDouble(""+right)});
                //If neither are doubles
                else
                    //Return the operation as an int
                    return new TreeNode(new Object[]{"literal", "int", Integer.parseInt(""+left) + Integer.parseInt(""+right)});
            //If the left type is a string
            else if (leftType.equals("str"))
                //Append the right operand to the left
                return appendStr(new TreeNode(new Object[]{"literal", leftType, left}),
                        new TreeNode(new Object[]{"literal", rightType, right}));
            //If the right type is a string
            else if(rightType.equals("str"))
                //Append the left operand to the right
                return appendStr(new TreeNode(new Object[]{"literal", rightType, right}),
                        new TreeNode(new Object[]{"literal", leftType, left}));
            //If both operands are arrays and one of them is a string
            if ((leftType.equals("str[]") || rightType.equals("str[]")) && leftIsArray && rightIsArray)
                return operateArrays(op, (Object[]) left, (Object[]) right);
        }
        //If the operation is subtraction
        if (op.equals("-")) {
            if (leftIsNum && rightIsNum)
                //Override int type with double
                if ((leftType.equals("double") || rightType.equals("double")))
                    return new TreeNode(new Object[]{"literal", "double", Double.parseDouble(""+left) - Double.parseDouble(""+right)});
                else
                    return new TreeNode(new Object[]{"literal", "int", Integer.parseInt(""+left) - Integer.parseInt(""+right)});
        }
        //If the operation is multiplication
        if (op.equals("*")) {
            if (leftIsNum && rightIsNum)
                //Override int type with double
                if ((leftType.equals("double") || rightType.equals("double")))
                    return new TreeNode(new Object[]{"literal", "double", Double.parseDouble(""+left) * Double.parseDouble(""+right)});
                else
                    return new TreeNode(new Object[]{"literal", "int", Integer.parseInt(""+left) * Integer.parseInt(""+right)});
            if(leftIsNum && rightIsArray){
                TreeNode transform = new TreeNode(new Object[]{"literal", leftType, left});
                transform = new TreeNode(new Object[]{"literal", leftType+"[]", new Object[]{transform}});
                return operateArrays(op, new Object[]{transform}, (Object[]) right);
            }
        }
        //If the operation is division
        if (op.equals("/")) {
            if (leftIsNum && rightIsNum)
                //Override int type with double
                if ((leftType.equals("double") || rightType.equals("double")))
                    return new TreeNode(new Object[]{"literal", "double", Double.parseDouble(""+left) / Double.parseDouble(""+right)});
                else
                    return new TreeNode(new Object[]{"literal", "int", Integer.parseInt(""+left) / Integer.parseInt(""+right)});
        }
        //If the operation is and
        if (op.equals("&&")) {
            if (leftType.equals("bool") && rightType.equals("bool"))
                return new TreeNode(new Object[]{"literal", "bool", Boolean.parseBoolean(""+left) && Boolean.parseBoolean(""+right)});
        }
        //If the operation is or
        if (op.equals("||")) {
            if (leftType.equals("bool") && rightType.equals("bool"))
                return new TreeNode(new Object[]{"literal", "bool", Boolean.parseBoolean(""+left) || Boolean.parseBoolean(""+right)});
        }
        //If the operation is not
        if (op.equals("!")) {
            if (leftType.equals("void") && rightType.equals("bool"))
                return new TreeNode(new Object[]{"literal", "bool", !Boolean.parseBoolean(""+right)});
        }
        //If the operation is xor
        if (op.equals("~|")) {
            if (leftType.equals("bool") && rightType.equals("bool"))
                return new TreeNode(new Object[]{"literal", "bool", Boolean.parseBoolean(""+left) ^ Boolean.parseBoolean(""+right)});
        }
        //If the operation is less than
        if (op.equals("<")) {
            if (leftIsNum && rightIsNum)
                return new TreeNode(new Object[]{"literal", "bool", Double.parseDouble(""+left) < Double.parseDouble(""+right)});
        }
        //If the operation is greater than
        if (op.equals(">")) {
            if (leftIsNum && rightIsNum)
                return new TreeNode(new Object[]{"literal", "bool", Double.parseDouble(""+left) > Double.parseDouble(""+right)});
        }
        //If the operation is less than or equal to
        if (op.equals("<=")) {
            if (leftIsNum && rightIsNum)
                return new TreeNode(new Object[]{"literal", "bool", Double.parseDouble(""+left) <= Double.parseDouble(""+right)});
        }
        //If the operation is greater than or equal to
        if (op.equals(">=")) {
            if (leftIsNum && rightIsNum)
                return new TreeNode(new Object[]{"literal", "bool", Double.parseDouble(""+left) >= Double.parseDouble(""+right)});
        }
        //If the operation checks equality
        if (op.equals("=="))
            return new TreeNode(new Object[]{"literal", "bool", left.equals(right)});

        //Preforms specific operations only if both operands are arrays
        if (leftIsArray && rightIsArray &&
                (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("~*") || op.equals("&&") || op.equals("||") || op.equals("~|")))
            return operateArrays(op, (Object[]) left, (Object[]) right);
        else if (leftType.equals("void") && rightType.contains("bool[]") && op.equals("!"))
            return operateArrays(op, new Object[]{new TreeNode(new Object[]{"literal", "void", "null"})}, (Object[]) right);
        //If no operations are matched, throw an exception
        throw new SimplexException("Type mismatch, '" + op + "' does not accept type '" + leftType + "' with type '" + rightType + "'", 1);
    }
    /**Executes program while modifying source*/
    private static Object[] execute(List<TreeNode> block) throws SimplexException { return execute(block, false); }
    /**Executes clone of program, source is unchanged*/
    private static Object[] executeClone(List<TreeNode> block) throws SimplexException { return execute(block, true); }
    /**Executes the given list of lines, cloning the line if needed*/
    private static Object[] execute(List<TreeNode> block, boolean clone) throws SimplexException {
        //The return value of the execution
        Object[] hasReturn = new Object[]{false, null};
        //For each line in the program
        for(int i=0; i<block.size(); i++) {
            try {
                //Clones the line if clone is set to true
                TreeNode line = clone ? block.get(i).clone() : block.get(i);
                //Skips if the line is null or under formed
                if (line.getData() == null) continue;
                if(line.getData().length < 3) continue;
                //If the line is a while loop
                if (line.getData()[2].equals("while")) {
                    //The argument for the loop
                    TreeNode arg = line.getRight();
                    //If the argument is not a boolean
                    if(!arg.getData()[1].equals("bool")) {
                        if(arg.getData()[0].equals("op")){
                            String returnType = Lexer.opReturn(""+arg.getData()[3], ""+arg.getData()[1], ""+arg.getData()[2]);
                            if(returnType == null || !returnType.equals("bool"))
                                throw new SimplexException("Expected argument type bool in 'while' or 'for'", 1);
                        }
                        else throw new SimplexException("Expected argument type bool in 'while' or 'for'", 1);
                    }
                    //The line number where the loop starts
                    int blockStart = i;
                    //The line number where the loop ends
                    int blockEnd;
                    //Number of open brackets encountered
                    int opens = 1;
                    //Loop until the end of the block has been reached
                    for (blockEnd = blockStart + 2; blockEnd < block.size(); blockEnd++)
                        if (block.get(blockEnd).getData() != null) {
                            if (block.get(blockEnd).getData()[1].equals("{")) opens++;
                            else if (block.get(blockEnd).getData()[1].equals("}")) {
                                if(opens == 1) break;
                                else opens--;
                            }
                        }
                    //The true or false value of the argument
                    TreeNode argValue = resolve(arg.clone());
                        //Loops while the argument value is true
                    while ((boolean) argValue.getData()[2]){
                        //Gets return from execution
                        hasReturn = executeClone(block.subList(blockStart + 2, blockEnd));
                        //If the code inside the loop has a return statement
                        if(hasReturn[0].equals(true))
                            return hasReturn;
                        //Re-evaluate the condition
                        argValue = resolve(arg.clone());
                    }
                    //Set it to the end of the block
                    i = blockEnd;
                }
                //If the line is an if statement
                if (line.getData()[2].equals("if")) {
                    //The condition for the if
                    TreeNode arg = line.getRight();
                    //Simplify the condition
                    resolve(arg);
                    //The start of the if statement
                    int ifStart = i;
                    //The end of the if block or the else block if it is present
                    int blockEnd;
                    //The number of brackets that have been encountered
                    int opens = 0;
                    //Glides to the end of the if block
                    for (blockEnd = ifStart + 1; blockEnd < block.size(); blockEnd++)
                        if (block.get(blockEnd).getData() != null) {
                            if (block.get(blockEnd).getData()[1].equals("{")) opens++;
                            else if (block.get(blockEnd).getData()[1].equals("}")) {
                                if(opens == 1) {
                                    opens = 0;
                                    break;
                                }
                                else opens--;
                            }
                        }
                    //Save the end of the if block
                    int ifEnd = blockEnd;
                        //The potential beginning of the else block
                    int elseBegin = blockEnd + 2;
                    if (blockEnd < block.size() - 1) {
                        //If the else keyword appears immediately after the if block
                        if (block.get(blockEnd + 1).getData()[2].equals("else")) {
                            //Glides to the end of the else block
                            for (blockEnd = blockEnd + 2; blockEnd < block.size(); blockEnd++)
                                if (block.get(blockEnd).getData() != null) {
                                    if (block.get(blockEnd).getData()[1].equals("{")) opens++;
                                    else if (block.get(blockEnd).getData()[1].equals("}")) {
                                        if(opens == 1) break;
                                        else opens--;
                                    }
                                }
                        }
                    }
                    //Sets i to the end of the if or else blocks
                    i = blockEnd;
                    //If the argument is not a boolean
                    if(!arg.getData()[1].equals("bool")) {
                        if(arg.getData()[0].equals("op")){
                            String returnType = Lexer.opReturn(""+arg.getData()[3], ""+arg.getData()[1], ""+arg.getData()[2]);
                            if(returnType == null || !returnType.equals("bool"))
                                throw new SimplexException("Expected argument type bool in 'while' or 'for'", 1);
                        }
                        else throw new SimplexException("Expected argument type bool in 'while' or 'for'", 1);
                    }
                    //If the condition is true
                    if ((boolean) (arg).getData()[2])
                        //if block
                        //Returns if return statement is inside block
                        hasReturn = execute(block.subList(ifStart + 1, ifEnd));
                    else if (blockEnd > elseBegin)
                        //else block
                        // Returns if return statement is inside block
                        hasReturn = execute(block.subList(elseBegin, blockEnd));
                    //Returns if a return statement was encountered
                    if(hasReturn[0].equals(true))
                        return hasReturn;

                }
                //If the line is a variable or array element assignment
                else if (line.getData().length > 3 && line.getData()[3].equals("=")) {
                    //The simplified form of the item being assigned a value to
                    TreeNode leftResolved = resolve(line.getLeft().clone());
                    TreeNode rightResolved = resolve(line.getRight());
                    //If the types of the left and right sides are equal
                    if (/*(*/leftResolved.getData()[1].equals(rightResolved.getData()[1]) /*&& line.getRight().getData()[0].equals("literal")) ||
                            (leftResolved.getData()[1].equals(line.getRight().getData()[2]) && !line.getRight().getData()[0].equals("literal"))*/) {
                        //If the variable is an array
                        if (("" + Interpreter.vars.get(""+line.getLeft().getData()[2])[0]).contains("["))
                            retrieveArray((Object[]) Interpreter.vars.get(""+line.getLeft().getData()[2])[1], line.getLeft()).copy(rightResolved);
                        //If the variable is not an array
                        else
                            Interpreter.vars.get(""+line.getLeft().getData()[2])[1] = rightResolved.getData()[2];
                    }
                }
                //If the current line is function call
                else if (line.getData()[0].equals("func")) {
                    //THe arguments for the function
                    Object[] args = (Object[]) line.getRight().getData()[1];
                    //Stores the type of each argument
                    String[] argTypes = new String[args.length];
                    //Fills in argTypes with the proper types
                    for(int j=0; j<argTypes.length; j++) {
                        //If the arg is not an operation
                        if(!((TreeNode) args[j]).getData()[0].equals("op"))
                            argTypes[j] = (String) ((TreeNode) args[j]).getData()[1];
                        //If the arg is an operation
                        else
                            //Assigns the result of the operation as the type
                            argTypes[j] = Lexer.opReturn(""+((TreeNode) args[j]).getData()[3],
                                    ""+((TreeNode) args[j]).getData()[1], ""+((TreeNode) args[j]).getData()[2]);

                    }
                    //If the types given to the function match
                    if(!funcTypeCheck((String) line.getData()[3], argTypes) && !(line.getData()[3].equals("help") &&
                            argTypes.length == 0)) {
                        //The name of the function
                        String funcName = (String) line.getData()[3];
                        //The args that were declared at function creation
                        String[] declaredArgs = new String[((Object[]) Interpreter.funcs.get(funcName)[1]).length];
                        for(int j=0; j<declaredArgs.length; j++)
                            declaredArgs[j] = ""+((TreeNode)((Object[]) Interpreter.funcs.get(funcName)[1])[j]).getData()[1];
                        //Throw exception
                        throw new SimplexException("'"+funcName.substring(funcName.indexOf(".")+1)+"()' expected args " +
                                Arrays.toString(declaredArgs) + " but received args " +
                                Arrays.toString(argTypes), 1);
                    }
                    //Simplify arguments
                    for (Object arg : args) {
                        Interpreter.actionTree((TreeNode) arg);
                        resolve((TreeNode) arg);
                    }
                    //The body of the function
                    ArrayList<TreeNode> body = (ArrayList<TreeNode>) Interpreter.funcs.get(""+line.getData()[3])[2];
                    //If the function is a builtin function
                    if (body.get(0).getData()[0].equals("builtin")) {
                        //Copy of the args to be modified
                        Object[] argCopy = args.clone();
                        //The types of each arg
                        String[] types = new String[argCopy.length];
                        for (int j = 0; j < argCopy.length; j++) {
                            //Fills in the proper types
                            types[j] = (String) ((TreeNode) argCopy[j]).getData()[1];
                            //Turns each arg into the data contained in that arg
                            argCopy[j] = ((TreeNode) argCopy[j]).getData()[2];
                        }
                        //Execute function
                        hasReturn = Util.executeUtil((String) body.get(0).getData()[1], types, argCopy);
                    } else {
                        //The arguments that were declared with the function
                        Object[] declaredArgArray = new Object[0];
                        if(((Object[]) line.getData()[2]).length != 0)
                            declaredArgArray = (Object[]) ((Object[]) line.getData()[2])[1];
                        //THe parameters passed into this instance of the function
                        Object[] givenArgArray = new Object[0];
                        if(line.getRight().getData().length != 0)
                            givenArgArray = (Object[]) line.getRight().getData()[1];
                        //Include the function in the stackFrame
                        Interpreter.stackFrame.push((String) line.getData()[1]);
                        //Update the variables with the given values
                        for (int j = 0; j < declaredArgArray.length; j++) {
                            TreeNode declaredArgTree = (TreeNode) declaredArgArray[j];
                            TreeNode givenArgTree = (TreeNode) givenArgArray[j];
                            Interpreter.vars.put((String) declaredArgTree.getData()[2], new Object[]{givenArgTree.getData()[1], givenArgTree.getData()[2]});
                        }
                        //Execute body of function
                        hasReturn = execute(body);
                        //Release function from stackFrame
                        Interpreter.stackFrame.pop();
                    }
                }
                //If the line is an assignment or return operation
                else if (line.getData().length > 3 && line.getData()[3].equals(":")) {
                    //If the line is a return operation
                    if (line.getLeft().getData()[2].equals("return")) {
                        //Simplify the node being returned
                        resolve(line.getRight());
                        hasReturn[0] = true;
                        //Update  return value
                        hasReturn[1] = line.getRight();
                        return hasReturn;
                    }
                    //If the line is an assignment
                    if (Lexer.primitives.contains("" + line.getData()[2]) || Lexer.primitives.contains(("" + line.getData()[2]).substring(0, ("" + line.getData()[2]).indexOf("[")))) {
                        //The name of the variable being assigned
                        String varName = (String) line.getRight().getLeft().getData()[2];
                        //THe simplified variable
                        TreeNode resolved = resolve(line.getRight().getRight());
                        //The declared type of the variable
                        String declared = (String) Interpreter.vars.get(varName)[0];
                        //The type given ot the assignment
                        String actual = (String) resolved.getData()[1];
                        //If the actual variable type does not match the declared variable type
                        if(!actual.equals(declared) && !declared.contains("any")) {
                            throw new SimplexException("Actual type '" + actual + "' does not match declared type '" + declared + "'", 1);
                        }
                        //Assign the new value to the variable
                        Interpreter.vars.put(varName, new Object[]{Interpreter.vars.get(varName)[0], resolved.getData()[2]});
                    }
                }
                else if(!line.getData()[0].equals("assignFunc") &&
                        !(line.getData().length > 2 && (line.getData()[2].equals("else") || line.getData()[2].equals("while") ||
                                line.getData()[2].equals("for"))))
                    throw new SimplexException("Nothing to execute", 1);
            } catch (SimplexException e) {
                //Add line number to any exception that has been reached
                throw new SimplexException(e.getError()+" at line "+(Lexer.lineMappings.get(i+1)), e.getCode());
            }
        }

        return hasReturn;
    }
    /**Uses indices to get each successive sub-array or item from the given array and returns that item
     * [[1, 3],[5, 2]][0][1] -> 3*/
    public static TreeNode retrieveArray(Object[] array, TreeNode indexes) throws SimplexException {
        //The first index to get
        TreeNode indexRecur = indexes.getRight();
        //The element that resides at the first index
        TreeNode recur = (TreeNode) array[(int) resolve(indexRecur).getData()[2]];
        //Loops while there are still more indices to go through
        while (!indexRecur.isLeaf()){
            //The nex next index
            indexRecur = indexes.getRight();
            //The nex next item
            recur = (TreeNode) array[(int) resolve(indexRecur).getData()[2]];
        }
        //Return the deepest needed item
        return recur;
    }

    /**Converts strings to array of single character strings*/
    public static void convertStr(TreeNode block){
        //If the tata item is a java string
        if(block.getData()[2].getClass().toString().equals("class java.lang.String"))
            //Convert to array of characters
            block.getData()[2] = ((String) block.getData()[2]).split("");
    }

    /**Adds the character elements contained in 'append' to the elements contained in 'str'*/
    public static TreeNode appendStr(TreeNode str, TreeNode append){
        //The array of strings contained in append
        String[] appendArr;
        //If append is of type string
        if(append.getData()[1].equals("str"))
            //Set to the data of append
            appendArr = (String[]) append.getData()[2];
        //If append is an array type
        else if(((String)append.getData()[1]).contains("["))
            //String representation of array
            appendArr = Util.stringTreeArray((Object[]) append.getData()[2]).split("");
        //If append is not a string type
        else
            //Convert the data to a string array and set appendArr to its value
            appendArr = ("" + append.getData()[2]).split("");
        //The string array being appended by the other
        String[] strArr = ((String[])str.getData()[2]);
        String[] newStr = new String[strArr.length+appendArr.length];
        //Copy contents of str and append to newStr
        System.arraycopy(strArr, 0, newStr, 0, strArr.length);
        System.arraycopy(appendArr, 0, newStr, strArr.length, appendArr.length);
        //Format as treeNode and return
        return new TreeNode(new Object[]{"literal", "str", newStr});
    }
    /**Checks if the given argument types are applicable o the given function*/
    public static boolean funcTypeCheck(String funcName, String[] argsTypes){
        //The declared args for the function
        Object[] funcArgs = (Object[])Interpreter.funcs.get(funcName)[1];
        //If the to argument arrays are not the same length
        if(funcArgs.length != argsTypes.length) return false;
        //Checks if every given arg is applicable to the declared arg
        for(int i=0; i<funcArgs.length; i++) {
            //If the types are not equal and the declared type is not any
            if (!((TreeNode)funcArgs[i]).getData()[1].equals(argsTypes[i]) && !((TreeNode)funcArgs[i]).getData()[1].equals("any"))
                //If the two types are not a combination of double and int
                if((!((TreeNode)funcArgs[i]).getData()[1].equals("double") && !argsTypes[i].equals("int"))
                        && (!((TreeNode)funcArgs[i]).getData()[1].equals("double") && !argsTypes[i].equals("int")))
                    return false;
        }
        return true;
    }
}
