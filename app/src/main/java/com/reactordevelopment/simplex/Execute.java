package com.reactordevelopment.simplex;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.reactordevelopment.simplex.simplexLang.*;
/**Execute tab where the output of executed code goes*/
public class Execute extends Fragment {
    /**The execute tab view*/
    private View view;
    /**The editor that the text is kept in*/
    private final Editor editorFrag;
    /**Initializes execute with given editor*/
    public Execute(Editor editor){
        super(R.layout.execute);
        editorFrag = editor;
    }
    /**Creates view with button to execute code*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.execute, container, false);
        //Gives execute button its listener
        view.findViewById(R.id.exeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editorFrag.getCode().equals("")) return;
                try {
                    ((TextView)view.findViewById(R.id.console)).append("Console:\n");
                    //Initialize print wrapper
                    Util.initUtil(new Util.PrintWrapper() {
                        @Override public void print(String s) {
                            ((TextView)view.findViewById(R.id.console)).append(s);
                        }});
                    //Execute code
                    Executor.buildAndExecute(editorFrag.getCode());
                } catch (SimplexException e) {
                    e.printStackTrace();
                }
            }
        });
        return view;
    }
}

