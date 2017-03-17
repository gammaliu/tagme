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

import it.acubelab.tagme.preprocessing.anchors.AnchorIndexer;
import it.acubelab.tagme.preprocessing.anchors.WikipediaAnchorParser;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.codec.binary.Base64;

public class Anchor implements Serializable, IntIterable {

	private static final long serialVersionUID = 1844814369363951532L;

	/**
	 * Drop pages that have commonness below this threshold
	 * See {@link AnchorIndexer}
	 * */
	public static final float MIN_COMM=0.001f; //0.1%
	/**
	 * Drop anchors that have Link Probability below this threshold.
	 * See {@link AnchorIndexer}.
	 */
	public static final float MIN_LP = 0.001f;//0.1%

	/**
	 * Drop (anchor-&gt;page) that occur less than this threshold.
	 * See {@link AnchorIndexer}. 
	 */
	public static final int MIN_LINKS = 2;

	/**
	 * Identifier
	 */
	int i;

	/**
	 * Frequency
	 */
	int f;

	/**
	 * Links (this number consider also dropped links)
	 */
	int l;

	/**
	 * Link Probability
	 */
	float k;

	/**
	 * The un-ambiguous page (if this anchor is un-ambiguous), otherwise it is set to -1
	 */
	int p;

	/**
	 * The pages pointed by this anchor, if many, otherwise see {@link #p}.
	 * Pages are sorted by their commonness.
	 */
	int[] q = null;

	/**
	 * The commonness values referred to the pages in {@link #p}, if not null.
	 */
	float[] c = null;


	public int freq(){
		return f;
	}
	public int links(){
		return l;
	}

	public float lp(){
		return k;
	}
	public boolean isAmbiguous(){
		return q != null;
	}
	public int ambiguity(){
		return q==null? 1 : q.length;
	}
	public int singlePage(){
		return p;
	}
	public int mostCommonPage(){
		return q==null? p : q[0];
	}
	public float maxCommonness(){
		return q==null? 1 : c[0];
	}

	public boolean equals(Anchor a){
		return a==null? false : this.i==a.i;
	}

	/**
	 * Requires a linear scan for searching for the page.
	 * @param page
	 * @return the commonness between this anchor and the given page.
	 */
	public float commonness(int page){
		if (q==null){
			if (p!=page) throw new IllegalArgumentException("Invalid page for this anchor");
			else return 1f;
		} else {
			for(int idx=0;idx<q.length;idx++)
				if (q[idx]==page) return c[idx];

			throw new IllegalArgumentException("Invalid page for this anchor");
		}
	}

	/**
	 * @param page the id of the page to search.
	 * @return the index of the given page in the ordering (note that pages are ordered by their commonness with the anchor). -1 if the page is not pointed by the anchor.
	 */
	public int pageIndex(int page){
		if (q==null){
			if (p == page) return 0;
			else return -1;
		} else {
			for (int i=0;i<q.length; i++)
				if (q[i] == page) return i;
			return -1;
		}
	}
	
	public int pageByIndex(int idx){
		if (q==null)
		{
			if (idx == 0) return p;
			else return -1;
		} else {
			if (idx>=0 && idx < q.length) return q[idx];
			else return -1;
		}
		
	}

	@Override
	public IntIterator iterator() {
		return pages();
	}
	/**
	 * Iterates pages sorted by their commonness
	 * @return
	 */
	public IntIterator pages()
	{
		return new IntIterator() {
			int idx=0;
			@Override
			public void remove() {throw new UnsupportedOperationException();}
			@Override
			public Integer next() { return nextInt(); }
			@Override
			public boolean hasNext() { return idx < (q==null? 1 : q.length); }
			@Override
			public int skip(int offset) {throw new UnsupportedOperationException();}
			@Override
			public int nextInt() {
				if (idx >= (q==null? 1 : q.length)) throw new IllegalArgumentException();
				idx++;
				return q==null? p : q[idx-1];
			}
		};
	}

