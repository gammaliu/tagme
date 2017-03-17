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

import it.acubelab.tagme.preprocessing.TopicSearcher;
import it.unimi.dsi.lang.MutableString;

import java.io.IOException;
import java.util.HashMap;

public class DisambiguationDebugger
{
	public static final int
		PRUNED_MINCOMM = -2,
		PRUNED_MINLINK = -3,
		PRUNED_PEOPLE = -4,
		PRUNED_UNDEF = -1,
		TOP_EPSILON = 1;
	
	public static class Votes extends HashMap<Annotation, float[]>
	{
		public int status = 0;
		public float sum = 0;
		public Votes() {super();}
		void vote(Annotation b, int b_page, float vote)
		{
			float[] b_votes = this.get(b);
			if (b_votes == null){
				b_votes = new float[b.anchor.ambiguity()];
				for (int i=0; i<b_votes.length; i++) b_votes[i] = 0;
				this.put(b, b_votes);
			}
			int idx = b.anchor.pageIndex(b_page);
			if (idx < 0) throw new RuntimeException("Unable to find page "+b_page+" in anchor");
			b_votes[idx] = vote;
			sum+=vote;
			
		}
	}
	
	public HashMap<Annotation, Votes[]> votes = new HashMap<Annotation, Votes[]>(); 
	void init(Annotation a)
	{
		if (!votes.containsKey(a))
		{
			Votes[] a_votes = new Votes[a.anchor.ambiguity()];
			for (int i=0; i<a_votes.length; i++) a_votes[i] = new Votes();
			votes.put(a, a_votes);
		}
	}
	void setStatus(Annotation a, int page, int status)
	{
		init(a);
		votes.get(a)[a.anchor.pageIndex(page)].status = status;
	}
	void vote(Annotation a, int a_page, Annotation b, int b_page, float vote){
		init(a);
		votes.get(a)[a.anchor.pageIndex(a_page)].vote(b, b_page, vote);
	}
	
	public String toString(AnnotatedText input, String lang)
	{
		try {
			TopicSearcher topic = new TopicSearcher(lang);
			MutableString buf = new MutableString();
			
			for(Annotation a :votes.keySet())
			{
				buf.append('\n');
				buf.append(String.format("Annotation [%s] (lp %.4f - links %d) > %s\n", input.getOriginalText(a), a.getAnchor().lp(), a.getAnchor().links(),
						(a.isDisambiguated()? topic.getTitle(a.getTopic()) : "N/A")));

				if (a.isIgnored() || a.isPruned())
				{
					buf.append("  PRUNED\n");
					continue;
				}
				for (int page_idx=0; page_idx<a.anchor.ambiguity(); page_idx++)
				{
					if (votes.get(a)[page_idx].status < 0)
					{
						buf.append(String.format("  Page [%d - %s] pruned: %d\n", a.anchor.pageByIndex(page_idx), topic.getTitle(a.anchor.pageByIndex(page_idx)), votes.get(a)[page_idx].status));
						continue;
					}
					Votes v = votes.get(a)[page_idx];
					if (v.sum == 0)
					{
						buf.append(String.format("  Page [%d - %s] zero-votes: %d\n", a.anchor.pageByIndex(page_idx), topic.getTitle(a.anchor.pageByIndex(page_idx)), votes.get(a)[page_idx].status));
						continue;
					}

					buf.append(String.format("  Page [%d - %s] %f\n", a.anchor.pageByIndex(page_idx), topic.getTitle(a.anchor.pageByIndex(page_idx)), v.sum));
					
					for (Annotation b : v.keySet())
					{
						buf.append(String.format("    Anchor [%s] (lp %.4f - links %d)\n", input.getOriginalText(b), b.getAnchor().lp(), b.getAnchor().links()));
						float[] b_votes = v.get(b);
						for (int idxb=0; idxb<b_votes.length; idxb++)
						{
							if (b_votes[idxb]>0)
								buf.append(String.format("      Vote Page [%d - %s]: %f\n", b.anchor.pageByIndex(idxb), topic.getTitle(b.anchor.pageByIndex(idxb)), b_votes[idxb]));
						}
					}
				}
				buf.append("\n\n");
			}
			
			return buf.toString();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		
	}
}
