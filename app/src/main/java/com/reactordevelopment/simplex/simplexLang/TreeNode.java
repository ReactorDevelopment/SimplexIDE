package com.reactordevelopment.simplex.simplexLang;

import java.util.Arrays;
/**One node of a binary tree*/
public class TreeNode {
    /**The data stored in the treeNode*/
    private Object[] data;
    /**The right and left branches*/
    private TreeNode left, right;
    /**Initializes a blank treeNode*/
    public TreeNode() {
        data = new Object[]{};
        left = null;
        right = null;
    }
    /**Initializes treeNode with the given data, but no branches*/
    public TreeNode(Object[] initialData) {
        data = initialData;
        left = null;
        right = null;
    }
    /**Initializes treeNode with the given data and the given right and left branches*/
    public TreeNode(Object[] initialData, TreeNode initialLeft, TreeNode initialRight) {
        data = initialData;
        left = initialLeft;
        right = initialRight;
    }
    /**Returns the data stored*/
    public Object[] getData() {return data;}
    /**Sets the data to the given value*/
    public void setData(Object[] newData) {data = newData;}
    /**Returns the left branch*/
    public TreeNode getLeft() {return left;}
    /**Returns the leftmost branch*/
    public TreeNode getLeftmost() {
        if (left == null)
            return this;
        else
            return left.getLeftmost();
    }
    /**Sets the left branch to the given value*/
    public void setLeft(TreeNode newLeft) {left = newLeft;}
    /**Sets the leftmost branch to the given value*/
    public void setLeftmost(TreeNode leftmost) {
        if (left == null)
            left = leftmost;
        else
            left.setLeftmost(leftmost);
    }
    /**Returns the right branch*/
    public TreeNode getRight() {return right;}
    /**Returns the rightmost branch*/
    public TreeNode getRightmost() {
        if (right == null)
            return this;
        else
            return right.getRightmost();
    }
    /**Sets the right branch to the given value*/
    public void setRight(TreeNode newRight) {right = newRight;}
    /**Sets the rightmost branch to the given value*/
    public void setRightmost(TreeNode rightmost) {
        if (right == null)
            right = rightmost;
        else
            right.setRightmost(rightmost);
    }
    /**Returns if the treeNode is a leaf (no right or left branches)*/
    public boolean isLeaf() {return (left == null) && (right == null);}
    /**Set the values to those of the template treeNode*/
    public void copy(TreeNode template){
        data = template.data;
        right = template.right;
        left = template.left;
    }
    /**Returns a complete clone of the current treeNode*/
    public TreeNode clone(){
        return new TreeNode(cloneData(data), left == null ? null : left.clone(), right == null ? null : right.clone());
    }
    /**Clones the data, so not ties are left to the original*/
    public Object[] cloneData(Object[] toClone){
        //Skip if the data is null
        if(toClone == null) return null;
        //Initialize cloned array
        Object[] cloned = new Object[toClone.length];
        for(int i=0; i<toClone.length; i++){
            //If the element is itself an array
            if(toClone[i] != null && toClone[i].getClass().toString().contains("["))
                //Recursive clone
                cloned[i] = cloneData((Object[]) toClone[i]);
            //If the element is a treeNode
            else if(toClone[i] != null && toClone[i].getClass().toString().equals("class simplexLang.TreeNode"))
                //Clone the treeNode
                cloned[i] = ((TreeNode)toClone[i]).clone();
            //If the element is immutable
            else
                cloned[i] = toClone[i];
        }
        return cloned;
    }
    /**Returns the string representation of the tree
     * [data] {left} {right}*/
    public String toString(){
        String s = Arrays.toString(data) +": ";
        s += left != null ? "{ "+left+" }, " : "{ }, ";
        s += right != null ? "{ "+right+" }": "{ }";
        return s;
    }
}

