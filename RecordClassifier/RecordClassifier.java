/* This file is part of RecordValidator, a progam for learning rules for
   validating reccrds based on a training set. It also includes utilities to
   generate synthetic record data, validate it based on a set of fixed rules,
   generate testing data fom validated record data, and comparing classified
   records to a baseline.

   Copyright (C) 2014   Ezra Erb

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License version 3 as published
   by the Free Software Foundation.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

   I'd appreciate a note if you find this program useful or make updates.
   Please contact me through LinkedIn or github 
*/

/* This class classifies records into valid and invalid states based on a
   training set. It uses the Induction Learning Algorithm developed by Mehmet
   Tolun and Saleh Abu-Soud to derive the classification rules. For details,
   see the paper at http://www.dis.uniroma1.it/~sassano/STAGE/LearningRule.pdf?origin=publication_detail

   By default, this algorithm uses all fields in the record except the
   classification itself for deriving rules. If any field is known a priori to
   not contribute, including it may increase the false positive rate. An
   exclusion list can be specified to eliminate these fields from
   consideration. The final classification rules are output to aid this manual
   tuning */
import java.util.*;

class RecordClassifier
{
    private FieldFilterCollection _rules; // Rules for classifying invalid records

    /* Create the classifier from a training set. The training set must have
       both valid and invalid examples. The more examples of each, the lower
       the false positive rate. The last field states whether a given record is
       valid, by values 'true' or 'false' */
    public RecordClassifier(RecordGroup trainingSet, int[] excludeFields)
    {
        if (trainingSet == null)
            throw new IllegalArgumentException("Training records for classifier passed null");
        else if (trainingSet.size() == 0)
            throw new IllegalArgumentException("Training records for classifier passed empty");

        /* Divide the records into classes. The classification is the last
           field in each record */
        int classificationField = trainingSet.getRecords().get(0).size() - 1;
        Map<String, RecordGroup> recsByCategory = splitByClass(trainingSet,
                                                               classificationField);

        /* The classifier is a field marking validity. Wanted values are
           true and false */
        RecordGroup validRecords = recsByCategory.get(String.valueOf(true));
        RecordGroup invalidRecords = recsByCategory.get(String.valueOf(false));

        if ((validRecords == null) || (invalidRecords == null))
            throw new IllegalArgumentException("Training records for classifier incomplete, need both valid and invalid examples");

        // Add the classification to the list of fields to exclude
        if (excludeFields == null)
            excludeFields = new int[1];
        else {
            int [] newExcludeFields = new int[excludeFields.length + 1];
            System.arraycopy(excludeFields, 0, newExcludeFields, 0,
                             excludeFields.length);
            excludeFields = newExcludeFields;
        }
        excludeFields[excludeFields.length - 1] = classificationField;

        // Initialize two sets of training data, one each for valid and invalid
        TrainingRecords validData = new TrainingRecords(validRecords,
                                                        excludeFields);
        TrainingRecords invalidData = new TrainingRecords(invalidRecords,
                                                          excludeFields);

        /* The ILA algoithm looks for filter conditions that select records in
           only a single category, working from general filters to more
           specific ones. Within a given filter specificity, it orders them by
           the number of training records they filter. In this case, the
           specifity of a filter depends on the number of record fields it
           uses.

           As records are selected by a successful filter, they are removed
           from the training set. When the training set is empty, all filters
           have been found. If a filter becomes specific enough to select all
           fields of a record, and it is not unique, it means that a
           combination of field values has inconstent classification and the
           training data is invalid.
           
           Since this class assumes all records are valid until proven
           otherwise, the invalid records are used to generate the filters */
        _rules = new FieldFilterCollection();
        while ((!invalidData.isEmpty()) &&
               (!invalidData.oneFiltersAllFields())) {
            FieldFilterGroup testFilter = invalidData.getLargestFilter();
            while (testFilter != null) {
                if (!validData.hasFilterGroup(testFilter)) {
                    // Found one!
                    _rules.add(testFilter);
                    testFilter = invalidData.deleteLastFilterGroup();
                }
                else
                    testFilter = invalidData.getNextLargestFilter();
            } // Filters to test
            /* If records remain, need to make the potential filters to test 
               more specific */
            if ((!invalidData.isEmpty()) &&
                (!invalidData.oneFiltersAllFields())) {
                invalidData.incrFilterSpecificity();
                /* Number of fields per filter must be equal in both sets of
                   training data for the search to work */
                validData.incrFilterSpecificity();
            }
        } // Still have training records to process

        /* If get to here without the invalid data being empty, not all
           training records were classified. This implies that at least one
           record has the same field data as a valid record, and the training
           data is invalid */
        if (!invalidData.isEmpty())
            throw new IllegalArgumentException("Training data invalid, valid and invalid record have same field values");
    }

