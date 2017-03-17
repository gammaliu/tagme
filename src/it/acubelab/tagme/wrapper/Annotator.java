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
package it.acubelab.tagme.wrapper;

import it.acubelab.tagme.AnnotatedText;
import it.acubelab.tagme.Annotation;
import it.acubelab.tagme.Disambiguator;
import it.acubelab.tagme.RelatednessMeasure;
import it.acubelab.tagme.RhoMeasure;
import it.acubelab.tagme.Segmentation;
import it.acubelab.tagme.TagmeParser;

import java.io.IOException;
import java.util.List;
import java.util.Vector;


/**
 * A wrapper-class fon an annotator. The annotator will parse, segment the text and disambuguate the spots,
 * also computing the rho-values for the annotations.
 */
public class Annotator {

	Segmentation segmentation;
	Disambiguator disambiguator;
	RhoMeasure rhoer;
	TagmeParser parser;
	RelatednessMeasure rel;
	String lang;
	/**
	 * @param lang the language of the annotator.
	 */
	public Annotator(String lang){
		this.lang=lang;
		try {
			parser = new TagmeParser(lang);
		} catch (IOException e) {
			e.printStackTrace();
		}
		segmentation = new Segmentation();
		disambiguator = new Disambiguator(lang);
		rhoer = new RhoMeasure();
		rel = RelatednessMeasure.create(lang);
	}
	
	/**Makes an annotated text out of a string.
	 * @param to_annot The text to annotate.
	 * @return the annotations found by TagMe with their rho-measure.
	 */
	public AnnotatedText annotates(String to_annot){
		AnnotatedText aText= new AnnotatedText(to_annot);
		parser.parse(aText);
		
		segmentation.segment(aText);
	
		disambiguator.disambiguate(aText,rel);
		rhoer.calc(aText, rel);
		return aText;
	}
	
	/**Makes an annotated text out of a string, returning the annotations as a list.
	 * @param to_annot The text to annotate.
	 * @return a list containing the annotations found by TagMe with their rho-measure.
	 */
	public List<Annotation> getAnnotationList(String to_annot){
		AnnotatedText aText = annotates(to_annot);
		List<Annotation> annots=aText.getAnnotations();
	
		List<Annotation> returned_list = new Vector<Annotation>() ;
	
		for(Annotation a:annots){
			
			if(a.isDisambiguated())
				returned_list.add(a);
		}
		return  returned_list;
	}
	
	/**Makes an annotated text out of a string, returning the annotations as a list
	 * and discarding those annotations that have a rho-value lower than the given limit.
	 * @param to_annot The text to annotate.
	 * @param rho_limit The limit under which the annotations are discarded.
	 * @return a list containing the annotations found by TagMe with their rho-measure.
	 */
	public List<Annotation> getAnnotationList(String to_annot,float rho_limit){
		AnnotatedText aText = annotates(to_annot);
		List<Annotation> annots=aText.getAnnotations();
		List<Annotation> returned_list = new Vector<Annotation>() ;
		for(Annotation a:annots){
			if(a.isDisambiguated() && a.getRho()>rho_limit)
				returned_list.add(a);
		}
		return  returned_list;
	}

	
}
