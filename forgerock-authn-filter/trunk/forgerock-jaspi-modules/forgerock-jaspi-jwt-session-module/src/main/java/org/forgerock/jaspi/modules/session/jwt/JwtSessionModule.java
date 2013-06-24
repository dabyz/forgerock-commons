/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.jaspi.modules.session.jwt;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.jose.builders.JwtBuilder;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jwt.keystore.KeystoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A JASPI Session Module which creates a JWT when securing the response from a successful authentication and sets it
 * as a Cookie on the response. Then on subsequent requests checks for the presents of the JWT as a Cookie on the
 * request and validates the signature and decrypts it and checks the expiration time of the JWT.
 *
 * @author Phill Cunnington
 */
public class JwtSessionModule implements ServerAuthModule {

    private static final Logger DEBUG = LoggerFactory.getLogger(JwtSessionModule.class);

    private static final String JWT_SESSION_COOKIE_NAME = "session-jwt";
    private static final String SKIP_SESSION_PARAMETER_NAME = "skipSession";

    public static final String KEY_ALIAS_KEY = "keyAlias";
    public static final String PRIVATE_KEY_PASSWORD_KEY = "privateKeyPassword";
    public static final String KEYSTORE_TYPE_KEY = "keystoreType";
    public static final String KEYSTORE_FILE_KEY = "keystoreFile";
    public static final String KEYSTORE_PASSWORD_KEY = "keystorePassword";
    public static final String TOKEN_IDLE_TIME_CLAIM_KEY = "tokenIdleTime";
    public static final String MAX_TOKEN_LIFE_KEY = "maxTokenLife";
    public static final String JWT_VALIDATED_KEY = "jwtValidated";

    private String keyAlias;
    private String privateKeyPassword;
    private String keystoreType;
    private String keystoreFile;
    private String keystorePassword;
    private int tokenIdleTime;
    private int maxTokenLife;

    /**
     * Initialises the module by getting the Keystore and Key alias properties out of the module configuration.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
                           Map options) throws AuthException {
        this.keyAlias = (String) options.get(KEY_ALIAS_KEY);
        this.privateKeyPassword = (String) options.get(PRIVATE_KEY_PASSWORD_KEY);
        this.keystoreType = (String) options.get(KEYSTORE_TYPE_KEY);
        this.keystoreFile = (String) options.get(KEYSTORE_FILE_KEY);
        this.keystorePassword = (String) options.get(KEYSTORE_PASSWORD_KEY);
        String tokenIdleTime = (String) options.get(TOKEN_IDLE_TIME_CLAIM_KEY);
        if (StringUtils.isEmpty(tokenIdleTime)) {
            tokenIdleTime = "0";
        }
        this.tokenIdleTime = Integer.parseInt(tokenIdleTime);
        String maxTokenLife = (String) options.get(MAX_TOKEN_LIFE_KEY);
        if (StringUtils.isEmpty(maxTokenLife)) {
            maxTokenLife = "0";
        }
        this.maxTokenLife = Integer.parseInt(maxTokenLife);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    /**
     * Checks for the presence of the JWT as a Cookie on the request and validates the signature and decrypts it and
     * checks the expiration time of the JWT. If all these checks pass then the method return AuthStatus.SUCCESS,
     * otherwise returns AuthStatus.SEND_FAILURE.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) {

        Jwt jwt = validateJwtSessionCookie(messageInfo);
        if (jwt == null) {
            DEBUG.debug("Session JWT NOT valid");
            return AuthStatus.SEND_FAILURE;
        } else {
            DEBUG.debug("Session JWT valid");
            return AuthStatus.SUCCESS;
        }
    }

    public Jwt validateJwtSessionCookie(MessageInfo messageInfo) {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        Cookie jwtSessionCookie = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : request.getCookies()) {
                if (JWT_SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    DEBUG.debug("Session JWT cookie found");
                    jwtSessionCookie = cookie;
                    break;
                }
            }
        }

        if (jwtSessionCookie != null && !StringUtils.isEmpty(jwtSessionCookie.getValue())) {

            Jwt jwt = verifySessionJwt(jwtSessionCookie.getValue());
            if (jwt != null) {
                //if all goes well!
                JwtClaimsSet jwtClaimsSet = jwt.getClaimsSet();
                for (String key : jwtClaimsSet.keys()) {
                    request.setAttribute(key, jwtClaimsSet.get(key));
                }

                if (hasCoolOffPeriodExpired(jwt)) {
                    // reset tokenIdleTime
//                    HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
//                    response.addCookie(
                            resetIdleTimeout(jwt, jwtSessionCookie);
//                    );
                }

                messageInfo.getMap().put(JWT_VALIDATED_KEY, true);

                return jwt;
            }
        } else {
            DEBUG.debug("Session JWT cookie not set");
        }

        return null;
    }

    /**
     * Verifies that the JWT has a valid signature and can be decrypted and that the JWT expiration time has not
     * passed.
     *
     * The method will return null in the case where the JWT is not valid.
     *
     * @param sessionJwt The JWT string.
     * @return The validated decrypted JWT.
     */
    private Jwt verifySessionJwt(String sessionJwt) {

        KeystoreManager keystoreManager = new KeystoreManager(privateKeyPassword, keystoreType,
                keystoreFile, keystorePassword);

        RSAPrivateKey privateKey = (RSAPrivateKey) keystoreManager.getPrivateKey(keyAlias);

        JwtBuilder jwtBuilder = new JwtBuilder();
        EncryptedJwt jwt = jwtBuilder.reconstruct(sessionJwt, EncryptedJwt.class);
        jwt.decrypt(privateKey);

        Date expirationTime = jwt.getClaimsSet().getExpirationTime();
        Date tokenIdleTime = new Date(jwt.getClaimsSet().getClaim(TOKEN_IDLE_TIME_CLAIM_KEY, Integer.class).longValue() * 1000L);

        Date now = new Date(System.currentTimeMillis());

        if ((now.getTime() < expirationTime.getTime()) && (now.getTime() < tokenIdleTime.getTime())) {
            return jwt;
        }

        return null;
    }

