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

import it.unimi.dsi.lang.MutableString;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class AnnotatedText {
	
	public static final int INIT_BUFFER_CAPACITY = 1024;

	/**
	 * The original text
	 */
	MutableString original;
	/**
	 * The cleaned text. The array could be greater than the actual text. See text_len
	 */
	char[] text;
	/**
	 * The length of the cleaned text
	 */
	int text_len;
	/**
	 * The offset of the cleaned char wrt the cleaned text.
	 * Means that text[i] was originally at the position offset[i] in the original text
	 */
	int[] offsets;
	/**
	 * Specify if the char at position i (of the cleaned text) was a breaking char
	 * See TagmeParser.breakingCharSet
	 */
	boolean[] breaking_pos;
	
	/**
	 * Positions of tagged already defined for this text. Used only for testing.
	 * If null (initialization value), the text is not pre-tagged
	 */
	int[] pre_tagged_pos = null;
	public int[] getPreTaggedPos() {
		return pre_tagged_pos;
	}
	public void setPreTaggedPos(int[] pre_tagged_pos) {
		this.pre_tagged_pos = pre_tagged_pos;
	}

	/**
	 * List of all annotations, pruned, ignored, disambiguated...
	 * The list is sorted based on the position of the anchor in the text
	 */
	List<Annotation> annotations;
	
	/**
	 * Length of the window size (in terms of tokens) for anchor parsing
	 */
	int parsingWindow = TagmeParser.DEFAULT_WINDOW;
	/**
	 * Minimum link probability used for parsing
	 */
	float minLP = Anchor.MIN_LP;
	
	
	// LONG TEXT MANAGEMENT
	boolean isLong;
	public static final int WIN_START = 0;
	public static final int WIN_END = 1;
	/**
	 * Disambiguation windows for all anchors in the text
	 * For each annotation at idx i of annotations,
	 * windows[i] contains the start and the end of the surrounding window for the annotation
	 * The window is expressed in terms of indexes of other annotation in the list
	 * The array has a dimension of [N][2], where N is the size of annotations list.
	 */
	int[][] windows;
	/**
	 * Information about pruning (substring pruning algorithm) for a certain window
	 * It could be that an anchor A could be pruned if you consider it in a certain window for anchor B
	 * (eg. because window for anchor B contains an anchor C that is super-string of A)
	 * But if you consider A in another window that doesn't contain C, A has not to be pruned.
	 * So windowPruning[B][A] specifies whether the anchor A in the window for anchor B has to be ignored for disambiguation
	 * The matrix has a dimension of [N][N], where N is the size of annotations list
	 */
	boolean [][] windowPruning;
	
	/**
	 * Leading chars removed at the beginning of the text.
	 */
	//private int removedLeadingChars = 0;
	
	/**Creates an annotated text object reading data from a reader of default capacity.
	 * @param reader where to read the data from.
	 * @throws IOException if an error occurred while reading from the reader.
	 */
	public AnnotatedText(Reader reader) throws IOException
	{
		this(reader, INIT_BUFFER_CAPACITY);
	}
	
	/**Creates an annotated text object reading data from a reader.
	 * @param reader where to read the data from.
	 * @param length the maximum length of the text to read.
	 * @throws IOException if an error occurred while reading from the reader.
	 */
	public AnnotatedText(Reader reader, int length) throws IOException
	{
		original = new MutableString(length+1);
		char[] buffer = new char[length+1];
		int read = 0;
		while((read=reader.read(buffer, 0, length+1)) >= 0)
			original.append(buffer, 0, read);
	}
	
	/** Create an annotated text from a given string, skipping all leading chars that are not letters nor digits.
	 * @param text the annotated text.
	 */
	public AnnotatedText(String text){
		/*while(removedLeadingChars<text.length() && !Character.isLetterOrDigit(text.charAt(removedLeadingChars)))
			removedLeadingChars++;
		this.original = new MutableString(text.substring(removedLeadingChars,text.length()));
		*/
		this.original=new MutableString(text);
	}
	
	
	public int getOriginalTextStart(Annotation s){
		//dobbiamo sommare removedLeadingChars perchè il chiamante che ha passato l'original text
		//non ha modificato il testo rimuovendo i leading chars all'inizio
		return /*removedLeadingChars+*/offsets[s.start];
	}
	
	public int getOriginalTextEnd(Annotation s){
		/*
		 * We need to find the position of the last character of the clean text in the original text
		 * and then add 1. (interval end is exclusive). The behavior below is no longer needed
 		 */
		return /*removedLeadingChars+*/offsets[s.end-1]+1;
		
		//quando la fine dello spot è fuori dagli offsets vuol dire che la fine corrisponde alla fine del testo reale
		//se invece l'offset e' negativo sono alla fine del testo pulito e quindi devo stare attento a possibili
		//caratteri terminali del testo pulito che sono stati strippati.
//		if (s.originalEnd >= offsets.length || offsets[s.originalEnd] <0)
//		else
//			s.originalEnd = offsets[s.originalEnd];
	}
	
	public String getOriginalText(Annotation a){
		/*
		 * qui non dobbiamo sommare removedLeadingChars,
		 * perchè il nostro original è già shiftato
		 */
		return original.subSequence(offsets[a.start], offsets[a.end-1]+1).toString();
	}
	public String getText(Annotation a){
		return new String(text, a.start, a.end-a.start);
	}
	public String getText(){
		return new String(text, 0, text_len);
	}
	
	public List<Annotation> getAnnotations(){
		return this.annotations;
	}
	
	public AnnotationWindow getWindowIterator(){
		return new AnnotationWindow();
	}
	public boolean isLong(){
		return isLong;
	}
	public MutableString getOriginal(){
		return original;
	}
	
	public boolean isPruned(Annotation a)
	{
		if (!isLong) return a.ignored || a.pruned;
		else {
			int id = annotations.indexOf(a);
			if (id < 0) throw new IllegalArgumentException();
			else return windows[id][WIN_START] < 0;
		}
	}
	
	/**
	 * Used to iterate through the list of disambiguation windows<br/>
	 * Usage:<br/>
	 * - call setStartAnnotation to initialize the iterator to a given anchor A<br/>
	 * - call next() to move the iterator<br/>
	 * - if next() has returned true, use current() to retrieve the anchor in the window<br/>
	 * - iterate until next() returns false<br/>
	 * To iterate the window of another anchor, re-call setStartAnnotation <br/>
	 */
	public final class AnnotationWindow
	{
		int annot;
		int cursor;
		boolean empty;
		boolean all = false;
		public void setStartAnnotation(int a){
			setStartAnnotation(a, false);
		}
		public void setStartAnnotation(int a, boolean all){
			annot = a;
			cursor = isLong? windows[a][WIN_START]-1 : -1;
			Annotation an = annotations.get(a);
			empty = an.pruned || an.ignored || (isLong && windows[a][WIN_START]<0);
			this.all = all;
		}
		public boolean empty(){
			return empty;
		}
		public boolean next(){
			if (empty) return false;
			cursor++;
			if (isLong){
				while (cursor <= windows[annot][WIN_END] &&
						(!all && windowPruning != null && windowPruning[annot][cursor] || cursor==annot)
					)
					cursor++;
				
				return cursor <= windows[annot][WIN_END];
				
			} else {
				while (cursor < annotations.size() &&
						(!all && (annotations.get(cursor).ignored || annotations.get(cursor).pruned) || cursor==annot)
					)
					cursor++;
				return cursor < annotations.size();
			}
		}
		public Annotation current(){
			return annotations.get(cursor);
		}
		public boolean currentIsPruned(){
			return isLong?
					windowPruning != null && windowPruning[annot][cursor] :
					(annotations.get(cursor).ignored || annotations.get(cursor).pruned);
		}
		
	}
}
