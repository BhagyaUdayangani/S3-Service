package com.example.s3_bucket.service;

import com.spordee.user.dto.request.PostCount;

public interface UserService {

    PostCount getPostCount(String authUserId, String token);

}