	/**
	 * A class for iterating Anchors frequently: you can re-use this class by setting the underlying Anchor obejct 
	 * with {@link #setAnchor(Anchor)} and then iterating with the same object.
	 * {@link Anchor.pages()} creates a new instance at each iteration
	 */
	public static class PageCommonnessIterator
	{
		Anchor instance = null;
		int idx = -1;
		public void setAnchor(Anchor a){
			this.instance = a;
			this.idx = -1;
		}
		public boolean next() {
			if (instance==null) throw new IllegalStateException();
			if (idx < (instance.q==null? 0 : instance.q.length-1)){
				idx++;
				return true;
			} else return false;
		}
		public int page(){
			//			if (instance==null || idx > (instance.q==null? 1 : instance.q.length)) throw new IllegalStateException();
			//			else 
			return instance.q==null? instance.p : instance.q[idx];
		}
		public float commonness(){
			//			if (instance==null || idx > (instance.q==null? 1 : instance.q.length)) throw new IllegalStateException();
			//			else 
			return instance.q==null? 1f : instance.c[idx];
		}
		public int index(){
			return idx;
		}
	}

	/**
	 * @descr Create a new Anchor 
	 * @param int id - a unique identifier for the anchor
	 * @param Int2IntMap links
	 * @param int frequency
	 * @return A new Anchor Object
	 * */

	public static Anchor build(int id, final Int2IntMap links, int frequency, IntSet people)
	{

		Anchor a = new Anchor();
		a.i = id;
		a.f = frequency;

		a.l = 0;
		int realLinks = 0;

		//we cannot remove elements from iterator dynamically
		IntSet to_remove = new IntArraySet(links.size());

		for(int page : links.keySet())
		{
			a.l += links.get(page);

			//links.get(page)
			if (links.get(page) >= MIN_LINKS)
			{
				//se questi link derivano dal boost per people, dobbiamo scartarli dal conteggio per la commonness
				if (people.contains(page) && links.get(page)==WikipediaAnchorParser.BOOST_TITLE) continue;
				realLinks += links.get(page);
			}
			else to_remove.add(page);
		}
		//giusto nel caso in cui questa ancora deriva solo da un boost di una persona (e quindi realLinks è rimasto a zero)
		if (realLinks < WikipediaAnchorParser.BOOST_TITLE) realLinks = WikipediaAnchorParser.BOOST_TITLE;

		for(int r : to_remove) links.remove(r);
		to_remove.clear();
		if (links.size() == 0) return null;

		//check link probability
		a.k = (float) a.l / (float) Math.max(frequency, a.l);
		if (a.k < MIN_LP) return null;

		
		//Check commonness
		for(int page : links.keySet())
		{
			float comm= (float) links.get(page) / (float) realLinks;
			if(comm<MIN_COMM) to_remove.add(page);
		}	

		for(int r : to_remove) links.remove(r);

		if (links.size() == 0) return null;
		//if we have removed some pages, we have to set the un-ambiguous page
		else if (links.size() == 1) 
		{
			a.p = links.keySet().iterator().nextInt();
		}
		else
		{
			a.p = -1;
			a.q = new int[links.size()];
			a.c = new float[links.size()];
			int i=0;
			for(int page : links.keySet())
				a.q[i++] = page;

			IntArrays.quickSort(a.q, new IntComparator() {
				@Override
				public int compare(Integer a, Integer b) {
					return compare(a.intValue(), b.intValue());
				}
				@Override
				public int compare(int a, int b) {
					return links.get(b)-links.get(a);
				}
			});

			for(i=0;i<a.q.length; i++)
				a.c[i] = (float) links.get(a.q[i]) / (float) realLinks;
		}

		return a;

	}

	/**Serialize the anchor object to a string.
	 * @param a the anchor.
	 * @return A string representation of the anchor.
	 * @throws IOException
	 */
	public static String serialize(Anchor a) throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ObjectOutputStream s = new ObjectOutputStream(bytes);
		s.writeObject(a);
		s.flush();
		//		return DatatypeConverter.printBase64Binary(bytes.toByteArray());
		return Base64.encodeBase64String(bytes.toByteArray());
	}

	/**Given a string, de-serialize it and create an Anchor object.
	 * @param obj the serialized Anchor object.
	 * @return the de-serialized Anchor object.
	 * @throws IOException
	 */
	public static Anchor deserialize(String obj) throws IOException
	{
		byte[] data = Base64.decodeBase64(obj);
		ByteArrayInputStream bytes = new ByteArrayInputStream(data);
		ObjectInputStream s = new ObjectInputStream(bytes);
		try {
			return (Anchor)s.readObject();
		} catch (Exception e){
			throw new IOException("Unable to parse the object", e);
		}
	}

}
