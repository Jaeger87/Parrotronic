package com.example.parrotronicandroid;

import java.util.ArrayList;

public class AudioNote {


    private String fileName;

    private ArrayList<Byte> amplitudeGraphicList;
    private ArrayList<Integer> amplitudeAnalogicList;

    private String durata;

    public AudioNote()
    {

    }

    public AudioNote(String fileName)
    {
        this.fileName = fileName;
        amplitudeGraphicList = new ArrayList<>();
        amplitudeAnalogicList = new ArrayList<>();
    }


    public String getFileName() {
        return fileName;
    }

    public ArrayList<Byte> getAmplitudeGraphicList() {
        return amplitudeGraphicList;
    }

    public void addToAmplitudeGraphicList(byte element)
    {
        amplitudeGraphicList.add(element);
    }

    public void setAmplitudeGraphicList(ArrayList<Byte> amplitudeGraphicList) {
        this.amplitudeGraphicList = amplitudeGraphicList;
    }

    public ArrayList<Integer> getAmplitudeAnalogicList() {
        return amplitudeAnalogicList;
    }

    public void addToAmplitudeAnalogicList(int element)
    {
        amplitudeAnalogicList.add(element);
    }

    public void setAmplitudeAnalogicList(ArrayList<Integer> amplitudeAnalogicList) {
        this.amplitudeAnalogicList = amplitudeAnalogicList;
    }

    public String getDurata() {
        return durata;
    }

    public void setDurata(String durata) {
        this.durata = durata;
    }

    public int getMaxAnalogicValue()
    {
        return amplitudeAnalogicList.stream().reduce(Integer::max).get();
    }
}
