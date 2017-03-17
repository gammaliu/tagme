import java.io.IOException;

import it.acubelab.tagme.RelatednessMeasure;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.config.Config.IndexType;
import it.acubelab.tagme.preprocessing.DatasetLoader;
import it.acubelab.tagme.preprocessing.TopicSearcher;
import it.acubelab.tagme.preprocessing.graphs.InGraphArray;
import it.acubelab.tagme.preprocessing.graphs.WikiGraphs;
import it.unimi.dsi.webgraph.ImmutableGraph;

public class Example1 {

	public static void main(String[] args) throws IOException {

		TagmeConfig.init();
		String lang = "en";

		RelatednessMeasure rel = RelatednessMeasure.create(lang);

		TopicSearcher searcher = new TopicSearcher(lang);

		int wid1 = 8485; // Wikipedia ID of the page "Diego Maradona"
		int wid2 = 808402; // Wikipedia ID of the page
							// "Mexico national football team"
		System.out.println("\n\nThe relatedness between "
				+ searcher.getTitle(wid1) + " and " + searcher.getTitle(wid2)
				+ " is " + rel.rel(wid1, wid2));

		int[][] in_graph = DatasetLoader.get(new InGraphArray(lang));

		System.out.println("\n\nThe number of pages in Wikipedia is "
				+ in_graph.length);
		System.out.println("The number of pages linking to "
				+ searcher.getTitle(8485) + " is " + in_graph[8485].length);

		int count = 0;
		for (int i = 0; i < in_graph.length; i++)
			if (searcher.getTitle(i) != null) {
				count++;
				// System.out.println(searcher.getTitle(i));
			}
		System.out.println("The number of actual unique pages in Wikipedia is "
				+ count);

		ImmutableGraph in_webgraph = WikiGraphs.get(lang, IndexType.IN_GRAPH);
		System.out.println("\n\nThe number of pages in Wikipedia is "
				+ in_webgraph.numNodes());
		System.out.println("The number of pages linking to "
				+ searcher.getTitle(wid1) + " is "
				+ in_webgraph.outdegree(wid1));

		ImmutableGraph webgraph = WikiGraphs.get(lang, IndexType.GRAPH);
		System.out.println("The number of pages in Wikipedia is "
				+ webgraph.numNodes());
		System.out.println("The number of pages linked by "
				+ searcher.getTitle(wid1) + " is " + webgraph.outdegree(wid1));

		System.out.println("\n\n\nThe pages linking to "
				+ searcher.getTitle(wid1) + " are the following:");
		int[] linkingTo = in_graph[wid1];
		for (int wid : linkingTo) {
			System.out.println("Page: " + searcher.getTitle(wid) + "\trel: "
					+ rel.rel(wid1, wid));
		}

	}

}
