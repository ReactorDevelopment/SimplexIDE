package com.reactordevelopment.simplex;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.reactordevelopment.simplex.simplexLang.Lexer;
import com.reactordevelopment.simplex.simplexLang.SimplexException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Editor extends Fragment {
    private View view;
    private Context context;
    private EditText editor;
    private EditText activeEditor;
    private Activity activity;
    private int cursor;
    private boolean shift = false;
    private final SpannableStringBuilder prevText = new SpannableStringBuilder("");
    //private final String prevText = "";
    //private static SpanWatcher watcher;
    private static final int OP_COLOR = Color.parseColor("#41abdc");
    private static final int RESERVED_COLOR = Color.parseColor("#001df5");
    private static final int NUM_COLOR = Color.parseColor("#1cd4b5");
    private static final int BOOL_COLOR = Color.parseColor("#4626e3");
    private static final int STR_COLOR = Color.parseColor("#16bb16");
    private static final int ID_COLOR = Color.parseColor("#8b5fc8");
    private static final int COMMENT_COLOR = Color.parseColor("#949494");
    private static final int BASE_COLOR = Color.parseColor("#e3e3e3");

    private static final List<String> completables = Arrays.asList("\"\"", "()", "[]", "{}");
    public Editor(){
        super(R.layout.editor);
        //cursorBlink();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        context = this.getActivity();
        activity = this.getActivity();
        view = inflater.inflate(R.layout.editor, container, false);

        cursor = 0;
        editor = view.findViewById(R.id.editor);
        editor.setText(prevText);
        editor.setShowSoftInputOnFocus(false);
        activeEditor = editor;
        /*watcher = new SpanWatcher() {
            @Override
            public void onSpanAdded(final Spannable text, final Object what, final int start, final int end) {Log.i("SelectionAdd", start+", "+end);}

            @Override
            public void onSpanRemoved(final Spannable text, final Object what, final int start, final int end) {Log.i("SelectionRemove", start+", "+end);}

            @Override
            public void onSpanChanged(final Spannable text, final Object what,
                                      final int ostart, final int oend, final int nstart, final int nend) {
                if (what == Selection.SELECTION_START) {
                    Log.i("SelectionStart", ostart+", "+oend+", "+nstart+", "+oend);
                } else if (what == Selection.SELECTION_END) {
                    Log.i("SelectionEnd", ostart+", "+oend+", "+nstart+", "+oend);
                }
            }
        };*/

        //editor.getText().setSpan(watcher, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        editor.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.i("Before", "Text: "+s+", To Replace: "+s.subSequence(start, start+count)+", Replacing Length: "+after);
                int startSpan = indexBefore(s.toString(), '\n', start+count);
                int endSpan = s.toString().indexOf('\n', start+count);
                if(endSpan >= editor.getText().length()) endSpan = editor.getText().length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.i("After", "Text: "+s+", New Text: "+s.subSequence(start, start+count)+", Replacing Length: "+before);
                int startSpan = indexBefore(s.toString(), '\n', start);
                int endSpan = s.toString().indexOf('\n', start+count);
                startSpan = startSpan == -1 ? 0 : startSpan;
                endSpan = endSpan == -1 ? s.length() : endSpan;
                if(startSpan == endSpan) return;
                Log.i("Timecheck", ""+System.currentTimeMillis());
                editor.getText().setSpan(new ForegroundColorSpan(BASE_COLOR), startSpan != -1 ? startSpan : 0, endSpan != -1 ? endSpan : editor.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                try {
                    ArrayList<Object[]> lexed = Lexer.lexer(s.subSequence(startSpan, endSpan).toString());
                    int tokenIndex;
                    int lostLen = 0;
                    String cloneStr = ""+s.toString();
                    for(Object[] token : lexed){
                        tokenIndex = cloneStr.indexOf(""+token[1]);
                        if(tokenIndex == -1) continue;
                        int color = Color.RED;
                        if(token[0].equals("op") && !token[1].equals("\"")) color = OP_COLOR;
                        if(token[0].equals("reserved")) color = RESERVED_COLOR;
                        if(token[0].equals("int") || token[0].equals("double")) color = NUM_COLOR;
                        if(token[0].equals("bool")) color = BOOL_COLOR;
                        if(token[0].equals("str") || token[1].equals("\"")) color = STR_COLOR;
                        if(token[0].equals("id")) color = ID_COLOR;
                        editor.getText().setSpan(new ForegroundColorSpan(color), tokenIndex+lostLen, tokenIndex+token[1].toString().length()+lostLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        String tmp = cloneStr.substring(tokenIndex+token[1].toString().length());
                        lostLen += cloneStr.length()-tmp.length();
                        cloneStr = tmp;
                    }
                } catch (SimplexException e) {
                    Log.i("LexerError", s.subSequence(startSpan, endSpan).toString()+" contains an error");
                }
                Log.i("Timecheck2", ""+System.currentTimeMillis());
                for (int i=0; i<s.length(); i++){
                    char at = s.toString().charAt(i);
                    if(at == '"')
                        editor.getText().setSpan(new ForegroundColorSpan(STR_COLOR), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if(at == ';')
                        editor.getText().setSpan(new ForegroundColorSpan(OP_COLOR), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if(at == '#'){
                        int nextLine = s.toString().indexOf("\n", i);
                        if(nextLine == -1) nextLine = s.length();
                        editor.getText().setSpan(new ForegroundColorSpan(COMMENT_COLOR), i, nextLine, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        i = nextLine;
                    }

                }
                Log.i("Timecheck3", ""+System.currentTimeMillis());
            }

            @Override
            public void afterTextChanged(Editable s) {
                //Log.i("AfterTextChange", s.toString());
                //editor.getText().setSpan(watcher, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                /*if(editor.getText().length() >= 5)
                    editor.getText().setSpan(new ForegroundColorSpan(Color.BLUE), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);*/
            }
        });

        ConstraintLayout keyboard = view.findViewById(R.id.keyLayout);
        for(int index = 0; index < keyboard.getChildCount(); index++) {
            TextView nextChild = (TextView) keyboard.getChildAt(index);
            if(nextChild.getText().length() == 1)
                nextChild.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!shift)
                            append(nextChild.getText().toString());
                        else
                            append(nextChild.getText().toString().toUpperCase());
                    }
                });
        }
        keyboard.findViewById(R.id.boardSpaceCover).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { append(" "); }
        });
        ConstraintLayout symKeyboard = view.findViewById(R.id.sym_keyLayout);
        for(int index = 0; index < symKeyboard.getChildCount(); index++) {
            TextView nextChild = (TextView) symKeyboard.getChildAt(index);
            if(nextChild.getText().length() == 1)
                nextChild.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        append(nextChild.getText().toString());
                        prevText.clear();
                        prevText.append(editor.getText());
                    }
                });

        }
        symKeyboard.findViewById(R.id.boardSpaceCover).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { append(" "); }
        });
        View.OnClickListener shiftListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shift = !shift;
                for(int index = 0; index < keyboard.getChildCount(); index++) {
                    TextView nextChild = (TextView) keyboard.getChildAt(index);
                    if(nextChild.getText().length() == 1) {
                        if(shift)
                            nextChild.setText(nextChild.getText().toString().toUpperCase());
                        else
                            nextChild.setText(nextChild.getText().toString().toLowerCase());
                    }
                }
            }
        };
        keyboard.findViewById(R.id.boardShift).setOnClickListener(shiftListener);
        symKeyboard.findViewById(R.id.boardShift).setOnClickListener(shiftListener);

        keyboard.findViewById(R.id.boardBacksp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { remove(); }});

        symKeyboard.findViewById(R.id.boardBacksp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { remove(); }});

        keyboard.findViewById(R.id.boardReturn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { append("\n"); }});

        symKeyboard.findViewById(R.id.boardReturn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { append("\n"); }});

        view.findViewById(R.id.boardSymbol).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                keyboard.setVisibility(View.GONE);
                symKeyboard.setVisibility(View.VISIBLE);
            }
        });

        view.findViewById(R.id.boardAlpha).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                symKeyboard.setVisibility(View.GONE);
                keyboard.setVisibility(View.VISIBLE);
            }
        });

        /*

        Button buttonLeft = view.findViewById(R.id.buttonLeft);
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cursorLeft();
            }
        });

        Button buttonRight = view.findViewById(R.id.buttonRight);
        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cursorRight();
            }
        });*/
        return view;
    }
    public void keyboardFocusTo(EditText editText){
        if(editText != null){
            editor.setVisibility(View.INVISIBLE);
            activeEditor = editText;
        }
        else{
            editor.setVisibility(View.VISIBLE);
            activeEditor = editor;
        }
    }
    public String getCode(){
        return activeEditor.getText().toString();
    }

    public void append(String append){
        Log.i("TimecheckAppend", ""+System.currentTimeMillis());
        int cursor = activeEditor.getSelectionStart();
        activeEditor.getText().insert(cursor, append);
        String text = activeEditor.getText().toString();
        Log.i("Curseo", cursor+", "+text.length());
        cursor ++;
        //editor.setSelection(cursor);
        if(cursor-1  == text.length()-1)
            for(String s : completables)
                if (append.equals(""+s.charAt(0))) {
                    activeEditor.getText().insert(cursor, String.valueOf(s.charAt(1)));
                    activeEditor.setSelection(cursor);
                }
                    //editor.setText(text.substring(0, cursor) + s.charAt(1) + text.substring(cursor));

        //editor.getText().setSpan(watcher, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    public void remove(){
        int cursor = activeEditor.getSelectionStart();
        if(cursor <= 0) return;
        
        String text = activeEditor.getText().toString();
        if(cursor < text.length() && cursor > 0){
            for(String s : completables)
                if(text.charAt(cursor-1) == s.charAt(0) && text.charAt(cursor) == s.charAt(1)) {
                    activeEditor.getText().replace(cursor, cursor+1, "");
                    //editor.setText(text.substring(0, cursor) + text.substring(cursor + 1));
                    break;
                }
        }
        //editor.setText(text.substring(0, cursor - 1) + text.substring(cursor));
        activeEditor.getText().replace(cursor-1, cursor, "");
        cursor --;
        activeEditor.setSelection(cursor);

        //editor.getText().setSpan(watcher, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    public void clear(){
        activeEditor.getText().replace(0, activeEditor.getText().length(), "");
    }

    private int indexBefore(String s, char search, int index){
        int foundIndex = -1;
        for(int i=0; i<index; i++){
            if(s.charAt(i) == search)
                foundIndex = i;
        }
        return foundIndex;
    }
    
    /*private void cursorBlink(){
        Fragment frag = this;
        new Thread(){
            @Override
            public void run() {
                boolean blinkOn = true;
                while(!isInterrupted()){
                    try {
                        Thread.sleep(500);
                        if(activity != null && editor != null) {
                            boolean finalBlinkOn = blinkOn;
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SpannableString content = new SpannableString(editor.getText().toString());
                                    if(cursor < content.length()) {
                                        if (finalBlinkOn)
                                            content.setSpan(new UnderlineSpan(), cursor, cursor + 1, 0);
                                        else content.setSpan(null, cursor, cursor + 1, 0);
                                    }

                                    editor.setText(content);
                                }
                            });
                        }
                        blinkOn = !blinkOn;
                    }catch (Exception e){e.printStackTrace();}
                }
            }
        }.start();
    }*/
}
