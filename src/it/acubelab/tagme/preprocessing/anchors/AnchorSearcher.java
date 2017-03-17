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
package it.acubelab.tagme.preprocessing.anchors;

import java.util.HashMap;

import it.acubelab.tagme.Anchor;
import it.acubelab.tagme.Annotation;
import it.acubelab.tagme.RelatednessMeasure;
import it.acubelab.tagme.RelatednessMeasure.RelType;
import it.acubelab.tagme.TagmeParser.TokenizedCharSequence;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.WordTrie;

public abstract class AnchorSearcher {

	protected String lang;
	protected WordTrie stopwords;

	protected AnchorSearcher(String lang){
		this.lang = lang;
	}

	/**
	 * It searches for all anchors that begin with the first token of the input string.
	 * @param input A cleaned and tokenized text
	 * @return The number of anchors found
	 */
	public abstract int search(TokenizedCharSequence input);

	public abstract Anchor search(CharSequence input);

	/**
	 * Used when the text has been already spotted
	 * @param input
	 * @return
	 */
	public int SearchBestMatch(TokenizedCharSequence input) 
	{
		int found =0;
		Anchor a=null;
		CharSequence text=input.subSequence(0, input.length()+1);
		//MutableString text=new MutableString(input.subSequence(0, input.length()));

		//Cerco Tutto il testo
		a=search(text);
		if(a!=null) found++;

		//se non ho trovato nulla provo sulla sottostringa togliendo dall'inizio
		for(int i=0;i<input.length() && found==0;i++){
			a=search(input.subSequence(i, input.length()+1));
			if(a!=null) found++;
		}

		//se ancora non è andata bene togliamo dalla fine

		for(int i=0;i<input.length() && found==0;i++){

			text=input.subSequence(0,input.length()-i);
			a=search(text);
			if(a!=null) found++;


		}

		if(a!=null) input.setAnchorAt(0, a);
		return found;

	}
	
	
	
}
