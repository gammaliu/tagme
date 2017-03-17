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

import it.acubelab.tagme.config.TagmeConfig;

/**
 * Class to deal with long texts, segmenting them into shorter texts.
 *
 */
public final class Segmentation {

	public static final float LONG_WINDOW_APPROX_FACTOR = 2f;
	

	boolean substringPruning = TagmeConfig.get().getSetting(Annotation.MODULE).getBooleanParam(Annotation.PARAM_SUBSTRING_PRUNING, Annotation.DEFAULT_SUBSTRING_PRUNING);
	int longTextLimit = TagmeConfig.get().getSetting(Annotation.MODULE).getIntParam(Annotation.PARAM_LONG_TEXT_LIMIT, Annotation.DEFAULT_LONG_TEXT_LIMIT);
	Annotation.LongMode longMode =  TagmeConfig.get().getSetting(Annotation.MODULE).getEnumParam(Annotation.PARAM_LONG_TEXT_MODE, Annotation.DEFAULT_LONG_TEXT_MODE, Annotation.LongMode.class);
	float linkProbFactor = TagmeConfig.get().getSetting(Annotation.MODULE).getFloatParam(Annotation.PARAM_LINK_PROB_FACTOR, Annotation.DEFAULT_LINK_PROB_FACTOR);

	public void setLongMode(Annotation.LongMode longmode) {
		this.longMode = longmode;
	}
	
	public Annotation.LongMode getLongMode() {
		return this.longMode;
	}
	
	
	public void setSubstringPruning(boolean substringPruning) {
		this.substringPruning = substringPruning;
	}
	
	public boolean  getSubstringPruning() {
		return this.substringPruning;
	}
	
	public void setLongTextLimit(int longTextLimit) {
		this.longTextLimit = longTextLimit;
	}

	public int getLongTextLimit() {
		return this.longTextLimit;
		}

	
	public float getLinkProbFactor() {
		return linkProbFactor;
	}

	public void setLinkProbFactor(float linkProbFactor) {
		this.linkProbFactor = linkProbFactor;
	}

