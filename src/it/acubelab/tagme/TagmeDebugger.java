/*******************************************************************************
 * Copyright 2014 A3 lab (Dipartimento di Informatica, Università di Pisa)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package it.acubelab.tagme;

import it.acubelab.tagme.AnnotatedText.AnnotationWindow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class TagmeDebugger {
	
	Disambiguator disambiguator;
	Segmentation segmenter;
	RelatednessMeasure rel;
	TagmeParser parser;
	RhoMeasure rhoer;
	
	AnnotatedText text;
	String lang;
	
	public TagmeDebugger(String lang) throws IOException
	{
		this.lang=lang;
		disambiguator = new Disambiguator(lang);
		segmenter = new Segmentation();
		rhoer = new RhoMeasure();
		
		parser = new TagmeParser(lang);
		rel = RelatednessMeasure.create(lang);
	}
	
	public void debug(String input)
	{
		text = new AnnotatedText(input);
		parser.parse(text);
		segmenter.segment(text);
		disambiguator.debug = new DisambiguationDebugger();
		disambiguator.disambiguate(text, rel);
		rhoer.calc(text, rel);
		
	}
	
	public Disambiguator getDisambiguator(){
		return this.disambiguator;
	}
	
	public AnnotatedText getText(){
		return text;
	}
	public DisambiguationDebugger getDisambDebugger(){
		return disambiguator.debug;
	}
	public AnnotationWindow getWindow(Annotation a){
		AnnotationWindow w = text.getWindowIterator();
		int idx = getAnnotationIndex(a);
		if (idx < 0) throw new RuntimeException("Unable to find annotation");
		w.setStartAnnotation(idx);
		return w;
	}
	public List<Annotation> annots(){
		return text.annotations;
	}
	public int getAnnotationIndex(Annotation a){
		return text.getAnnotations().indexOf(a);
	}

}
