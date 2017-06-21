package com.manishm.imagerecognizer.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by manish on 6/20/2017.
 */

public class JsonResponse {

    @SerializedName("responses")
    ArrayList<Responses> responses;


    public ArrayList<Responses> getResponses() {
        return responses;
    }

    public void setResponses(ArrayList<Responses> responses) {
        this.responses = responses;
    }


}
