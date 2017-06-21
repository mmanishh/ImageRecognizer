package com.manishm.imagerecognizer.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by manish on 6/20/2017.
 */

public class Responses {


    @SerializedName("labelAnnotations")
    ArrayList<LabelAnnotation> labelAnnotations;
    @SerializedName("logoAnnotations")
    ArrayList<LabelAnnotation> logoAnnotations;


    public ArrayList<LabelAnnotation> getLabelAnnotations() {
        return labelAnnotations;
    }

    public void setLabelAnnotations(ArrayList<LabelAnnotation> labelAnnotations) {
        this.labelAnnotations = labelAnnotations;
    }

    public ArrayList<LabelAnnotation> getLogoAnnotations() {
        return logoAnnotations;
    }

    public void setLogoAnnotations(ArrayList<LabelAnnotation> logoAnnotations) {
        this.logoAnnotations = logoAnnotations;
    }


    public Responses() {
    }

    @Override
    public String toString() {
        return labelAnnotations.toString() + logoAnnotations.toString();

    }
}
