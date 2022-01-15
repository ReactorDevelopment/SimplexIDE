package com.reactordevelopment.simplex.simplexLang;

import java.util.*;

public class Util {
    /**The wrapper for printing, calls its print() method whenever printing to console is needed
     * Method of printing is declared at the start of the program*/
    private static PrintWrapper printWrapper;
    /**Mathematical constants to be referenced by consts()*/
    public static final HashMap<String, TreeNode> constants = new HashMap<>();
    static {
        //Pi
        constants.put("pi", new TreeNode(new Object[]{"literal", "double", Math.PI}));
        //e
        constants.put("e", new TreeNode(new Object[]{"literal", "double", Math.E}));
        //Speed of light in a vacuum in m/s
        constants.put("c", new TreeNode(new Object[]{"literal", "int", 299792458}));
        //Acceleration due to gravity at Earth's surface in m/s^2
        constants.put("g", new TreeNode(new Object[]{"literal", "double", 9.81}));
        //Universal gravitational constant in N*m^2/kg^2
        constants.put("G", new TreeNode(new Object[]{"literal", "double", 6.67430*Math.pow(10, -11)}));
        //Electron charge in Coulombs
        constants.put("ec", new TreeNode(new Object[]{"literal", "double", 1.602176634 	*Math.pow(10, -19)}));
        //Atmospheric pressure at sea level in Pascals
        constants.put("atm", new TreeNode(new Object[]{"literal", "int", 101325}));
        constants.put("MAX_INT", new TreeNode(new Object[]{"literal", "int", Integer.MAX_VALUE}));
        constants.put("MIN_INT", new TreeNode(new Object[]{"literal", "int", Integer.MIN_VALUE}));
    }
    /**Initializes the printWrapper and adds built in functions to function list*/
    public static void initUtil(PrintWrapper wrapper){
        printWrapper = wrapper;
        //Add built ins
        //Prints all of the given arguments to the console
        addUtil("print", "void", "any");
        //Returns the value of the requested constant
        addUtil("consts", "any", "any");
        //Returns an identity matrix of the given size
        addUtil("identity", "int[][]", "int");
        //Returns the determinate of the given matrix
        addUtil("det", "double", "any");
        //Returns the inverse matrix of the given matrix
        addUtil("inv", "any", "any");
        //Returns the adjoint matrix of the given matrix
        addUtil("adj", "any", "any");
        //Returns the sine of the given radian value
        addUtil("sin", "double", "double");
        //Returns the sine of the given degree value
        addUtil("sinD", "double", "double");
        //Returns the cosine of the given radian value
        addUtil("cos", "double", "double");
        //Returns the cosine of the given degree value
        addUtil("cosD", "double", "double");
        //Returns the tangent of the given radian value
        addUtil("tan", "double", "double");
        //Returns the tangent of the given degree value
        addUtil("tanD", "double", "double");
        //Returns the 'astley' string
        addUtil("astley", "str", "");
        //Displays help information
        addUtil("help", "void", "str");
    }
    /**Adds a built in function with the given name, return type, and args to the function list*/
    private static void addUtil(String name, String returnType, String...argTypes){
        //The body of the function
        List<TreeNode> body = new ArrayList<>(0);
        //Mark that the function is a built in
        body.add(new TreeNode(new Object[]{"builtin", name}));
        //Treat "" as an argument list of length 0
        if(argTypes.length == 1 && argTypes[0].equals("")) argTypes = new String[0];
        //THe declared arguments
        Object[] args = new Object[argTypes.length];
        //Fill in the declared arguments
        for(int i=0; i<argTypes.length; i++)
            args[i] = new TreeNode(new Object[]{"var", argTypes[i], ""});
        //Add function to list
        Interpreter.funcs.put(name, new Object[]{returnType, args, body});
    }
    /**Executes the functionality of the given function with the given arguments*/
    public static Object[] executeUtil(String func, String[] types, Object[] args) throws SimplexException {
        //If the util is print()
        if(func.equals("print")) {
            //The string to printed
            String printStr = "";
            //For every arg in the list
            for(int i=0; i<args.length; i++) {
                //If the argument is an array type
                if (types[i].contains("[")) {
                    printStr = stringTreeArray((Object[]) args[i]);
                }
                //If the argument is a str
                else if (types[i].equals("str"))
                    for (String s : (String[]) args[i])
                        printStr += s;
                //If the argument is a java array
                else if (args[i].getClass().toString().contains("["))
                    //Stringify java array
                    printStr = strList((Object[]) args[i]);
                //Stringify element
                else printStr = args[i].toString();
            }
            //Print the string using the method defined in the wrapper
            printWrapper.print(printStr + "\n");
        }
        //If the util is consts()
        if(func.equals("consts")){
            String constant = "";
            if (types[0].equals("str"))
                //Convert the str (an array of string characters) to a java string
                for (String s : (String[]) args[0])
                    constant += s;
            //If the argument is contained in consts
            if (constants.containsKey(constant))
                //Return the value of that constant
                return new Object[]{true, constants.get(constant)};

        }
        //If the util is identity()
        if(func.equals("identity")) {
            //THe size of the requested identity matrix
            int size = (int) args[0];
            //The top level of the matrix
            Object[] matrix = new Object[size];
            //Fill in the values of the identity matrix
            for (int i = 0; i < size; i++) {
                //Fill in the sub-array
                matrix[i] = new TreeNode(new Object[]{"literal", "int[]", new Object[size]});
                //Give each element in the sub-array a 1 or 0 value
                for (int j = 0; j < size; j++)
                    ((Object[])((TreeNode)matrix[i]).getData()[2])[j] = new TreeNode(new Object[]{"literal", "int", i == j ? 1 : 0});
            }
            //Return the completed matrix
            return new Object[]{true, new TreeNode(new Object[]{"literal", "int[][]", matrix})};
        }
        //If the util is det()
        if(func.equals("det")) {
            //The size of the square matrix
            int size = ((Object[])((TreeNode)((Object[])args[0])[0]).getData()[2]).length;
            //Return the determinate of the matrix
            return new Object[]{true, determinantOfMatrix(size, (Object[]) args[0])};
        }
        //If the util is inv()
        if(func.equals("inv")) {
            //The size of the square matrix
            int size = ((Object[])((TreeNode)((Object[])args[0])[0]).getData()[2]).length;
            //The inverse of the matrix
            TreeNode inverse = Executor.calculateOp("*", "double", types[0], 1/Double.parseDouble(""+determinantOfMatrix(size, (Object[]) args[0]).getData()[2]), adjoint(size, (Object[]) args[0]));
            return new Object[]{true, inverse};
        }
        //If the util is adj()
        if(func.equals("adj")) {
            //The size of the square matrix
            int size = ((Object[])((TreeNode)((Object[])args[0])[0]).getData()[2]).length;
            Object[] adjoint = adjoint(size, (Object[]) args[0]);
            //Return the determinate of the matrix
            return new Object[]{true, new TreeNode(new Object[]{"literal", ((TreeNode)adjoint[0]).getData()[1]+"[]", adjoint})};
        }
        //If the util is sin()
        if(func.equals("sin"))
            return new Object[]{true, new TreeNode(new Object[]{"literal", "double", Math.sin(Double.parseDouble(""+args[0]))})};
        //If the util is sinD()
        if(func.equals("sinD"))
            return new Object[]{true, new TreeNode(new Object[]{"literal", "double", Math.sin(Double.parseDouble(""+args[0])*Math.PI/180)})};
        //If the util is cos()
        if(func.equals("cos"))
            return new Object[]{true, new TreeNode(new Object[]{"literal", "double", Math.cos(Double.parseDouble(""+args[0]))})};
        //If the util is cosD()
        if(func.equals("cosD"))
            return new Object[]{true, new TreeNode(new Object[]{"literal", "double", Math.cos(Double.parseDouble(""+args[0])*Math.PI/180)})};
        //If the util is tan()
        if(func.equals("tan"))
            return new Object[]{true, new TreeNode(new Object[]{"literal", "double", Math.tan(Double.parseDouble(""+args[0]))})};
        //If the util is tanD()
        if(func.equals("tanD"))
            return new Object[]{true, new TreeNode(new Object[]{"literal", "double", Math.tan(Double.parseDouble(""+args[0])*Math.PI/180)})};
        //If the util is astley()
        if(func.equals("astley"))
            return new Object[]{true, new TreeNode(new Object[]{"literal", "str", RICK.split("")})};
        //If the util is help()
        if(func.equals("help")) {
            if(args.length == 0) args = new Object[]{new Object[]{""}};
            String argStr = "";
            for (Object s : (Object[]) args[0])
                argStr += s;
            args[0] = argStr.replace("()", "");
            TreeNode helpText;
            if(args[0].equals("print"))
                helpText = new TreeNode(new Object[]{"literal", "str", "print(str) -> void\nPrints out a string representation of the given object"});
            else if(args[0].equals("consts")) {
                helpText = new TreeNode(new Object[]{"literal", "str", "consts(str) -> num\nReturns a number corresponding to the inputted string"});
                helpText.getData()[2] += "\n\t'pi' -> The value of pi (3.14...)" +
                        "\n\t'e' -> The value of e (2.78...)" +
                        "\n\t'c' -> The speed of light in a vacuum in m/s (299792458)" +
                        "\n\t'g' -> The acceleration due to gravity at Earth's surface in m/s^2 in m/s (9.81)" +
                        "\n\t'G' -> The universal gravitational constant in N*m^2/kg^2 (6.67430*10^-11)" +
                        "\n\t'ec' -> The electron charge in Coulombs (1.602176634*10^-19)" +
                        "\n\t'atm' -> The atmospheric pressure at sea level in Pascals (101325)" +
                        "\n\t'MAX_INT' -> The highest value that can be contained within a 32 bit integer (2147483647)" +
                        "\n\t'MIN_INT' -> The lowest value that can be contained within a 32 bit integer (-2147483648)";
            }
            else if(args[0].equals("identity"))
                helpText = new TreeNode(new Object[]{"literal", "str", "identity(int) -> int[][]\nReturns an identity matrix of the given size"});
            else if(args[0].equals("det"))
                helpText = new TreeNode(new Object[]{"literal", "str", "det(num[][]) -> double\nReturns the determinate of the given square matrix"});
            else if(args[0].equals("inv"))
                helpText = new TreeNode(new Object[]{"literal", "str", "inv(int[][]) -> int[][]\nReturns the inverse of the given matrix"});
            else if(args[0].equals("adj"))
                helpText = new TreeNode(new Object[]{"literal", "str", "adj(int[][]) -> int[][]\nReturns the adjoint of the given matrix"});
            else if(args[0].equals("sin"))
                helpText = new TreeNode(new Object[]{"literal", "str", "sin(double) -> double\nReturns sine of the given number in radians"});
            else if(args[0].equals("sinD"))
                helpText = new TreeNode(new Object[]{"literal", "str", "sinD(double) -> double\nReturns sine of the given number in degrees"});
            else if(args[0].equals("cos"))
                helpText = new TreeNode(new Object[]{"literal", "str", "cos(double) -> double\nReturns cosine of the given number in radians"});
            else if(args[0].equals("cosD"))
                helpText = new TreeNode(new Object[]{"literal", "str", "cosD(double) -> double\nReturns cosine of the given number in degrees"});
            else if(args[0].equals("tan"))
                helpText = new TreeNode(new Object[]{"literal", "str", "tan(double) -> double\nReturns tangent of the given number in radians"});
            else if(args[0].equals("tanD"))
                helpText = new TreeNode(new Object[]{"literal", "str", "tanD(double) -> double\nReturns tangent of the given number in degrees"});
            else if(args[0].equals("astley"))
                helpText = new TreeNode(new Object[]{"literal", "str", "astley() -> str\n███████ █████████ ██ ██████████████"});
            else if(args[0].equals("help") || args[0].equals(""))
                helpText = new TreeNode(new Object[]{"literal", "str", "help(str) -> str\nReturns help information for any of the builtin functions" +
                        "\n\tprint(), consts(), identity(), det(), inv(), adj(), sin(), sinD(), cos(), cosD(), tan(), tanD(), astley(), help()"});
            else
                helpText = new TreeNode(new Object[]{"literal", "str", "No builtin function matching '"+args[0]+"()'"});
            Executor.convertStr(helpText);
            return new Object[]{true, helpText};
        }
        return new Object[]{false, null};
    }
    /**Returns the string representation of a given array of treeNodes*/
    public static String stringTreeArray(Object[] treeArray){
        //The string representation of the array
        String printStr = "[";
        //For every element in the array
        for(Object tree : treeArray){
            //If the element is another treeNode array
            if(tree.getClass().toString().equals("class [Ljava.lang.Object;"))
                //Add string of element from recursive call
                printStr += stringTreeArray((Object[]) tree)+", ";
            //If the element is a treeNode
            else if(tree.getClass().toString().equals("class simplexLang.TreeNode"))
                //If the data for the treeNode is an array
                if(((TreeNode)tree).getData()[2].getClass().toString().equals("class [Ljava.lang.Object;"))
                    //Add the result of the recursive call
                    printStr += stringTreeArray((Object[]) ((TreeNode)tree).getData()[2])+", ";
                else
                    //Add the string representation of the treeNode data
                    printStr += ((TreeNode)tree).getData()[2]+", ";
            else
                //Add the string representation of whatever the element may be
                printStr += tree+", ";
        }
        //Cap off the string with a final bracket
        return printStr.substring(0, printStr.lastIndexOf(",")) + "]";
    }
    /**Returns the string representation of a java array*/
    public static String strList(Object[] array){
        //The string representation of the array
        String printStr = "[";
        for(Object o : array)
            //Add element to stirng
            printStr += o.toString()+", ";
        //Cap off the string with a final bracket
        return printStr.substring(0, printStr.lastIndexOf(",")) + "]";
    }
    /**Counts the number of times the search stirng appears in str*/
    public static int stringOccurrences(String str, String search){
        int count = 0, fromIndex = 0;
        //Counts occurrences of stirng
        while ((fromIndex = str.indexOf(search, fromIndex)) != -1 ){
            count++;
            fromIndex++;
        }
        return count;
    }
    /**Function to get adjoint matrix of the given matrix with the given size*/
    public static Object[] adjoint(int size, Object[] matrix) {
        //int[] or double[]
        String lowerType = ""+((TreeNode)matrix[0]).getData()[1];
        //The upper level of the adjoint matrix
        Object[] adjoint = new Object[size];
        //If the size is 1
        if (size == 1) {
            //Return a base matrix ([[1]])
            adjoint[0] = new TreeNode(new Object[]{"literal", lowerType, new Object[size]});
            ((Object[])((TreeNode)adjoint[0]).getData()[2])[0] = 1;
            return adjoint;
        }
        //Stores cofactors of matrix
        Object[] temp = new Object[size];
        //Fill in null elements
        for (int i = 0; i < size; i++) {
            temp[i] = new TreeNode(new Object[]{"literal", lowerType, new Object[size]});
            adjoint[i] = new TreeNode(new Object[]{"literal", lowerType, new Object[size]});
            //For each element in the sub-array
            for (int j = 0; j < size; j++) {
                //Fill element with zeroes
                ((Object[])((TreeNode)adjoint[i]).getData()[2])[j] = new TreeNode(new Object[]{"literal", lowerType.substring(0, lowerType.indexOf("[")), 0});
                ((Object[])((TreeNode)temp[i]).getData()[2])[j] = new TreeNode(new Object[]{"literal", lowerType.substring(0, lowerType.indexOf("[")), 0});
            }
        }
        //The sign of the current element
        int sign;
        //For each element in the arrays
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                //Get cofactor of matrix[i][j]
                getCofactor(size, matrix, temp, i, j);

                //Sign of adj[j][i] positive if sum of row and column indexes is even
                sign = ((i + j) % 2 == 0)? 1: -1;

                //Get the correct element to go at the current index
                Object element = (sign)*Double.parseDouble(""+determinantOfMatrix(size-1, temp).getData()[2]);
                ((Object[])((TreeNode)adjoint[j]).getData()[2])[i] = new TreeNode(new Object[]{"literal", lowerType.substring(0, lowerType.indexOf("[")), element});
            }
        }
        return adjoint;
    }
    // Function to get cofactor of A[p][q] in temp[][]. n is current
    // dimension of A[][]
    /**Modifies the temp matrix into the cofactor of the given matrix*/
    public static void getCofactor(int size, Object[] matrix, Object[] temp, int reservedRow, int reservedCol) {
        int i = 0, j = 0;

        // Looping for each element of the matrix
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                //Copy from matrix into temp as long as the row and column pair is not reserved
                if (row != reservedRow && col != reservedCol) {
                    ((Object[])((TreeNode)temp[i]).getData()[2])[j++] = ((Object[])((TreeNode)matrix[row]).getData()[2])[col];
                    //Increase row index when it is filled
                    if (j == size - 1) {
                        j = 0;
                        i++;
                    }
                }
            }
        }
    }
    /**Returns the determinant of the given matrix with the given size*/
    public static TreeNode determinantOfMatrix(int size, Object[] matrix) {
        //Initialize the determinant as a treeNode
        TreeNode determinate = new TreeNode(new Object[]{"literal", "double", 0.0});
        //If the matrix is of size 1
        if (size == 1)
            //Return the base element of the matrix
            return (TreeNode) ((Object[])((TreeNode)matrix[0]).getData()[2])[0];

        //To store cofactors
        Object[] temp = new Object[size];
        for (int i = 0; i < size; i++) {
            //Initialize sub-arrays
            temp[i] = new TreeNode(new Object[]{"literal", "int[]", new Object[size]});
            //Fill in sub-arrays with zeroes
            for (int j = 0; j < size; j++)
                ((Object[])((TreeNode)temp[i]).getData()[2])[j] = new TreeNode(new Object[]{"literal", "double", 0});
        }
        //To store sign of element
        int sign = 1;

        //For each element in the matrix
        for (int i = 0; i < size; i++) {
            //Getting cofactor of matrix[0][i]
            getCofactor(size, matrix, temp, 0, i);
            //Add partial determinate to determinate
            determinate.getData()[2] = ((double)determinate.getData()[2]) + sign *
                    Double.parseDouble(""+((TreeNode)((Object[])((TreeNode)matrix[0]).getData()[2])[i]).getData()[2]) *
                    Double.parseDouble(""+determinantOfMatrix(size-1, temp).getData()[2]);

            //Switch sign to opposite value
            sign = -sign;
        }

        return determinate;
    }
    /**Stores the method that will be used to print to console
     * print() is called whenever printing is needed*/
    public interface PrintWrapper{
        void print(String s);
    }
    /**Ascii art of Rick Astley*/
    public static final String RICK =
            "you know the rules...\n" +
            "            `````````````````````````````````````.,-~”””’~–,,_\n" +
            "            ````````````````.. `````````````.,-~”-,::::::::::::”-,\n" +
            "            ````````````````.. ```````````..,~”::::::’,:::::::::::|’,\n" +
            "            ````````````````.. ```````````..|::::::~”’___””~~–~”’:}\n" +
            "            ````````````````.. ```````````..’|:::::|: : : : :  : :\n" +
            "            ````````````````.. ```````````..|:::::|: ~~—: : : —–: |\n" +
            "            ````````````````.. ```````````.(_”~-’: : : : : : : : |\n" +
            "            ````````````````.. ```````````..”’~-,|: :: ~—’: : :,’ never gonna\n" +
            "            ````````````````.. `````````````|,: : : : :-~~–: : /   give you up!\n" +
            "            ````````````````.. ````````````.,-”\\\\’:\\\\:’~,,_: -’\n" +
            "            ````````````````.. ```````.__,-’;;;;;\\\\: :”-,~—~”/|       never gonna\n" +
            "            ````````````````.. ````.__,-~”;;;;;;;;;\\\\: :\\\\: :____/’,__   let you down\n" +
            "            ````````````````.. .,-~~~””_’;;,. .”-,:|::::|. . |;;;  ”-,__\n" +
            "            ````````````````../;;;;;;;;;;;;\\\\. . .”|::::|. .,’;;;;;;;”-,      never gonna run around\n" +
            "            ````````````````,’ ;;;;;;;;;;;;;\\\\. . .\\\\:::,’. ./|;;;;;;;;|      and desert you\n" +
            "            ```````````````,-”;;;;;;;;;;;;;;;;;’,: : _|. . .|;;;;;;,’;;|\n" +
            "            ``````````````.,-”;;;;;’;;;; ;;;; \\\\. . |::|. .”,;;;;;;;|;;/\n" +
            "            ``````````````/;;;;;;;;;;;;\\\\;;;;;;\\\\. .|::|.  . |;;;;;;;|/\n" +
            "            `````````````./;;;;;;;;;;;;;;,;; ;;;|. .\\\\/. . .|;;;;;;;;;;|\n" +
            "            `````````````/;;;;;;;;;;;;;;; ;;;;;;”,: |;|. . . \\\\;;;;;;;|\n" +
            "            ````````````,~”;;;; ;;;;;;;;;;;\\\\;;;;;;|.|;|. . . .|;;;;;;;|\n" +
            "            ``````````..,~”;;;;;;;; ;;;;;;;;;;,-’;;;|:|. . . |\\\\;;;;;;;|\n" +
            "            ``````````.,’;;;;;;;;;;; ;;;;;;;/;;;,-| |:|. . .’|;;’,;;;;;;|\n" +
            "            ``````````|;,-’;;;;;;;;;;;;;,-’;;;,;-’;| |:|. . .,’;;’,;;;;;;|_\n" +
            "            ``````````/;;;;;;;;;;;,-’_;;;;;;,’;;;;|.|:|. . .|;;;;;;;;;;|””~-,\n" +
            "            `````````./;;;;;;;;;;/_”`,;;;,’;;;;;;| |:|. . ./;;;;;;;;;;;;|-,,__\n" +
            "            ````````../;;;;;;;,-’```|;;,;;;;;;;;| |:|._,-’;;;;;;;;;|;;;;|;;;;;;;”’-,_\n" +
            "            ````````/;;;;;;;;,-’```.,’;;,;;;;;;;;;;;|.|:|::”’~–~”’;;;||;;;;;|;;;;;;;,-~””~–,\n" +
            "            ```````.,’;;;;;;;;,’````/;;;;;;;;;;; ;;|.|:|::::::::::::::|;’,;;-,: : :”’~-,:”’~~–,\n" +
            "            ```````/;;;;;;;,-’````,’;;;;;;;;;;;;;;;|:|:|::::::::’,;;;;|_””~–,,-~,___,-~~”’__”~-\n" +
            "            ``````,-’;;;;;,’`````../;;;;;;;;;;;;;; ;;;;|:|:::::::::|;;;;|`````````````”-,\\\\_”-,”~\n" +
            "            ``````/;;;;;;;;/``````.,-’;;;;;;;;;;;;;;;;;;;;;;::::::|;;;;;|````.";
}
