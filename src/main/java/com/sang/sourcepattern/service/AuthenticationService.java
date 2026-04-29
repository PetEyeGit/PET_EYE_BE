package com.sang.sourcepattern.service;

import com.nimbusds.jose.JOSEException;
import com.sang.sourcepattern.dto.request.AuthenticationRequest;
import com.sang.sourcepattern.dto.request.IntrospectRequest;
import com.sang.sourcepattern.dto.request.LogoutRequest;
import com.sang.sourcepattern.dto.request.RefreshRequest;
import com.sang.sourcepattern.dto.request.SocialLoginRequest;
import com.sang.sourcepattern.dto.response.AuthenticationResponse;
import com.sang.sourcepattern.dto.response.IntrospectResponse;

import java.text.ParseException;

public interface AuthenticationService {
    AuthenticationResponse authenticated(AuthenticationRequest request);

    IntrospectResponse introspect(IntrospectRequest request)
            throws ParseException, JOSEException;

    void logout(LogoutRequest request)
            throws ParseException, JOSEException;

    AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException;

    AuthenticationResponse socialLoginGoogle(SocialLoginRequest request);
    AuthenticationResponse socialLoginFacebook(SocialLoginRequest request);
    AuthenticationResponse socialLoginZalo(SocialLoginRequest request);
}
