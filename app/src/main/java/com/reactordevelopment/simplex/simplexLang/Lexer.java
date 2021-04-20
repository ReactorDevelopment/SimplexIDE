package com.reactordevelopment.simplex.simplexLang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Lexer {
    public static final List<String> delimiters = Arrays.asList("\"", "'");

    public static final List<String> operators = Arrays.asList("(", ")", "[", "]", "{", "}", "+", "-", "*", "/", "=", ":", "&&", "||", "<", ">", "<=", ">=", "==");

    public static final List<String> mathOperators = Arrays.asList("+", "-", "*", "/", "&&", "||", "<", ">", "<=", ">=", "==");
    
    //[op, [inType], returnType]
    public static final HashMap<String, String[]> contextMathOps = new HashMap<>();
    static {
        contextMathOps.put("+", new String[]{ "str, int, double", "input" });
        contextMathOps.put("-", new String[]{ "int, double", "input" });
        contextMathOps.put("*", new String[]{ "int, double", "input" });
        contextMathOps.put("/", new String[]{ "int, double", "input" });
        contextMathOps.put("&&", new String[]{ "bool", "bool" });
        contextMathOps.put("||", new String[]{ "bool", "bool" });
        contextMathOps.put("<", new String[]{ "str, int, double, bool", "bool" });
        contextMathOps.put(">", new String[]{ "str, int, double, bool", "bool" });
        contextMathOps.put("<=", new String[]{ "str, int, double, bool", "bool" });
        contextMathOps.put(">=", new String[]{ "str, int, double, bool", "bool" });
        contextMathOps.put("==", new String[]{ "str, int, double, bool", "bool" });
    }
    public static final List<String> oneSidedOps = Arrays.asList("!", "@");

    public static final List<String> primitives = Arrays.asList("int", "bool", "double", "void", "str");

    public static final List<String> reserved = Arrays.asList("func", "return", "if", "else", "null");

    private static ArrayList<Object[]> tokens;

    public static ArrayList<Object[]> lexer(String unLexed) throws SimplexException {
        tokens = new ArrayList<>(0);
        //String[] split = unLexed.split(" ");
        int startToken = 0;
        char tokenAt;
        int lineStart = 0;
        int lineNum = 1;
        for (int i = 0; i <= unLexed.length(); i++) {
            tokenAt = unLexed.charAt(startToken);
            if(tokenAt == '#'){
                i = unLexed.indexOf("\n", startToken);
                if(i == -1) break;
                startToken = i+1;
                continue;
            }
            if(tokenAt == ' ' || tokenAt == '\r'){
                startToken = i;
                continue;
            }
            boolean doubleOpCheck = false;
            if(startToken < unLexed.length()-1)
                doubleOpCheck = operators.contains(""+unLexed.substring(startToken, startToken+2));
            if(delimiters.contains(""+tokenAt) || operators.contains(""+tokenAt) ||
                    doubleOpCheck ||
                    tokenAt == '\n' || tokenAt == ';' || tokenAt == ','){

                if (/*unLexed.substring(startToken, i).equals("\n") ||*/ unLexed.substring(startToken, i).equals(";")) {
                    try{
                        checkErrors(lineStart);
                    } catch (SimplexException e) {
                        throw new SimplexException(e.getError()+" at line "+lineNum, e.getCode());
                    }
                    tokens.add(new Object[]{"newline", unLexed.substring(startToken, i)});
                    lineStart = tokens.size();
                    lineNum ++;
                }

                else if (tokenAt == ']' && tokens.size() > 1) {
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
                    else tokens.add(new Object[]{"op", unLexed.substring(startToken, i)});

                }

                /*else if (i < unLexed.length()-1)
                    if(operators.contains(unLexed.substring(startToken, i+1))){
                        tokens.add(new Object[]{"op", unLexed.substring(startToken, i+1)});
                        i++;
                    }*/

                else if (operators.contains(unLexed.substring(startToken, i))){
                    boolean doubleOp = false;
                        if (i < unLexed.length()-1)
                            if(operators.contains(unLexed.substring(startToken, i+1))){
                                tokens.add(new Object[]{"op", unLexed.substring(startToken, i+1)});
                                i++;
                                doubleOp = true;
                            }
                    if(!doubleOp)
                        tokens.add(new Object[]{"op", unLexed.substring(startToken, i)});
                }

                else if (unLexed.substring(startToken, i).equals(",")) tokens.add(new Object[]{"separator", unLexed.substring(startToken, i)});

                else if (delimiters.contains(unLexed.substring(startToken, i))) {
                    i++;
                    startToken ++;
                    String combined;
                    if(i-1 < unLexed.length())
                        while (unLexed.charAt(i - 1) != '"' && unLexed.charAt(i - 1) != '\'') {
                            i++;
                            if(i-1 == unLexed.length()) break;
                        }

                    combined = unLexed.substring(startToken, i-1);
                    //combined = combined.substring(0, combined.length() - 1);
                    tokens.add(new Object[]{"str", combined});
                }
                startToken = i;
                continue;
            }
            if(i < unLexed.length())
                while(!delimiters.contains("" + unLexed.charAt(i)) && !operators.contains("" + unLexed.charAt(i)) &&
                        unLexed.charAt(i) != '\n' && unLexed.charAt(i) != ' ' && unLexed.charAt(i) != ';' && unLexed.charAt(i) != ',') {
                    i++;
                    if(i >= unLexed.length()) break;
                }

            try {
                tokens.add(new Object[]{"int", Integer.parseInt(unLexed.substring(startToken, i))});
                startToken = i;
                continue;
            } catch (Exception ignored) { }

            try {
                tokens.add(new Object[]{"double", Double.parseDouble(unLexed.substring(startToken, i))});
                startToken = i;
                continue;
            } catch (Exception ignored) { }

            if (unLexed.substring(startToken, i).equals("true"))
                tokens.add(new Object[]{"bool", true});
            else if (unLexed.substring(startToken, i).equals("false")) tokens.add(new Object[]{"bool", false});



            else if (primitives.contains(unLexed.substring(startToken, i))) tokens.add(new Object[]{"primitive", unLexed.substring(startToken, i)});

            else if (reserved.contains(unLexed.substring(startToken, i))) tokens.add(new Object[]{"reserved", unLexed.substring(startToken, i)});

            else tokens.add(new Object[]{"id", unLexed.substring(startToken, i)});

            startToken = i;

            /*String s = "" + unLexed.charAt(i);
            boolean newAlpha = false;
            String lastS = tokens.size() > 0 ? ""+tokens.get(tokens.size()-1)[1] : "";
            if((unLexed.charAt(i) > 47 && unLexed.charAt(i) < 58) || (unLexed.charAt(i) > 64 && unLexed.charAt(i) < 91) ||
                    (unLexed.charAt(i) > 96 && unLexed.charAt(i) < 123))
                if(delimiters.contains(lastS)  || operators.contains(lastS) || lastS.equals("\n"))
                    newAlpha = true;
            if (delimiters.contains(s) || operators.contains(s) || s.equals("\n") || newAlpha) {

            }*/
        }

        return tokens;
    }

    public static void checkErrors(int lineStart) throws SimplexException {
        List<Object[]> line = tokens.subList(lineStart, tokens.size());
        int size = line.size();
        int index = 0;
        if((index = innerSearch(line, 1, "=")) != -1){
            if(index == size-1)
                throw new SimplexException("Expected expression after '='", 1);

            
            if(index > 0)
                if(!line.get(index-1)[0].equals("id") && !line.get(index-1)[1].equals("]"))
                    throw new SimplexException("Expected variable before '='", 1);


            if(index > 1)
                if(!line.get(index-2)[0].equals("op") && !line.get(index-1)[1].equals("]"))
                    throw new SimplexException("Variable assignment not allowed here", 1);


            if(index > 2)
                if(line.get(index-2)[1].equals(":") && !line.get(index-3)[0].equals("primitive"))
                    throw new SimplexException("Expected type before ':'", 1);
        }

        if((index = innerSearch(line, 1, "func")) != -1){
            if(index < size-1)
                if(!line.get(index+1)[1].equals(":"))
                    throw new SimplexException("Expected ':' after 'func'", 1);


            if(index < size-2)
                if(!line.get(index+2)[0].equals("primitive"))
                    throw new SimplexException("Expected type after ':'", 1);


            if(index < size-3)
                if(!line.get(index+3)[1].equals(":"))
                    throw new SimplexException("Expected ':' after return type type", 1);


            if(index < size-4)
                if(!line.get(index+4)[0].equals("id"))
                    throw new SimplexException("Expected identifier as function name", 1);


            if(index < size-5)
                if(!line.get(index+5)[1].equals("("))
                    throw new SimplexException("Expected '(' after function name", 1);

        }

        for (int i = 0; i < size-1; i++) {
            if((line.get(i)[0].equals("id") || primitives.contains(line.get(i)[0])) &&
                    (line.get(i+1)[0].equals("id") || primitives.contains(line.get(i+1)[0])))
                throw new SimplexException("Expected operation between tokens", 1);
        }
    }

    private static int innerSearch(List<Object[]> list, int index, Object search){
        for (int i = 0; i < list.size(); i++) {
            Object[] token = list.get(i);
            if (token[index].equals(search)) return i;
        }
        return -1;
    }
}


