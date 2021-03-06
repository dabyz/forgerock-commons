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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.json.resource;

/**
 * The configuration which should be used when persisting {@code ServerContext}
 * instances to and from their JSON representation.
 */
public final class PersistenceConfig {
    /**
     * A builder for incremental construction of a {@code PersistenceConfig}.
     *
     * @see PersistenceConfig#builder()
     */
    public static final class Builder {
        private ClassLoader classLoader;

        private Builder() {
            // No implementation required.
        }

        /**
         * Returns a new persistence configuration having the properties of this
         * builder.
         *
         * @return A new persistence configuration having the properties of this
         *         builder.
         * @throws IllegalStateException
         *             If the connection provider has not been specified.
         */
        public PersistenceConfig build() {
            return new PersistenceConfig(classLoader);
        }

        /**
         * Sets the class loader which should be used when deserializing
         * contexts from their JSON representation.
         *
         * @param classLoader
         *            The class loader which should be used when deserializing
         *            contexts from their JSON representation, or {@code null}
         *            if the class loader associated with this class should be
         *            used.
         * @return This builder.
         */
        public Builder classLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

    }

    /**
     * Returns a new builder which can be used to incrementally configure and
     * construct a new persistence configuration.
     *
     * @return A new builder which can be used to incrementally configure and
     *         construct a new persistence configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    private final ClassLoader classLoader;

    private PersistenceConfig(final ClassLoader classLoader) {
        this.classLoader = classLoader != null ? classLoader : PersistenceConfig.class
                .getClassLoader();
    }

    /**
     * Returns the class loader which should be used when deserializing contexts
     * from their JSON representation.
     *
     * @return The class loader which should be used when deserializing contexts
     *         from their JSON representation.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

}
