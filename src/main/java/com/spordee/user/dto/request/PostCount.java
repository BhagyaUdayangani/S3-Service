package com.spordee.user.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class PostCount {

    private Long count;
    private Long imageCount;
    private Long videoCount;
    private String authUserId;

}
