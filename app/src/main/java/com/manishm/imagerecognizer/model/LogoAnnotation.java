package com.manishm.imagerecognizer.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by manish on 6/20/2017.
 */

public class LogoAnnotation {
    @SerializedName("description")
    String description;
    @SerializedName("score")
    double score;
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }



    public LogoAnnotation(){}

}