	public void segment(AnnotatedText input)
	{

		/*
		 * verifichiamo se si tratta di un testo che possiamo considerare "lungo"
		 */
		int longCheck = 0;
		if (longMode == Annotation.LongMode.BY_ANCHOR){
			for(Annotation s : input.annotations)
				if (!s.ignored && !s.pruned)
					longCheck++;
			//		} else if (longMode == Annotation.LongMode.BY_CHAR){
			//			longCheck = input.text_len;
		}



		if (longMode != Annotation.LongMode.NEVER && longCheck > longTextLimit*LONG_WINDOW_APPROX_FACTOR)
		{
			//TESTO LUNGO!!
			int N = input.annotations.size();

			input.isLong = true;
			input.windows = new int[N][2];

			//if (substringPruning)
			input.windowPruning = new boolean[N][N];

			int halfWindow = longTextLimit / 2;

			//Calcolo le finestre per ogni singola anchor
			for (int i=0; i<N; i++)
			{
				Annotation i_spot = input.annotations.get(i);
				if (i_spot.ignored || i_spot.pruned) {
					//Se l'ancora è stata rimossa o ignorata in fase di parsing non ha finestra e settiamo START e END a -1
					input.windows[i][AnnotatedText.WIN_START] = -1;
					input.windows[i][AnnotatedText.WIN_END] = -1;
					continue;
				}

				//FIXED: in realtà qui sotto stavamo contando anche le anchor che sono state ignorate o pruned...
				//				int spotStart = i-halfWindow - Math.max(i+halfWindow-N, 0);
				//				if (spotStart < 0) spotStart = 0;
				//				int spotEnd = i+halfWindow - Math.min(i-halfWindow, 0);
				//				if (spotEnd >= N) spotEnd = N-1;

				int spotStart = i, spotEnd = i;
				int windowCount = 0;
				if (i < N/2)
				{
					while(spotStart > 0 && windowCount < halfWindow){
						spotStart--;
						if (!input.annotations.get(spotStart).ignored && !input.annotations.get(spotStart).pruned)
							windowCount++;
					}
					while(spotEnd < N-1 && windowCount < longTextLimit){
						spotEnd++;
						if (!input.annotations.get(spotEnd).ignored && !input.annotations.get(spotEnd).pruned)
							windowCount++;
					}
				} else {
					while(spotEnd < N-1 && windowCount < halfWindow){
						spotEnd++;
						if (!input.annotations.get(spotEnd).ignored && !input.annotations.get(spotEnd).pruned)
							windowCount++;
					}
					while(spotStart > 0 && windowCount < longTextLimit){
						spotStart--;
						if (!input.annotations.get(spotStart).ignored && !input.annotations.get(spotStart).pruned)
							windowCount++;
					}
				}



				//siccome le anchor sono ordinate per token di partenza E POI lunghezza, potrebbe accadere che
				//l'ultima anchor sia inclusa in una piu' ampia che inizia con lo stesso token
				//oppure che la prima anchor sia sovrastringa di una piu' piccola che inizia con lo stesso token
				while(spotStart > 0 && input.annotations.get(spotStart).start == input.annotations.get(spotStart-1).start)
					spotStart--;
				while(spotEnd < N-1 && input.annotations.get(spotEnd).start == input.annotations.get(spotEnd+1).start)
					spotEnd++;

				input.windows[i][AnnotatedText.WIN_START] = spotStart;
				input.windows[i][AnnotatedText.WIN_END] = spotEnd;
			}


			//uso una cache per i controlli di superior perchè siccome le finestre sono
			//intersecate, molti controlli sono gli stessi
			// 0=not checked
			// 1=is superior
			//-1=not superior
			//if(substringPruning){
			int[][] superiorChecks = new int[N][N];

			//devo controllare in ogni finestra il pruning
			for (int i=0; i<N; i++)
			{
				//(questo pero' lo dovremmo aver gia' controllato sopra)
				Annotation i_spot = input.annotations.get(i);
				if (i_spot.ignored || i_spot.pruned) continue;


				//controllo innanzitutto se lo spot i-esimo (il centro della finestra) non venga rimosso col pruning,
				//perche' in tal caso elimino subito tutta la finestra.
				for(int j=input.windows[i][AnnotatedText.WIN_START]; j<=input.windows[i][AnnotatedText.WIN_END]; j++)
				{

					if (i==j) continue;
					//System.out.println(input.getOriginalText(input.annotations.get(j))+" is superior wrt "+input.getOriginalText(input.annotations.get(i))+" ?: ");

					int superior = superiorChecks[j][i];
					//il check non e' mai stato fatto
					if (superior == 0) superiorChecks[j][i] = superior = isSuperior(input, j, i)? 1 : -1;
					
					if (superior == 1){
						//System.out.println("Deleted spot "+i+" - "+input.getOriginalText(input.annotations.get(i))+" by "+input.getOriginalText(input.annotations.get(j)));
						//ho trovato dentro la finestra una anchor che cancella lo spot i-esimo
						//quindi posso eliminare tutta la finestra di i
						input.windows[i][AnnotatedText.WIN_START] = -1;
						input.windows[i][AnnotatedText.WIN_END] = -1;
						i_spot.superior = input.annotations.get(j);
						break;
					}
				}

				//se non ho cancellato tutta la finestra
				//devo controllare fra loro tutti i componenti della finestra
				//escluso i-esimo che ho gia' controllato
				if (input.windows[i][AnnotatedText.WIN_START] >= 0)
				{
					for(int j=input.windows[i][AnnotatedText.WIN_START]; j<=input.windows[i][AnnotatedText.WIN_END]; j++)
					{
						if (j==i) continue;

						Annotation j_spot = input.annotations.get(j);

						//se questo spot era gia' stato rimosso in fase di parsing va rimosso anche nella finestra
						if (j_spot.ignored || j_spot.pruned) {
							input.windowPruning[i][j] = true;
							continue;
						}

						for(int k=input.windows[i][AnnotatedText.WIN_START]; k<=input.windows[i][AnnotatedText.WIN_END]; k++)
						{

							if (k==j) continue;
							int superior = superiorChecks[k][j];
							if (superior == 0) superiorChecks[k][j] = superior = isSuperior(input, k, j)? 1 : -1;

							if (superior == 1){
								//ho trovato un altro elemento della finestra che e' superiore a j
								//quindi in questa finestra j va rimosso

								input.windowPruning[i][j] = true;
								break;
							}
						}
					}

				}
			}
			//}



		} else {
			
			//TESTO CORTO

			for(int i=0; i<input.annotations.size(); i++)
			{
				Annotation i_spot = input.annotations.get(i);
				//se questo spot e' gia' stato rimosso o e' da ignorare andiamo oltre
				if (i_spot.pruned || i_spot.ignored) continue;

				for(int j=0;j<input.annotations.size(); j++)
				{
					if (i==j) continue;

					if (isSuperior(input, j, i)){
						//System.out.println(input.getOriginalText(input.annotations.get(j))+" is superior wrt "+input.getOriginalText(input.annotations.get(i)));
						//lo spot in posizione j e' superiore a quello in posizione i
						//quindi ho trovato uno spot che cancella quello in posizione i
						i_spot.pruned = true;
						i_spot.superior = input.annotations.get(j);
						break;
					}
				}

			}

		}

	}

