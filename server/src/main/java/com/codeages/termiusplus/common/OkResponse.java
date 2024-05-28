package com.codeages.termiusplus.common;

public class OkResponse {

    public static final OkResponse TRUE = new OkResponse();

    private OkResponse() {

    }

    public Boolean getOk() {
        return true;
    }
}
