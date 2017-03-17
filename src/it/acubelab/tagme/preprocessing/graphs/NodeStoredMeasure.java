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

import java.io.IOException;

public final class NodeStoredMeasure extends RelatednessMeasure{

	private static final int EL_SIZE = NodeRelatednessMatrix.EL_SIZE;
	private static final float MIN_REL = NodeRelatednessMatrix.MIN_REL;

	NodeMatrix data;

	public NodeStoredMeasure(String lang){
		super(lang);
		data = DatasetLoader.get(new NodeRelatednessMatrix(lang));
	}
	

	public float rel(int a,int b)
	{
		if(a==b) return 1f;
		
		int nodeA = data.map[a];
		int nodeB = data.map[b];
		if (nodeA < 0 || nodeB < 0) return 0f;
		
		int key = nodeA>nodeB ? nodeB : nodeA;
		byte[] array = nodeA>nodeB ? data.matrix[nodeA] : data.matrix[nodeB];

		if (array == null) return 0f;
		
		int start=0;
		int end = array.length-EL_SIZE;
		int pos=-1;
		while(pos==-1 && start<=end)
		{
			int idx=((start+end)/EL_SIZE)/2;
//			byte[] value={array[idx*5],array[idx*5+1],array[idx*5+2],array[idx*5+3]};
			int idx_value= ((array[idx*EL_SIZE] & 0xFF) << 16)
            				+ ((array[idx*EL_SIZE+1] & 0xFF) << 8)
            				+ ( array[idx*EL_SIZE+2] & 0xFF);
			
			if(idx_value==key) pos = idx;
			else{
				if(key>idx_value)
					start=(idx+1)*EL_SIZE;
				else
					end=(idx-1)*EL_SIZE;
			}
		}

		
		if(pos==-1) return 0f;
		else
		{
			byte by = array[pos*EL_SIZE+3];
			float normRel = by<0 ? by+255 : by;
			return MIN_REL + normRel/255f;
		}
	
	}
}
