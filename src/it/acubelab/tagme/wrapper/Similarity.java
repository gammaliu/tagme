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

import java.io.IOException;
import java.util.List;
import it.acubelab.tagme.AnnotatedText;
import it.acubelab.tagme.Annotation;
import it.acubelab.tagme.RelatednessMeasure;
import it.acubelab.tagme.preprocessing.TopicSearcher;

/**
 * Similarity calculator between list of annotations. The calculator exploits the tags found by tagMe and 
 * uses the information contained in the graph structure to find the semantic relatedness between texts.
 */
public class Similarity {

	String lang;
	float rho;
	TopicSearcher searcher;
	private Annotator annotator;
	private RelatednessMeasure rel;
	
	public Similarity(String lang,float rho,RelatednessMeasure rel){
		this.lang=lang;
		this.rho=rho;
		this.rel=rel;
		annotator=new Annotator(lang);
		try {
			searcher = new TopicSearcher("en");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**Compute the similarity between text_a and text_b
	 * @param text_a
	 * @param text_b
	 * @return a value between 0 and 1 representing the similarity of the 2 texts. 0 = no similarity 1= high similarity.
	 * @throws IOException
	 */
	public float sim(String text_a,String text_b) throws IOException{
		
		AnnotatedText aText_a= annotator.annotates(text_a);
		AnnotatedText aText_b= annotator.annotates(text_b);
		List<Annotation> annotsA = aText_a.getAnnotations();
//		for(Annotation a:annotsA){
//			if(a.isDisambiguated() && a.getRho()>0.15){
//			System.out.println(searcher.getTitle(a.getTopic()));
//			}
//		}
	
		List<Annotation> annotsB = aText_b.getAnnotations();
//		for(Annotation a:annotsB){
//			if(a.isDisambiguated() && a.getRho()>0.15){
//			System.out.println(searcher.getTitle(a.getTopic()));
//			}
//		}
//		if (annotsA.size() == 0 || annotsB.size() == 0) return 0.0f;
		return sim(annotsA,annotsB, rel ,rho);
	}

	/**Compute the similarity between two lists of annotation.
	 * @param annotsA the first list of annotations.
	 * @param annotsB the second list of annotations.
	 * @param rel the relatedness computer.
	 * @param rhoNA the rho limit under which annotations are not considered.
	 * @return a value between 0 and 1 representing the similarity of the 2 texts. 0 = no similarity 1= high similarity.
	 */
	@SuppressWarnings("unchecked")
	public float sim(List<Annotation> annotsA, List<Annotation> annotsB, RelatednessMeasure rel, float rhoNA)
	{
		float pairSim = 0;
		float normFactor = 0; 
		for(List<Annotation> list : new List[]{annotsA, annotsB})
		{
			List<Annotation> others = list == annotsA ? annotsB : annotsA;
			for(Annotation annotation : list)
			{
				if (!annotation.isDisambiguated() || annotation.getRho() < rhoNA) continue;
				Annotation nearest = getNearest(annotation, others, rel, rhoNA);
				if (nearest != null) {
					float avgRho = (annotation.getRho()+nearest.getRho()) / 2f;
					pairSim += avgRho * rel.rel(annotation.getTopic(), nearest.getTopic());
//					try {
//						System.out.println(annotation.getTopic()+":"+searcher.getTitle(annotation.getTopic()));
//						System.out.println(nearest.getTopic()+":"+searcher.getTitle(nearest.getTopic()));
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					
					
					//System.out.println("Relatedness:"+rel.rel(annotation.getTopic(), nearest.getTopic()));
					normFactor += avgRho;
				} else {
					normFactor += annotation.getRho();
				}
			}
		}
		if (normFactor == 0) return 0;
		else return pairSim / normFactor;
	}

	/**Find the annotation contained in others that is nearest (most related) to ann.
	 * @param ann an annotation.
	 * @param others the set of annotations that must be tested.
	 * @param rel the relatedness calculator.
	 * @param rhoNA the rho limit under which annotations are not considered.
	 * @return the nearest annotation, null if no annotations were found.
	 */
	Annotation getNearest(Annotation ann, List<Annotation> others, RelatednessMeasure rel, float rhoNA)
	{
		Annotation nearest = null;
		float maxrel = 0;
		for(Annotation b : others)
			if (b.isDisambiguated() && b.getRho() >= rhoNA && rel.rel(ann.getTopic(),b.getTopic()) > maxrel)
			{
				maxrel = rel.rel(ann.getTopic(),b.getTopic());
				nearest = b;
			}
		return nearest;
	}
}