    // Splits training records into classifications
    private static Map<String, RecordGroup> splitByClass(RecordGroup trainingSet, int classifyField)
    {
        /* Extract the classification field and use it to classify the records.
           If its not in any record this will cause an exception, which should
           be allowed to propagate */
        Map<String, RecordGroup> result = new TreeMap<String, RecordGroup>();
        Iterator<ArrayList<String> > index = trainingSet.getRecords().iterator();
        while (index.hasNext()) {
            ArrayList<String> record = index.next();
            String classification = record.get(classifyField);
            if (result.containsKey(classification))
                result.get(classification).add(record);
            else
                result.put(classification, new RecordGroup(record));
        } // While records to process
        return result;
    }

    /* Classifies records based on the derived rules. The validity is added
       to every record as a new last field */
    public void classifyRecords(RecordGroup records)
    {
        if (records != null) {
            Iterator<ArrayList<String> > index = records.getRecords().iterator();
            while (index.hasNext()) {
                /* This is a copy of the pointer to the actual record.
                   Modifying it will modify the record in the RecordGroup,
                   which is wanted in this case */
                ArrayList<String> record = index.next();

                /* Records are classified by determining their validity, and
                   adding the value to the record as the last field. Remember
                   that the rules state when a record fails, so the status
                   needs to be flipped */
                record.add(String.valueOf(!_rules.passes(record)));
            } // Records to process
        } // Records were passed
    }

    // Converts the classification rules to a multi-line string
    public String toString()
    {
        return _rules.toString();
    }
    
    /* Helper test method to take an existing record and append the given
       validity */
    private static void setValidity(ArrayList<String> record, boolean validity)
    {
        if (record == null)
            throw new IllegalArgumentException("Test record passed null");
        record.add(String.valueOf(validity));
    }

    /* Helper test method to create a record with two fields of the specified
       values */
    private static ArrayList<String> makeTestRecord(String field1, String field2)
    {
        ArrayList<String> result = new ArrayList<String>();
        result.add("Tracking field");
        result.add(field1);
        result.add(field2);
        return result;
    }

    /* Helper test method to create a record with two fields of the specified
       values, and the specified validity */
    private static ArrayList<String> makeTestRecord(String field1, String field2,
                                                    boolean validity)
    {
        ArrayList<String> result = makeTestRecord(field1, field2);
        setValidity(result, validity);
        return result;
    }

    /* Helper test method to create a record with three fields of the specified
       values */
    private static ArrayList<String> makeTestRecord(String field1, String field2,
                                                    String field3)
    {
        ArrayList<String> result = makeTestRecord(field1, field2);
        result.add(field3);
        return result;
    }

    /* Helper test method to create a record with three fields of the specified
       values, and the specified validity */
    private static ArrayList<String> makeTestRecord(String field1, String field2,
                                                    String field3,
                                                    boolean validity)
    {
        ArrayList<String> result = makeTestRecord(field1, field2, field3);
        setValidity(result, validity);
        return result;
    }

    /* Helper test method to determine whether the expected record in a
       record group has the wanted validity */
    private static boolean testValidity(RecordGroup records, int recordCount,
                                        boolean expectValidity)
    {
        if (records == null)
            return false;
        else if ((recordCount < 0) || (recordCount >= records.size()))
            return false;
        else {
            ArrayList<String> testRecord = records.getRecords().get(recordCount);
            // Validity is always the last field
            return testRecord.get(testRecord.size() - 1).equals(String.valueOf(expectValidity));
        }
    }

