/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * If applicable, add the following below this MPL 2.0 HEADER, replacing
 * the fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *     Portions Copyright [yyyy] [name of copyright owner]
 *
 *     Copyright 2012-2014 ForgeRock AS
 *
 */

package org.forgerock.doc.maven;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * Implementation to build core documentation from <a
 * href="http://www.docbook.org/tdg51/en/html/docbook.html">DocBook XML</a>
 * using <a href="http://code.google.com/p/docbkx-tools/">docbkx-tools</a>. This
 * Mojo builds various output formats, but does not lay the documentation out in
 * a site format, as layout is performed in the site phase to allow for other
 * pre-site work, such as building Javadoc and other reference content.
 *
 * @Checkstyle:ignoreFor 2
 * @goal build
 * @phase pre-site
 */
public class PreSiteBuildMojo extends AbstractBuildMojo {

    // Where the sources are currently depends on what has been done.
    // If I relied on developer skills to feed my family, we'd starve.
    private File sourceDirectory;

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execute() throws MojoExecutionException {

        if (!doUseFilteredSources()) {
            throw new MojoExecutionException("This version of the doc build"
                + " plugin requires that you use filtered sources.");
        }

        if (getJCiteOutputDirectory().exists()) {
            sourceDirectory = getJCiteOutputDirectory();
        } else {
            sourceDirectory = getFilteredDocbkxSourceDirectory();
        }

        // The Executor is what actually calls other plugins.
        Executor exec = new Executor();
        exec.unpackBranding();

        List<String> formats = getOutputFormats();

        // Prepare FOP for printable output, e.g. PDF.
        if (formats.contains("pdf") || formats.contains("rtf")) {
            getLog().info("Preparing Apache FOP...");
            copyFonts();
            exec.prepareFOP();
        }


        // Build and prepare EPUB for publishing.
        if (formats.contains("epub")) {
            getLog().info("Building EPUB...");
            exec.buildEPUB();
            getLog().info("...post-processing EPUB...");
            postProcessEPUB(getDocbkxOutputDirectory().getPath()
                    + File.separator + "epub");
        }

        // Build and prepare PDF for publishing.
        if (formats.contains("pdf")) {
            getLog().info("Building PDF...");
            getLog().info("...generating olink DB files for PDF...");
            exec.buildFoOlinkDB("pdf");
            getLog().info("...generating PDF files...");
            exec.buildPDF();
            getLog().info("...post-processing PDF...");
            postProcessPDF(getDocbkxOutputDirectory().getPath()
                    + File.separator + "pdf");
        }

        // Build and prepare RTF for publishing.
        if (formats.contains("rtf")) {
            getLog().info("Building RTF...");
            getLog().info("...generating olink DB files for RTF...");
            exec.buildFoOlinkDB("rtf");
            getLog().info("...generating RTF files...");
            exec.buildRTF();
            getLog().info("...post-processing RTF...");
            postProcessRTF(getDocbkxOutputDirectory().getPath()
                    + File.separator + "rtf");
        }

        // Build and prepare man pages for publishing.
        if (formats.contains("man")) {
            getLog().info("Building man pages...");
            exec.buildManpages();
        }

        // Build and prepare HTML for publishing.
        if (formats.contains("html")) {
            getLog().info("Drop in CSS for HTML...");
            exec.addCustomCss();
            getLog().info("Building single page HTML...");
            getLog().info("...generating olink DB files for single page HTML...");
            exec.buildSingleHTMLOlinkDB();
            getLog().info("...generating single page HTML files...");
            exec.buildSingleHTML();

            getLog().info("Building chunked HTML...");
            getLog().info("...generating olink DB files for chunked HTML...");
            exec.buildChunkedHTMLOlinkDB();
            getLog().info("...generating chunked HTML files...");
            exec.buildChunkedHTML();

            getLog().info("Add JavaScript used in HTML...");
            addScript();
            getLog().info("Add files for SyntaxHighlighter in HTML...");
            addShScripts();
            addShCss();
            getLog().info("...additional post-processing for HTML...");
            postProcessHTML(getDocbkxOutputDirectory().getPath()
                    + File.separator + "html");
        }

        // Build and prepare webhelp for publishing.
        if (formats.contains("webhelp")) {
            getLog().info("Building webhelp...");
            getLog().info("...generating olink DB files for webhelp...");
            exec.buildWebHelpOlinkDB();
            getLog().info("...generating webhelp output...");
            exec.buildWebHelp();
        }
    }

