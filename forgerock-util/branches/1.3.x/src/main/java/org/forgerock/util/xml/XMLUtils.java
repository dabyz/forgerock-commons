/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: XMLUtils.java,v 1.15 2009/10/19 18:19:20 asyhuang Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */
package org.forgerock.util.xml;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * Utility classes for handling XML.
 */
public final class XMLUtils {

    private static final Logger LOGGER = Logger.getLogger(XMLUtils.class.getName());
    private static final Object SECURITY_MANAGER;

    /**
     * When Xerces is used for XML parsing, the only way to control entityExpansionLimit is to override the default
     * SecurityManager. The following block will ensure that a Xerces SecurityManager is created and configured to have
     * a less permissive entityExpansionLimit.
     * In case Xerces is not used, but the JDK's XML parser implementation is leveraged, applications should enforce
     * entity expansion limits by following the <a href="JAXP.java.net/1.4/JAXP-Compatibility.html#JAXP_security">
     * JAXP configuration guide</a>.
     */
    static {
        Object securityManager;
        try {
            Class<?> securityManagerClass = Class.forName("org.apache.xerces.util.SecurityManager");
            securityManager = securityManagerClass.newInstance();
            Integer limit = Integer.getInteger("org.forgerock.util.xml.entity.expansion.limit", 5000);
            Method setEntityExpansionLimit = securityManagerClass.getMethod("setEntityExpansionLimit", int.class);
            setEntityExpansionLimit.invoke(securityManager, limit);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Unable to set expansion limit for Xerces, using default settings", ex);
            securityManager = null;
        }
        SECURITY_MANAGER = securityManager;
    }

    private XMLUtils() {
        // No impl.
    }

    /**
     * Provides a secure DocumentBuilder implementation, which is protected against
     * different types of entity expansion attacks and makes sure that only locally
     * available DTDs can be referenced within the XML document.
     * @param validating Whether the returned DocumentBuilder should validate input.
     * @return A secure DocumentBuilder instance.
     * @throws javax.xml.parsers.ParserConfigurationException In case xerces does not support one
     * of the required features.
     */
    public static DocumentBuilder getSafeDocumentBuilder(boolean validating) throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(validating);
        dbf.setNamespaceAware(true);
        dbf.setXIncludeAware(false);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setExpandEntityReferences(false);
        if (SECURITY_MANAGER != null) {
            dbf.setAttribute("http://apache.org/xml/properties/security-manager", SECURITY_MANAGER);
        }
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new XMLHandler());
        return db;
    }

    /**
     * Provides a secure SAXParser instance, which is protected against different
     * types of entity expension, DoS attacks and makes sure that only locally
     * available DTDs can be referenced within the XML document.
     * @param validating Whether the returned DocumentBuilder should validate input.
     * @return A secure SAXParser instance.
     * @throws ParserConfigurationException In case Xerces does not support one of
     * the required features.
     * @throws SAXException In case Xerces does not support one of the required
     * features.
     */
    public static SAXParser getSafeSAXParser(boolean validating) throws ParserConfigurationException, SAXException {
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        saxFactory.setValidating(validating);
        saxFactory.setNamespaceAware(true);
        saxFactory.setXIncludeAware(false);
        saxFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        saxFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        saxFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        SAXParser sp = saxFactory.newSAXParser();
        if (SECURITY_MANAGER != null) {
            sp.setProperty("http://apache.org/xml/properties/security-manager", SECURITY_MANAGER);
        }
        sp.getXMLReader().setEntityResolver(new XMLHandler());
        return sp;
    }
}
