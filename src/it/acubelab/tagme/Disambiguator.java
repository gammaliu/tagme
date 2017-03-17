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

import it.acubelab.tagme.Anchor.PageCommonnessIterator;
import it.acubelab.tagme.AnnotatedText.AnnotationWindow;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.DatasetLoader;
import it.acubelab.tagme.preprocessing.support.PeopleWIDs;
import it.unimi.dsi.fastutil.ints.IntSet;

import org.apache.log4j.Logger;

public final class Disambiguator {

	static Logger log = Logger.getLogger(Disambiguator.class);
	
	private static final float LOWEST_EPSILON = 0.000001f; 

	public float getEpsilon() {
		return epsilon;
	}
	public void setEpsilon(float epsilon) {
		this.epsilon = epsilon;
	}
	public float getMinCommonness() {
		return minCommonness;
	}
	public void setMinCommonness(float minCommonness) {
		this.minCommonness = minCommonness;
	}
	public int getMinLinks() {
		return minLinks;
	}
	public void setMinLinks(int minLinks) {
		this.minLinks = minLinks;
	}
	public boolean isQuadraticMean() {
		return quadraticMean;
	}
	public void setQuadraticMean(boolean quadraticMean) {
		this.quadraticMean = quadraticMean;
	}
	public boolean isRobustnessTest() {
		return robustnessTest;
	}
	public void setRobustnessTest(boolean robustnessTest) {
		this.robustnessTest = robustnessTest;
	}
	public float getRobustnessLimit() {
		return robustnessLimit;
	}
	public void setRobustnessLimit(float robustnessLimit) {
		this.robustnessLimit = robustnessLimit;
	}
	public boolean isSelectByNVotes() {
		return selectByNVotes;
	}
	public void setSelectByNVotes(boolean selectByNVotes) {
		this.selectByNVotes = selectByNVotes;
	}

	public IntSet getPeople() {
		return people;
	}
	public void setPeople(IntSet people) {
		this.people = people;
	}

	float epsilon;
	float minCommonness;
	int minLinks;
	boolean quadraticMean;
	boolean robustnessTest;
	float robustnessLimit;
	boolean selectByNVotes;
	boolean checkNames;
	
	public static final float
	DEFAULT_EPSILON = 0.15f,
	DEFAULT_ROBUSTNESS_LIMIT = 0.90f,
	DEFAULT_MIN_COMMONNESS = 0.002f;
	public static final int
	DEFAULT_MIN_LINKS = 0;
	public static final boolean
	DEFAULT_QUADRATIC_MEAN = false,
	DEFAULT_ROBUSTNESS_TEST = false,
	DEFAULT_SELECT_BY_NVOTES = false,
	DEFAULT_CHECK_PEOPLE_NAMES=true;
	public static final String
	MODULE ="disambiguator",
	PARAM_EPSILON = "epsilon",
	PARAM_ROBUSTNESS_LIMIT = "robustnessLimit",
	PARAM_MIN_COMMONNESS = "minCommonness",
	PARAM_MIN_LINKS = "minLinks",
	PARAM_QUADRATIC_MEAN = "quadraticMean",
	PARAM_ROBUSTNESS_TEST = "robustnessTest",
	PARAM_SELECT_BY_NVOTES = "selectByNVotes",
	PARAM_CHECK_ON_PEOPLES="checkPeopleNames";
	IntSet people = null;
	
	public Disambiguator(){
		this.epsilon = TagmeConfig.get().getSetting(MODULE).getFloatParam(PARAM_EPSILON, DEFAULT_EPSILON);
		this.robustnessLimit = TagmeConfig.get().getSetting(MODULE).getFloatParam(PARAM_ROBUSTNESS_LIMIT, DEFAULT_ROBUSTNESS_LIMIT);
		this.minCommonness = TagmeConfig.get().getSetting(MODULE).getFloatParam(PARAM_MIN_COMMONNESS, DEFAULT_MIN_COMMONNESS);
		this.minLinks = TagmeConfig.get().getSetting(MODULE).getIntParam(PARAM_MIN_LINKS, DEFAULT_MIN_LINKS);
		this.quadraticMean = TagmeConfig.get().getSetting(MODULE).getBooleanParam(PARAM_QUADRATIC_MEAN, DEFAULT_QUADRATIC_MEAN);
		this.robustnessTest = TagmeConfig.get().getSetting(MODULE).getBooleanParam(PARAM_ROBUSTNESS_TEST, DEFAULT_ROBUSTNESS_TEST);
		this.selectByNVotes = TagmeConfig.get().getSetting(MODULE).getBooleanParam(PARAM_SELECT_BY_NVOTES, DEFAULT_SELECT_BY_NVOTES);
		this.checkNames= TagmeConfig.get().getSetting(MODULE).getBooleanParam(PARAM_CHECK_ON_PEOPLES, DEFAULT_CHECK_PEOPLE_NAMES);
	}
	/**
	 * This constructor is used to load Data about people for people check
	 * @param lang
	 */
	public Disambiguator(String lang){
		this();
		people = DatasetLoader.get(new PeopleWIDs(lang));
	}

