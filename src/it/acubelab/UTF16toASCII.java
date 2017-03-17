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
package it.acubelab;

import java.text.Normalizer;
import java.text.Normalizer.Form;

public final class UTF16toASCII {

	private static final int MAX_UTF16 = 65536;
	private static final char NEUTRAL_CHAR = ' ';
	public static final int MAX_ASCII = 127; 
	
	public static final char[] MAP = new char[MAX_UTF16];	
	
	static{
		
		for(int i=0; i<MAX_UTF16; i++){
			char c = (char)i;
			String normalizedStr = Normalizer.normalize(""+c, Form.NFKD);
			char normalizedChar = Character.toLowerCase(normalizedStr.toCharArray()[0]);
			if (normalizedChar<=MAX_ASCII) MAP[c] = normalizedChar;
			else MAP[c] = NEUTRAL_CHAR;
		}
		
		/**
		 * DASHES {'‒','–','—','―'}
		 * See http://en.wikipedia.org/wiki/Dash
		 */
		MAP['‒'] = '-';
		MAP['–'] = '-';
		MAP['—'] = '-';
		MAP['―'] = '-';
	}

	
	
	
	
	
}
