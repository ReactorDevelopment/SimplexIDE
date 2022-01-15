package com.reactordevelopment.simplex;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.reactordevelopment.simplex.simplexLang.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**Represents treeNodes as a dropdown with data on the first level, and its branches on the second*/
public class Dropdown extends LinearLayout {
    /**The view for the dropdown*/
    private final LinearLayout view;
    /**The list of textViews tht represent the data*/
    private final ArrayList<View> data;
    /**The left branch of the tree*/
    private final Dropdown left;
    /**The right branch of the tree*/
    private final Dropdown right;
    /**Marks if the dropdown is open*/
    private boolean mainOpen = false;
    /**Marks if the data is open*/
    private boolean dataOpen = false;
    /**Marks if the data is open*/
    private boolean leftOpen = false;
    /**Marks if the data is open*/
    private boolean rightOpen = false;
    /**Initializes the title, data, and branches*/
    public Dropdown(Context context, String title, List<Object> initialData, Dropdown initialLeft, Dropdown initialRight) {
        super(context);
        view = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.dropdown, this, true);
        data = new ArrayList<>();
        LinearLayout rightHolder = view.findViewById(R.id.rightHolder);
        LinearLayout leftHolder = view.findViewById(R.id.leftHolder);
        left = initialLeft;
        right = initialRight;
        ((TextView)view.findViewById(R.id.title)).setText(title);
        //Add the branches
        if(left != null) {
            ((LinearLayout) view.findViewById(R.id.leftHolder)).addView(left);
            view.findViewById(R.id.leftDropLayout).setVisibility(VISIBLE);
        }
        if(right != null) {
            ((LinearLayout) view.findViewById(R.id.rightHolder)).addView(right);
            view.findViewById(R.id.rightDropLayout).setVisibility(VISIBLE);
        }
        //Add listener to open and close dropdown
        view.findViewById(R.id.drop).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mainOpen = !mainOpen;
                if(mainOpen) view.findViewById(R.id.mainDropdown).setVisibility(VISIBLE);
                else closeAll();
            }
        });
        //Add listener to open and close left section
        view.findViewById(R.id.leftDrop).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                leftOpen = !leftOpen;
                if(leftOpen)
                    leftHolder.setVisibility(VISIBLE);
                else
                    leftHolder.setVisibility(GONE);
            }
        });
        //Add listener to open and close right section
        view.findViewById(R.id.rightDrop).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rightOpen = !rightOpen;
                if(rightOpen)
                    rightHolder.setVisibility(VISIBLE);
                else
                    rightHolder.setVisibility(GONE);
                Log.i("Right", rightOpen ? "Open" : "Closed");
            }
        });
        //Add listener to open and close data section
        view.findViewById(R.id.dataDrop).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dataOpen = !dataOpen;
                if(dataOpen) {
                    view.findViewById(R.id.dataDropdown).setVisibility(VISIBLE);
                    if(data.size() == 0){
                        if(initialData != null)
                            //Turn raw data into group of Views
                            friendlyAdd(initialData);
                        LinearLayout dataDropdown = view.findViewById(R.id.dataDropdown);
                        //Adds textViews to layout
                        for(View dataView : data)
                            dataDropdown.addView(dataView);
                    }
                }
                else
                    view.findViewById(R.id.dataDropdown).setVisibility(GONE);
            }
        });

    }
    /**Initializes dropdown with empty data and branches*/
    public Dropdown(Context context, String title){
        this(context, title, null, null, null);
    }
    /**Initializes dropdown with the data from the given treeNode*/
    public Dropdown(Context context, TreeNode tree){
        this(context, "", Arrays.asList(tree.getData()), tree.getLeft() != null ? new Dropdown(context, tree.getLeft()) : null, tree.getRight() != null ? new Dropdown(context, tree.getRight()) : null);
        Object item = tree.getData().length != 2 ? tree.getData()[tree.getData().length-1] : tree.getData()[1];
        if(item.getClass().toString().contains("["))
            ((TextView)view.findViewById(R.id.title)).setText(R.string.objectArr);
        else
            ((TextView)view.findViewById(R.id.title)).setText(""+item);
    }
    /**Adds all the data in the given list, and adds textViews to 'data' with the string representations of those objects*/
    public void friendlyAdd(List<Object> raw){
        //Height and width of textViews
        int textHeight = 50;
        int textWidth = 200;
        //For each value in raw
        for(Object obj : raw){
            //Get class of object
            String objClass = (""+obj.getClass()).substring(6);
            View dataItem = new TextView(view.getContext());
            //Compare class of item to known classes and do correct operation
            switch (objClass){
                //Convert to string
                case "java.lang.String":
                    ((TextView) dataItem).setText(view.getContext().getString(R.string.str)+": \""+obj+"\"");
                    dataItem.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, textHeight));
                    break;
                case "java.lang.Integer":
                    ((TextView) dataItem).setText(view.getContext().getString(R.string.itgr)+": "+obj);
                    dataItem.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, textHeight));
                    break;
                case "java.lang.Boolean":
                    ((TextView) dataItem).setText(view.getContext().getString(R.string.bool)+": "+obj);
                    dataItem.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, textHeight));
                    break;
                case "java.lang.Double":
                    ((TextView) dataItem).setText(view.getContext().getString(R.string.dubl)+": "+obj);
                    dataItem.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, textHeight));
                    break;
                //Use as treeNode or array
                case "[Ljava.lang.Object;":
                    dataItem = new Dropdown(view.getContext(), "Object[]", Arrays.asList((Object[]) obj), null, null);
                    dataItem.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    break;
                case "[Ljava.lang.String;":
                    dataItem = new Dropdown(view.getContext(), "String[]", Arrays.asList((Object[]) obj), null, null);
                    dataItem.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    break;
                case "com.reactordevelopment.simplex.simplexLang.TreeNode":
                    dataItem = new Dropdown(view.getContext(), (TreeNode) obj);
                    dataItem.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    break;
                default:
                    ((TextView) dataItem).setText(view.getContext().getString(R.string.any)+": "+obj.toString());
            }
            dataItem.setMinimumWidth(textWidth);
            Log.i("Friendly Add", obj+", |"+objClass);
            //Add item to data
            data.add(dataItem);
        }
    }
    //Closes all dropdowns in hierarchy
    public void closeAll(){
        if(left !=null)
            left.closeAll();
        if(right !=null)
            right.closeAll();
        view.findViewById(R.id.dataDropdown).setVisibility(GONE);
        view.findViewById(R.id.leftHolder).setVisibility(GONE);
        view.findViewById(R.id.rightHolder).setVisibility(GONE);
        view.findViewById(R.id.mainDropdown).setVisibility(GONE);
        dataOpen = false;
        mainOpen = false;
    }
}
