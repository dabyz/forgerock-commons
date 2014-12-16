<?xml version="1.0" encoding="UTF-8"?>
<!--
  ! This work is licensed under the Creative Commons
  ! Attribution-NonCommercial-NoDerivs 3.0 Unported License.
  ! To view a copy of this license, visit
  ! http://creativecommons.org/licenses/by-nc-nd/3.0/
  ! or send a letter to Creative Commons, 444 Castro Street,
  ! Suite 900, Mountain View, California, 94041, USA.
  !
  ! You can also obtain a copy of the license at
  ! legal/CC-BY-NC-ND.txt.
  ! See the License for the specific language governing permissions
  ! and limitations under the License.
  !
  ! If applicable, add the following below this CCPL HEADER, with the fields
  ! enclosed by brackets "[]" replaced with your own identifying information:
  !      Portions Copyright [yyyy] [name of copyright owner]
  !
  !      Copyright 2011-2014 ForgeRock AS
  !
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
 xmlns:d="http://docbook.org/ns/docbook" exclude-result-prefixes="d">
 <xsl:import href="urn:docbkx:stylesheet" />
 <xsl:output encoding="UTF-8" media-type="application/xhtml+xml" indent="no" />

 <xsl:template match="d:programlisting">
  <xsl:choose>
   <xsl:when test="@language='aci'">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: aci;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='csv'">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: csv;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='html'">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: html;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='http'">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: http;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='ini'">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: ini;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='java'">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: java;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='javascript'">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: javascript;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='ldif'">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: ldif;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='shell'">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: shell;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='xml'">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: xml;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:otherwise>
    <pre xmlns="http://www.w3.org/1999/xhtml" class="brush: plain;"><xsl:value-of select="." /></pre>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:param name="html.stylesheet">
  sh/css/shCore.css
  sh/css/shCoreEclipse.css
  sh/css/shThemeEclipse.css
 </xsl:param>
 <xsl:param name="html.script">
  sh/js/shCore.js
  sh/js/shBrushAci.js
  sh/js/shBrushBash.js
  sh/js/shBrushCsv.js
  sh/js/shBrushHttp.js
  sh/js/shBrushJava.js
  sh/js/shBrushJScript.js
  sh/js/shBrushLDIF.js
  sh/js/shBrushPlain.js
  sh/js/shBrushProperties.js
  sh/js/shBrushXml.js
  sh/js/shAll.js
 </xsl:param>

 <xsl:param name="generate.legalnotice.link" select="1" />
 <xsl:param name="root.filename">index</xsl:param>
 <xsl:param name="use.id.as.filename" select="1" />

 <xsl:param name="generate.toc">
  appendix  toc,title
  article/appendix  nop
  article   nop
  book      toc,title
  chapter   toc,title
  part      toc,title
  preface   nop
  qandadiv  nop
  qandaset  nop
  reference toc,title
  sect1     nop
  sect2     nop
  sect3     nop
  sect4     nop
  sect5     nop
  section   toc
  set       toc,title
 </xsl:param>
 <xsl:param name="generate.section.toc.level" select="1" />
 <xsl:param name="toc.section.depth" select="2" />
 <xsl:param name="toc.max.depth" select="1" />
 <xsl:param name="generate.meta.abstract" select="1" />

 <!-- START DOCS-243: Clean up XHTML5 -->

 <!-- Remove inline style attributes (does not purge all inline CSS, but most) -->
 <xsl:param name="admon.style"></xsl:param>
 <xsl:param name="css.decoration" select="0" />
 <xsl:param name="make.clean.html" select="1" />
 <xsl:param name="table.borders.with.css" select="0" />

 <!-- Prefix class names with 'db-' -->
 <xsl:template match="*" mode="class.value">
  <xsl:param name="class" select="local-name(.)"/>
  <xsl:if test="string-length($class) != 0">
   <xsl:value-of select="concat('db-', $class)"/>
  </xsl:if>
 </xsl:template>

 <!-- Fix image representation: no tables, no height/width -->
 <xsl:param name="make.graphic.viewport" select="0" />
 <xsl:param name="ignore.image.scaling" select="1" />

 <!-- END DOCS-243: Clean up XHTML5 -->
</xsl:stylesheet>
