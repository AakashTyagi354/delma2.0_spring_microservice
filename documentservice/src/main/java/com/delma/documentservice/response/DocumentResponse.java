package com.delma.documentservice.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentResponse {
    private Long id;
    private String userId;
    private String name;
    private String type;
    private String url;
}
