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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.authz.basic.crest;

import static org.forgerock.authz.filter.api.AuthorizationResult.accessDenied;
import static org.forgerock.authz.filter.api.AuthorizationResult.accessPermitted;
import static org.forgerock.json.fluent.JsonValue.*;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.http.context.ServerContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Allows every request except for update requests.
 *
 * @since 1.5.0
 */
public class NotUpdateAuthorizationModule implements CrestAuthorizationModule {

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeCreate(ServerContext context,
            CreateRequest request) {
        return Promises.newResultPromise(accessPermitted());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeRead(ServerContext context, ReadRequest request) {
        return Promises.newResultPromise(accessPermitted());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeUpdate(ServerContext context,
            UpdateRequest request) {
        return Promises.newResultPromise(accessDenied("Update is not allowed",
                json(object(field("internalCode", 123)))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeDelete(ServerContext context,
            DeleteRequest request) {
        return Promises.newResultPromise(accessPermitted());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizePatch(ServerContext context, PatchRequest request) {
        return Promises.newResultPromise(accessPermitted());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeAction(ServerContext context,
            ActionRequest request) {
        return Promises.newResultPromise(accessPermitted());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<AuthorizationResult, ResourceException> authorizeQuery(ServerContext context, QueryRequest request) {
        return Promises.newResultPromise(accessPermitted());
    }
}
