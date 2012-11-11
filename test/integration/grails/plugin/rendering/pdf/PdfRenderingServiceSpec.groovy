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
package grails.plugin.rendering.pdf

import org.apache.pdfbox.util.PDFTextStripper
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument

import grails.plugin.rendering.RenderingServiceSpec

import grails.plugin.spock.*

import spock.lang.*

class PdfRenderingServiceSpec extends RenderingServiceSpec {

	def pdfRenderingService
	
	def getRenderer() {
		pdfRenderingService
	}

	def renderWithLocaleArgument(){
		given:
		def response = createMockResponse()
		when:
		renderer.render(getSimpleTemplate([locale: Locale.FRENCH]), response)
		def matcher = new PDFTextStripper().getText(PDDocument.load(new ByteArrayInputStream(response.contentAsByteArray))) =~ /french/
		then:
		matcher.size() == 1
	}
	
	protected extractTextLines(Map renderArgs) {
		extractTextLines(createPdf(renderArgs))
	}
	
	protected extractTextLines(byte[] bytes) {
		extractTextLines(createPdf(new ByteArrayInputStream(bytes)))
	}
	
	protected extractTextLines(PDDocument pdf) {
		protected lines = new PDFTextStripper().getText(pdf).readLines()
		pdf.close()
		lines
	}

	protected createPdf(Map renderArgs) {
		def inStream = new PipedInputStream()
		def outStream = new PipedOutputStream(inStream)
		pdfRenderingService.render(renderArgs, outStream)
		outStream.close()
		createPdf(inStream)
	}
	
	protected createPdf(InputStream inputStream) {
		def parser = new PDFParser(inputStream)
		parser.parse()
		parser.getPDDocument()
	}

}
