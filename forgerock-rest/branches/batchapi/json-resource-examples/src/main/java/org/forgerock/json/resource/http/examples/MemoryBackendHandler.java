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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource.http.examples;

import org.forgerock.http.Handler;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.UriRouter;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.util.Reject;

/**
 * Default in-memory {@link Handler}.
 */
final class MemoryBackendHandler {

    private MemoryBackendHandler() {
    }

    static Handler getHandler() {
        return CrestHttp.newHttpHandler(Resources.newInternalConnectionFactory(
                Resources.newCollection(new MemoryBackend())));
    }
}
