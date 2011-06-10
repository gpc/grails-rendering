/*
 * Copyright 2010 Grails Plugin Collective
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.rendering.document

import org.w3c.dom.Document
import org.xml.sax.InputSource
import org.xhtmlrenderer.resource.XMLResource
import groovy.text.Template
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import grails.util.GrailsWebUtil
import grails.util.GrailsUtil
import org.xml.sax.XMLReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.TransformerFactory
import javax.xml.transform.Transformer
import org.xml.sax.EntityResolver
import org.xml.sax.SAXException
import javax.xml.parsers.SAXParserFactory
import javax.xml.parsers.SAXParser
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource

class XhtmlDocumentService {

    static transactional = false

    def groovyPagesTemplateEngine
    def groovyPagesUriService
    def grailsApplication

    Document createDocument(Map args) {
        createDocument(generateXhtml(args))
    }

    protected createDocument(String xhtml) {
        try {
            createDocument(new InputSource(new StringReader(xhtml)))
        } catch (XmlParseException e) {
            if (log.errorEnabled) {
                GrailsUtil.deepSanitize(e)
                log.error("caught xml parse exception for xhtml: $xhtml", e)
            }
            throw new XmlParseException(xhtml, e)
        }
    }

    protected Document createDocument(InputSource xhtml) {
        try {
            //xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
            //  XMLReader xmlReader = XMLResource.newXMLReader().setFeature("http://xml.org/sax/features/namespaces", false)



     //       XMLResource.load(xhtml).document

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
            xmlReader.setFeature("http://xml.org/sax/features/namespaces", false)
            xmlReader.setEntityResolver(new MyResolver());

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(false);
            Document document = dbf.newDocumentBuilder().parse(xhtml)
            return document

        } catch (Exception e) {
            if (log.errorEnabled) {
                GrailsUtil.deepSanitize(e)
                log.error("xml parse exception for input source: $xhtml", e)
            }
            throw new XmlParseException(xhtml, e)
        }
    }

    protected generateXhtml(Map args) {
        def xhtmlWriter = new StringWriter()

        RenderEnvironment.with(grailsApplication.mainContext, xhtmlWriter) {
            createTemplate(args).make(args.model).writeTo(xhtmlWriter)
        }

        def xhtml = xhtmlWriter.toString()
        xhtmlWriter.close()

        if (log.debugEnabled) {
            log.debug("xhtml for $args -- \n ${xhtml}")
        }

        xhtml
    }

    protected createTemplate(Map args) {
        if (!args.template) {
            throw new IllegalArgumentException("The 'template' argument must be specified")
        }
        def templateName = args.template

        if (templateName.startsWith("/")) {
            if (!args.controller) {
                args.controller = ""
            }
        } else {
            if (!args.controller) {
                throw new IllegalArgumentException("template names must start with '/' if controller is not provided")
            }
        }

        def contextPath = getContextPath(args)
        def controllerName = args.controller instanceof CharSequence ? args.controller : groovyPagesUriService.getLogicalControllerName(args.controller)
        def templateUri = groovyPagesUriService.getTemplateURI(controllerName, templateName)
        def uris = ["$contextPath/$templateUri", "$contextPath/grails-app/views/$templateUri"] as String[]
        def template = groovyPagesTemplateEngine.createTemplateForUri(uris)

        if (!template) {
            throw new UnknownTemplateException(args.template, args.plugin)
        }

        template
    }

    protected getContextPath(args) {
        def contextPath = args.contextPath?.toString() ?: ""
        def pluginName = args.plugin

        if (pluginName) {
            def plugin = PluginManagerHolder.pluginManager.getGrailsPlugin(pluginName)
            if (plugin && !plugin.isBasePlugin()) {
                contextPath = plugin.pluginPath
            }
        }

        contextPath
    }

    public static class MyResolver implements EntityResolver {
        public InputSource resolveEntity(String publicId, String systemID)
        throws SAXException {
            System.out.println("publicID = " + publicId + ", systemID=" + systemID);
            try {
                if (publicId.equals("-//W3C// DTD XHTML 1.0 Strict//EN")) {
                    return new InputSource(new FileInputStream("xhtml1-strict.dtd"));
                } else if (publicId.equals("-// W3C//ENTITIES Latin 1 for XHTML//EN")) {
                    return new InputSource(new FileInputStream("xhtml-lat1.ent"));
                } else if (publicId.equals("-// W3C//ENTITIES Symbols for XHTML//EN")) {
                    return new InputSource(new FileInputStream("xhtml-symbol.ent"));
                } else if (publicId.equals("-// W3C//ENTITIES Special for XHTML//EN")) {
                    return new InputSource(new FileInputStream("xhtml-special.ent"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}