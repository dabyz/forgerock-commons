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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

/**
 * A convenience class that dispatches requests to specific methods.
 *
 * @author Paul C. Bryan
 */
public class SimpleJsonResource implements JsonResource {

    /** Standard JSON resource request methods. */
    public enum Method {
        create, read, update, delete, patch, query, action
    }

    /**
     * Handles a JSON resource request by dispatching to the method corresponding with the
     * method member of the request. If the request method is not one of the standard JSON
     * resource request methods, a {@code JsonResourceException} is thrown. 
     * <p>
     * This method catches any thrown {@code JsonValueException}, and rethrows it as a
     * {@link JsonResourceException#BAD_REQUEST}. This allows the use of JsonValue methods
     * to validate the content of the request.
     *
     * @param request the JSON resource request.
     * @return the JSON resource response.
     * @throws if there is an exception handling the request.
     */ 
    @Override
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        try {
            try {
                switch (request.get("method").required().asEnum(Method.class)) {
                case create: return create(request);
                case read: return read(request);
                case update: return update(request);
                case delete: return delete(request);
                case patch: return patch(request);
                case query: return query(request);
                case action: return action(request);
                default: throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
                }
            } catch (JsonValueException jve) {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, jve);
            }
        }
        catch (Exception e1) {
            try {
                onException(e1); // give handler opportunity to throw its own exception
                throw e1;
            } catch (Exception e2) {
                if (e2 instanceof JsonResourceException) { // no rethrowing necessary
                    throw (JsonResourceException)e2;
                } else { // need to rethrow as resource exception
                    throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e2);
                }
            }
        }
    }

    /**
     * Called to handle a "create" JSON resource request. This implementation throws a
     * {@link JsonResourceException#FORBIDDEN} exception.
     *
     * @param request the JSON resource request.
     * @return the JSON resource response.
     * @throws if there is an exception handling the request.
     */
    protected JsonValue create(JsonValue request) throws JsonResourceException {
        throw new JsonResourceException(JsonResourceException.FORBIDDEN);
    }

    /**
     * Called to handle a "read" JSON resource request. This implementation throws a
     * {@link JsonResourceException#FORBIDDEN} exception.
     *
     * @param request the JSON resource request.
     * @return the JSON resource response.
     * @throws if there is an exception handling the request.
     */
    protected JsonValue read(JsonValue request) throws JsonResourceException {
        throw new JsonResourceException(JsonResourceException.FORBIDDEN);
    }

    /**
     * Called to handle a "update" JSON resource request. This implementation throws a
     * {@link JsonResourceException#FORBIDDEN} exception.
     *
     * @param request the JSON resource request.
     * @return the JSON resource response.
     * @throws if there is an exception handling the request.
     */
    protected JsonValue update(JsonValue request) throws JsonResourceException {
        throw new JsonResourceException(JsonResourceException.FORBIDDEN);
    }

    /**
     * Called to handle a "delete" JSON resource request. This implementation throws a
     * {@link JsonResourceException#FORBIDDEN} exception.
     *
     * @param request the JSON resource request.
     * @return the JSON resource response.
     * @throws if there is an exception handling the request.
     */
    protected JsonValue delete(JsonValue request) throws JsonResourceException {
        throw new JsonResourceException(JsonResourceException.FORBIDDEN);
    }

    /**
     * Called to handle a "patch" JSON resource request. This implementation throws a
     * {@link JsonResourceException#FORBIDDEN} exception.
     *
     * @param request the JSON resource request.
     * @return the JSON resource response.
     * @throws if there is an exception handling the request.
     */
    protected JsonValue patch(JsonValue request) throws JsonResourceException {
        throw new JsonResourceException(JsonResourceException.FORBIDDEN);
    }

    /**
     * Called to handle a "query" JSON resource request. This implementation throws a
     * {@link JsonResourceException#FORBIDDEN} exception.
     *
     * @param request the JSON resource request.
     * @return the JSON resource response.
     * @throws if there is an exception handling the request.
     */
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        throw new JsonResourceException(JsonResourceException.FORBIDDEN);
    }

    /**
     * Called to handle an "action" JSON resource request. This implementation throws a
     * {@link JsonResourceException#FORBIDDEN} exception.
     *
     * @param request the JSON resource request.
     * @return the JSON resource response.
     * @throws if there is an exception handling the request.
     */
    protected JsonValue action(JsonValue request) throws JsonResourceException {
        throw new JsonResourceException(JsonResourceException.FORBIDDEN);
    }

    /**
     * Provides the ability to handle an exception by taking additional steps such as
     * logging, and optionally to override by throwing its own {@link JsonResourceException}.
     * This implementation does nothing; it is intended to be overridden by a subclass.
     *
     * @param exception the exception that was thrown.
     * @throws JsonResourceException an optional exception to be thrown instead.
     */
    protected void onException(Exception exception) throws JsonResourceException {
        // default implementation does nothing
    }
}
