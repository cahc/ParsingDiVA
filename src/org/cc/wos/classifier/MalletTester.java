package org.cc.wos.classifier;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Labeling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by crco0001 on 9/9/2016.
 */
public class MalletTester {

    public static void main(String[] arg) throws IOException, ClassNotFoundException {


        Classifier classifier = SerializeClassifier.loadClassifier("E:\\SubjectCategoreReclassifier\\Classifier.bin");

        ArrayList<Instance> unpipedInstances = MalletTrainer.createunPipedListOfInstances(new File("E:\\SubjectCategoreReclassifier\\unSeenData.txt") );

        // System.out.println((String) unpipedInstances.get(0).getData());

        //put the an instance trough the same pipe
        Pipe pipe = classifier.getInstancePipe();

        for (int i = 7379; i < 7396; i++) {


            Instance instance = pipe.instanceFrom(unpipedInstances.get(i));

            //   Alphabet completeAlphabet = instance.getDataAlphabet();
            //    SparseVector sparseVector = ((SparseVector) instance.getData());

            //   for (int j = 0; j < sparseVector.getIndices().length; j++) {

            //         System.out.println(completeAlphabet.lookupObject(sparseVector.indexAtLocation(j)));
            //       }


            Classification classification = classifier.classify(instance);

            Labeling labeling = classification.getLabeling();

            for (int rank = 0; rank < labeling.numLocations(); rank++) {
                System.out.print(labeling.getLabelAtRank(rank) + ":" + labeling.getValueAtRank(rank) + " ");
            }

            System.out.println();

        }

    }
}
