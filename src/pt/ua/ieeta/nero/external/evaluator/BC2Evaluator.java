/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.external.evaluator;

import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author david
 */
public class BC2Evaluator {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(BC2Evaluator.class);
    private String[] command;

    public BC2Evaluator(String gene, String altgene, String annotations) {
        command = new String[]{"perl",
            "resources/evaluation/bc2gm/alt_eval.perl",
            "-gene",
            gene,
            "-altgene",
            altgene,
            annotations};
    }

    public Performance getPerformance() {
        Performance performance = null;
        try {
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String[] parts;
            double precision, recall, f1;

            while ((line = br.readLine()) != null) {
                if (line.contains("Precision: ") && line.contains("Recall: ") && line.contains("F: ")) {
                    parts = line.split(" ");
                    precision = Double.parseDouble(parts[1]);
                    recall = Double.parseDouble(parts[3]);
                    f1 = Double.parseDouble(parts[5]);

                    performance = new Performance(precision, recall, f1);
                    
//                    logger.info("LINE: {}", line);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("There was a problem parsing the performance result. Check if you have Perl installed.", ex);
        }

        if (performance == null) {
            throw new RuntimeException("There is a problem with the provided annotations file. It is not possible to collect a valid performance result.");
        }

        return performance;
    }

    public static void main(String[] args) {

        String gene = "resources/corpus/bc2gm/dev/1k";
        String altgene = "resources/corpus/bc2gm/dev/1k";
        String annotations = "resources/corpus/bc2gm/dev/2k";

        BC2Evaluator eval = new BC2Evaluator(gene, altgene, annotations);
        Performance p = eval.getPerformance();
        
        logger.info("{}", p);
    }
}
