package com.manishm.imagerecognizer.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by manish on 6/20/2017.
 */

public class LabelAnnotation {

    @SerializedName("description")
    String description;
    @SerializedName("mid")
    String mid;
    @SerializedName("score")
    double score;


    public LabelAnnotation() {}
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }



    public LabelAnnotation(String description, String mid, double score) {
        this.description = description;
        this.mid = mid;
        this.score = score;
    }


}
