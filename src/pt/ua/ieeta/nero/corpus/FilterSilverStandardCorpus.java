/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.corpus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.lang.StringEscapeUtils;
import pt.ua.tm.gimli.external.gdep.GDepParser;

/**
 *
 * @author david
 */
public class FilterSilverStandardCorpus {

    private static String XML_SENTENCE = "s";
    private static String XML_ANNOTATION = "e";
    private static int numSentences;
    private static int MAX_SENTENCES = 100000;

    private static void readFile(File file, ArrayList<String> sentences) throws IOException, XMLStreamException {
        GZIPInputStream f = new GZIPInputStream(new FileInputStream(file));

        // First create a new XMLInputFactory
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();

        // Setup a new eventReader
        XMLEventReader eventReader = inputFactory.createXMLEventReader(f);

        // Helpers
        boolean inSentence = false;
        boolean hasAnnotation = false;
        int count = 0;
        StringBuilder sb = new StringBuilder();
        String s;

        // Read the XML document
        while (eventReader.hasNext()) {
            if (numSentences > MAX_SENTENCES) {
                break;
            }

            XMLEvent event = eventReader.nextEvent();
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();

                if (startElement.getName().getLocalPart() == XML_SENTENCE) {
                    sb = new StringBuilder();
                    inSentence = true;
                    hasAnnotation = false;
                    count = 0;
                }


                if (startElement.getName().getLocalPart() == XML_ANNOTATION) {
                    hasAnnotation = true;
                    event = eventReader.nextEvent();
                    count++;
                }
            }

            if (inSentence) {
                if (event.isCharacters()) {
                    sb.append(event.asCharacters().getData());
                }
            }

            if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                if (endElement.getName().getLocalPart() == XML_SENTENCE) {
                    inSentence = false;
                    if (hasAnnotation && count >= 2) {
                    //if (!hasAnnotation) {
                        s = sb.toString().trim();
                        s = StringEscapeUtils.unescapeXml(s);
                        numSentences++;
                        // Customize tokenisation
                        //s = s.replaceAll("/", " / ");
                        //s = s.replaceAll("-", " - ");
                        //s = s.replaceAll("[.]", " . ");
                        //s = s.replaceAll("//s+", " ");

                        sentences.add(s);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            ArrayList<String> sentences = new ArrayList<String>();

            numSentences = 0;
            File inputDir = new File("/Users/david/Downloads/calbc/fixed/");
            File[] files = inputDir.listFiles();
            for (File f : files) {
                System.out.println(f.getName());
                readFile(f, sentences);
            }

            GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream("resources/corpus/silver/2_100k.gz"));

            //String[] tokens;
            //Tokenizer tokenizer = new Tokenizer(false);
            
            GDepParser parser = new GDepParser(true, false);
            parser.launch();
            
            List<Object> result;
            String[] parts;
            String token_parsing, token, lemma, pos, chunk;
            //tokenizer.launch();

            for (String sentence : sentences) {
                //sentence = sentence.replaceAll("/", " / ");
                //sentence = sentence.replaceAll("-", " - ");
                //sentence = sentence.replaceAll("[.]", " . ");
                //sentence = sentence.replaceAll("//s+", " ");

                //tokens = tokenizer.tokenize(sentence);
                
                result = parser.parse(sentence);
                
                for (Object o:result){
                    token_parsing = (String) o;
                    parts = token_parsing.split("\t");
                    
                    token = parts[1];
                    lemma = parts[2];
                    pos = parts[3];
                    chunk = parts[4];
                    
                    out.write(token.getBytes());
                    out.write("\t".getBytes());
                    out.write("LEMMA=".getBytes());
                    out.write(lemma.getBytes());
                    out.write("\t".getBytes());
                    out.write("POS=".getBytes());
                    out.write(pos.getBytes());
                    out.write("\t".getBytes());
                    out.write("CHUNK=".getBytes());
                    out.write(chunk.getBytes());
                    out.write("\t".getBytes());
                    out.write("O".getBytes());
                    out.write("\n".getBytes());
                }
                

                /*for (String t : tokens) {
                    out.write(t.getBytes());
                    out.write("\t".getBytes());
                    out.write("O".getBytes());
                    out.write("\n".getBytes());
                }*/
                out.write("\n".getBytes());
            }
            out.close();
            parser.terminate();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }
}
