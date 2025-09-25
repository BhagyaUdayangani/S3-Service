package com.example.s3_bucket.enums;

import lombok.Getter;

@Getter
public enum AuthProvider {
    PROVIDER_LOCAL,
    PROVIDER_GOOGLE,
    PROVIDER_APPLE;
    public boolean equalsProviderLocal() {
        return this == PROVIDER_LOCAL;
    }
}
