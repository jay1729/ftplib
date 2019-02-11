package com.jayanthag.ftp.parsers;

import com.jayanthag.ftp.Exceptions;
import com.jayanthag.ftp.models.FileElement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;

/*
    Parses the output of MLSD command
 */
public class MLSDParser implements Parser{

    protected ArrayList<String> mlsdOutput;
    protected FileElement currentElement;
    protected String currentAttribute;
    protected String currentValue;
    protected ArrayList<FileElement> output;
    protected String currentLine;

    public void perm(){
        currentElement.setPerm(currentValue);
    }

    public void modify(){
        try {
            currentElement.setLastModified(currentValue);
        }catch (ParseException e){
            e.printStackTrace();
        }
    }

    public void size(){
        currentElement.setSize(currentValue);
    }

    public void type(){
        try {
            currentElement.setType(currentValue);
        } catch (Exceptions.FileTypeException e){
            e.printStackTrace();
        }
    }

    protected void recordName(int startIndex){
        currentElement.setName(currentLine.substring(startIndex));
    }

    protected void recordNewAttribute(){
        try {
            Method method = this.getClass().getMethod(currentAttribute);
            method.invoke(this);
        }catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
        }
    }

    protected void clearForCurrentLine(){
        currentLine = null;
        currentElement = new FileElement();
        currentAttribute = null;
        currentValue = null;
    }

    protected void parseLine(int index){
        currentLine = mlsdOutput.get(index);
        int size = currentLine.length();
        StringBuilder temp = new StringBuilder();
        for(int i=0;i<size;i++){
            if(currentLine.charAt(i) == ';'){
                currentValue = temp.toString();
                recordNewAttribute();
                temp = new StringBuilder();
            }else if(currentLine.charAt(i) == '='){
                currentAttribute = temp.toString();
                temp = new StringBuilder();
            }else if(currentLine.charAt(i) == ' '){
                recordName(i+1);
                break;
            }else{
                temp.append(currentLine.charAt(i));
            }
        }
        output.add(currentElement);
        clearForCurrentLine();
    }

    public ArrayList<FileElement> parse(ArrayList<String> mlsdOutput){
        output = new ArrayList<>();
        this.mlsdOutput = mlsdOutput;
        clearForCurrentLine();
        int size = mlsdOutput.size();
        for(int i=0;i<size;i++) parseLine(i);
        return output;
    }

}
