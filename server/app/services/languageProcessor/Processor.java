package services.languageProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.lang.reflect.*;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;

public class Processor {
	

	public static String processQuestion(String text) throws IOException,
			ClassNotFoundException, NoSuchMethodException,
			InvocationTargetException,IllegalAccessException {
		
		DecisionTree dt = new DecisionTree();

			// Create the Stanford CoreNLP pipeline
			Properties props = PropertiesUtils.asProperties("annotators", "tokenize,ssplit,pos");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

			// Annotate an example statement.
			//String text = "Who is working on POET-1?";
			//String text = "What is TKT-123 about?";
			//String text = "What is POET-1 for?";
			//String text = "Who is on POET-1?";
			//String text = "Person working on POET-1";
			System.out.println("Question:");
			System.out.println(text);
			Annotation ann = new Annotation(text);
			pipeline.annotate(ann);

			//POS tagging
			//ArrayList<ArrayList<String>> posTagging = posTagging(ann);
			System.out.println("\nPOS tagging:");
			HashMap<String, String> posTagging = posTagging_(ann);

			//Noun extraction
			//For now we assume that only one noun is present
			String keyword = infoExtraction(posTagging, "NN").get(0);
			//System.out.println(keyword);
        
        
        /*
         * check for Wh-pronoun (WP) - Who, what, 
         * then for Wh-adverb (WRB)  - when/ where,
         * and then Wh-determiner (WDT)  - which
         */
			String ques = null;
			if (infoExtraction(posTagging, "WP").size() != 0)
				ques = infoExtraction(posTagging, "WP").get(0).toLowerCase();
			else if (infoExtraction(posTagging, "WRB").size() != 0)
				ques = infoExtraction(posTagging, "WRB").get(0).toLowerCase();
			else if (infoExtraction(posTagging, "WDT").size() != 0)
				ques = infoExtraction(posTagging, "WDT").get(0).toLowerCase();


			//Create an arrayList for keywords
			ArrayList<String> keywords_found = new ArrayList<String>();
			//Analysis of the question
			System.out.println("\nAnalysis of the question:");
			String topic = QuestionTopicMapping(keyword).toLowerCase();
			String keyy = QuestionTypeMapping(ques).toLowerCase();

			keywords_found.add(ques);
			keywords_found.add(topic);
			keywords_found.add(keyy);
			keywords_found.add(keyword);

			System.out.println("\nKeywords found:");
			System.out.println(keywords_found.toString());

			keywords_found = QuestionMapping(keywords_found);

			System.out.println("\nFinal keyword list:");
			System.out.println(keywords_found.toString());

			System.out.println("\nQuestion mapping: ");
			System.out.println(dt.traverse(keywords_found) + "(" + keyword + ")");
			return TaskMap.questionMapping(dt.traverse(keywords_found), keyword);


	}
	


	private static ArrayList<String> QuestionMapping(ArrayList<String> keywords) {
		ArrayList<String> c = new ArrayList<>();
		c.add("person");
		c.add("ticket");
		if (keywords.containsAll(c))
			keywords.add("assignee");

		return keywords;
	}


	
	/*
	 * This method extracts words with certain tag
	 * For e.g words with tag NOUN or words with tag VERB
	 * */
	public static ArrayList<String> infoExtraction(HashMap<String, String> posTagging, String tag){
	
		ArrayList<String> list = new ArrayList<String>();
		
		if (posTagging.containsKey(tag)){
			list.add(posTagging.get(tag));
        	//System.out.println(posTagging.get(tag));
        }
		return list;
	}
	
	/*
	 * This method performs POS tagging.
	 * It takes annotation as an argument and returns ArrayList<String[]>
	 * */
	public static ArrayList<ArrayList<String>> posTagging(Annotation ann){
		
		ArrayList<ArrayList<String>> posTagging = new ArrayList<ArrayList<String>>();
        
		List<CoreMap> sentences = ann.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
		    for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
		        String word = token.get(CoreAnnotations.TextAnnotation.class);
		        // this is the POS tag of the token
		        String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
		        String[] POSS = new String[2];
		        POSS[0] = word;
		        POSS[1] = pos;
		        
		        ArrayList<String> arr = new ArrayList<String>();
		        arr.add(word);
		        posTagging.add(arr);
		        
		        //System.out.println(word + "/" + pos);
		    }
		}
		
		//Print POS tag along with the words
		for (ArrayList<String> e: posTagging)
			System.out.println(e.get(0) + " -> " + e.get(1));
		
		return posTagging;
	}
	
	/*
	 * This method performs POS tagging.
	 * It takes annotation as an argument and returns ArrayList<String[]>
	 * */
	public static HashMap<String, String> posTagging_(Annotation ann){
		
		HashMap<String, String> posTagging = new HashMap<String, String>();
        
		List<CoreMap> sentences = ann.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
		    for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
		        String word = token.get(CoreAnnotations.TextAnnotation.class);
		        // this is the POS tag of the token
		        String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);

		        
		        //Problem here is tat when another noun is found, it replaces the old value
		        posTagging.put(pos, word);
		        //posTagging.merge(pos, word, String::concat); //find a merging function
		        
		        //System.out.println(word + "/" + pos);
		    }
		}
		
		//Print POS tag along with the words
		Iterator it = posTagging.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " -> " + pair.getValue());
	        //it.remove(); // avoids a ConcurrentModificationException
	    }
		
		return posTagging;
	}
	
	
	/*
	 * 
	 */
	public static String QuestionTopicMapping(String keyword){
		System.out.println("This question is about "+ keyword);
		String topic = "ticket";
		return topic;
	}
	
	public static String QuestionTypeMapping(String keyword){
		//System.out.println(keyword);
		String keyy = null;
		switch (keyword){
			case "who": keyy = "person"; System.out.println("Wants a name. So get a name associated with the topic."); break;
			case "what": keyy = "description"; System.out.println("Wants a description of the topic."); break;
			case "when": keyy = "date/time"; System.out.println("Wants a date/time related to the topic."); break;
			default: break;
		}
		return keyy;	
	}
}