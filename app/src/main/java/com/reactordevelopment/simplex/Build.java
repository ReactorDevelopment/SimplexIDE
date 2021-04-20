package com.reactordevelopment.simplex;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

public class Build extends Fragment {
    private View view;
    private Context context;
    private LinearLayout dropdownLayout;
    private TextView console;
    private TextView buildText;
    private Editor editorFrag = null;
    public Build(Editor editor){
        super(R.layout.execute);
        editorFrag = editor;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        context = this.getActivity();
        view = inflater.inflate(R.layout.build, container, false);
        console = view.findViewById(R.id.buildConsole);
        ImageButton toInterpreted = view.findViewById(R.id.toInterpreted);
        ImageButton toParsed = view.findViewById(R.id.toParsed);
        ImageButton toLexed = view.findViewById(R.id.toLexed);
        buildText = view.findViewById(R.id.buildText);
        dropdownLayout = view.findViewById(R.id.dropdownLayout);

        toInterpreted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dropdownLayout.removeAllViews();
                buildText.setText(R.string.interpView);
                if(editorFrag.getCode().equals("")) return;
                try {
                    if(editorFrag.getCode().equals("")) return;
                    ArrayList<TreeNode<Object[]>> programTree = Executor.build(editorFrag.getCode());
                    if(programTree == null) return;
                    for(TreeNode<Object[]> node : programTree){
                        Dropdown line = new Dropdown(context, node);
                        dropdownLayout.addView(line);
                    }
                } catch (SimplexException e) {
                    e.printStackTrace();
                    console.append("Build Error: "+e.getError()+", Code: "+e.getCode());
                }
            }
        });

        toParsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dropdownLayout.removeAllViews();
                buildText.setText(R.string.parsedView);
                if(editorFrag.getCode().equals("")) return;
                try {
                    Util.initUtil();
                    Interpreter.stackFrame.push("main");
                    ArrayList<Object[]> lexed = Lexer.lexer(editorFrag.getCode());

                    ArrayList<ArrayList<Object[]>> program = new ArrayList<>();

                    Executor.prepareLines(lexed, program);
                    ArrayList<TreeNode<Object[]>> programTree =  Interpreter.processBlock(program, true);

                    for(TreeNode<Object[]> node : programTree){
                        Dropdown line = new Dropdown(context, node);
                        dropdownLayout.addView(line);
                    }
                } catch (SimplexException e) {
                    e.printStackTrace();
                    console.append("Build Error: "+e.getError()+", Code: "+e.getCode());
                }
            }
        });

        toLexed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dropdownLayout.removeAllViews();
                buildText.setText(R.string.lexedView);
                if(editorFrag.getCode().equals("")) return;
                showTokens();
            }
        });
        showTokens();
        return view;
    }
    private void showTokens(){
        if(editorFrag.getCode().length() > 0)
            try {
                ArrayList<Object[]> lexed = Lexer.lexer(editorFrag.getCode());
                for(Object[] token : lexed){
                    TextView tokenText = new TextView(view.getContext());
                    tokenText.setText(token[0]+": "+token[1]);
                    dropdownLayout.addView(tokenText);
                }
            } catch (SimplexException e) {
                e.printStackTrace();
                console.append("Build Error: "+e.getError()+", Code: "+e.getCode());
            }
    }
}

