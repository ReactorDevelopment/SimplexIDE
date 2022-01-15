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
/**Editor tab of environment, able to edit text files using custom keyboard*/
public class Editor extends Fragment {
    /**The original editing pane for the code*/
    private EditText editor;
    /**Any temporary editing pane that needs to be filled with text*/
    private EditText activeEditor;
    /**If the shift key has been pressed*/
    private boolean shift = false;
    /**Saves text of editor, used when editor is refreshed and text has been cleared to restore text*/
    private final SpannableStringBuilder prevText = new SpannableStringBuilder("");
    /**Color of operator in code*/
    private static final int OP_COLOR = Color.parseColor("#41abdc");
    /**Color of reserved word in code*/
    private static final int RESERVED_COLOR = Color.parseColor("#001df5");
    /**Color of number in code*/
    private static final int NUM_COLOR = Color.parseColor("#1cd4b5");
    /**Color of boolean in code*/
    private static final int BOOL_COLOR = Color.parseColor("#4626e3");
    /**Color of string in code*/
    private static final int STR_COLOR = Color.parseColor("#16bb16");
    /**Color of id in code*/
    private static final int ID_COLOR = Color.parseColor("#8b5fc8");
    /**Color of comment in code*/
    private static final int COMMENT_COLOR = Color.parseColor("#949494");
    /**Color of generic text in code*/
    private static final int BASE_COLOR = Color.parseColor("#e3e3e3");
    /**List of items where the second half if autocompleted after the first half*/
    private static final List<String> completables = Arrays.asList("\"\"", "()", "[]", "{}");

    /**Initializes the editor*/
    public Editor(){ super(R.layout.editor); }

