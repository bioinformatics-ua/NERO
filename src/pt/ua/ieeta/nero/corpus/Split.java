package pt.ua.ieeta.nero.corpus;

/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants.EntityType;
import pt.ua.tm.gimli.config.Constants.LabelFormat;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.util.FileUtil;
import pt.ua.tm.gimli.writer.BCWriter;
import pt.ua.tm.gimli.writer.JNLPBAWriter;

/**
 *
 * @author david
 */
public class Split {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(Split.class);

    private static void splitBC2() {
        try {
            Corpus c = new Corpus(LabelFormat.BIO, EntityType.protein, FileUtil.getFile(new FileInputStream("../gimli/resources/corpus/gold/bc2gm/train/corpus_ndp.gz")));

            // Get Sizes
            int total = c.size();
            int trainSize = (int) (total * (2.0 / 3.0));
            int devSize = total - trainSize;

            trainSize = 500;
            
            logger.info("TRAIN SIZE: {}", trainSize);
            logger.info("DEV SIZE: {}", devSize);
            
            // Get Sentences
            ArrayList<Sentence> sentences = c.getSentences();

            // Random sort
            Collections.shuffle(sentences);

            // Get parts
            Corpus train = new Corpus(LabelFormat.BIO, EntityType.protein);
            Corpus dev = new Corpus(LabelFormat.BIO, EntityType.protein);

            int count = 0;
            for (Sentence s : sentences) {
                count++;

                if (count <= trainSize) {
                    train.addSentence(s.clone(train));
                } else {
                    dev.addSentence(s.clone(dev));
                }
            }
            logger.info("TRAIN: {}", train.size());
            logger.info("DEV: {}", dev.size());

            // Write corpus and annotations
            // Train
            train.write( new GZIPOutputStream(new FileOutputStream("resources/corpus/bc2gm/train/500.gz")));
            BCWriter bc = new BCWriter();
            //bc.write(train, new FileOutputStream("resources/corpus/bc2gm/train/annotations"));

            // Test
            //dev.write(new GZIPOutputStream(new FileOutputStream("resources/corpus/bc2gm/dev/2k.gz")));
            //bc.write(dev, new FileOutputStream("resources/corpus/bc2gm/dev/2k"));

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (GimliException ex) {
            ex.printStackTrace();
        }
    }

    /*private static void splitJNLPBA() {
        try {
            Corpus c = new Corpus(LabelFormat.BIO, EntityType.protein, "resources/corpus/gold/jnlpba/train/corpus_protein.gz");

            // get list of abstracts
            ArrayList<String> abstracts = new ArrayList<String>();
            String id;
            for (Sentence s : c.getSentences()) {
                id = s.getId();
                if (!abstracts.contains(id)) {
                    abstracts.add(id);
                }
            }

            //Get number of abstracts for each
            int total = abstracts.size();
            logger.info("ABSTRACTS: {}", total);
            int sizeTrain = (int) (total * 0.8);

            // Shuffle abstracts
            Collections.shuffle(abstracts);

            // Split abstracts IDs
            ArrayList<String> abstractsTrain = new ArrayList<String>();
            ArrayList<String> abstractsDev = new ArrayList<String>();

            for (int i = 0; i < abstracts.size(); i++) {
                if (i <= sizeTrain) {
                    abstractsTrain.add(abstracts.get(i));
                } else {
                    abstractsDev.add(abstracts.get(i));
                }
            }

            // Write UIDs files
            FileOutputStream fosTrain = new FileOutputStream("resources/corpus/gold/jnlpba/train/uids_train");
            for (String s : abstractsTrain) {
                s = s.substring(s.indexOf(":") + 1);
                fosTrain.write(s.getBytes());
                fosTrain.write("\n".getBytes());
            }
            fosTrain.close();

            FileOutputStream fosDev = new FileOutputStream("resources/corpus/gold/jnlpba/train/uids_dev");
            for (String s : abstractsDev) {
                s = s.substring(s.indexOf(":") + 1);
                fosDev.write(s.getBytes());
                fosDev.write("\n".getBytes());
            }
            fosDev.close();


            JNLPBAWriter w = new JNLPBAWriter();

            // Protein
            Corpus train_protein = new Corpus(LabelFormat.BIO, EntityType.protein);
            Corpus dev_protein = new Corpus(LabelFormat.BIO, EntityType.protein);

            for (Sentence s : c.getSentences()) {
                id = s.getId();
                if (abstractsTrain.contains(id)) {
                    train_protein.addSentence(s.clone(train_protein));
                } else {
                    dev_protein.addSentence(s.clone(dev_protein));
                }
            }
            train_protein.writeToFile("resources/corpus/gold/jnlpba/train/corpus_train_protein.gz");
            dev_protein.writeToFile("resources/corpus/gold/jnlpba/train/corpus_dev_protein.gz");

            // DNA
            c = new Corpus(LabelFormat.BIO, EntityType.DNA, "resources/corpus/gold/jnlpba/train/corpus_DNA.gz");
            Corpus train_dna = new Corpus(LabelFormat.BIO, EntityType.DNA);
            Corpus dev_dna = new Corpus(LabelFormat.BIO, EntityType.DNA);

            for (Sentence s : c.getSentences()) {
                id = s.getId();
                if (abstractsTrain.contains(id)) {
                    train_dna.addSentence(s.clone(train_dna));
                } else {
                    dev_dna.addSentence(s.clone(dev_dna));
                }
            }
            train_dna.writeToFile("resources/corpus/gold/jnlpba/train/corpus_train_dna.gz");
            dev_dna.writeToFile("resources/corpus/gold/jnlpba/train/corpus_dev_dna.gz");

            // RNA
            c = new Corpus(LabelFormat.BIO, EntityType.RNA, "resources/corpus/gold/jnlpba/train/corpus_RNA.gz");
            Corpus train_rna = new Corpus(LabelFormat.BIO, EntityType.RNA);
            Corpus dev_rna = new Corpus(LabelFormat.BIO, EntityType.RNA);

            for (Sentence s : c.getSentences()) {
                id = s.getId();
                if (abstractsTrain.contains(id)) {
                    train_rna.addSentence(s.clone(train_rna));
                } else {
                    dev_rna.addSentence(s.clone(dev_rna));
                }
            }
            train_rna.writeToFile("resources/corpus/gold/jnlpba/train/corpus_train_rna.gz");
            dev_rna.writeToFile("resources/corpus/gold/jnlpba/train/corpus_dev_rna.gz");

            // Cell type
            c = new Corpus(LabelFormat.BIO, EntityType.cell_type, "resources/corpus/gold/jnlpba/train/corpus_cell_type.gz");
            Corpus train_cell_type = new Corpus(LabelFormat.BIO, EntityType.cell_type);
            Corpus dev_cell_type = new Corpus(LabelFormat.BIO, EntityType.cell_type);

            for (Sentence s : c.getSentences()) {
                id = s.getId();
                if (abstractsTrain.contains(id)) {
                    train_cell_type.addSentence(s.clone(train_cell_type));
                } else {
                    dev_cell_type.addSentence(s.clone(dev_cell_type));
                }
            }
            train_cell_type.writeToFile("resources/corpus/gold/jnlpba/train/corpus_train_cell_type.gz");
            dev_cell_type.writeToFile("resources/corpus/gold/jnlpba/train/corpus_dev_cell_type.gz");

            // Cell line
            c = new Corpus(LabelFormat.BIO, EntityType.cell_line, "resources/corpus/gold/jnlpba/train/corpus_cell_line.gz");
            Corpus train_cell_line = new Corpus(LabelFormat.BIO, EntityType.cell_line);
            Corpus dev_cell_line = new Corpus(LabelFormat.BIO, EntityType.cell_line);

            for (Sentence s : c.getSentences()) {
                id = s.getId();
                if (abstractsTrain.contains(id)) {
                    train_cell_line.addSentence(s.clone(train_cell_line));
                } else {
                    dev_cell_line.addSentence(s.clone(dev_cell_line));
                }
            }
            train_cell_line.writeToFile("resources/corpus/gold/jnlpba/train/corpus_train_cell_line.gz");
            dev_cell_line.writeToFile("resources/corpus/gold/jnlpba/train/corpus_dev_cell_line.gz");

            // Write
            w.write(new Corpus[]{train_protein, train_dna, train_rna, train_cell_type, train_cell_line}, "resources/corpus/gold/jnlpba/train/corpus_train");
            w.write(new Corpus[]{dev_protein, dev_dna, dev_rna, dev_cell_type, dev_cell_line}, "resources/corpus/gold/jnlpba/train/corpus_dev");

            logger.info("ABSTRACTS TRAIN: {}", abstractsTrain.size());
            logger.info("ABSTRACTS DEV: {}", abstractsDev.size());
            logger.info("SENTENCES TRAIN: {}", train_protein.size());
            logger.info("SENTENCES DEV: {}", dev_protein.size());

        } catch (Exception ex) {
            logger.error("Problem.", ex);
        }
    }*/

    public static void main(String[] args) {
        splitBC2();
    }
}
