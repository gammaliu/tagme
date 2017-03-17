import java.io.IOException;
import java.util.List;

import it.acubelab.tagme.AnnotatedText;
import it.acubelab.tagme.Annotation;
import it.acubelab.tagme.Disambiguator;
import it.acubelab.tagme.RelatednessMeasure;
import it.acubelab.tagme.RhoMeasure;
import it.acubelab.tagme.Segmentation;
import it.acubelab.tagme.TagmeParser;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.config.Config.IndexType;
import it.acubelab.tagme.preprocessing.DatasetLoader;
import it.acubelab.tagme.preprocessing.TopicSearcher;
import it.acubelab.tagme.preprocessing.graphs.InGraphArray;
import it.acubelab.tagme.preprocessing.graphs.WikiGraphs;
import it.unimi.dsi.webgraph.ImmutableGraph;

public class Example2 {

	public static void main(String[] args) throws IOException {

		TagmeConfig.init();
		String test = "Nelson Mandela was the father of the current democratic South Africa that replaced the odious apartheid state.";
		String lang = "en";

		AnnotatedText ann_text = new AnnotatedText(test);

		RelatednessMeasure rel = RelatednessMeasure.create(lang);

		TagmeParser parser = new TagmeParser(lang, true);
		Disambiguator disamb = new Disambiguator(lang);
		Segmentation segmentation = new Segmentation();
		RhoMeasure rho = new RhoMeasure();

		parser.parse(ann_text);
		segmentation.segment(ann_text);
		disamb.disambiguate(ann_text, rel);
		rho.calc(ann_text, rel);

		List<Annotation> annots = ann_text.getAnnotations();
		TopicSearcher searcher = new TopicSearcher(lang);

		for (Annotation a : annots) {
			if (a.isDisambiguated() && a.getRho() >= 0.1) {
				System.out.println("wid: " + a.getTopic() + "\nspot: "
						+ ann_text.getOriginalText(a) + "\nrho: " + a.getRho());
				System.out.println("Wikipedia Page: "
						+ searcher.getTitle(a.getTopic()));
			}
		}

	}

}
