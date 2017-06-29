package org.cc.wos.classifier;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.*;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.FeatureCountTool;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

    /**
     * Created by crco0001 on 9/7/2016.
     */
    public class MalletTrainer {

        public static ArrayList<Instance> createunPipedListOfInstances(File trainingWithKeywords) throws IOException {

            File trainingFile = trainingWithKeywords;
            Pattern threeFieldsTabSeparated = Pattern.compile("^(.*?)[\\t](.*?)[\\t](.*?)[\\t]([0-9]*)");
            BufferedReader reader2 = new BufferedReader(new FileReader(trainingFile));
            String line;
            ArrayList<Instance> instanceArrayList = new ArrayList<>(100);
            int onLine = 0;
            while ((line = reader2.readLine()) != null) {

                Matcher m = threeFieldsTabSeparated.matcher(line);

                if (m.find()) {
                    //group 0 is the whole thing..
                    String lable = m.group(1);
                    String data = m.group(2);
                    String keywords = m.group(3);
                    String name = m.group(4);
                    Instance instance = new Instance(data, lable, name, keywords);
                    instanceArrayList.add(instance);


                } else {

                    System.out.println("NO MATCH ON LINE " + onLine);
                    System.out.println("THE LINE: " + line);
                    System.exit(1);
                }

                onLine++;
            }


            return  instanceArrayList;
        }

        public static InstanceList createInstanceList(File trainingWithKeywords, boolean useStopwords) throws IOException {

            ArrayList<Instance> instanceArrayList = createunPipedListOfInstances(trainingWithKeywords);

            ArrayList<Pipe> pipeList = new ArrayList<>();

            pipeList.add(new Input2CharSequence("UTF-8"));
            pipeList.add(new CharSequenceLowercase());

            PatternMatcher removeCopyRightPattern = new PatternMatcher();
            pipeList.add(new RemoveCopyRightStatement(removeCopyRightPattern)); //code adopted from VOSviewer

            Pattern splittPattern = Pattern.compile("[\\p{L}\\p{N}_]{3,15}"); //min length 3, max length 15
            pipeList.add(new CharSequence2TokenSequence(splittPattern)); //ArrayList<Token>
            pipeList.add(new KeywordFromSourceToTokens()); //add keywords from source field
            UEALite ueaLite = new UEALite();
            pipeList.add(new TokenSequenceStemmer(ueaLite)); //using UEA-light english stemmer

            if(useStopwords) {
                MyStopWords stopwords = new MyStopWords(new File("E:\\customStopwordList.txt"),true, "UTF-8");
                pipeList.add(stopwords);
            }
            pipeList.add(new TokenSequence2FeatureSequence()); //int[]
            pipeList.add(new Target2Label());
            //  pipeList.add( new PrintInput());
            pipeList.add(new FeatureSequence2FeatureVector()); // int -> double map (lose order, count duplicates)
            SerialPipes pipeLine = new SerialPipes(pipeList);

            InstanceList instances = new InstanceList(pipeLine);

            for (Instance i : instanceArrayList) instances.addThruPipe(i);

            instanceArrayList = null;
            return instances;
        }

        public static void generateCustomStopwordList(InstanceList instances, double minDocFreq, int maxDocFreq) throws IOException {

            System.out.println("Number of training exemplars : " + instances.size());

            FeatureCountTool featureCountTool = new FeatureCountTool(instances);
            featureCountTool.count();

            Alphabet alphabet = instances.getAlphabet();

            double minDocs = minDocFreq;
            File file = new File("E:\\customStopwordList.txt");
            BufferedWriter writer = new BufferedWriter( new FileWriter(file,false) );
            int maxDocs  = maxDocFreq;
            int[] docFreq = featureCountTool.getDocumentFrequencies();
            //double[] countFreq = featureCountTool.getFeatureCounts();

            System.out.println("Number of features: " + alphabet.size());
            int pruned1 =0;
            int pruned2 =0;
            for(int feature = 0; feature < alphabet.size(); feature++) {

                String stringFeature = alphabet.lookupObject(feature).toString();

                if(docFreq[feature] >= maxDocs) { writer.write(stringFeature); writer.newLine(); pruned1++;}
                if(docFreq[feature] < minDocs){writer.write(stringFeature); writer.newLine(); pruned2++;}


            }

            writer.flush();
            writer.close();
            System.out.println(pruned1 +" features above DocFeq, " + pruned2 + " features below docFreq");


        }



        public static void main(String[] arg) throws IOException {

            ////////Generate a custom stopwordList////////
            System.out.println("Doing a pass over the data to create a custom stopwordList.. ");
            InstanceList temp = createInstanceList(new File("E:\\SubjectCategoreReclassifier\\trainingDataPY2014.txt"),false); // no filtering

            generateCustomStopwordList(temp,2,1500); // minimum docFreq (inclusive) maximum docFreq (equal or above).

            temp = null;

            System.out.println("Custom stopwordlist created");
            //////////////////////////////////////////

            ////read data with custom stopwordlist////////
            System.out.println("Pass 2..");
            InstanceList instances = createInstanceList(new File("E:\\SubjectCategoreReclassifier\\trainingDataPY2014.txt"),true); //filtering from pass 2

            System.out.println("Pass 2 completed, now training..");

            MaxEntTrainer classifierTrainer = new MaxEntTrainer(1.0D); //GAUSSIAN PRIOR (INVERSE OF L2?)
            Classifier classifier = classifierTrainer.train(instances);

            System.out.println("Saving classifier to file");
            SerializeClassifier.saveClassifier(classifier, new File("E:\\Classifier.bin"));
            System.out.println("Done!");

        }
    }


        /*
        /////////////////////////////////MOCK VALIDATION/////////////////////////


        //Classifier classifier = classifierTrainer.train(instances);
        Trial trial = new Trial(classifier, instances);
        for (int i = 0; i < indices.getTargetAlphabet().size(); i++) {

            String target = (String) indices.getTargetAlphabet().lookupObject(i);
            System.out.println(target + " F1: " + trial.getF1(target) + " PRECISION: " + trial.getPrecision(target) + " RECALL: " + trial.getRecall(target));
        }


        //////////////////////////////////MOCK VALIDATION/////////////////////////


        //classify new objects without lable

        // Create an iterator that will pass each instance through
        //  the same pipe that was used to create the training data
        //  for the classifier.
        System.out.println("Running again!");

        InstanceList testInstances = new InstanceList(classifier.getInstancePipe());
        testInstances.addThruPipe(instanceArrayList.get(0));


            Labeling labeling = classifier.classify(testInstances.get(0)).getLabeling();

            // print the labels with their weights in descending order (ie best first)

            for (int rank = 0; rank < labeling.numLocations(); rank++) {
                System.out.print(labeling.getLabelAtRank(rank) + ":" +
                        labeling.getValueAtRank(rank) + " ");
            }
            System.out.println();

    }


}





    ////////////////////////////////////////////////////////////CROSSVALIDATION/////////////////////////////////////////////////////////////////////////

        /*
        System.out.println("Crossvalidation evaluation (5-fold):");

        CrossValidationIterator crossvalidator = new CrossValidationIterator(instances, 2);


        while (crossvalidator.hasNext()) {

            InstanceList[] trainingTesting = crossvalidator.nextSplit();

            System.out.print("Training size: " + trainingTesting[0].size());
            System.out.println(" Testing size: " + trainingTesting[1].size());

           Classifier classifier = classifierTrainer.train(trainingTesting[0]);
           Trial trial = new Trial(classifier,trainingTesting[1]);

            for(int i=0; i<classToIntegerMapping.size(); i++) {

                String target =   (String)classToIntegerMapping.lookupObject(i);
                System.out.println(target +" F1: " + trial.getF1(i) + " PRECISION: " + trial.getPrecision(i) + " RECALL: " + trial.getRecall(i) );
            }


        }

        */

    ////////////////////////////////////////////////////////////CROSSVALIDATION/////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////SIMPLE VALIDATION/////////////////////////////////

        /*
        InstanceList[] instanceSplit = instances.split(new Random(), new double[] {0.9,0.1} );

        System.out.print("Training size: " + instanceSplit[0].size());
        System.out.println(" Testing size: " + instanceSplit[1].size());
        Classifier classifier = classifierTrainer.train(instanceSplit[0]);
        Trial trial = new Trial(classifier,instanceSplit[1]);

        ConfusionMatrix  confusionMatrix = new ConfusionMatrix(trial);

        System.out.println(confusionMatrix);

            //System.out.println(target +" F1: " + trial.getF1(target) + " PRECISION: " + trial.getPrecision(target) + " RECALL: " + trial.getRecall(target) );

    */

    ////////////////////////////////////////////////////////////SIMPLE VALIDATION/////////////////////////////////




/*
    Trial trial = new Trial(classifier,instances);
        Alphabet classToIntegerMapping = instances.getTargetAlphabet();
        System.out.println("Classes: " +  classToIntegerMapping.size() );

        for(int i=0; i<classToIntegerMapping.size(); i++) {

            String target =   (String)classToIntegerMapping.lookupObject(i);
            System.out.println(target +" F1: " + trial.getF1(i) + " PRECISION: " + trial.getPrecision(i) + " RECALL: " + trial.getRecall(i) );
        }



    }


    // Reader reader = new BufferedReader(new FileReader(trainingFile) );
    // CsvIterator csvIterator = new CsvIterator(reader,threeFieldsTabSeparated,2,1,3);  //data target name group


    //instances.addThruPipe(csvIterator);
}

*/






