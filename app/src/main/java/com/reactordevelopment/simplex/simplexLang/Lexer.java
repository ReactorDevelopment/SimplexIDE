package com.reactordevelopment.simplex.simplexLang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Lexer {
    /**Characters that signify a string*/
    public static final List<String> delimiters = Arrays.asList("\"", "'");
    /**Characters that preform operations or regulate them*/
    public static final List<String> operators = Arrays.asList("(", ")", "[", "]", "{", "}", "+", "-", "*", "~*", "/", "=", ":", "&&", "||", "<", ">", "<=", ">=", "==", "!", "~|");
    /**Subset of operators that preform mathematical operations*/
    public static final List<String> mathOperators = Arrays.asList("+", "-", "*", "~*", "/", "&&", "||", "<", ">", "<=", ">=", "==", "!", "~|");

    /**The expected inputs and outputs for all mathematical operations*/
    public static final HashMap<String, String[]> contextMathOps = new HashMap<>();
    static {
        contextMathOps.put("+", new String[]{"str,any,str", "num,num,num", "any[],any[],any[]"});
        contextMathOps.put("-", new String[]{"num,num,num", "num[],num[],num[]"});
        contextMathOps.put("*", new String[]{"num,num,num", "num[],num[],num[]"});
        contextMathOps.put("~*", new String[]{"num[],num[],num[]"});
        contextMathOps.put("/", new String[]{"num,num,num", "num[],num[],num[]"});
        contextMathOps.put("!", new String[]{"void,bool,bool", "void,bool[],bool[]"});
        contextMathOps.put("&&", new String[]{"bool,bool,bool", "bool[],bool[],bool[]"});
        contextMathOps.put("||", new String[]{"bool,bool,bool", "bool[],bool[],bool[]"});
        contextMathOps.put("~|", new String[]{"bool,bool,bool", "bool[],bool[],bool[]"});
        contextMathOps.put("<", new String[]{"num,num,bool", "num[],num[],bool"});
        contextMathOps.put(">", new String[]{"num,num,bool", "num[],num[],bool"});
        contextMathOps.put("<=", new String[]{"num,num,bool", "num[],num[],bool"});
        contextMathOps.put(">=", new String[]{"num,num,bool", "num[],num[],bool"});
        contextMathOps.put("==", new String[]{"any,any,bool", "any[],any[],bool"});
    }
    /**List of primitive types*/
    public static final List<String> primitives = Arrays.asList("int", "bool", "double", "void", "str", "any");
    /**List of reserved words*/
    public static final List<String> reserved = Arrays.asList("func", "return", "if", "else", "null", "while", "for");
    /**The lexed (tokenized) form of the given string*/
    private static ArrayList<Object[]> tokens;
    /**Mapseach executable line of the code to the line it appears to be in the editor*/
    public static final HashMap<Integer, Integer> lineMappings = new HashMap<>();
    /**Converts the given string to a list of useful tokens
     * Loops through every character and adds the character and its type to {@link Lexer#tokens}*/
    public static ArrayList<Object[]> lexer(String unLexed) throws SimplexException {
        tokens = new ArrayList<>(0);
        //Append newline to string if it does not end with one
        if(unLexed.charAt(unLexed.length()-1) != ';' && unLexed.charAt(unLexed.length()-1) != '\n')
            unLexed += ";";
        //Strip return characters
        unLexed = unLexed.replace("\r", "");
        //Strip duplicate new line characters
        unLexed = unLexed.replace(";\n", ";");
        //The index where the current token starts at
        int startToken = 0;
        //The char at the current string index
        char charAt;
        //Index where the current line starts
        int lineStart = 0;
        //The line number
        int lineNum = 1;
        //The number of lines of executable code
        int codeLineNum = 1;
        //If loop is inside index block [0, 2, ...]->[0:2]<-
        int inIndex = -1;
        //Loops through every character
        for (int i = 0; i <= unLexed.length(); i++) {
            charAt = unLexed.charAt(startToken);
            //Skips line if the first character signifies a comment
            if(charAt == '#'){
                lineNum ++;
                //Skip to next line
                i = unLexed.indexOf("\n", startToken);
                if(i == -1) break;
                startToken = i+1;
                continue;
            }
            //Skips over any spaces
            if(charAt == ' '){
                startToken = i;
                continue;
            }
            //Checks if the the current token is an operator of length 2
            boolean doubleOpCheck = false;
            if(startToken < unLexed.length()-1)
                doubleOpCheck = operators.contains(""+unLexed.substring(startToken, startToken+2));
            //If the current token is an operator or delimiter
            if(delimiters.contains(""+charAt) || operators.contains(""+charAt) ||
                    doubleOpCheck || charAt == '\n' || charAt == ';' || charAt == ','){
                //Marks the start of an array
                if(charAt == '[')
                    inIndex = tokens.size();
                //Checks for any errors and adds line to lines if the current token is a newline
                if (unLexed.substring(startToken, i).equals("\n") || unLexed.substring(startToken, i).equals(";")) {
                    try{
                        checkErrors(lineStart);
                    } catch (SimplexException e) {
                        throw new SimplexException(e.getError()+" at line "+Lexer.lineMappings.get(lineNum), e.getCode());
                    }
                    tokens.add(new Object[]{"newline", unLexed.substring(startToken, i)});
                    lineStart = tokens.size();
                    lineMappings.put(codeLineNum, lineNum);
                    lineNum ++;
                    codeLineNum++;
                }
                //If the end of an array is reached
                else if (charAt == ']' && tokens.size() > 1) {
                    //If the array was the index marker for an array or string ([0, 2, ...]->[0:2]<-)
                    if(inIndex != -1 && inIndex < tokens.size() && tokens.get(inIndex)[1].equals(":"))
                        //Completes wrap contents of the index marker in parentheses to maintain order of operations
                        tokens.add(new Object[]{"op", ")"});
                    //Resets marker
                    inIndex = -1;
                    //Collapses {"primitive", "["} into "primitive[]" to avoid confusion with arrays
                    if(tokens.get(tokens.size()-2)[0].equals("primitive") && tokens.get(tokens.size()-1)[1].equals("[")) {
                        String compound = tokens.get(tokens.size() - 2)[1] + "[]";
                        tokens.remove(tokens.size() - 1);
                        tokens.remove(tokens.size() - 1);
                        tokens.add(new  Object[]{"primitive", compound});
                    }
                    else if (tokens.get(tokens.size()-1)[1].equals("[")){
                        tokens.add(new Object[]{"void", "null"});
                        tokens.add(new Object[]{"op", unLexed.substring(startToken, i)});
                    }
                    //Adds '[' to tokens list
                    else tokens.add(new Object[]{"op", unLexed.substring(startToken, i)});

                }
                //If the current token is an operator
                else if (operators.contains(unLexed.substring(startToken, i))
                        || (i<unLexed.length()-1 && operators.contains(unLexed.substring(startToken, i+1)))){
                    //If the operator has a length of 2 ex. '<='
                    boolean doubleOp = false;
                    //If inside an index marker with a ':'
                    if(charAt == ':' && inIndex != -1){
                        //Each side of ':' in parentheses to preserve order of operations
                        tokens.add(inIndex+1, new Object[]{"op", "("});
                        tokens.add(new Object[]{"op", ")"});
                        tokens.add(new Object[]{"op", ":"});
                        tokens.add(new Object[]{"op", "("});
                        inIndex = tokens.size()-2;
                    }
                    // Checks if the operator has a length of 2 ex. '<='
                    else if (i < unLexed.length()-1) {
                        if (operators.contains(unLexed.substring(startToken, i + 1))) {
                            //Adds double length operator to tokens list
                            tokens.add(new Object[]{"op", unLexed.substring(startToken, i + 1)});
                            i++;
                            doubleOp = true;
                        }
                    }
                    //Adds single length operator to tokens list
                    if(!doubleOp && !(charAt == ':' && inIndex != -1))
                        tokens.add(new Object[]{"op", unLexed.substring(startToken, i)});
                }
                //Adds ',' as a separator token
                else if (unLexed.substring(startToken, i).equals(",")) tokens.add(new Object[]{"separator", unLexed.substring(startToken, i)});
                //If the current token is the beginning of a string
                else if (delimiters.contains(unLexed.substring(startToken, i))) {
                    i++;
                    startToken ++;
                    //Loops until the end of the string is reached
                    String combined;
                    if(i-1 < unLexed.length())
                        while (unLexed.charAt(i - 1) != '"' && unLexed.charAt(i - 1) != '\'') {
                            i++;
                            if(i-1 == unLexed.length()) break;
                        }
                    //Add the string to the tokens list
                    combined = unLexed.substring(startToken, i-1);
                    tokens.add(new Object[]{"str", combined});
                }
                startToken = i;
                continue;
            }
            //The current token is the beginning of an id/primitive, loop until the end of the id is reached
            if(i < unLexed.length())
                while(!delimiters.contains("" + unLexed.charAt(i)) && !operators.contains("" + unLexed.charAt(i)) &&
                        unLexed.charAt(i) != '\n' && unLexed.charAt(i) != ' ' && unLexed.charAt(i) != ';' && unLexed.charAt(i) != ',') {
                    i++;
                    if(i >= unLexed.length()) break;
                }
            //Adds the current token if it is an integer
            try {
                tokens.add(new Object[]{"int", Integer.parseInt(unLexed.substring(startToken, i))});
                startToken = i;
                continue;
            } catch (Exception ignored) { }
            //Adds the current token if it is a double
            try {
                tokens.add(new Object[]{"double", Double.parseDouble(unLexed.substring(startToken, i))});
                startToken = i;
                continue;
            } catch (Exception ignored) { }
            //Adds the current token if it is a boolean
            if (unLexed.substring(startToken, i).equals("true"))
                tokens.add(new Object[]{"bool", true});
            else if (unLexed.substring(startToken, i).equals("false")) tokens.add(new Object[]{"bool", false});
            //Adds the current token if it is contained in the primitives list
            else if (primitives.contains(unLexed.substring(startToken, i))) tokens.add(new Object[]{"primitive", unLexed.substring(startToken, i)});
            //Adds the current token if it is contained in the reserved list
            else if (reserved.contains(unLexed.substring(startToken, i))) tokens.add(new Object[]{"reserved", unLexed.substring(startToken, i)});
            //Adds the current token as an id
            else tokens.add(new Object[]{"id", unLexed.substring(startToken, i)});
            //set the start of the next token as the end of this token
            startToken = i;
        }
        //Convert for loops to whiles
        forToWhile();
        return tokens;
    }
    /**Returns the return type of the given operation that takes the two given types an inputs*/
    public static String opReturn(String op, String leftType, String rightType){
        String[] context = contextMathOps.get(op);
        boolean leftMatch;
        boolean rightMatch;
        for(String s : context){
            String leftDeclared = s.substring(0, s.indexOf(","));
            String rightDeclared = s.substring(s.indexOf(",")+1, s.lastIndexOf(","));
            leftMatch = leftType.equals(leftDeclared) || (leftDeclared.equals("num") && (leftType.equals("int") || leftType.equals("double")));
            rightMatch = rightType.equals(rightDeclared) || (rightDeclared.equals("num") && (rightType.equals("int") || rightType.equals("double")));
            if(leftMatch && rightMatch) return s.substring(s.lastIndexOf(",")+1);
        }
        return null;
    }
    /**Checks for any errors in syntax in the given string,
     * throws an exception if there is*/
    public static void checkErrors(int lineStart) throws SimplexException {
        //All the tokens starting at the given line
        List<Object[]> line = tokens.subList(lineStart, tokens.size());
        //The number of token in the line
        int size = line.size();
        int index;
        //If the line contains '='
        if((index = innerSearch(line, 1, "=")) != -1){
            //If '=' is the last token in a line
            if(index == size-1)
                throw new SimplexException("Expected expression after '='", 1);
            //If there isn't an id or an array before '='
            if(index > 0)
                if(!line.get(index-1)[0].equals("id") && !line.get(index-1)[1].equals("]"))
                    throw new SimplexException("Expected variable or array before '='", 1);
            //If a variable is assigned without a proceeding operator and
            //the variable isn't the first token in the line
            if(index > 1)
                if(!line.get(index-2)[0].equals("op") && !line.get(index-1)[1].equals("]"))
                    throw new SimplexException("Variable assignment not allowed here", 1);
            //If variable declaration isn't formatted properly
            if(index > 2)
                if(line.get(index-2)[1].equals(":") && !line.get(index-3)[0].equals("primitive"))
                    throw new SimplexException("Expected type before ':'", 1);
        }
        //If the line contains a function declaration
        if((index = innerSearch(line, 1, "func")) != -1){
            //If there is no ':' after the 'func' keyword
            if(index < size-1 && !line.get(index+1)[1].equals(":") || size == 1)
                throw new SimplexException("Expected ':' after 'func'", 1);
            //If there is no primitive after 'func: '
            if(index < size-2  && !line.get(index+2)[0].equals("primitive") || size == 2)
                throw new SimplexException("Expected return type after 'func:'", 1);
            //If there is no ':' after 'func: type'
            if(index < size-3 && !line.get(index+3)[1].equals(":") || size == 3)
                throw new SimplexException("Expected ':' after return type type", 1);
            //If there is no function name after 'func: type:'
            if(index < size-4 && !line.get(index+4)[0].equals("id") || size == 4)
                throw new SimplexException("Expected identifier as function name", 1);
            //If there is no '(' after 'func: type: name'
            if(index < size-5 && !line.get(index+5)[1].equals("(") || size == 5)
                throw new SimplexException("Expected '(' after function name", 1);
        }
        //Loops through every token in the line, checking for two primitives or id's next ot each other
        for (int i = 0; i < size-1; i++) {
            if((line.get(i)[0].equals("id") || primitives.contains(""+line.get(i)[0])) &&
                    (line.get(i+1)[0].equals("id") || primitives.contains(""+line.get(i+1)[0])))
                throw new SimplexException("Expected operation between tokens", 1);
        }
    }
    /**Searches for a given object within a list of tokens and their types
     * Returns if the given object is in the first or second position within the token,
     * specified by {@code inToken}*/
    private static int innerSearch(List<Object[]> list, int inToken, Object search){
        for (int i = 0; i < list.size(); i++) {
            Object[] token = list.get(i);
            if (token[inToken].equals(search)) return i;
        }
        return -1;
    }
    /**Converts for syntax to while syntax*/
    private static void forToWhile(){
        //Loops through every token in tokens
        for(int i=0; i<tokens.size(); i++){
            //If the current token is the 'for' keyword
            if(tokens.get(i)[1].equals("for")){
                //Replaces 'for' with 'while'
                tokens.get(i)[1] = "while";
                //The beginning of the for loop
                int forStart = i;
                //Set i to the index of the first statement
                i += 2;
                //Moves each element in the first statement to before the for loop
                // ex. ('for(int: i=0;...)' -> 'int: i=0; for(...)'
                while (!tokens.get(i)[0].equals("newline")) {
                    Object[] token = tokens.get(i);
                    tokens.remove(i);
                    tokens.add(forStart, token);
                    i++;
                    forStart ++;
                }
                //Transports ';' to before for loop
                Object[] token = tokens.get(i);
                tokens.remove(i);
                tokens.add(forStart, token);
                i++;
                //Loops until i is at the start of the third statement
                while (!tokens.get(i)[0].equals("newline"))
                    i++;
                //Remove ';'
                tokens.remove(i);
                //Saves the start of the third statement
                int saveI = i;
                //Number of open '{' encountered
                int opens = 0;
                //Loops until i is at the end of the for loop
                // ex. ('for(...; ->i<- =i+1){' -> 'for(...){... -><-} ')
                while (true){
                    if(tokens.get(i)[1].equals("{"))
                        opens ++;
                    if(tokens.get(i)[1].equals("}")) {
                        opens--;
                        if(opens == 0)
                            break;
                    }
                    i++;
                }
                //Saves index of the end of the loop
                int endLoop = i-1;
                //Restores i to the beginning of the third statement
                i = saveI;
                //Transport the third statement to the end of the loop
                // ex. ('for(...;i=i+1){' -> 'for(...){...;i=i+1;}')
                while (true) {
                    token = tokens.get(i);
                    tokens.remove(i);
                    tokens.add(endLoop, token);

                    if(tokens.get(i)[1].equals("("))
                        opens ++;
                    if(tokens.get(i)[1].equals(")")) {
                        opens--;
                        if(opens == -1)
                            break;
                    }
                }
            }
        }
    }
}


