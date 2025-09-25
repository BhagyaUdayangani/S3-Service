package com.example.s3_bucket.dto;

import com.example.s3_bucket.enums.StatusType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class CommonResponse {
    private StatusType status;
    private Object data;
    private MetaData meta;


}
