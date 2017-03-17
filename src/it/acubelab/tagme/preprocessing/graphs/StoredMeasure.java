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
package it.acubelab.tagme.preprocessing.graphs;

import it.acubelab.tagme.RelatednessMeasure;
import it.acubelab.tagme.preprocessing.DatasetLoader;

public final class StoredMeasure extends RelatednessMeasure{


	byte[][] matrix;

	public StoredMeasure(String lang){
		super(lang);
		matrix= DatasetLoader.get(new RelatednessMatrix(lang));
	}

	public float rel(int a,int b)
	{
		if(a==b) return 1f;
		
		int key = a>b ? b : a;
		byte[] array = a>b ? matrix[a] : matrix[b];
		//byte[] array = matrix[key];

		int e_size=5;
		int start=0;
		if(array==null) return 0.0f;
		int end=array.length-e_size;
		int pos=-1;
		while(pos==-1 && start<=end)
		{
			int idx=((start+end)/e_size)/2;
			
//			byte[] value={array[idx*5],array[idx*5+1],array[idx*5+2],array[idx*5+3]};
//			int idx_value=TUtils.byteArrayToInt(value);
			
			int idx_value = ( array[idx*5] << 24)
	                 		+ ((array[idx*5+1] & 0xFF) << 16)
	                 		+ ((array[idx*5+2] & 0xFF) << 8)
	                 		+ ( array[idx*5+3] & 0xFF);
			
			if(idx_value==key) pos = idx;
			else{
				if(key>idx_value)
					start=(idx*5)+e_size;
				else
					end=(idx*5)-e_size;
			}
		}

		
		if(pos==-1) return 0.0f;
		else{
			
			byte by = array[pos*5+4];
			int i;
			if(by<0) i=(int)by+255;
			else i=(int) by;
			
			float f=i*(1f/255f);
			f = (float) Math.rint(f*10000)/10000;
			return f;
		}
	
	}
}
