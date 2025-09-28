package com.time_tracker.be.utils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.time_tracker.be.exception.BadRequestException;

import java.util.Collections;

public class GoogleTokenUtils {

    private static final String CLIENT_ID = "${spring.security.oauth2.authorizationserver.client.google.client-id}";

    public static GoogleIdToken.Payload verifyGoogleToken(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance() // ✅ ganti JacksonFactory
        )
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            return idToken.getPayload(); // email, name, dsb
        } else {
            throw new BadRequestException("Invalid ID token.");
        }
    }
}