    // Self test method
    private static void selfTest()
    {
        /* Simple test: Two records, one valid and one invalid. They share a
           value for one field, so the other determines validity */
        RecordGroup trainingData1 = new RecordGroup(makeTestRecord("value1",
                                                                   "value2",
                                                                   true));
        trainingData1.add(makeTestRecord("value1", "value3", false));

        RecordGroup resultData1 = new RecordGroup(makeTestRecord("value1",
                                                                 "value4"));
        resultData1.add(makeTestRecord("value5", "value3"));
        System.out.println("Simple classification test, only one field needed");
        try {
            RecordClassifier test1 = new RecordClassifier(trainingData1, null);
            System.out.println("Classifier: " + test1);
            test1.classifyRecords(resultData1);
            // Expected result is that first record passes, second fails
            if (testValidity(resultData1, 0, true) &&
                testValidity(resultData1, 1, false))
                System.out.println("Simple classification produced expected results");
            else {
                System.out.println(resultData1);
                System.out.println("Simple classification failed, invalid results");
            }
        }
        catch (Exception e) {
            System.out.println("Simple classification failed. Caught exception " + e);
        }

        /* Test with only invalid records. This is an error and should cause an
           exception */
        RecordGroup trainingData2 = new RecordGroup(makeTestRecord("value1",
                                                                   "value3",
                                                                   false));
        trainingData2.add(makeTestRecord("value5", "value6", false));
        System.out.println("Classifier creation with only invalid records, expect exception");
        try {
            RecordClassifier test2 = new RecordClassifier(trainingData2, null);
            System.out.println("Classifier: " + test2);
            System.out.println("Test failed, expected exception");
        }
        catch (Exception e) {
            System.out.println("Test passed, caught exception: " + e);
        }


        /* Complex test. This one must reach multiple fields to determine
           validity
           The validity rules are [0->test3] and [1->test3][2->test5] make the
           record invalid. To avoid a single field rule for the latter, need
           records with [1->test3] and [2->test5] which are valid and have the
           same first field value as an invalid record. Also need the
           second and third fields of the [0->test3] record to appear in valid
           records */
        RecordGroup trainingData3 = new RecordGroup(makeTestRecord("test1",
                                                                   "test3",
                                                                   "test6",
                                                                   true));
        trainingData3.add(makeTestRecord("test1", "test3", "test5", false));
        trainingData3.add(makeTestRecord("test3", "test4", "test6", false));
        trainingData3.add(makeTestRecord("test1", "test4", "test5", true));

        RecordGroup resultData2 = new RecordGroup(makeTestRecord("test2",
                                                                 "test3",
                                                                 "test5"));
        resultData2.add(makeTestRecord("test1", "test4", "test6"));
        resultData2.add(makeTestRecord("test3", "test2", "test1"));

        System.out.println("Complex classification test, both single and multiple record filters needed");
        try {
            RecordClassifier test3 = new RecordClassifier(trainingData3, null);
            System.out.println("Classifier: " + test3);
            test3.classifyRecords(resultData2);
            System.out.println("Results: " + resultData2);
            // Expected result is that second record passes, other two fail
            if (testValidity(resultData2, 0, false) &&
                testValidity(resultData2, 1, true) &&
                testValidity(resultData2, 2, false))
                System.out.println("Complex classification produced expected results");
            else {
                System.out.println("Complex classification failed, invalid results");
            }
        }
        catch (Exception e) {
            System.out.println("Complex classification failed. Caught exception " + e);
        }

        /* Test with two records with the exact same field values but different
           validity. Mix in other records that can be classified. Expect the
           test to fail */
        RecordGroup trainingData4 = new RecordGroup(makeTestRecord("value1",
                                                                   "value3",
                                                                   "value5",
                                                                   false));
        trainingData4.add(makeTestRecord("value1", "value6", "value5", false));
        trainingData4.add(makeTestRecord("value1", "value6", "value6", true));
        trainingData4.add(makeTestRecord("value1", "value3", "value5", true));

        System.out.println("Classifier creation with matching records but different validity, expect exception");
        try {
            RecordClassifier test4 = new RecordClassifier(trainingData4, null);
            System.out.println("Classifier: " + test4);
            System.out.println("Test failed, expected exception");
        }
        catch (Exception e) {
            System.out.println("Test passed, caught exception: " + e);
        }
    }

    // Driver for the classifier
    public static void main(String[] args)
    {
        // If the only argument is 'selftest', run the self test and quit
        if ((args.length == 1) && args[0].equals("selftest"))
            selfTest();
        else if ((args.length < 3) || (args.length > 4)) {
            System.out.println("Use: [training records file] [records to classify file] [results file] [optional fields to ignore for classification, comma seperated]");
            System.exit(1);
        }
        else {
            try {
                /* Extract the classification fields from the last argument.
                   They are comma seperated without spaces */
                int[] ignoreFields = null;
                if (args.length > 3) {
                    String[] ignoreFieldsStr = args[3].split(",");
                    ignoreFields = new int[ignoreFieldsStr.length];
                    int index;
                    for (index = 0; index < ignoreFieldsStr.length; index++)
                        ignoreFields[index] = Integer.parseInt(ignoreFieldsStr[index]);
                }
                RecordGroup trainingRecords = RecordParser.readRecords(args[0]);
                RecordClassifier classifier = new RecordClassifier(trainingRecords,
                                                                   ignoreFields);
                System.out.println("Rules for classifying invalid records:");
                System.out.println(classifier);
                RecordGroup procRecords = RecordParser.readRecords(args[1]);
                classifier.classifyRecords(procRecords);
                RecordParser.outputRecords(args[2], procRecords);
            }
            catch (Exception e) {
                System.out.println("Processing failed with exception: " + e);
                e.printStackTrace();
            }
        } // Arguments are correct
    }
}
