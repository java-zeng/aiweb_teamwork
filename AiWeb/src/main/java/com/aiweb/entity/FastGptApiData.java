package com.aiweb.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FastGptApiData {

    @JsonProperty("datasetId")
    private final String datasetId;

    @JsonProperty("trainingType")
    private final String trainingType;

    @JsonProperty("parentId")
    private final String parentId;

    @JsonProperty("indexPrefixTitle") private final Boolean indexPrefixTitle;
    @JsonProperty("customPdfParse") private final Boolean customPdfParse;
    @JsonProperty("autoIndexes") private final Boolean autoIndexes;
    @JsonProperty("imageIndex") private final Boolean imageIndex;
    @JsonProperty("chunkSettingMode") private final String chunkSettingMode;
    @JsonProperty("chunkSplitMode") private final String chunkSplitMode;
    @JsonProperty("chunkSize") private final Integer chunkSize;
    @JsonProperty("indexSize") private final Integer indexSize;
    @JsonProperty("chunkSplitter") private final String chunkSplitter;
    @JsonProperty("qaPrompt") private final String qaPrompt;
    @JsonProperty("tags") private final List<String> tags;
    @JsonProperty("createTime") private final String createTime;

}