    private boolean hasCoolOffPeriodExpired(Jwt jwt) {

        Date issuedAtTime = jwt.getClaimsSet().getIssuedAtTime();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, -1);

        return calendar.getTime().compareTo(issuedAtTime) > 0;
    }

    private Cookie resetIdleTimeout(Jwt jwt, Cookie jwtSessionCookie) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        final Date now = calendar.getTime();
        Date nbf = now;
        Date iat = now;
        calendar.add(Calendar.MINUTE, tokenIdleTime);
        Date tokenIdleTime = calendar.getTime();

        jwt.getClaimsSet().setIssuedAtTime(iat);
        jwt.getClaimsSet().setNotBeforeTime(nbf);
        jwt.getClaimsSet().setClaim(TOKEN_IDLE_TIME_CLAIM_KEY, tokenIdleTime.getTime() / 1000L); //TODO maybe get jwt to make conversion?


        KeystoreManager keystoreManager = new KeystoreManager(privateKeyPassword, keystoreType,
                keystoreFile, keystorePassword);

        RSAPublicKey publicKey = (RSAPublicKey) keystoreManager.getPublicKey(keyAlias);

        String jwtString = new EncryptedJwt((JweHeader) jwt.getHeader(), jwt.getClaimsSet(), publicKey).build();

        jwtSessionCookie.setValue(jwtString);

        return jwtSessionCookie;
    }

    /**
     * Creates a JWT after a successful authentication and sets it as a Cookie on the response. An expiration time
     * is included in the JWT to limit the life of the JWT.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {

        Map<String, Object> jwtParameters = new HashMap<String, Object>(messageInfo.getMap());

        if (jwtParameters.containsKey(SKIP_SESSION_PARAMETER_NAME)
                && ((Boolean) jwtParameters.get(SKIP_SESSION_PARAMETER_NAME))) {
            DEBUG.debug("Skipping creating session as jwtParameters contains, " + SKIP_SESSION_PARAMETER_NAME);
            return AuthStatus.SEND_SUCCESS;
        }

        boolean jwtValidated = messageInfo.getMap().containsKey(JWT_VALIDATED_KEY);
        if (!jwtValidated) {
            // create jwt
            HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
            Cookie jwtSessionCookie = createSessionJwtCookie(jwtParameters);
            response.addCookie(jwtSessionCookie);
        }


        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * Creates the session JWT, including the custom parameters in the payload and adding the expiration time and then
     * sets the JWT onto the response as a Cookie.
     *
     * @param jwtParameters The parameters that should be added to the JWT payload.
     * @return The JWT Session Cookie.
     * @throws AuthException If there is a problem creating and encrypting the JWT.
     */
    private Cookie createSessionJwtCookie(Map<String, Object> jwtParameters) throws AuthException {

        KeystoreManager keystoreManager = new KeystoreManager(privateKeyPassword, keystoreType,
                keystoreFile, keystorePassword);

        RSAPublicKey publicKey = (RSAPublicKey) keystoreManager.getPublicKey(keyAlias);

        JwtBuilder jwtBuilder = new JwtBuilder();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        final Date now = calendar.getTime();
        calendar.add(Calendar.MINUTE, maxTokenLife);
        final Date exp = calendar.getTime();
        Date nbf = now;
        Date iat = now;
        calendar.setTime(now);
        calendar.add(Calendar.MINUTE, tokenIdleTime);
        Date tokenIdleTime = calendar.getTime();
        String jti = UUID.randomUUID().toString();

        String jwtString = jwtBuilder
                .jwe(publicKey)
                .headers()
                .alg(JweAlgorithm.RSAES_PKCS1_V1_5)
                .enc(EncryptionMethod.A128CBC_HS256)
                .done()
                .claims()
                .jti(jti)
                .exp(exp)
                .nbf(nbf)
                .iat(iat)
                .claim(TOKEN_IDLE_TIME_CLAIM_KEY, tokenIdleTime.getTime() / 1000L) //TODO maybe get jwt to make conversion?
                .claims(jwtParameters)
                .done()
                .build();


        Cookie cookie = new Cookie(JWT_SESSION_COOKIE_NAME, jwtString);
        cookie.setPath("/");
        cookie.setMaxAge(new Long(exp.getTime() - now.getTime()).intValue() / 1000);

        return cookie;
    }

    /**
     * No cleaning for the Subject is required for this module.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
    }
}