	/**
	 * Check that a-th spot "is superior" with respect to the b-th spot, such that
	 * you can annotate only the a-th spot.
	 * @param input
	 * @param a
	 * @param b
	 * @return
	 */
	boolean isSuperior(AnnotatedText input, int a, int b)
	{
		//Se è pretaggato non deve mai essere eliminato
		if(input.annotations.get(b).isTagged()) return false;
		
		//non dovrebbe mai accadere
		if (a==b) return false;

		Annotation a_spot = input.annotations.get(a);
		//se questo spot e' gia' stato rimosso o e' da ignorare sicuramente non e' superiore a B
		//il metodo non dovrebbe mai essere chiamato su questo tipo di spot
		if (a_spot.pruned || a_spot.ignored) return false;

		Annotation b_spot = input.annotations.get(b);

		//se B è ignorato o pruned, allora A e' sicuramente superiore
		if (b_spot.pruned || b_spot.ignored)
			return true;

		//se A e B hanno la stessa anchor vuol dire che hanno lo stesso spot
		if (b_spot.anchor.equals(a_spot.anchor))
		{
			//tra due anchor uguali conservo solo la prima
			if (a<b) {
				//memorizzo questa informazione cosi' tramite a_spot posso ritrovare l'annotazione
				//(o cercando a cascata di b_spot.equal.equal...)
				b_spot.equal = a_spot;

				return true; //A e' precedente a B quindi e' superiore a B
			}
			else return false; //viceversa

		}
		else
		{
			if (substringPruning)
			{

				int a_len = a_spot.end - a_spot.start;
				int b_len = b_spot.end - b_spot.start;
				
				//ho controllato prima che avessero lo stesso oggetto anchor
				if (a_len == b_len) return false;
				
				//A è più corto di B, ma potrebbe avere una LP molto maggiore di B
				else if (a_len < b_len)
				{
					//Se A è contenuto nella stessa porzione di B allora controllo
					//che non abbia una LP molto maggiore di B per evitare casi in cui
					//ho due spot 'abc' e 'a' con lp(abc)=0,0001 e lp(a)=10
					//'abc' può essere tranquillamente eliminato per evitare che faccia rumore
					//Esempio 'in indianapolis' e 'indianapolis'
					if (linkProbFactor > 0) //se 0.0 il check è disabilitato
					{
						if (a_spot.start >= b_spot.start && a_spot.end <= b_spot.end)
						{
							if (a_spot.anchor.lp() / b_spot.anchor.lp() >= linkProbFactor) return true;
						}
					}
					return false;
				}
				
				//A è strettamente più lungo di B
				else if (contains(input, a_spot, b_spot))
				{
					if(a_spot.anchor.lp() >= b_spot.anchor.lp()) return true;
					else return false;
				}
	
			}
		}
		return false;
	}
	
	/**
	 * Check if one of the spots contains the other one
	 * @param input
	 * @param big
	 * @param small
	 * @return
	 */
	boolean contains(AnnotatedText input, Annotation a, Annotation b)
	{
		Annotation small = a.end-a.start > b.end-b.start ? b : a;
		Annotation big = small==b? a : b;
		
		//controllo che sia sottostringa con gli indici nel testo
		//così evito il confronto del contenuto testuale
		if (small.start >= big.start && small.end <= big.end) return true;
		
		//scorro carattere per carattere finchè non trovo small dentro big
		int small_len = small.end - small.start;
		for(int k=big.start;k <= big.end-small_len;k++)
		{
			int l=k;
			for(int h=small.start;h<small.end; h++)
			{
				if(input.text[h]!=input.text[l]) break;
				else if(h==small.end-1) return true;
				l++;
			}
		}
		return false;
	}
}
