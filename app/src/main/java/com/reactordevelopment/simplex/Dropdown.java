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

public class Dropdown extends LinearLayout {
    private LinearLayout displayLayout;
    private LinearLayout dataDropdown;
    private LinearLayout view;
    private ArrayList<View> data;
    private Dropdown left;
    private Dropdown right;
    private boolean mainOpen = false;
    private boolean dataOpen = false;

    public Dropdown(Context context, String title, List<Object> initialData, Dropdown initialLeft, Dropdown initialRight) {
        super(context);
        view = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.dropdown, this, true);
        data = new ArrayList<>();

        if(initialData != null)
            friendlyAdd(initialData);

        left = initialLeft;
        right = initialRight;
        ((TextView)view.findViewById(R.id.title)).setText(title);
        displayLayout = view.findViewById(R.id.display);
        dataDropdown = view.findViewById(R.id.dataDropdown);

        for(View v : data)
            dataDropdown.addView(v);

        if(left != null)
            ((LinearLayout)view.findViewById(R.id.main)).addView(left);
        if(right != null)
            ((LinearLayout)view.findViewById(R.id.main)).addView(right);

        view.findViewById(R.id.drop).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mainOpen = !mainOpen;
                if(mainOpen)
                    view.findViewById(R.id.mainDropdown).setVisibility(VISIBLE);
                else
                    closeAll();

            }
        });
        view.findViewById(R.id.dataDrop).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dataOpen = !dataOpen;
                if(dataOpen)
                    view.findViewById(R.id.dataDropdown).setVisibility(VISIBLE);
                else
                    view.findViewById(R.id.dataDropdown).setVisibility(GONE);
            }
        });

    }

    public Dropdown(Context context, String title){
        this(context, title, null, null, null);
    }

    public Dropdown(Context context, TreeNode<Object[]> tree){
        this(context, ""+(tree.getData()[0].equals("literal") ? tree.getData()[2] : tree.getData()[1]), Arrays.asList(tree.getData()), tree.getLeft() != null ? new Dropdown(context, tree.getLeft()) : null, tree.getRight() != null ? new Dropdown(context, tree.getRight()) : null);
    }

    public void friendlyAdd(List<Object> raw){
        int textHeight = 50;
        int textWidth = 150;
        for(Object obj : raw){
            String objClass = (""+obj.getClass()).substring(6);
            View dataItem = new TextView(view.getContext());
            switch (objClass){
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
                case "[Ljava.lang.Object;":
                    dataItem = new Dropdown(view.getContext(), "any[]", Arrays.asList((Object[]) obj), null, null);
                    dataItem.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    break;
                case "[Ljava.lang.String;":
                    dataItem = new Dropdown(view.getContext(), "str[]", Arrays.asList((Object[]) obj), null, null);
                    dataItem.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    break;
                case "com.reactordevelopment.simplex.simplexLang.TreeNode":
                    dataItem = new Dropdown(view.getContext(), (TreeNode<Object[]>) obj);
                    dataItem.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    break;
                default:
                    ((TextView) dataItem).setText(view.getContext().getString(R.string.any)+": "+obj.toString());
            }
            dataItem.setMinimumWidth(textWidth);
            Log.i("Friendly Add", obj+", |"+objClass);
            data.add(dataItem);
        }
    }

    public void closeAll(){
        if(left !=null)
            left.closeAll();
        if(right !=null)
            right.closeAll();
        view.findViewById(R.id.dataDropdown).setVisibility(GONE);
        view.findViewById(R.id.mainDropdown).setVisibility(GONE);
        dataOpen = false;
        mainOpen = false;
    }

    public void expandAll(){
        view.findViewById(R.id.mainDropdown).setVisibility(VISIBLE);
        view.findViewById(R.id.dataDropdown).setVisibility(VISIBLE);
        dataOpen = true;
        mainOpen = true;
    }
    /*@Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.i("On", "layout");
        displayLayout.addView(title);
        displayLayout.addView(drop);
        if(data != null)
            for(View v : data)
                dropdownLayout.addView(v);

        mainLayout.addView(displayLayout);
        mainLayout.addView(dropdownLayout);

        addView(mainLayout);
        addView(title);
    }

    public void setLayoutParams(LayoutParams params){
        super.setLayoutParams(params);
        drop.setLayoutParams(new LayoutParams(50, 50));
        title.setLayoutParams(new LayoutParams(50, 50));
        displayLayout.setLayoutParams(new LayoutParams(params.width, 50));
        dropdownLayout.setLayoutParams(new LayoutParams(params.width, LayoutParams.WRAP_CONTENT));
        mainLayout.setLayoutParams(new LayoutParams(params.width, LayoutParams.WRAP_CONTENT));
    }*/

}