    /**
     * Copy fonts needed to the project build directory.
     *
     * @throws MojoExecutionException Copy failed
     */
    final void copyFonts() throws MojoExecutionException {
        // If you update this method, also see buildFO().
        String[] fonts = {"/fonts/DejaVuSans-Oblique.ttf",
            "/fonts/DejaVuSans.ttf",
            "/fonts/DejaVuSansCondensed-Bold.ttf",
            "/fonts/DejaVuSansCondensed-BoldOblique.ttf",
            "/fonts/DejaVuSansMono-Bold.ttf",
            "/fonts/DejaVuSansMono-BoldOblique.ttf",
            "/fonts/DejaVuSansMono-Oblique.ttf",
            "/fonts/DejaVuSansMono.ttf",
            "/fonts/DejaVuSerif-Italic.ttf",
            "/fonts/DejaVuSerif.ttf",
            "/fonts/DejaVuSerifCondensed-Bold.ttf",
            "/fonts/DejaVuSerifCondensed-BoldItalic.ttf"};

        for (String font : fonts) {
            URL src = getClass().getResource(font);
            File dest = new File(getBuildDirectory()
                    + font.replaceAll("/", File.separator));
            try {
                FileUtils.copyURLToFile(src, dest);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to copy file: "
                        + font + "\n" + e.getMessage());
            }
        }
    }

    /**
     * EPUB XSL stylesheet customization file, relative to the build directory.
     * <p/>
     * docbkx-tools element: &lt;epubCustomization&gt;
     *
     * @parameter default-value="docbkx-stylesheets/epub/coredoc.xsl"
     * @required
     */
    private String epubCustomization;

    /**
     * Prepare built EPUB documents for publication. Currently this method
     * does nothing.
     *
     * @throws MojoExecutionException Something went wrong updating files.
     */
    final void postProcessEPUB(final String epubDir) throws MojoExecutionException {
        // Do nothing.
    }

    /**
     * Rename built documents named ${getDocumentSrcName()} + extension. For
     * example, rename <code>admin-guide/index.epub</code> to
     * <code>admin-guide/OpenAM-Admin-Guide.epub</code>.
     *
     * @param base Directory under which to find the built documents
     * @param ext  File name extension including the dot, e.g. .pdf
     * @throws MojoExecutionException Something went wrong renaming files.
     */
    final void renameDocuments(final String base, final String ext) throws MojoExecutionException {
        String s = File.separator;
        String baseName = FilenameUtils.getBaseName(getDocumentSrcName());

        Set<String> docNames = DocUtils.getDocumentNames(
                sourceDirectory, getDocumentSrcName());
        if (docNames.isEmpty()) {
            throw new MojoExecutionException("No document names found.");
        }

        for (String docName : docNames) {
            String newName = DocUtils.renameDoc(getProjectName(), docName, ext);
            File src = new File(base + s + docName + s + baseName + "." + ext);
            File dest = new File(base + s + docName + s + newName);
            if (newName.equals("") || !src.renameTo(dest)) {
                throw new MojoExecutionException("Failed to rename "
                        + src.getPath() + " to " + dest.getPath());
            }
        }
    }

    /**
     * Rename a single built document ${getDocumentSrcName()} + extension. For
     * example, rename <code>index.pdf</code> to
     * <code>OpenAM-Admin-Guide.pdf</code>.
     *
     * @param file    File to rename, such as <code>index.pdf</code>
     * @param docName Simple document name such as <code>admin-guide</code>
     * @throws MojoExecutionException Something went wrong renaming the file.
     */
    final void renameDocument(final File file, final String docName) throws MojoExecutionException {

        String ext = FilenameUtils.getExtension(file.getName());
        String newName = file.getParent() + File.separator
                + DocUtils.renameDoc(getProjectName(), docName, ext);
        try {
            File newFile = new File(newName);
            if (!newFile.exists()) {
                FileUtils.moveFile(file, newFile);
            }
            getLog().info("Renamed " + file.getName() + " to " + newFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to rename " + file);
        }
    }

    /**
     * Get absolute path to a temporary Olink target database XML document that
     * points to the individual generated Olink DB files, for FO (PDF, RTF).
     *
     * @param extension File extension without the ., such as "pdf" or "rtf"
     * @return Absolute path to the temporary file
     * @throws MojoExecutionException Could not write target DB file.
     */
    final String buildFOTargetDB(final String extension) throws MojoExecutionException {
        File targetDB = new File(getBuildDirectory(), "olinkdb-" + extension + ".xml");

        try {
            StringBuilder content = new StringBuilder();
            content.append("<?xml version='1.0' encoding='utf-8'?>\n")
                    .append("<!DOCTYPE targetset[\n");

            String targetDbDtd = IOUtils.toString(getClass()
                    .getResourceAsStream("/targetdatabase.dtd"));
            content.append(targetDbDtd).append("\n");

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {
/*  <targetsFilename> is ignored with docbkx-tools 2.0.15.
                String sysId = getBuildDirectory().getAbsolutePath()
                        + File.separator + docName + "-" + extension + ".target.db";
*/
                String sysId = getBaseDir().getAbsolutePath()
                        + "/target/docbkx/" + extension + "/" + docName
                        + "/index.fo.target.db";

                content.append("<!ENTITY ").append(docName)
                        .append(" SYSTEM '").append(sysId).append("'>\n");
            }

            content.append("]>\n")

                    .append("<targetset>\n")
                    .append(" <targetsetinfo>Target DB for ForgeRock DocBook content,\n")
                    .append(" for use with ")
                    .append(extension.toUpperCase())
                    .append(" only.</targetsetinfo>\n")
                    .append(" <sitemap>\n")
                    .append("  <dir name='doc'>\n");

            final String version = getReleaseVersion();
            for (String docName : docNames) {
                String fileName = DocUtils.renameDoc(
                        getProjectName(), docName, version, extension);

                content.append("   <document targetdoc='").append(docName).append("'")
                        .append("             baseuri='").append(fileName).append("'>")
                        .append("    &").append(docName).append(";")
                        .append("   </document>\n");
            }
            content.append("  </dir>\n")
                    .append(" </sitemap>\n")
                    .append("</targetset>\n");

            FileUtils.writeStringToFile(targetDB, content.toString());
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to write link target database: " + e.getMessage());
        }
        return targetDB.getPath();
    }

    /**
     * FO XSL stylesheet customization file (for PDF, RTF), relative to the
     * build directory.
     * <p/>
     * docbkx-tools element: &lt;foCustomization&gt;
     *
     * @parameter default-value="docbkx-stylesheets/fo/coredoc.xsl"
     * @required
     */
    private String foCustomization;

    /**
     * Prepare built PDF documents for publication. Currently this method does
     * not do anything.
     *
     * @param pdfDir Directory under which to find the built files
     * @throws MojoExecutionException Something went wrong updating files.
     */
    final void postProcessPDF(final String pdfDir) throws MojoExecutionException {
    }

    /**
     * Prepare built RTF documents for publication. Currently this method does
     * not do anything.
     *
     * @param rtfDir Directory under which to find the built files
     * @throws MojoExecutionException Something went wrong updating files.
     */
    final void postProcessRTF(final String rtfDir) throws MojoExecutionException {
    }

    /**
     * Man page XSL stylesheet customization file, relative to the build
     * directory.
     * <p/>
     * docbkx-tools element: &lt;manpagesCustomization&gt;
     *
     * @parameter default-value="docbkx-stylesheets/man/coredoc.xsl"
     * @required
     */
    private String manpagesCustomization;

    /**
     * Single page HTML XSL stylesheet customization file, relative to the build
     * directory.
     * <p/>
     * docbkx-tools element: &lt;htmlCustomization&gt;
     *
     * @parameter default-value="/docbkx-stylesheets/html/coredoc.xsl"
     * @required
     */
    private String singleHTMLCustomization;

    /**
     * URL to site for published documentation.
     *
     * @parameter default-value="http://docs.forgerock.org/" property="docsSite"
     * @required
     */
    private String docsSite;

    /**
     * JavaScript file name, found under {@code /js/} in plugin resources.
     *
     * @parameter default-value="uses-jquery.js" property="javaScriptFileName"
     * @required
     */
    private String javaScriptFileName;

    /**
     * Add JavaScript to include in HTML in each document source directory.
     * See <a href="http://docbook.sourceforge.net/release/xsl/current/doc/html/html.script.html"
     * >html.script</a> for details.
     *
     * @throws MojoExecutionException Failed to add script.
     */
    void addScript() throws MojoExecutionException {

        final URL scriptUrl = getClass().getResource("/js/" + javaScriptFileName);
        String scriptString;
        try {
            scriptString = IOUtils.toString(scriptUrl);
        } catch (IOException ie) {
            throw new MojoExecutionException("Failed to read " + scriptUrl, ie);
        }

        if (scriptString != null) {
            scriptString = scriptString.replace("PROJECT_NAME", getProjectName().toLowerCase());
            scriptString = scriptString.replace("PROJECT_VERSION", getProjectVersion());
            scriptString = scriptString.replace("LATEST_JSON", getLatestJson());
            scriptString = scriptString.replace("DOCS_SITE", docsSite);
        } else {
            throw new MojoExecutionException(scriptUrl + " was empty");
        }

        final Set<String> docNames = DocUtils.getDocumentNames(
                sourceDirectory, getDocumentSrcName());
        if (docNames.isEmpty()) {
            throw new MojoExecutionException("No document names found.");
        }

        // The html.script parameter should probably take URLs.
        // When local files are referenced,
        // the DocBook XSL stylesheets do not copy the .js files.
        // Instead the files must be copied to the output directories.
        final String[] outputDirectories =
        {"", File.separator + FilenameUtils.getBaseName(getDocumentSrcName())};

        for (final String outputDirectory : outputDirectories) {

            for (final String docName : docNames) {

                final File parent = new File(getDocbkxOutputDirectory(),
                        "html" + File.separator + docName + outputDirectory);
                final File scriptFile = new File(parent, javaScriptFileName);

                try {
                    FileUtils.writeStringToFile(scriptFile, scriptString, "UTF-8");
                } catch (IOException ie) {
                    throw new MojoExecutionException(
                            "Failed to write to " + scriptFile.getPath(), ie);
                }

            }

        }

    }

    /**
     * SyntaxHighlighter JavaScript files.
     */
    private final String[] shJavaScriptFiles = {"shCore.js",
        "shBrushAci.js",
        "shBrushBash.js",
        "shBrushCsv.js",
        "shBrushHttp.js",
        "shBrushJava.js",
        "shBrushJScript.js",
        "shBrushLDIF.js",
        "shBrushPlain.js",
        "shBrushProperties.js",
        "shBrushXml.js"};

    /**
     * Add SyntaxHighlighter JavaScript files in each HTML document source directory.
     *
     * @throws MojoExecutionException Failed to add scripts.
     */
    void addShScripts() throws MojoExecutionException {

        final Set<String> docNames = DocUtils.getDocumentNames(
                sourceDirectory, getDocumentSrcName());
        if (docNames.isEmpty()) {
            throw new MojoExecutionException("No document names found.");
        }

        for (String scriptName : shJavaScriptFiles) {
            URL scriptUrl = getClass().getResource("/js/" + scriptName);

            // The html.script parameter should probably take URLs.
            // When local files are referenced,
            // the DocBook XSL stylesheets do not copy the .js files.
            // Instead the files must be copied to the output directories.
            final String[] outputDirectories =
            {"", File.separator + FilenameUtils.getBaseName(getDocumentSrcName())};

            for (final String outputDirectory : outputDirectories) {

                for (final String docName : docNames) {

                    final File parent = new File(getDocbkxOutputDirectory(),
                            "html" + File.separator + docName + outputDirectory
                                    + File.separator + "sh");
                    final File scriptFile = new File(parent, scriptName);

                    try {
                        FileUtils.copyURLToFile(scriptUrl, scriptFile);
                    } catch (IOException ie) {
                        throw new MojoExecutionException(
                                "Failed to write to " + scriptFile.getPath(), ie);
                    }
                }
            }
        }
    }

    /**
     * SyntaxHighlighter CSS files.
     */
    private final String[] shCssFiles = {"shCore.css", "shCoreEclipse.css", "shThemeEclipse.css"};

    /**
     * Add SyntaxHighlighter CSS files in each HTML document source directory.
     *
     * @throws MojoExecutionException Failed to add scripts.
     */
    void addShCss() throws MojoExecutionException {

        final Set<String> docNames = DocUtils.getDocumentNames(
                sourceDirectory, getDocumentSrcName());
        if (docNames.isEmpty()) {
            throw new MojoExecutionException("No document names found.");
        }

        for (String styleSheetName : shCssFiles) {
            URL styleSheetUrl = getClass().getResource("/css/" + styleSheetName);

            // The html.stylesheet parameter should probably take URLs.
            // When local files are referenced,
            // the DocBook XSL stylesheets do not copy the .css files.
            // Instead the files must be copied to the output directories.
            final String[] outputDirectories =
            {"", File.separator + FilenameUtils.getBaseName(getDocumentSrcName())};

            for (final String outputDirectory : outputDirectories) {

                for (final String docName : docNames) {

                    final File parent = new File(getDocbkxOutputDirectory(),
                            "html" + File.separator + docName + outputDirectory
                                + File.separator + "sh");
                    final File styleSheetFile = new File(parent, styleSheetName);

                    try {
                        FileUtils.copyURLToFile(styleSheetUrl, styleSheetFile);
                    } catch (IOException ie) {
                        throw new MojoExecutionException(
                                "Failed to write to " + styleSheetFile.getPath(), ie);
                    }
                }
            }
        }
    }
    /**
     * Get absolute path to a temporary Olink target database XML document that
     * points to the individual generated Olink DB files, for single page HTML.
     *
     * @return Absolute path to the temporary file
     * @throws MojoExecutionException Could not write target DB file.
     */
    final String buildSingleHTMLTargetDB() throws MojoExecutionException {
        File targetDB = new File(getBuildDirectory() + File.separator
                + "olinkdb-single-page-html.xml");
        try {
            StringBuilder content = new StringBuilder();
            content.append("<?xml version='1.0' encoding='utf-8'?>\n")
                    .append("<!DOCTYPE targetset [\n");

            String targetDbDtd = IOUtils.toString(getClass()
                    .getResourceAsStream("/targetdatabase.dtd"));
            content.append(targetDbDtd).append("\n");

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {

/*  <targetsFilename> is ignored with docbkx-tools 2.0.15.
                String sysId = getBuildDirectory().getAbsolutePath()
                        + File.separator + docName + "-single.target.db";
*/
                String sysId = getBaseDir().getAbsolutePath()
                        + "/target/docbkx/html/" + docName + "/index.html.target.db";

                content.append("<!ENTITY ").append(docName)
                        .append(" SYSTEM '").append(sysId).append("'>\n");
            }

            content.append("]>\n")

                    .append("<targetset>\n")
                    .append(" <targetsetinfo>Target DB for ForgeRock DocBook content,\n")
                    .append(" for use with single page HTML only.</targetsetinfo>\n")
                    .append(" <sitemap>\n")
                    .append("  <dir name='doc'>\n");

            for (String docName : docNames) {
                content.append("   <document targetdoc='").append(docName).append("'\n")
                        .append("             baseuri='../").append(docName)
                        .append("/").append(FilenameUtils.getBaseName(getDocumentSrcName()))
                        .append(".html'>\n")
                        .append("    &").append(docName).append(";\n")
                        .append("   </document>\n");
            }
            content.append("  </dir>\n")
                    .append(" </sitemap>\n")
                    .append("</targetset>\n");

            FileUtils.writeStringToFile(targetDB, content.toString());
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to write link target database: " + e.getMessage());
        }
        return targetDB.getPath();
    }

    /**
     * Chunked HTML XSL stylesheet customization file, relative to the build
     * directory.
     * <p/>
     * docbkx-tools element: &lt;htmlCustomization&gt;
     *
     * @parameter default-value="docbkx-stylesheets/html/chunked.xsl"
     * @required
     */
    private String chunkedHTMLCustomization;

    /**
     * Get absolute path to a temporary Olink target database XML document that
     * points to the individual generated Olink DB files, for chunked HTML.
     *
     * @return Absolute path to the temporary file
     * @throws MojoExecutionException Could not write target DB file.
     */
    final String buildChunkedHTMLTargetDB() throws MojoExecutionException {
        File targetDB = new File(getBuildDirectory() + File.separator
                + "olinkdb-chunked-html.xml");
        try {
            StringBuilder content = new StringBuilder();
            content.append("<?xml version='1.0' encoding='utf-8'?>\n")
                    .append("<!DOCTYPE targetset [\n");

            String targetDbDtd = IOUtils.toString(getClass()
                    .getResourceAsStream("/targetdatabase.dtd"));
            content.append(targetDbDtd).append("\n");

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {
/*  <targetsFilename> is ignored with docbkx-tools 2.0.15.
                String sysId = getBuildDirectory().getAbsolutePath()
                        + File.separator + docName + "-chunked.target.db";
*/
                String sysId = getBaseDir().getAbsolutePath() + "/target/docbkx/html/"
                        + docName + "/index.html.target.db";

                content.append("<!ENTITY ").append(docName)
                        .append(" SYSTEM '").append(sysId).append("'>\n");
            }

            content.append("]>\n")

                    .append("<targetset>\n")
                    .append(" <targetsetinfo>Target DB for ForgeRock DocBook content,\n")
                    .append(" for use with chunked HTML only.</targetsetinfo>\n")
                    .append(" <sitemap>\n")
                    .append("  <dir name='doc'>\n");

            String baseName = FilenameUtils.getBaseName(getDocumentSrcName());
            for (String docName : docNames) {
                content.append("   <dir name='").append(docName).append("'>\n")
                        .append("    <dir name='").append(baseName).append("'>\n")
                        .append("     <document targetdoc='").append(docName).append("'\n")
                        .append("               baseuri='../../")
                        .append(docName).append("/").append(baseName).append("/'>\n")
                        .append("      &").append(docName).append(";\n")
                        .append("     </document>\n")
                        .append("    </dir>\n")
                        .append("   </dir>\n");
            }

            content.append("  </dir>\n")
                    .append(" </sitemap>\n")
                    .append("</targetset>\n");

            FileUtils.writeStringToFile(targetDB, content.toString());
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to write link target database: " + e.getMessage());
        }
        return targetDB.getPath();
    }

    /**
     * Prepare single and chunked HTML for publication.
     * <p/>
     * The HTML built by docbkx-tools does not currently include the following,
     * which this method adds.
     * <ul>
     * <li>A DOCTYPE declaration (needed to get Internet Explorer to interpret
     * CSS correctly</li>
     * <li>A meta tag for controlling crawling and indexing</li>
     * <li>JavaScript to workaround a long-standing Firefox issue, and the
     * fold/unfold long lines in Screen elements</li>
     * <li>A favicon link</li>
     * <li>JavaScript used by Google Analytics</li>
     * <li>CSS to style the HTML</li>
     * </ul>
     *
     * @param htmlDir Directory under which to find HTML output
     * @throws MojoExecutionException Something went wrong when updating HTML.
     */
    final void postProcessHTML(final String htmlDir) throws MojoExecutionException {
        try {
            getLog().info("Editing built HTML...");
            HashMap<String, String> replacements = new HashMap<String, String>();

            String doctype = IOUtils.toString(
                    getClass().getResourceAsStream("/starthtml-doctype.txt"),
                    "UTF-8");
            replacements.put("<html>", doctype);

            // See https://developers.google.com/webmasters/control-crawl-index/docs/robots_meta_tag
            String robots = "<head>" + System.getProperty("line.separator")
                    + IOUtils.toString(getClass().getResourceAsStream("/robots.txt"), "UTF-8");
            replacements.put("<head>", robots);

            String favicon = IOUtils.toString(getClass()
                    .getResourceAsStream("/endhead-favicon.txt"), "UTF-8");
            favicon = favicon.replace("FAVICON-LINK", getFaviconLink());
            replacements.put("</head>", favicon);

            String linkToJira = getLinkToJira();
            String gascript = IOUtils.toString(
                    getClass().getResourceAsStream("/endbody-ga.txt"), "UTF-8");
            gascript = gascript.replace("ANALYTICS-ID", getGoogleAnalyticsId());
            replacements.put("</body>", linkToJira + "\n" + gascript);

            HTMLUtils.updateHTML(htmlDir, replacements);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to update output HTML correctly: " + e.getMessage());
        }
    }

    /**
     * Return a &lt;p&gt; containing a link to log a bug in Jira. The string
     * is not localized.
     *
     * @return &lt;p&gt; containing a link to log a bug in Jira
     */
    final String getLinkToJira() {
        String link = "<p>&nbsp;</p><div id=\"footer\"><p>Something wrong on this page? "
                + "<a href=\"JIRA-URL\">Log a documentation bug.</a></p></div>";

        // https://confluence.atlassian.com/display/JIRA/Creating+Issues+via+direct+HTML+links
        String jiraURL = "";
        if (getProjectName().equalsIgnoreCase("OpenAM")) {
            jiraURL = "https://bugster.forgerock.org/jira/secure/CreateIssueDetails!init.jspa?"
                    + "pid=10000&components=10007&issuetype=1";
        }
        if (getProjectName().equalsIgnoreCase("OpenDJ")) {
            jiraURL = "https://bugster.forgerock.org/jira/secure/CreateIssueDetails!init.jspa?"
                    + "pid=10040&components=10132&issuetype=1";
        }
        if (getProjectName().equalsIgnoreCase("OpenICF")) {
            jiraURL = "https://bugster.forgerock.org/jira/secure/CreateIssueDetails!init.jspa?"
                    + "pid=10041&components=10170&issuetype=1";
        }
        if (getProjectName().equalsIgnoreCase("OpenIDM")) {
            jiraURL = "https://bugster.forgerock.org/jira/secure/CreateIssueDetails!init.jspa?"
                    + "pid=10020&components=10164&issuetype=1";
        }
        if (getProjectName().equalsIgnoreCase("OpenIG")) {
            jiraURL = "https://bugster.forgerock.org/jira/secure/CreateIssueDetails!init.jspa?"
                    + "pid=10060&components=10220&issuetype=1";
        }
        if (getProjectName().equalsIgnoreCase("ForgeRock")) { // Just testing
            jiraURL = "https://bugster.forgerock.org/jira/secure/CreateIssueDetails!init.jspa?"
                    + "pid=10010&issuetype=1";
        }

        if (jiraURL.equals("")) {
            link = "";
        } else {
            link = link.replaceFirst("JIRA-URL", jiraURL);
        }
        return link;
    }

    /**
     * Get absolute path to a temporary Olink target database XML document that
     * points to the individual generated Olink DB files, for webhelp.
     *
     * @return Absolute path to the temporary file
     * @throws MojoExecutionException Could not write target DB file.
     */
    final String buildWebHelpTargetDB() throws MojoExecutionException {

        File targetDB = new File(getBuildDirectory() + File.separator + "olinkdb-webhelp.xml");

        try {
            StringBuilder content = new StringBuilder();
            content.append("<?xml version='1.0' encoding='utf-8'?>\n")
                    .append("<!DOCTYPE targetset [\n");

            String targetDbDtd = IOUtils.toString(getClass()
                    .getResourceAsStream("/targetdatabase.dtd"));
            content.append(targetDbDtd).append("\n");

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {

/*  <targetsFilename> is ignored with docbkx-tools 2.0.15.
                String sysId = getBuildDirectory().getAbsolutePath()
                        + File.separator + docName + "-webhelp.target.db";
*/
                String sysId = getBaseDir().getAbsolutePath()
                        + "/target/docbkx/webhelp/" + docName + "/index.webhelp.target.db";

                content.append("<!ENTITY ").append(docName)
                        .append(" SYSTEM '").append(sysId).append("'>\n");
            }

            content.append("]>\n")

                    .append("<targetset>\n")
                    .append(" <targetsetinfo>Target DB for ForgeRock DocBook content,\n")
                    .append(" for use with webhelp only.</targetsetinfo>\n")
                    .append(" <sitemap>\n")
                    .append("  <dir name='doc'>\n");

            for (String docName : docNames) {
                content.append("   <document targetdoc='").append(docName).append("'\n")
                        .append("             baseuri='../").append(docName).append("/'>\n")
                        .append("    &").append(docName).append(";\n")
                        .append("   </document>\n");
            }
            content.append("  </dir>\n")
                    .append(" </sitemap>\n")
                    .append("</targetset>\n");

            FileUtils.writeStringToFile(targetDB, content.toString());
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to write link target database: " + e.getMessage());
        }
        return targetDB.getPath();
    }

    /**
     * Directory where fonts and font metrics are stored, relative to the build
     * directory.
     *
     * @parameter default-value="fonts" property="fontsDirectory"
     * @required
     */
    private String fontsDirectory;

    /**
     * Should WinAnsi encoding be used for embedded fonts?
     *
     * @parameter default-value="false"
     * @required
     */
    private String ansi;

    /**
     * Are these draft documents, rather than final documents?
     * <p/>
     * docbkx-tools element: &lt;draftMode&gt;
     *
     * @parameter default-value="yes" property="isDraftMode"
     * @required
     */
    private String isDraftMode;

    /**
     * For draft mode, URL to the background watermark image.
     * <p/>
     * docbkx-tools element: &lt;draftWatermarkImage&gt;
     *
     * @parameter default-value=
     * "http://docbook.sourceforge.net/release/images/draft.png"
     * property="draftWatermarkURL"
     * @required
     */
    private String draftWatermarkURL;

    /**
     * Should &lt;programlisting&gt; content have syntax highlighting?
     * <p/>
     * docbkx-tools element: &lt;highlightSource&gt;
     *
     * @parameter default-value="1" property="useSyntaxHighlighting"
     * @required
     */
    private String useSyntaxHighlighting;

    /**
     * Should sections have numeric labels?
     * <p/>
     * docbkx-tools element: &lt;sectionAutolabel&gt;
     *
     * @parameter default-value="true" property="areSectionsAutolabeled"
     * @required
     */
    private String areSectionsAutolabeled;

    /**
     * Should section labels include parent numbers, like 1.1, 1.2, 1.2.1,
     * 1.2.2?
     * <p/>
     * docbkx-tools element: &lt;sectionLabelIncludesComponentLabel&gt;
     *
     * @parameter default-value="true"
     * property="doesSectionLabelIncludeComponentLabel"
     * @required
     */
    private String doesSectionLabelIncludeComponentLabel;

    /**
     * Should documents be allowed to include other documents?
     * <p/>
     * docbkx-tools element: &lt;xincludeSupported&gt;
     *
     * @parameter default-value="true" property="isXincludeSupported"
     * @required
     */
    private String isXincludeSupported;

    /**
     * EPUB XSL stylesheet customization file, relative to the build directory.
     * <p/>
     * docbkx-tools element: &lt;epubCustomization&gt;
     *
     * @return {@link #epubCustomization}
     */
    public final File getEpubCustomization() {
        return new File(getBuildDirectory(), epubCustomization);
    }

    /**
     * FO XSL stylesheet customization file (for PDF, RTF), relative to the
     * build directory.
     * <p/>
     * docbkx-tools element: &lt;foCustomization&gt;
     *
     * @return {@link #foCustomization}
     */
    public final File getFoCustomization() {
        return new File(getBuildDirectory(), foCustomization);
    }

    /**
     * Man page XSL stylesheet customization file, relative to the build
     * directory.
     * <p/>
     * docbkx-tools element: &lt;manpagesCustomization&gt;
     *
     * @return {@link #manpagesCustomization}
     */
    public final File getManpagesCustomization() {
        return new File(getBuildDirectory(), manpagesCustomization);
    }

    /**
     * Single page HTML XSL stylesheet customization file, relative to the build
     * directory.
     * <p/>
     * docbkx-tools element: &lt;htmlCustomization&gt;
     *
     * @return {@link #singleHTMLCustomization}
     */
    public final File getSingleHTMLCustomization() {
        return new File(getBuildDirectory(), singleHTMLCustomization);
    }

    /**
     * Chunked HTML XSL stylesheet customization file, relative to the build
     * directory.
     * <p/>
     * docbkx-tools element: &lt;htmlCustomization&gt;
     *
     * @return {@link #chunkedHTMLCustomization}
     */
    public final File getChunkedHTMLCustomization() {
        return new File(getBuildDirectory(), chunkedHTMLCustomization);
    }

    /**
     * Webhelp XSL stylesheet customization file, relative to the build
     * directory.
     * <p/>
     * docbkx-tools element: &lt;webhelpCustomization&gt;
     *
     * @parameter default-value="docbkx-stylesheets/webhelp/coredoc.xsl"
     * @required
     */
    private String webhelpCustomization;

    /**
     * Webhelp XSL stylesheet customization file, relative to the build
     * directory.
     * <p/>
     * docbkx-tools element: &lt;webhelpCustomization&gt;
     *
     * @return {@link #webhelpCustomization}
     */
    public final File getWebHelpCustomization() {
        return new File(getBuildDirectory(), webhelpCustomization);
    }

    /**
     * Logo for webhelp documents.
     *
     * @parameter default-value="docbkx-stylesheets/webhelp/logo.png" property="webhelpLogo"
     * @required
     */
    private String webhelpLogo;

    /**
     * Logo for webhelp documents.
     *
     * @return {@link #webhelpLogo}
     */
    public final File getWebHelpLogo() {
        return new File(getBuildDirectory(), webhelpLogo);
    }

    /**
     * Main CSS for webhelp documents.
     *
     * @parameter default-value="docbkx-stylesheets/webhelp/positioning.css" property="webhelpCss"
     * @required
     */
    private String webhelpCss;

    /**
     * Main CSS for webhelp documents.
     *
     * @return {@link #webhelpCss}
     */
    public final File getWebHelpCss() {
        return new File(getBuildDirectory(), webhelpCss);
    }

    /**
     * Directory where fonts and font metrics are stored, relative to the build
     * directory.
     *
     * @return {@link #fontsDirectory}
     */
    public final File getFontsDirectory() {
        return new File(getBuildDirectory(), fontsDirectory);
    }

    /**
     * Should WinAnsi encoding be used for embedded fonts?
     *
     * @return {@link #ansi}
     */
    public final String useAnsi() {
        return ansi;
    }

    /**
     * Are these draft documents, rather than final documents?
     * <p/>
     * docbkx-tools element: &lt;draftMode&gt;
     *
     * @return {@link #isDraftMode}
     */
    public final String isDraftMode() {
        return isDraftMode;
    }

    /**
     * For draft mode, URL to the background watermark image.
     * <p/>
     * docbkx-tools element: &lt;draftWatermarkImage&gt;
     *
     * @return {@link #draftWatermarkURL}
     */
    public final String getDraftWatermarkURL() {
        return draftWatermarkURL;
    }

    /**
     * Should &lt;programlisting&gt; content have syntax highlighting?
     * <p/>
     * docbkx-tools element: &lt;highlightSource&gt;
     *
     * @return {@link #useSyntaxHighlighting}
     */
    public final String useSyntaxHighlighting() {
        return useSyntaxHighlighting;
    }

    /**
     * Should sections have numeric labels?
     * <p/>
     * docbkx-tools element: &lt;sectionAutolabel&gt;
     *
     * @return {@link #areSectionsAutolabeled}
     */
    public final String areSectionsAutolabeled() {
        return areSectionsAutolabeled;
    }

    /**
     * Should section labels include parent numbers, like 1.1, 1.2, 1.2.1,
     * 1.2.2?
     * <p/>
     * docbkx-tools element: &lt;sectionLabelIncludesComponentLabel&gt;
     *
     * @return {@link #doesSectionLabelIncludeComponentLabel}
     */
    public final String doesSectionLabelIncludeComponentLabel() {
        return doesSectionLabelIncludeComponentLabel;
    }

    /**
     * Should documents be allowed to include other documents?
     * <p/>
     * docbkx-tools element: &lt;xincludeSupported&gt;
     *
     * @return {@link #isXincludeSupported}
     */
    public final String isXincludeSupported() {
        return isXincludeSupported;
    }

    /**
     * Version for this release.
     *
     * @parameter property="releaseVersion"
     */
    private String releaseVersion;

    /**
     * Version for this release.
     *
     * @return {@link #releaseVersion}
     */
    public final String getReleaseVersion() {
        return releaseVersion;
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Unpack branding.
         *
         * @throws MojoExecutionException
         */
        void unpackBranding() throws MojoExecutionException {
            final String outputDir = FilenameUtils.separatorsToUnix(
                    getBuildDirectory().getPath());

            executeMojo(
                    plugin(
                            groupId("org.apache.maven.plugins"),
                            artifactId("maven-dependency-plugin"),
                            version("2.8")),
                    goal("unpack"),
                    configuration(
                            element("artifactItems",
                                    element("artifactItem",
                                            element("groupId", getBrandingGroupId()),
                                            element("artifactId", getBrandingArtifactId()),
                                            element("version", getBrandingVersion()),
                                            element("type", "jar"),
                                            element("overWrite", "true"),
                                            element("outputDirectory", outputDir),
                                            element("includes", "**/*.*")))),
                    executionEnvironment(
                            getProject(),
                            getSession(),
                            getPluginManager()));

        }

        /**
         * Support a subset of formats described in the documentation for the <a
         * href="http://www.docbook.org/tdg/en/html/imagedata.html">ImageData</a>
         * element.
         */
        private final String[] imageFileSuffixes =
        {".bmp", ".eps", ".gif", ".jpeg", ".jpg", ".png", ".svg", ".tiff"};

        /**
         * Copy images from source to destination.
         * <p>
         * DocBook XSL does not copy the images,
         * because XSL does not have a facility for copying files.
         * Unfortunately, neither does docbkx-tools.
         *
         * @param docType Type of output document such as {@code epub} or {@code html}
         * @param baseName Directory name to add, such as {@code index}.
         * @param outBaseDir Base directory where the output is found.
         *
         * @throws MojoExecutionException Something went wrong copying images.
         */
        private void copyImages(final String docType, final String baseName, final File outBaseDir)
                throws MojoExecutionException {

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            String s = File.separator;
            String extra = "";
            if (!baseName.equalsIgnoreCase("")) {
                extra = s + baseName;
            }

            FileFilter onlyImages = new SuffixFileFilter(imageFileSuffixes);

            for (String docName : docNames) {

                // Copy images specific to the document.
                File srcDir = new File(sourceDirectory, docName + s + "images");
                File destDir = new File(outBaseDir, docType + s + docName + extra + s + "images");
                try {
                    if (srcDir.exists()) {
                        FileUtils.copyDirectory(srcDir, destDir, onlyImages);
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException(
                            "Failed to copy images from " + srcDir + " to " + destDir);
                }

                // Copy any shared images.
                String shared = "shared" + s + "images";
                srcDir = new File(sourceDirectory, shared);
                destDir = new File(outBaseDir, docType + s + docName + extra + s + shared);
                try {
                    if (srcDir.exists()) {
                        FileUtils.copyDirectory(srcDir, destDir, onlyImages);
                    }
                } catch (IOException ioe) {
                    throw new MojoExecutionException(
                            "Failed to copy images from " + srcDir + " to " + destDir);
                }
            }
        }

        private void copyImages(final String docType, final String baseName)
                throws MojoExecutionException {
            copyImages(docType, baseName, getDocbkxOutputDirectory());
        }

        private void copyImages(final String docType) throws MojoExecutionException {
            copyImages(docType, "");
        }

        /**
         * Prepare Apache FOP for output formats like PDF. This step involves
         * font metrics generation.
         *
         * @throws MojoExecutionException Failed to prepare FOP.
         */
        void prepareFOP() throws MojoExecutionException {
            String fontsDir = FilenameUtils.separatorsToUnix(
                    getFontsDirectory().getPath());
            executeMojo(
                    plugin(groupId("com.agilejava.docbkx"),
                            artifactId("docbkx-fop-support"),
                            version(getDocbkxVersion())),
                    goal("generate"),
                    configuration(element(name("ansi"), useAnsi()),
                            element(name("sourceDirectory"), fontsDir),
                            element(name("targetDirectory"), fontsDir)),
                    executionEnvironment(getProject(), getSession(),
                            getPluginManager()));
        }

        /**
         * Returns element array for common configuration of all executions of
         * the docbkx-maven-plugin.
         *
         * @return Configuration applicable to all executions
         */
        ArrayList<MojoExecutor.Element> getBaseConfiguration() {
            ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();

            cfg.add(element(name("draftMode"), isDraftMode()));
            cfg.add(element(name("draftWatermarkImage"), getDraftWatermarkURL()));
            cfg.add(element(name("highlightSource"), useSyntaxHighlighting()));
            cfg.add(element(name("sectionAutolabel"), areSectionsAutolabeled()));
            cfg.add(element(name("sectionLabelIncludesComponentLabel"),
                    doesSectionLabelIncludeComponentLabel()));
            cfg.add(element(name("xincludeSupported"), isXincludeSupported()));
            cfg.add(element(name("sourceDirectory"), FilenameUtils
                    .separatorsToUnix(sourceDirectory.getPath())));

            return cfg;
        }

        /**
         * Build EPUB documents from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void buildEPUB() throws MojoExecutionException {

            copyImages("epub");

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.addAll(getBaseConfiguration());
                cfg.add(element(name("epubCustomization"), FilenameUtils
                        .separatorsToUnix(getEpubCustomization().getPath())));
                cfg.add(element(name("targetDirectory"), FilenameUtils
                        .separatorsToUnix(getDocbkxOutputDirectory().getPath()
                                + File.separator + "epub")));

                cfg.add(element(name("includes"), docName + "/"
                        + getDocumentSrcName()));

                executeMojo(
                    plugin(groupId("com.agilejava.docbkx"),
                            artifactId("docbkx-maven-plugin"),
                            version(getDocbkxVersion())),
                    goal("generate-epub"),
                    configuration(cfg.toArray(new Element[cfg.size()])),
                    executionEnvironment(getProject(), getSession(),
                            getPluginManager()));

                File outputEpub = new File(
                        new File(getDocbkxOutputDirectory(), "epub"),
                        FilenameUtils.getBaseName(getDocumentSrcName()) + ".epub");
                renameDocument(outputEpub, docName);
            }
        }

        /**
         * Prepare Olink database files for FO output.
         *
         * @param extension File extension without the ., "pdf" or "rtf"
         * @throws MojoExecutionException Failed to prepare the target DB files.
         */
        void buildFoOlinkDB(final String extension) throws MojoExecutionException {

            if (!(extension.equalsIgnoreCase("pdf") || extension.equalsIgnoreCase("rtf"))) {
                throw new MojoExecutionException("Output format " + extension
                        + " is not supported." + " Use either pdf or rtf.");
            }

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.addAll(getBaseConfiguration());
                cfg.add(element(name("xincludeSupported"), isXincludeSupported()));
                cfg.add(element(name("sourceDirectory"), FilenameUtils
                        .separatorsToUnix(sourceDirectory.getPath())));
                cfg.add(element(name("fop1Extensions"), "1"));
                cfg.add(element(name("collectXrefTargets"), "yes"));
                if (extension.equalsIgnoreCase("pdf")) {
                    cfg.add(element(name("insertOlinkPdfFrag"), "1"));
                }
                cfg.add(element(name("includes"), docName + "/"
                        + getDocumentSrcName()));
                cfg.add(element(name("currentDocid"), docName));

                // <targetsFilename> is ignored with docbkx-tools 2.0.15,
                // but not with 2.0.14.

                // The following configuration should be kept
                // for versions of docbkx-tools that honor <targetsFilename>.
                cfg.add(element(
                        name("targetsFilename"),
                        FilenameUtils.separatorsToUnix(getBuildDirectory().getPath())
                                + "/" + docName + "-" + extension + ".target.db"));

                // Due to https://code.google.com/p/docbkx-tools/issues/detail?id=112
                // RTF generation does not work with docbkx-tools 2.0.15.
                // If the format is RTF, stick with 2.0.14 for now.
                // TODO: Remove this sick hack when docbkx-tools #112 is fixed.
                String docbkxVersion = getDocbkxVersion();
                if (extension.equalsIgnoreCase("rtf")) {
                    docbkxVersion = "2.0.14";
                }

                executeMojo(
                        plugin(groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(docbkxVersion)),
                        goal("generate-" + extension),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(getProject(), getSession(),
                                getPluginManager())
                );

                File outputDir = new File(getBaseDir(),
                        "target" + File.separator + "docbkx" + File.separator
                                + extension + File.separator + docName);

                // <targetsFilename> is ignored with docbkx-tools 2.0.15,
                // but not with 2.0.14.
                // The following output directory should be where the files are
                // for versions of docbkx-tools that honor <targetsFilename>.
                if (!outputDir.exists()) {
                    outputDir = new File(getDocbkxOutputDirectory(),
                            extension + File.separator + docName);
                }

                try {
                    String[] extensions = {"fo", extension};
                    Iterator<File> files =
                            FileUtils.iterateFiles(outputDir, extensions, true);
                    while (files.hasNext()) {
                        FileUtils.forceDelete(files.next());
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException(
                            "Cannot delete a file: " + e.getMessage());
                }
            }
        }

        /**
         * Return a fonts element that includes all the custom fonts.
         * <p>
         * If you update this list, also see copyFonts().
         *
         * @param fontDir Directory containing the custom fonts.
         * @return Fonts element.
         */
        final Element getFontsElement(final String fontDir) {
            return element(
                    name("fonts"),
                    element(name("font"),
                            element(name("name"), "DejaVuSans"),
                            element(name("style"), "normal"),
                            element(name("weight"), "normal"),
                            element(name("embedFile"), fontDir + "/DejaVuSans.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSans-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSans"),
                            element(name("style"), "normal"),
                            element(name("weight"), "bold"),
                            element(name("embedFile"), fontDir + "/DejaVuSansCondensed-Bold.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSansCondensed-Bold-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSans"),
                            element(name("style"), "italic"),
                            element(name("weight"), "normal"),
                            element(name("embedFile"), fontDir + "/DejaVuSans-Oblique.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSans-Oblique-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSans"),
                            element(name("style"), "italic"),
                            element(name("weight"), "bold"),
                            element(name("embedFile"), fontDir + "/DejaVuSansCondensed-BoldOblique.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSansCondensed-BoldOblique-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSansMono"),
                            element(name("style"), "normal"),
                            element(name("weight"), "normal"),
                            element(name("embedFile"), fontDir + "/DejaVuSansMono.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSansMono-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSansMono"),
                            element(name("style"), "normal"),
                            element(name("weight"), "bold"),
                            element(name("embedFile"), fontDir + "/DejaVuSansMono-Bold.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSansMono-Bold-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSansMono"),
                            element(name("style"), "italic"),
                            element(name("weight"), "normal"),
                            element(name("embedFile"), fontDir + "/DejaVuSansMono-Oblique.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSansMono-Oblique-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSansMono"),
                            element(name("style"), "italic"),
                            element(name("weight"), "bold"),
                            element(name("embedFile"), fontDir + "/DejaVuSansMono-BoldOblique.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSansMono-BoldOblique-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSerif"),
                            element(name("style"), "normal"),
                            element(name("weight"), "normal"),
                            element(name("embedFile"), fontDir + "/DejaVuSerif.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSerif-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSerif"),
                            element(name("style"), "normal"),
                            element(name("weight"), "bold"),
                            element(name("embedFile"), fontDir + "/DejaVuSerifCondensed-Bold.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSerifCondensed-Bold-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSerif"),
                            element(name("style"), "italic"),
                            element(name("weight"), "normal"),
                            element(name("embedFile"), fontDir + "/DejaVuSerif-Italic.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSerif-Italic-metrics.xml")),
                    element(name("font"),
                            element(name("name"), "DejaVuSerif"),
                            element(name("style"), "italic"),
                            element(name("weight"), "bold"),
                            element(name("embedFile"), fontDir + "/DejaVuSerifCondensed-BoldItalic.ttf"),
                            element(name("metricsFile"), fontDir + "/DejaVuSerifCondensed-BoldItalic-metrics.xml")));
        }

        /**
         * Build FO documents from DocBook XML sources, including fonts.
         *
         * @param format            Specific output format (pdf, rtf)
         * @throws MojoExecutionException Failed to build the output.
         */
        void buildFO(final String format) throws MojoExecutionException {

            if (!(format.equalsIgnoreCase("pdf") || format
                    .equalsIgnoreCase("rtf"))) {
                throw new MojoExecutionException("Output format " + format
                        + " is not supported." + " Use either pdf or rtf.");
            }

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.addAll(getBaseConfiguration());
                cfg.add(element(name("foCustomization"),
                        FilenameUtils.separatorsToUnix(getFoCustomization().getPath())));
                cfg.add(element(name("targetDirectory"), FilenameUtils
                        .separatorsToUnix(getDocbkxOutputDirectory().getPath()
                                + File.separator + format.toLowerCase())));
                cfg.add(element(name("fop1Extensions"), "1"));
                cfg.add(element(name("targetDatabaseDocument"), buildFOTargetDB(format)));
                if (format.equalsIgnoreCase("pdf")) {
                    cfg.add(element(name("insertOlinkPdfFrag"), "1"));
                }
                cfg.add(element(name("targetDirectory"),
                        FilenameUtils.separatorsToUnix(getDocbkxOutputDirectory().getPath())
                                + "/" + format));

                String fontDir = FilenameUtils.separatorsToUnix(getFontsDirectory().getPath());
                cfg.add(getFontsElement(fontDir));

                cfg.add(element(name("includes"), docName + "/" + getDocumentSrcName()));
                cfg.add(element(name("currentDocid"), docName));

                // Due to https://code.google.com/p/docbkx-tools/issues/detail?id=112
                // RTF generation does not work with docbkx-tools 2.0.15.
                // If the format is RTF, stick with 2.0.14 for now.
                // TODO: Remove this sick hack when docbkx-tools #112 is fixed.
                String docbkxVersion = getDocbkxVersion();
                if (format.equalsIgnoreCase("rtf")) {
                    docbkxVersion = "2.0.14";
                }

                executeMojo(
                        plugin(
                                groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(docbkxVersion),
                                dependencies(
                                        dependency(
                                                groupId("net.sf.offo"),
                                                artifactId("fop-hyph"),
                                                version("1.2")))),
                        goal("generate-" + format),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(getProject(), getSession(),
                                getPluginManager()));

                // Avoid each new document overwriting the last.
                File file = new File(getDocbkxOutputDirectory(), format
                        + File.separator
                        + FilenameUtils.getBaseName(getDocumentSrcName()) + "."
                        + format);
                renameDocument(file, docName);
            }
        }

        /**
         * Build PDF documents from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void buildPDF()
                throws MojoExecutionException {
            buildFO("pdf");
        }

        /**
         * Build RTF documents from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void buildRTF() throws MojoExecutionException {
            buildFO("rtf");
        }

        /**
         * Build reference manual pages from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void buildManpages() throws MojoExecutionException {
            ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
            cfg.addAll(getBaseConfiguration());
            cfg.add(element(name("includes"), "*/" + getDocumentSrcName()));
            cfg.add(element(name("manpagesCustomization"), FilenameUtils
                    .separatorsToUnix(getManpagesCustomization().getPath())));
            cfg.add(element(name("targetDirectory"), FilenameUtils
                    .separatorsToUnix(getDocbkxOutputDirectory().getPath()
                            + File.separator + "manpages")));

            executeMojo(
                    plugin(groupId("com.agilejava.docbkx"),
                            artifactId("docbkx-maven-plugin"),
                            version(getDocbkxVersion())),
                    goal("generate-manpages"),
                    configuration(cfg.toArray(new Element[cfg.size()])),
                    executionEnvironment(getProject(), getSession(),
                            getPluginManager()));
        }

        /**
         * Add custom CSS with XML wrapper to document source directories.
         * See <a href="http://docbook.sourceforge.net/release/xsl/current/doc/html/custom.css.source.html"
         * >custom.css.source</a> for details.
         *
         * @throws MojoExecutionException Failed to add custom CSS.
         */
        void addCustomCss() throws MojoExecutionException {

            // For each document source directory, add CSS with an XML wrapper.
            final String cssFileName = getPreSiteCssFileName();
            final File cssFile = new File(getBuildDirectory(), cssFileName);

            if (!cssFile.exists()) {
                throw new MojoExecutionException(cssFile.getPath() + " not found");
            }

            final String cssString;
            try {
                cssString = FileUtils.readFileToString(cssFile);
            } catch (IOException ie) {
                throw new MojoExecutionException(
                        "Failed to read CSS " + cssFile.getPath(), ie);
            }

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {

                final File parent = new File(sourceDirectory, docName);
                final File xmlFile = new File(parent, cssFileName + ".xml");

                try {
                    FileUtils.write(xmlFile, "<?xml version=\"1.0\"?>\n", true);
                    FileUtils.write(xmlFile, "<style>\n", true);
                    FileUtils.write(xmlFile, cssString, true);
                    FileUtils.write(xmlFile, "</style>\n", true);
                } catch (IOException ie) {
                    throw new MojoExecutionException(
                            "Failed to write XML to " + xmlFile.getPath(), ie);
                }

            }

        }

        /**
         * Prepare Olink database files for single page HTML output.
         *
         * @throws MojoExecutionException Failed to prepare the target DB files.
         */
        void buildSingleHTMLOlinkDB() throws MojoExecutionException {

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.add(element(name("xincludeSupported"), isXincludeSupported()));
                cfg.add(element(name("sourceDirectory"), FilenameUtils
                        .separatorsToUnix(sourceDirectory.getPath())));
                cfg.add(element(name("chunkedOutput"), "false"));
                cfg.add(element(name("htmlCustomization"), FilenameUtils
                        .separatorsToUnix(getSingleHTMLCustomization().getPath())));
                cfg.add(element(name("collectXrefTargets"), "only"));

                cfg.add(element(name("includes"), docName + "/" + getDocumentSrcName()));

/*  <targetsFilename> is ignored with docbkx-tools 2.0.15.
                cfg.add(element(
                        name("targetsFilename"),
                        FilenameUtils.separatorsToUnix(getBuildDirectory()
                                .getPath())
                                + "/"
                                + docName
                                + "-single.target.db"));
*/

                executeMojo(
                        plugin(groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(getDocbkxVersion())),
                        goal("generate-html"),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(getProject(), getSession(),
                                getPluginManager()));
            }
        }

        /**
         * Build single page HTML from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void buildSingleHTML() throws MojoExecutionException {

            ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
            cfg.addAll(getBaseConfiguration());
            cfg.add(element(name("includes"), "*/" + getDocumentSrcName()));
            cfg.add(element(name("chunkedOutput"), "false"));
            cfg.add(element(name("htmlCustomization"), FilenameUtils
                    .separatorsToUnix(getSingleHTMLCustomization().getPath())));
            cfg.add(element(name("targetDatabaseDocument"),
                    buildSingleHTMLTargetDB()));
            cfg.add(element(name("targetDirectory"), FilenameUtils
                    .separatorsToUnix(getDocbkxOutputDirectory().getPath()
                            + File.separator + "html")));

            copyImages("html");

            executeMojo(
                    plugin(groupId("com.agilejava.docbkx"),
                            artifactId("docbkx-maven-plugin"),
                            version(getDocbkxVersion())),
                    goal("generate-html"),
                    configuration(cfg.toArray(new Element[cfg.size()])),
                    executionEnvironment(getProject(), getSession(),
                            getPluginManager()));
        }

        /**
         * Prepare Olink database files for chunked HTML output.
         *
         * @throws MojoExecutionException Failed to prepare the target DB files.
         */
        void buildChunkedHTMLOlinkDB() throws MojoExecutionException {

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.add(element(name("xincludeSupported"), isXincludeSupported()));
                cfg.add(element(name("sourceDirectory"), FilenameUtils
                        .separatorsToUnix(sourceDirectory.getPath())));
                cfg.add(element(name("chunkedOutput"), "true"));

                cfg.add(element(name("htmlCustomization"), FilenameUtils
                        .separatorsToUnix(getChunkedHTMLCustomization().getPath())));
                cfg.add(element(name("collectXrefTargets"), "only"));

                cfg.add(element(name("currentDocid"), docName));
                cfg.add(element(name("includes"), docName + "/" + getDocumentSrcName()));

/*  <targetsFilename> is ignored with docbkx-tools 2.0.15.
                cfg.add(element(
                        name("targetsFilename"),
                        FilenameUtils.separatorsToUnix(getBuildDirectory()
                                .getPath())
                                + "/"
                                + docName
                                + "-chunked.target.db"));
*/

                final String chunkBaseDir = FilenameUtils
                        .separatorsToUnix(getDocbkxOutputDirectory().getPath())
                        + "/html/" + docName + "/index/";
                cfg.add(element(name("chunkBaseDir"), chunkBaseDir));

                executeMojo(
                        plugin(groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(getDocbkxVersion())),
                        goal("generate-html"),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(getProject(), getSession(),
                                getPluginManager()));
            }
        }

        /**
         * Build chunked HTML pages from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void buildChunkedHTML() throws MojoExecutionException {

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            copyImages("html", FilenameUtils.getBaseName(getDocumentSrcName()));

            for (String docName : docNames) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.addAll(getBaseConfiguration());
                cfg.add(element(name("chunkedOutput"), "true"));
                cfg.add(element(name("htmlCustomization"), FilenameUtils
                        .separatorsToUnix(getChunkedHTMLCustomization().getPath())));
                cfg.add(element(name("targetDatabaseDocument"),
                        buildChunkedHTMLTargetDB()));
                cfg.add(element(name("generateManifest"), "1"));

                cfg.add(element(name("includes"), docName + "/" + getDocumentSrcName()));

                final String chunkBaseDir = FilenameUtils
                        .separatorsToUnix(getDocbkxOutputDirectory().getPath())
                        + "/html/" + docName + "/index/";
                cfg.add(element(name("chunkBaseDir"), chunkBaseDir));

                cfg.add(element(name("manifest"),
                        getDocbkxOutputDirectory().getPath()
                                + "/" + docName + ".manifest"));

                executeMojo(
                        plugin(groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(getDocbkxVersion())),
                        goal("generate-html"),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(getProject(), getSession(),
                                getPluginManager()));
            }
        }

        /**
         * Prepare Olink database files for webhelp output.
         *
         * @throws MojoExecutionException Failed to prepare the target DB files.
         */
        void buildWebHelpOlinkDB() throws MojoExecutionException {

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            for (String docName : docNames) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.addAll(getBaseConfiguration());

                cfg.add(element(name("webhelpAutolabel"), "1"));
                cfg.add(element(name("webhelpCustomization"),
                        FilenameUtils.separatorsToUnix(getWebHelpCustomization().getPath())));
                cfg.add(element(name("collectXrefTargets"), "only"));

                cfg.add(element(name("currentDocid"), docName));
                cfg.add(element(name("includes"), docName + "/" + getDocumentSrcName()));

/*  <targetsFilename> is ignored with docbkx-tools 2.0.15.
                cfg.add(element(
                        name("targetsFilename"),
                        FilenameUtils.separatorsToUnix(getBuildDirectory()
                                .getPath())
                                + "/"
                                + docName
                                + "-webhelp.target.db"));
*/
                executeMojo(
                        plugin(groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(getDocbkxVersion())),
                        goal("generate-webhelp"),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(getProject(), getSession(),
                                getPluginManager()));
            }
        }

        /**
         * Build webhelp from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void buildWebHelp() throws MojoExecutionException {

            Set<String> docNames = DocUtils.getDocumentNames(
                    sourceDirectory, getDocumentSrcName());
            if (docNames.isEmpty()) {
                throw new MojoExecutionException("No document names found.");
            }

            copyImages("webhelp");

            for (String docName : docNames) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.addAll(getBaseConfiguration());

                cfg.add(element(name("webhelpAutolabel"), "1"));
                cfg.add(element(name("webhelpCustomization"),
                        FilenameUtils.separatorsToUnix(getWebHelpCustomization().getPath())));

                cfg.add(element(name("targetDatabaseDocument"), buildWebHelpTargetDB()));

                final File webHelpBase = new File(getDocbkxOutputDirectory(), "webhelp");
                cfg.add(element(name("targetDirectory"), FilenameUtils
                        .separatorsToUnix(webHelpBase.getPath())));

                cfg.add(element(name("currentDocid"), docName));
                cfg.add(element(name("includes"), docName + "/" + getDocumentSrcName()));

                executeMojo(
                        plugin(groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(getDocbkxVersion())),
                        goal("generate-webhelp"),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(getProject(), getSession(),
                                getPluginManager()));

                // Copy CSS and logo into place in the new webhelp output.
                final File webHelpCss = new File(webHelpBase,
                        docName + File.separator
                                + "common" + File.separator
                                + "css" + File.separator
                                + "positioning.css");
                final File webHelpLogo = new File(webHelpBase,
                        docName + File.separator
                                + "common" + File.separator
                                + "images" + File.separator
                                + "logo.png");
                try {
                    FileUtils.copyFile(getWebHelpCss(), webHelpCss);
                    FileUtils.copyFile(getWebHelpLogo(), webHelpLogo);
                } catch (IOException ie) {
                    throw new MojoExecutionException("Failed to copy file", ie);
                }
            }
        }
    }
}
