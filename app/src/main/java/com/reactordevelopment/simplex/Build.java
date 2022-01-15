package com.reactordevelopment.simplex;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.reactordevelopment.simplex.simplexLang.Executor;
import com.reactordevelopment.simplex.simplexLang.Interpreter;
import com.reactordevelopment.simplex.simplexLang.Lexer;
import com.reactordevelopment.simplex.simplexLang.SimplexException;
import com.reactordevelopment.simplex.simplexLang.TreeNode;
import com.reactordevelopment.simplex.simplexLang.Util;

import java.util.ArrayList;
/**The main build tab, shows the user to different steps that go into building the program*/
public class Build extends Fragment {
    /**The view associated with the build tab*/
    private View view;
    /**The context for the view*/
    private Context context;
    /**The layout that contains the content of each step of building (lexed form, parsed form, ect)*/
    private LinearLayout stepsLayout;
    /**Tells the user if there has been an error in the building process*/
    private TextView console;
    /**Shows the step of building that the user has selected*/
    private TextView buildTitle;
    /**The editor fragment that contains the code*/
    private final Editor editorFrag;
    /**Initializes view and editor instance*/
    public Build(Editor editor){
        super(R.layout.execute);
        editorFrag = editor;
    }
    /**Creates the view for the build tab*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = this.getActivity();
        view = inflater.inflate(R.layout.build, container, false);
        console = view.findViewById(R.id.buildConsole);
        //Initializes the buttons that switch between the build tabs
        ImageButton toInterpreted = view.findViewById(R.id.toInterpreted);
        ImageButton toParsed = view.findViewById(R.id.toParsed);
        ImageButton toLexed = view.findViewById(R.id.toLexed);
        buildTitle = view.findViewById(R.id.buildText);
        stepsLayout = view.findViewById(R.id.dropdownLayout);
        //Gives the tab buttons their click listeners
        toInterpreted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Resets items inside layout
                stepsLayout.removeAllViews();
                buildTitle.setText(R.string.interpView);
                if(editorFrag.getCode().equals("")) return;
                //Get action tree form of code
                showTrees(false);
            }
        });

        toParsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Resets items inside layout
                stepsLayout.removeAllViews();
                buildTitle.setText(R.string.parsedView);
                if(editorFrag.getCode().equals("")) return;
                //Get parsed form of code
                showTrees(true);
            }
        });

        toLexed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Resets items inside layout
                stepsLayout.removeAllViews();
                buildTitle.setText(R.string.lexedView);
                if(editorFrag.getCode().equals("")) return;
                //Show lexed form
                showTokens();
            }
        });
        //Show tokens as the first tab
        showTokens();
        Util.initUtil(new Util.PrintWrapper() {
            @Override public void print(String s) { }});
        return view;
    }
    /**Adds each token to the layout as its own line*/
    private void showTokens(){
        if(editorFrag.getCode().length() > 0)
            try {
                ArrayList<Object[]> lexed = Lexer.lexer(editorFrag.getCode());
                for(Object[] token : lexed){
                    TextView tokenText = new TextView(view.getContext());
                    tokenText.setText(token[0]+": "+token[1]);
                    stepsLayout.addView(tokenText);
                }
            } catch (SimplexException e) {
                e.printStackTrace();
                console.append("Build Error: "+e.getError()+", Code: "+e.getCode());
            }
    }
    /**Adds the Dropdowns to the layout*/
    private void showTrees(boolean parsed){
        try {
            Interpreter.stackFrame.push("main");
            ArrayList<Object[]> lexed = Lexer.lexer(editorFrag.getCode());

            ArrayList<ArrayList<Object[]>> program = new ArrayList<>();

            Executor.prepareLines(lexed, program);
            ArrayList<TreeNode> programTree =  Interpreter.processBlock(program, parsed);
            //Add dropdowns to view
            for(TreeNode node : programTree){
                Dropdown line = new Dropdown(context, node);
                stepsLayout.addView(line);
            }
        } catch (SimplexException e) {
            e.printStackTrace();
            console.append("Build Error: "+e.getError()+", Code: "+e.getCode());
        }
    }
}