	/**
	 * TODO: if the debugger has been set, it collects detailed information about votes so that
	 * these figures can be shown in a proper interface to easy debugging of annotation process
	 */
	public DisambiguationDebugger debug = null;
	
	/**Disambiguate an annotated text. This method will result in a text with no more than 1 annotation for each spot.
	 * @param input The text to disambiguate. For each spot there can be any number of possible annotations.
	 * @param relatedness The object to compute the relatedness between wikipedia pages.
	 */
	public void disambiguate(AnnotatedText input, RelatednessMeasure relatedness)
	{
		//final IntSet people = DatasetLoader.get(new PeopleWIDs(lang));

		PageCommonnessIterator pageIterator = new PageCommonnessIterator();
		PageCommonnessIterator innerPageIterator = new PageCommonnessIterator();	
		AnnotationWindow windowIterator = input.getWindowIterator();

		for(int aId=0; aId<input.annotations.size(); aId++)
		{
			
			Annotation a = input.annotations.get(aId);

			// Recall that, in preprocessing for each people page we have inflated the anchor composed by the surname, adding 2 fake links
			// Thus, if checkNames is enabled, we perform additional check to avoid the following scenario:
			// - in the original text the anchor doesn't look like a name (upper case of the first letter)
			// - the sense has just the 2 fake links
			// in such a case the target can be ignored for this anchor
			boolean likesPeople=false;
			if(checkNames && Character.isUpperCase(input.getOriginalText(a).charAt(0))){
				likesPeople=true;
			}

			//String page_t1=input.getOriginalText(a);
			
			windowIterator.setStartAnnotation(aId);

			if (windowIterator.empty || a.ignored || a.pruned){
				a.topic = Annotation.NOT_DISAMBIGUATED;
				continue;
			}
			
			Anchor anchor = a.anchor;
			float[] votes = new float[anchor.ambiguity()];
			int[] n_votes = new int[anchor.ambiguity()];
			float maxVotes = 0;
			int maxNVotes = 0;
			int realAmbiguity = 0;
			int notAmbiguousAnchorSense = Annotation.NOT_DISAMBIGUATED;
			int notAmbiguousAnchorLinks = 0;

			/**
			 *  PREPRUNING BY COMMONNESS OR BY SUPPORT
			 */
			if (minCommonness > 0 || minLinks > Anchor.MIN_LINKS || checkNames)
			{
				pageIterator.setAnchor(anchor);
				while(pageIterator.next())
				{
					int p = pageIterator.page();
					float commonness = pageIterator.commonness();
					int links = (int) (commonness * anchor.links());

					if ( (minCommonness>0 && commonness < minCommonness) || 
							(minLinks>0 && links < minLinks) ||
							( checkNames && !likesPeople && links <= Anchor.MIN_LINKS && people!=null && people.contains(p)) )
					{
						votes[pageIterator.index()] = -1;
						
						//DEBUGGER
						if (debug != null)
						{
							int pruned = DisambiguationDebugger.PRUNED_UNDEF;
							if (minCommonness>0 && commonness < minCommonness) pruned = DisambiguationDebugger.PRUNED_MINCOMM;
							else if (minLinks>0 && links < minLinks) pruned = DisambiguationDebugger.PRUNED_MINLINK;
							else if ( checkNames && !likesPeople && links <= Anchor.MIN_LINKS && people!=null && people.contains(p)) pruned = DisambiguationDebugger.PRUNED_PEOPLE;
							debug.setStatus(a, p, pruned);
						}
					}
					else
					{
						votes[pageIterator.index()] = 0;
						realAmbiguity++;
						notAmbiguousAnchorSense = pageIterator.page();
						notAmbiguousAnchorLinks = links;
					}
				}
			} else {

				realAmbiguity = anchor.ambiguity();
				//if there is only one choice, the sense is defined as non-ambiguous
				if (realAmbiguity == 1){
					notAmbiguousAnchorSense = anchor.singlePage();
					notAmbiguousAnchorLinks = anchor.links();
				}
			}



			/**
			 * VOTES from other anchors
			 * must be done only if there is more than one possible sense.
			 */
			if (realAmbiguity > 1) 
			{		
				//String page_t=input.getOriginalText(a);
				
				pageIterator.setAnchor(anchor);

				while(pageIterator.next())
				{

					if (votes[pageIterator.index()] < 0) continue;


					while(windowIterator.next())
					{

						Annotation b = input.annotations.get(windowIterator.cursor);
						float bVote = 0;
						/**
						 * VOTES from each sense of anchor b
						 */
						innerPageIterator.setAnchor(b.anchor);
						while(innerPageIterator.next())
						{
							int bpage=innerPageIterator.page();
							float bpage_comm = innerPageIterator.commonness();
							int bpage_links = (int) (bpage_comm * anchor.links());

							if ( (minCommonness>0 && bpage_comm < minCommonness) || 
									(minLinks>0 && bpage_links < minLinks) ||
									( checkNames && !likesPeople && bpage_links <= Anchor.MIN_LINKS && people!=null && people.contains(bpage)) )
								continue;

							float rel = relatedness.rel(pageIterator.page(), bpage);
							bVote += rel * bpage_comm;

							// DEBUG!
							if (debug != null) debug.vote(a, pageIterator.page(), b, innerPageIterator.page(), rel * bpage_comm);

						}
						if (quadraticMean)
							bVote = bVote / (float)b.anchor.ambiguity();

						votes[pageIterator.index()] += bVote;
						if (bVote > 0) n_votes[pageIterator.index()] ++;
					}

					if (votes[pageIterator.index()] > maxVotes) maxVotes = votes[pageIterator.index()];
					if (n_votes[pageIterator.index()] > maxNVotes) maxNVotes = n_votes[pageIterator.index()];
					windowIterator.setStartAnnotation(aId);
				}
			}

			/**
			 * DISAMBIGUATION
			 */
			//NO votes
			if (maxVotes == 0)
			{
				//String page_t=input.getOriginalText(a);
				
				if (realAmbiguity == 1)
				{
					//see above the reason of this check
					if(checkNames && !likesPeople && notAmbiguousAnchorLinks <= Anchor.MIN_LINKS && people!=null && people.contains(notAmbiguousAnchorSense)) 
						a.topic=Annotation.NOT_DISAMBIGUATED; 
					else 
						a.topic = notAmbiguousAnchorSense;
				} 
				//if there are no votes and there is ambiguity, there are no means to decide which sense is right.
				//else a.topic = Annotation.NOT_DISAMBIGUATED;
				//New Version
				else 
				{
					a.topic=a.anchor.mostCommonPage();
				}
				a.votes = 0;
				a.n_votes = 0;
			}

			//If there is more than one vote, choose the candidate with the TOP-epsilon
			else 
			{
				/**
				 * ROBUSTNESS TEST
				 */
				float maxAncComm = a.anchor.maxCommonness();
				//If the robustness test must be done, the epsilon must be changed to the lowest.
				//If the maximum commonness is higher than the robustness limit, the normal epsilon can be used.
				//Otherwise, a dummy epsilon equal to 0 can be used.
				float robust_epsilon = robustnessTest && maxAncComm < robustnessLimit ? LOWEST_EPSILON : epsilon; 

				float maxComm = 0;
				pageIterator.setAnchor(anchor);
				while(pageIterator.next())
				{
					if (
							selectByNVotes && ((float)maxNVotes-n_votes[pageIterator.index()])/(float)maxNVotes < robust_epsilon ||
							!selectByNVotes && (maxVotes-votes[pageIterator.index()])/maxVotes < robust_epsilon
					)
					{
						float icomm = pageIterator.commonness();
						
						if (debug != null) debug.setStatus(a, pageIterator.page(), DisambiguationDebugger.TOP_EPSILON);
						
						if (icomm > maxComm){
							maxComm = icomm;
							a.topic = pageIterator.page();
							a.votes = votes[pageIterator.index()];
							a.n_votes = n_votes[pageIterator.index()];		
							//If the robustness test is active, then this is the best topic.
							if (robustnessTest && maxAncComm < robustnessLimit) break;
						}
					}
				}
			}
		}
		
//		if (debug!=null && log.isTraceEnabled())
//		{
//			log.trace(debug.toString(input, relatedness.getLang()));
//		}
		
	}

}