    /**Creates view for editor*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //The view
        View view = inflater.inflate(R.layout.editor, container, false);

        editor = view.findViewById(R.id.editor);
        //If the selected database text is not null
        if(MainActivity.dbText() != null)
            editor.setText(MainActivity.dbText());
        else
            //Restore text
            editor.setText(prevText);

        editor.setShowSoftInputOnFocus(false);
        //Set the current editor as the active editor
        activeEditor = editor;
        //Listens for any change in the text
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Log.i("Before", "Text: "+s+", To Replace: "+s.subSequence(start, start+count)+", Replacing Length: "+after);
                //int startSpan = indexBefore(s.toString(), '\n', start+count);
                //int endSpan = s.toString().indexOf('\n', start+count);
                //if(endSpan >= editor.getText().length()) endSpan = editor.getText().length();
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.i("After", "Text: "+s+", New Text: "+s.subSequence(start, start+count)+", Replacing Length: "+before);
                int startSpan = indexBefore(s.toString(), '\n', start);
                int endSpan = s.toString().indexOf('\n', start+count);
                startSpan = startSpan == -1 ? 0 : startSpan;
                endSpan = endSpan == -1 ? s.length() : endSpan;
                if(startSpan == endSpan) return;
                editor.getText().setSpan(new ForegroundColorSpan(BASE_COLOR), startSpan != -1 ? startSpan : 0, endSpan != -1 ? endSpan : editor.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                //Gets tokens of text, and colors text accordingly
                try {
                    ArrayList<Object[]> lexed = Lexer.lexer(s.subSequence(startSpan, endSpan).toString());
                    int tokenIndex;
                    int lostLen = 0;
                    String cloneStr = ""+s.toString();
                    //For each token in the lexed form
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
                        //Set color span
                        editor.getText().setSpan(new ForegroundColorSpan(color), tokenIndex+lostLen, tokenIndex+token[1].toString().length()+lostLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        String tmp = cloneStr.substring(tokenIndex+token[1].toString().length());
                        lostLen += cloneStr.length()-tmp.length();
                        cloneStr = tmp;
                    }
                } catch (SimplexException e) {
                    Log.i("LexerError", s.subSequence(startSpan, endSpan).toString()+" contains an error");
                }
                //Fill in spans for strings, new lines, and comments
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
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        //The keyboard layout
        ConstraintLayout keyboard = view.findViewById(R.id.keyLayout);
        //Give each key an action for when shift is and isn't true
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
        //The symbol keyboard layout
        ConstraintLayout symKeyboard = view.findViewById(R.id.sym_keyLayout);
        //Give each key itd actions
        for(int index = 0; index < symKeyboard.getChildCount(); index++) {
            TextView nextChild = (TextView) symKeyboard.getChildAt(index);
            if(nextChild.getText().length() == 1)
                nextChild.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Add text
                        append(nextChild.getText().toString());
                        //Reset prevText
                        prevText.clear();
                        prevText.append(editor.getText());
                    }
                });

        }
        //Add character for space ar
        symKeyboard.findViewById(R.id.boardSpaceCover).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { append(" "); }
        });
        //Give listener to shift keys
        View.OnClickListener shiftListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shift = !shift;
                /*for(int index = 0; index < keyboard.getChildCount(); index++) {
                    TextView nextChild = (TextView) keyboard.getChildAt(index);
                    if(nextChild.getText().length() == 1) {
                        if(shift)
                            nextChild.setText(nextChild.getText().toString().toUpperCase());
                        else
                            nextChild.setText(nextChild.getText().toString().toLowerCase());
                    }
                }*/
            }
        };
        //Give shift listeners to both shift keys
        keyboard.findViewById(R.id.boardShift).setOnClickListener(shiftListener);
        symKeyboard.findViewById(R.id.boardShift).setOnClickListener(shiftListener);
        //Assign backspace listeners
        keyboard.findViewById(R.id.boardBacksp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { remove(); }});
        symKeyboard.findViewById(R.id.boardBacksp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { remove(); }});
        //Assign enter keys
        keyboard.findViewById(R.id.boardReturn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { append("\n"); }});
        symKeyboard.findViewById(R.id.boardReturn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { append("\n"); }});
        //Add listener to switch to symbol board
        view.findViewById(R.id.boardSymbol).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                keyboard.setVisibility(View.GONE);
                symKeyboard.setVisibility(View.VISIBLE);
            }
        });
        //Add listener to switch from symbol board
        view.findViewById(R.id.boardAlpha).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                symKeyboard.setVisibility(View.GONE);
                keyboard.setVisibility(View.VISIBLE);
            }
        });
        return view;
    }
    /**Shift activeEditor away from original and to new editor
     * If editText is null, shift back to original editor pane*/
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
    /**Returns the code contained in the active editor*/
    public String getCode(){
        return activeEditor.getText().toString();
    }
    /**Inserts the given string into the active editor where the cursor is currently placed*/
    public void append(String append){
        //Get the cursor position
        int cursor = activeEditor.getSelectionStart();
        //Inserts the given text at the cursor
        activeEditor.getText().insert(cursor, append);
        //The new text of the editor
        String text = activeEditor.getText().toString();
        cursor ++;
        if(cursor-1  == text.length()-1)
            //Loop for text to autocomplete
            for(String s : completables)
                //If the added text matches
                if (append.equals(""+s.charAt(0))) {
                    //Add the completion
                    activeEditor.getText().insert(cursor, String.valueOf(s.charAt(1)));
                    //Puts cursor between append and the completed half
                    activeEditor.setSelection(cursor);
                }
    }
    /**Removes the character at the cursor*/
    public void remove(){
        //Get the cursor
        int cursor = activeEditor.getSelectionStart();
        if(cursor <= 0) return;
        //Get the text of the editor
        String text = activeEditor.getText().toString();
        if(cursor < text.length()){
            //Removed other half of completables
            for(String s : completables)
                if(text.charAt(cursor-1) == s.charAt(0) && text.charAt(cursor) == s.charAt(1)) {
                    activeEditor.getText().replace(cursor, cursor+1, "");
                    break;
                }
        }
        //Removes character and decrements cursor
        activeEditor.getText().replace(cursor-1, cursor, "");
        cursor --;
        activeEditor.setSelection(cursor);
    }
    /**Clears active editor of all text*/
    public void clear(){ activeEditor.getText().replace(0, activeEditor.getText().length(), ""); }
    /**Finds occurrence of search before index in string s*/
    private int indexBefore(String s, char search, int index){
        int foundIndex = -1;
        for(int i=0; i<index; i++){
            if(s.charAt(i) == search)
                foundIndex = i;
        }
        return foundIndex;
    }
}
