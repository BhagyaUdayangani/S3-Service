package com.example.s3_bucket.dto;

import com.example.s3_bucket.enums.CommonMessages;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetaData {
    private boolean error;
    private CommonMessages message;
    private int statusCode;
    private String description;

}

