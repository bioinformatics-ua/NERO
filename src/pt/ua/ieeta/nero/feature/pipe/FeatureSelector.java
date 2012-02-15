/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.ieeta.nero.feature.pipe;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import cc.mallet.util.PropertyList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author david
 */
public class FeatureSelector extends Pipe implements Serializable {

    private List<String> features;

    public FeatureSelector(List<String> features) {
        this.features = features;
    }

    @Override
    public Instance pipe(Instance carrier) {
        TokenSequence data = (TokenSequence) carrier.getData();

        for (int j = 0; j < data.size(); j++) {
            Token t = data.get(j);
            PropertyList pl = t.getFeatures();
            PropertyList.Iterator it = pl.numericIterator();
            
            List<String> newFeatures = new ArrayList<String>();
            
            while (it.hasNext()) {
                it.nextProperty();
                String key = it.getKey();
                if (features.contains(key)) {
                    newFeatures.add(key);
                }
            }
            
            
            Token nt = new Token(t.getText());
            for (String f:newFeatures){
                nt.setFeatureValue(f, 1.0);
            }
            data.set(j, nt);
        }

        return carrier;
    }
}
