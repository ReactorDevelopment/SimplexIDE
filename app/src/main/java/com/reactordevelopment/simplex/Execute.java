package com.reactordevelopment.simplex;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.reactordevelopment.simplex.simplexLang.*;

public class Execute extends Fragment {
    private View view;
    private Context context;
    private Editor editorFrag = null;
    public Execute(Editor editor){
        super(R.layout.execute);
        editorFrag = editor;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        context = this.getActivity();
        view = inflater.inflate(R.layout.execute, container, false);

        view.findViewById(R.id.exeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editorFrag.getCode().equals("")) return;
                Executor.execute(editorFrag.getCode());
                ((TextView)view.findViewById(R.id.console)).setText(Util.console());
            }
        });
        return view;
    }
}

