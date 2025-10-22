package com.aiweb.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FastGptApiResponse {
    private Integer code;
    private String statusTest;
    private String message;
    private DataPayLoad data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataPayLoad{
        @JsonProperty("collectionId")
        private String collectionId;
        @JsonProperty("results")
        private ResultPayLoad results;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResultPayLoad{
        @JsonProperty("insertLen")
        private String insertLen;
        @JsonProperty("overToken")
        private List<Object> overToken;
        @JsonProperty("repeat")
        private List<Object> repeat;
        @JsonProperty("error")
        private List<Object> error;
    }
}
