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

import it.acubelab.Chars;
import it.acubelab.UTF16toASCII;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.WordTrie;
import it.acubelab.tagme.preprocessing.WordTrie.CharByCharSearcher;
import it.acubelab.tagme.preprocessing.anchors.AnchorSearcher;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class TagmeParser
{

	public static final int
	MAX_ASCII = 127,
	MAX_NORM_CHARS = 3;

	public static final String
	MODULE = "parsing",
	PARAM_DATA = "data",
	PARAM_WINDOW = "window",
	PARAM_MIN_LP = "minLinkProb";

	public static final int DEFAULT_WINDOW = 8;
	public static final DataType DEFAULT_DATA = DataType.LUCENE;
	public static final float DEFAULT_MIN_LP = Anchor.MIN_LP;

	/**
	 * Number of tokens for anchor parsing
	 */
	float minLinkProb;
	int windowSize;
	String lang;
	AnchorSearcher searcher;
	/**
	 * A trie containing the anchor that has to be ignored (aka stop-anchors)
	 */
	WordTrie ignoreAnchors = null;


	/**
	 * A set of chars that 'break' the anchor parsing.
	 * Namely if a parsing window contains one of those char, the window is truncated to that char
	 * Used to avoid that an anchor covers two words that were separated by brackets in the original text.
	 * (because bracket chars are removed when cleaning the text)
	 */
	static CharSet breakingCharSet = new CharOpenHashSet();
	//TODO: load breaking chars from configuration
	static {
		breakingCharSet.add('(');
		breakingCharSet.add(')');
		breakingCharSet.add('[');
		breakingCharSet.add(']');
	}	


	public TagmeParser(String lang) throws IOException
	{
		this(lang, false);
	}
	public TagmeParser(String lang, boolean loadData) throws IOException
	{
		this.lang = lang;
		this.windowSize = TagmeConfig.get().getSetting(MODULE).getIntParam(PARAM_WINDOW, DEFAULT_WINDOW);
		this.minLinkProb = TagmeConfig.get().getSetting(MODULE).getFloatParam(PARAM_MIN_LP, DEFAULT_MIN_LP);

		if (loadData){
			this.searcher = DataType.createAnchorSearcher(lang);
			String ignore_path=TagmeConfig.get().getRepositoryDir()+"/"+lang+"/support/IgnoreAnchors";
			//System.out.println(ignore_path);
			this.ignoreAnchors=WordTrie.read(ignore_path);
			//TODO: load ignore anchors from configuration?
		}
		else {
			ignoreAnchors = null;
			searcher = null;
		}
	}

	public void setAnchorSearcher(AnchorSearcher searcher)
	{
		this.searcher = searcher;
	}
	public void setIgnoreAnchors(WordTrie ignores){
		this.ignoreAnchors = ignores;
		
		
	}

	public void setWindowSize(int w){
		this.windowSize = w;
	}

	public int getWindowSize(){
		return this.windowSize;
	}
	public float getMinLinkProb() {
		return minLinkProb;
	}
	public void setMinLinkProb(float minLinkProb) {
		this.minLinkProb = minLinkProb;
	}
	public void parse(AnnotatedText input)
	{
		char[] copy = input.original.toCharArray();
		input.text = new char[copy.length];
		input.offsets = new int[copy.length];
		input.breaking_pos=new boolean[copy.length];

		input.text_len = TagmeParser.clean(copy, input.text, input.offsets,input.breaking_pos);

		input.annotations = parse(input.text, 0, input.text_len,input.breaking_pos,input.offsets,input.pre_tagged_pos);
		input.parsingWindow = windowSize;
	}


	/**
	 * Search for anchors in a subsequence a of cleaned text
	 * @param text The text in which to search for anchors
	 * @param offset The offset of the subsequence of text
	 * @param len The length of the subsequence
	 * @param breakingChars The positions of breaking chars
	 * @return The list of found spots with their anchors, positions in the text, and possibily the pruned/ignored statuses
	 */
	public List<Annotation> parse(char[] text, int offset, int len, boolean[] breakingChars, int[] offsets, int[] pre_tagged_pos)
	{
		if (text.length == 0 || len == 0) return new ArrayList<Annotation>(0);

		//contiene gli indici dove sono posizionati gli spazi (separatori di token)
		int[] tokens = new int[len];
		int tokens_len = 0;
		for(int i=offset; i<offset+len; i++)
			if (text[i] == ' ')
				tokens[tokens_len++] = i;


		ArrayList<Annotation> spots = new ArrayList<Annotation>(tokens_len*windowSize);

		CharByCharSearcher searchIgnoreAnchors = ignoreAnchors != null ? ignoreAnchors.getSearcher() : null;

		//per creare le CharSequence sull'array text[]
		TokenizedCharSequence tokenizedWindow = new TokenizedCharSequence(text, offset, offset+len, windowSize);

		//System.out.println(Arrays.toString(pre_tagged_pos));

		for (int currentToken=-1; currentToken<tokens_len; currentToken++)
		{
			//UGO: controllare che tra l'inizio e la fine della finestra non ci sia un brokenChars di mezzo
			//in tal caso la fine della finestra coincide con la posizione del brokenChars

			//l'inizio della finestra
			int windowStart = currentToken<0? 0: tokens[currentToken]+1;
			//la fine della finestra, se è superiore al numero di token, si usa la lunghezza totale del testo
			int windowEnd = currentToken+windowSize < tokens_len ? tokens[currentToken+windowSize] : len;

			windowStart+=offset;
			windowEnd+=offset;

			//testo pretaggato
			if(pre_tagged_pos!=null && pre_tagged_pos[offsets[windowStart]]==1)
			{
				for(int i=windowStart;i<windowEnd;i++)
				{
					if(pre_tagged_pos[offsets[i]]==-1)
					{
						windowEnd=i;
						continue;
					}
				}
			}
			//controllo breaking chars
			else if (breakingChars != null)
			{
				//Check for breaking chars
				int breaking_pos = windowStart+1;
				while(breaking_pos < windowEnd)
				{
					if(breakingChars[breaking_pos])
					{
						//the window is truncated to the breaking char
						windowEnd = breaking_pos;
						continue;
					}
					breaking_pos++;
				}
			}


			//Imposta la nuova finestra sull'array text
			tokenizedWindow.setNewWindow(windowStart, windowEnd-windowStart);

			//System.out.println(windowStart+":"+windowEnd);
			//System.out.println(tokenizedWindow.subSequence(windowStart+1,windowEnd-windowStart));


			int found=0;

			if(pre_tagged_pos!=null && pre_tagged_pos[offsets[windowStart]]==1 ){
				//found=1;
				found =searcher.SearchBestMatch(tokenizedWindow);

			}else{

				found = searcher.search(tokenizedWindow);

			}

			//		System.out.println(found+" anchors found in window: "+tokenizedWindow.subSequence(0,tokenizedWindow.length()));

			//NON HO TROVATO ANCORE
			if (found == 0) continue;


			//SE IL TESTO é PRETAGGATO NON DEVO FARE NESSUNA VERIFICA sulle ancore da ignorare
			//SEMPLICEMENTE PRENDO L'ANCORA RESTITUITA CHE SI TROVERA IN POSIZIONE 0 di anchorIdx
			if(pre_tagged_pos!=null && pre_tagged_pos[offsets[windowStart]]==1)
			{
				Annotation s = new Annotation();
				s.anchor=tokenizedWindow.anchors[0];
				s.pruned=false;
				s.ignored=false;
				s.id=offsets[windowStart];
				s.start=windowStart;
				s.end=windowEnd+1;
				s.tagged=true;
				spots.add(s);
				currentToken=currentToken+tokenizedWindow.getActualWindowSize();
				continue;	
			}			

			//IGNORE ANCHORS

			//k contiene l'indice corrente dove abbiamo effettuato l'ultima ricerca per le ignore-anchor
			//siccome le eventuali ancore sono una sottostringa dell'altra, la ricerca riprende da dove era rimasta precedentemente
			//sfruttando i metodi dell'oggeto CharByCharSearcher di WordTrie
			int k=windowStart;
			//resetto la ricerca delle ignore-anchors
			if (ignoreAnchors != null) searchIgnoreAnchors.reset();

			for (int tokenIdx=0; tokenIdx<windowSize; tokenIdx++)
			{
				//nell'array tokenizedWindow.anchors ci sono le anchor trovate, dove all'i-esima posizione trovo
				//l'ancora corrispondente all'i-esimo+1 token (ovviamente la partenza è sempre dal primo token)
				if (tokenizedWindow.anchors[tokenIdx] != null)
				{
					Annotation s = new Annotation();
					s.anchor = tokenizedWindow.anchors[tokenIdx];
					s.start = windowStart;
					//prima cerco dove finisce, nel testo pulito, il token di quest'anchor
					//se e' l'ultimo token, vuol dire che sono alla fine del testo
					s.end = currentToken+tokenIdx+1 < tokens_len ? tokens[currentToken+tokenIdx+1] : offset+len;

					//Pruning per link probability
					if (minLinkProb > Anchor.MIN_LP && s.anchor.lp() < minLinkProb)
					{
						s.pruned = true;
					}

					//System.out.println("");
					//se non e' stata prunata devo controllare che non sia fra la lista delle ancore da ignorare
					if (!s.pruned && ignoreAnchors != null)
					{
						//k rimane nella posizione dell'ultima ricerca e lo stato di questo CharByCharSearcher e' rimasto invariato
						//Se l'ultima ricerca e' finita male, il while si interrompe subito e anche il metodo end() ritorna falso
						while(k<s.end && searchIgnoreAnchors.next(text[k++]));
						//adesso ho terminato di scorrere il trie perche'
						//	1. non ho trovato text[windowStart<->s.end] e quindi searcher.end() ritornera' false
						//	2. ho trovato tutto text[windowStart<->s.end] e ora devo controllare che in quella posizione
						//		nel trie ci sia una terminazione.
						//Il searcher rimane in questo stato (su text[s.end]) e va bene, perche' la prossima anchor e' sicuramente una
						//superstringa di quella corrente.
						s.ignored = searchIgnoreAnchors.end();
						//System.out.println("Ignored:"+s.getTopic());
					}

					//in ogni caso aggiungo sempre lo spot alla lista.
					spots.add(s);
				}
			}
		}

		return spots;	
	}

	// UGO: public static int clean(char[] input, char[] output, int[] offsets, boolean[] brokenChars)

	/**
	 *	Contiene la logica principale per ripulire un testo. Come regola generale vengono mantenuti solo lettere e numeri
	 *	e ogni occorrenza (anche multipla) di un carattere diverso, viene sostituita da un singolo spazio.
	 *	Il singolo spazio diventa quindi il carattere separatore di token.<br/>
	 *	Vengono applicate le seguenti logiche speciali:<br/><ul>
	 *	<li>per i punti, dove si cerca di capire se si tratta di punti che identificano
	 *	un acronimo (nel qual caso lo spazio non si inserisce).</li>
	 *	<li>per i 'breakingChar', ovvero caratteri che troncano la finestra di parsing delle anchor
	 *	In questo caso, se si incontra un breaking char, lo spazio viene inserito ugualmente ma l'array breakingChars viene aggiornato
	 *	per tenere traccia del fatto che quel separatore di token è relativo ad un breaking char.</li>
	 *	<li>per i dashes (i trattini): vengono rimossi e nessuno spazio viene inserito.</li></ul>
	 * @param input Viene ripulito dai caratteri non ASCII
	 * @param output Viene aggiornato con il testo ripulito.
	 * @param offsets Viene aggiornato con la posizione del testo in quello original.
	 * In pratica offsets[i] contiene la posizione nel testo originale del carattere i-esimo del testo pulito
	 * @param breakingChars Viene aggiornato con indicatori dei breaking chars.
	 * Le posizioni sono riferite al testo pulito, ovvero breakingChars[i]=true se il separatore di token che si trova nel testo pulito
	 * alla posizione i-esima era un breaking char.
	 * @return La lung	hezza del testo pulito (output).
	 */
	public static int clean(char[] input, char[] output, int[] offsets, boolean[] breakingChars)
	{
		if (input.length > output.length || input.length > offsets.length) throw new IndexOutOfBoundsException();

		for(int i=0; i<input.length; i++)
			input[i] = UTF16toASCII.MAP[(int)input[i]];


		int out_len = 0;
		int i=0, last=0;
		int currentOffset = 0;
		boolean addSpace = false;

		int t=0;
		while(t<input.length && !Character.isLetterOrDigit(input[t])){
			t++;	
		}
		if(t>0 && offsets.length>1){
			i=t;
			last=i;
			output[0]=' ';
			offsets[1]=t;
			currentOffset+=t;
			
		}
		while(i<input.length)
		{
			//CICLO SUI CARATTERI BUONI
			while(i<input.length && validChar(input, i))
			{
				offsets[i-currentOffset] = i;
				i++;
			}

			//APPENDO I CARATTERI BUONI
			if (i>last)
			{
				if (addSpace && out_len > 0)  output[out_len++] = ' ';
				if (i-last == 1) output[out_len++] = input[last];
				else {
					System.arraycopy(input, last, output, out_len, i-last);
					out_len += i-last;
				}
			}


			//CICLO I CARATTERI DA STRIPPARE
			addSpace = false;
			while(i<input.length && !validChar(input, i))
			{

				//choose if to add a space char between the two words
				if (!addSpace && (
						Character.isWhitespace(input[i]) ||
						(input[i]<=MAX_ASCII && input[i]!='.' && input[i]!='-') ||
						(input[i]=='.' && !acronymDot(input, i))
						)
						)
				{
					offsets[i-currentOffset] = i;
					currentOffset--;
					addSpace = true;
				}

				if (breakingChars != null)
					if (breakingCharSet.contains(input[i])/* ||  (input[i]=='.' && addSpace)*/ )
						breakingChars[out_len] = true;



				i++;
				currentOffset++;
				last=i;
			}
			//			if (addSpace && out_len > 0 && i<input.length) {
			//				currentOffset--;
			//				offsets[i-currentOffset] = i;
			//			}
		}


		if (out_len < input.length)
			offsets[out_len] = -1;



		return out_len;


	}



	//	 ||
	//	//when the dot is in the second-last position and not part of an acronym
	//	(input[i]=='.' && i==input.length-2 && i>1 && input[i-2]!='.' && !Character.isWhitespace(input[i-2])) ||
	//	//when the dot is not part of an acronym
	//	(input[i]=='.' && i<input.length-2 && input[i+2]<=MAX_ASCII && input[i+2]!='.' && !Character.isWhitespace(input[i+2]))
	//	))
	/**
	 * Controlla che sia un punto facente parte di un acronimo. Trova gli acronimi solo di singole lettere
	 * @param input
	 * @param i
	 * @return
	 */
	static boolean acronymDot(char[] input, int i)
	{
		//se il carattere alla posizione i+2 è un punto o un whitespace allora questo è potenzialmente un punto di un acronimo
		//se nel mezzo (i+1) cè della merda non me ne frega niente, tanto verrà strippata
		if (input[i]=='.' && i<input.length-2 && (input[i+2]=='.' || Character.isWhitespace(input[i+2])) ) return true;

		//quando siamo alla fine del testo devo controllare che ci fosse un punto o un withespace prima di del carattere i, alla posizione i-2
		if (input[i]=='.' && i==input.length-2 && i>1 && (input[i-2]=='.' || Character.isWhitespace(input[i-2])) ) return true;

		return false;

	}

	static boolean validChar(char[] input, int i)
	{
		return input[i] <= MAX_ASCII && ( Character.isLetter(input[i]) || Character.isDigit(input[i]) );
	}

	/**
	 * Clean the input text
	 * WARNING: it possibly modifies the input array!
	 * @param input The input chars, this could be modified!
	 * @param output The cleaned text chars, its length must be greater or equal to input.length.
	 * @param offsets The positions of cleaned chars in the original input
	 * (i.e. output[i] was in position offsets[i] of the input array).
	 * Its length must be greater or equal to input.length
	 * @return The length of the cleaned text that is lower than or equal to input.length.
	 */
	public static int clean(char[] input, char[] output, int[] offsets)
	{
		return clean(input,output,offsets,null);
	}

	public static final class TokenizedCharSequence implements CharSequence
	{
		int offset, len, hash;
		char[] array;
		Anchor[] anchors;
		public TokenizedCharSequence(char[] array){
			this(array, 0, array.length, array.length);
		}
		public TokenizedCharSequence(char[] array, int offset, int len, int windowSize){
			this.array = array;
			this.offset = offset;
			this.len = len;
			this.hash = -1;
			this.anchors = new Anchor[windowSize];
		}
		void setNewWindow(int offset, int len){
			this.offset = offset;
			this.len = len;
			this.hash = -1;
			for (int i=0;i<anchors.length; i++) anchors[i] = null;
		}
		public boolean isEndToken(int i){
			return i+1>=len || array[offset+i+1] == ' ';
		}
		public int getWindowSize(){
			return anchors.length;
		}
		public void setAnchorAt(int tokenIdx, Anchor a){
			this.anchors[tokenIdx] = a;
		}
		public Anchor getAnchorAt(int tokenIdx){
			return anchors[tokenIdx];
		}
		@Override
		public char charAt(int i) {
			return array[offset+i];
		}
		@Override
		public int length() {
			return len;
		}
		@Override
		public CharSequence subSequence(int start, int end) {
			return new String(array, this.offset+start, this.offset+end - (this.offset+start));
		}
		@Override
		public int hashCode() {
			if ( hash >= -1 ) {
				hash = 0;
				for ( int i = offset; i < offset+len; i++ ) hash = 31 * hash + array[ i ];
				hash |= ( 1 << 31 );
			}
			return hash;
		}
		@Override
		public boolean equals(Object o) {
			if (o instanceof CharSequence){
				CharSequence s = (CharSequence)o;
				if (len != s.length()) return false;
				for(int i=0; i<len; i++)
					if (array[i+offset]!=s.charAt(i))
						return false;
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return new String(array, offset, len);
		}

		public int getActualWindowSize(){
			int n=0;
			for(int i=this.offset;i<this.offset+len || i<len ;i++){
				if(array[i]==' ') n++;
			}

			return n;
		}
	}



}
