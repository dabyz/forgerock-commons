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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.pre;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.PngUtils;

import java.io.File;
import java.io.IOException;

/**
 * Create thumbnails of .png images in the modifiable copy of the sources.
 *
 * <p>
 *
 * This class creates resized thumbnail files prefixed with 'thumb_' in the
 * same directory as the original image.
 */
public class CreateThumbs {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public CreateThumbs(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Create thumbnails of .png images in the modifiable copy of
     * the sources.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException Failed to edit image file.
     */
    public void execute() throws MojoExecutionException {

        try {

            for (File image : FileUtils.listFiles(
                    m.getDocbkxModifiableSourcesDirectory(),
                    new WildcardFileFilter("*.png"),
                    TrueFileFilter.INSTANCE)) {
                PngUtils.resizePng(image);
            }

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
