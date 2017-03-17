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

/**
 * A class that provides a method to calculate the Rho-measure for a specific annotation. The rho-measure is a value between
 * 0 and 1 that describes the likelihood that a disambiguation is done correctlyin the specific context.
 */
public final class RhoMeasure {

	/** Calculate a rho-measure for all the annotations in the input text, using the measure of relatedness rel. (Will set input.annotations.get(i).rho for each i)
	 * @param input the annotated and disambiguated text.
	 * @param rel the measure of relativity between wikipedia pages.
	 */
	public void calc(AnnotatedText input, RelatednessMeasure rel)
	{
		AnnotationWindow window = input.getWindowIterator();
		
		for(int a=0; a<input.annotations.size(); a++)
		{
			window.setStartAnnotation(a);
			
			Annotation an = input.annotations.get(a);
			
			if (window.empty || ! an.isDisambiguated())
			{
				an.coherence = 0;
				an.rho = 0;
				continue;
			}
			
			float relsum = 0;
			float n = 0;
			while(window.next())
			{
				Annotation b = input.annotations.get(window.cursor);
				if (b.isDisambiguated()){
					relsum += rel.rel(an.topic, b.topic);
					n++;
				}
			}
			
			an.coherence = n>0? relsum/n : 0;
			
			an.rho = (an.anchor.lp() + an.coherence) / 2f ;
			
		}
		
	}
	
}
