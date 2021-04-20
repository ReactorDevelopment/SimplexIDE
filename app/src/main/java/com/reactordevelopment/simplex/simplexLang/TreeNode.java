package com.reactordevelopment.simplex.simplexLang;

public class TreeNode<E> {

    private E data;
    private TreeNode<E> left, right, parent;

    public TreeNode(E initialData, TreeNode<E> initialLeft, TreeNode<E> initialRight/*, TreeNode<E> initialParent*/) {
        data = initialData;
        left = initialLeft;
        right = initialRight;
        /*parent = initialParent;*/
    }

    public E getData( ) {
        return data;
    }

    public TreeNode<E> getParent( ) {
        return parent;
    }

    public TreeNode<E> getRoot(){
        if (parent == null)
            return this;
        else
            return parent.getRoot();
    }

    public TreeNode<E> getLeft( ) {
        return left;
    }

    public TreeNode<E> getLeftmost( ) {
        if (left == null)
            return this;
        else
            return left.getLeftmost( );
    }


    public TreeNode<E> getRight( ) {
        return right;
    }

    public TreeNode<E> getRightmost( ) {
        if (right == null)
            return this;
        else
            return right.getRightmost( );
    }

    public void inorderPrint( ) {
        if (left != null)
            left.inorderPrint( );
        System.out.println(data);
        if (right != null)
            right.inorderPrint( );
    }


    public boolean isLeaf( ) {
        return (left == null) && (right == null);
    }


    public void preorderPrint( ) {
        System.out.println(data.toString());
        if (left != null)
            left.preorderPrint( );
        if (right != null)
            right.preorderPrint( );
    }


    public void postorderPrint( ) {
        if (left != null)
            left.postorderPrint( );
        if (right != null)
            right.postorderPrint( );
        System.out.println(data.toString());
    }

    public String toString(int depth) {
        int i;
        String toStrung = "";
        // Print the indentation and the data from the current node:
        String indent = "";
        String singleIndent = "    ";
        for (i = 1; i <= depth; i++)
            indent += singleIndent;
        toStrung += indent;
        String type = ""+data.getClass();
        type = type.substring(type.indexOf("class")+6);
        if(type.charAt(0) == '[') {
            toStrung += "Data " + arrayRecur(indent, singleIndent, (Object[]) data);
        }
        else toStrung += "Data\n"+indent+singleIndent+data.toString()+"\n";
        // Print the left subtree (or a dash if there is a right child and no left child)
        if (left != null)
            toStrung += left.toString(depth+1);
        else if (right != null)
        {
            for (i = 1; i <= depth+1; i++)
                toStrung += "    ";
            toStrung += "--\n";
        }

        // Print the right subtree (or a dash if there is a left child and no left child)
        if (right != null)
            toStrung += right.toString(depth+1);
        else if (left != null)
        {
            for (i = 1; i <= depth+1; i++)
                toStrung += "    ";
            toStrung += "--\n";
        }

        return toStrung;
    }
    public String arrayRecur(String indent, String singleIndent, Object[] array){
        String toStrung = "Array\n";
        for (Object item : array) {
            String type = ""+item.getClass();
            type = type.substring(type.indexOf("class")+6);
            if(type.charAt(0) == '[')
                toStrung += indent + singleIndent + arrayRecur(indent+singleIndent, singleIndent, (Object[]) item)+"\n";
            else
                toStrung += indent + singleIndent + item.toString()+"\n";
        }

        return toStrung;
    }

    public String toString(){
        return toString(8);
    }

    public TreeNode<E> removeLeftmost( ) {
        if (left == null)
            return right;
        else
        {
            left = left.removeLeftmost( );
            return this;
        }
    }


    public TreeNode<E> removeRightmost( ) {
        if (right == null)
            return left;
        else
        {
            right = right.removeRightmost( );
            return this;
        }
    }

    public void setData(E newData) {
        data = newData;
    }


    public void setLeft(TreeNode<E> newLeft) {
        left = newLeft;
    }


    public void setRight(TreeNode<E> newRight) {
        right = newRight;
    }

    public void copy(TreeNode<E> template){
        data = template.data;
        right = template.right;
        left = template.left;
    }
    public TreeNode<E> clone(){
        return new TreeNode<>(data, left == null ? null : left.clone(), right == null ? null : right.clone());
    }
    public static <E> TreeNode<E> treeCopy(TreeNode<E> source) {
        TreeNode<E> leftCopy, rightCopy;

        if (source == null)
            return null;
        else
        {
            leftCopy = treeCopy(source.left);
            rightCopy = treeCopy(source.right);
            return new TreeNode<E>(source.data, leftCopy, rightCopy);
        }
    }

    public static <E> long treeSize(TreeNode<E> root) {
        if (root == null)
            return 0;
        else
            return 1 + treeSize(root.left) + treeSize(root.right);
    }

}

