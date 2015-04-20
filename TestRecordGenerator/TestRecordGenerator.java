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

/* This class generates random record data and validates it using hard-coded
   rules. They were chosen over allowing configuation because this was
   adequate for the purpose of testing validation learning algorithms */
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;

class TestRecordGenerator
{
    /* Generate the wanted number of records and insert them into the passed
       file */
    public static PrintWriter createRecords(TestRecordCreator creator,
                                            PrintWriter outputFile,
                                            int count, boolean debug)
    {
        ArrayList<ArrayList<String> > results = creator.generateRecords(count,
                                                                        debug);
        int recIndex, fieldIndex;
        for (recIndex = 0; recIndex < results.size(); recIndex++) {
            for (fieldIndex = 0; fieldIndex < results.get(recIndex).size();
                 fieldIndex++) {
                if (fieldIndex > 0)
                    outputFile.print(",");
                outputFile.print(results.get(recIndex).get(fieldIndex));
            }
            outputFile.println(); // Force a newline
        }
        return outputFile;
    }

    /* Code to generate records and output them. The arguments are the number of
       records to generate, the output file, and an optional flag to debug trace
       the record validation process. Rows will be output as csv */
    public static void main(String[] args)
    {
        PrintWriter outputFile = null;
        if ((args.length != 2) && (args.length != 3))
            System.out.println("Usage [number of records] [output file] [debug flag]");
        else {
            int recordCount = 0;
            try {
                recordCount = Integer.parseInt(args[0]);
            }
            catch (RuntimeException e) {
                System.out.println("Caught " + e + " reading record count; must be an integer");
                System.exit(1);
            }
            if (recordCount < 1) {
                System.out.println("Number of bills to generate must be positive. Specified " + recordCount);
                System.exit(1);
            }
            // Anything in the third argument will produce the debug trace
            boolean debug = (args.length == 3);
            try {
                // Open file. Overwrite if already exists
                outputFile = new PrintWriter(new FileOutputStream(args[1]));
                /* Construct the rules for generating and validating the
                   records. For now, just hard-code them. These are highly
                   simplified versions of the Medicare service record validation
                   rules */
                /* This code generates records with the following format:
                   Record format:	Type	   Source of random value
                   Transaction ID       Number     Any positive
                   Transaction date     Date	   system date
                   Claim type	        Number     choose from 1-3
                   Provider ID	        Number     any positive
                   Provider category    Number     choose from 7 values: 100, 150, 174, 201, 250, 300, 320
                   Subscriber name      Char 	   records blank at random
                   Subscriber birthdate	Date	   some dates in future
                   Service date		Date	   some dates in future
                   Diagnosis code	Number     choose from 10 values: 100, 101, 102, 110, 200, 210, 211, 215, 300, 301
                   Service code		Number     choose from 10 values: 500, 502, 503, 510, 517, 520, 525, 530, 531, 535
                   Amount		Number     values negative at random
                */

                ArrayList<DataElementInterface> fields = new ArrayList<DataElementInterface>();
                SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy"); // m means minutes
                Calendar dateManipulator = Calendar.getInstance();
                Date systemDate = new Date();

                // Transaction ID
                fields.add(new DataElementIncremental(1));
                
                // Transaction Date
                String systemDateStr = dateFormatter.format(systemDate);
                fields.add(new DateRandomRange(systemDateStr, systemDateStr,
                                               1));
                
                // Claim type
                fields.add(new RandomRange(1, 3, 1));

                // Provider ID
                fields.add(new RandomRange(1, 10000, 1));

                // Provider category
                DataElementValuesEqual provider = new DataElementValuesEqual();
                provider.insertValue("100");
                provider.insertValue("150");
                provider.insertValue("174");
                provider.insertValue("201");
                provider.insertValue("250");
                provider.insertValue("300");
                provider.insertValue("320");
                fields.add(provider);
                
                // Subscriber name
                DataElementValuesEqual subscriber = new DataElementValuesEqual();
                subscriber.insertValue("test1");
                subscriber.insertValue("test2");
                subscriber.insertValue("test3");
                subscriber.insertValue("test4");
                subscriber.insertValue("test5");
                subscriber.insertValue("test6");
                subscriber.insertValue("test7");
                subscriber.insertValue("test8");
                subscriber.insertValue("test9");
                subscriber.insertValue(""); // Deliberately empty string
                fields.add(subscriber);
                
                // The clunky Java pattern to add and subtract days from dates
                dateManipulator.setTime(systemDate);
                dateManipulator.add(Calendar.DATE, -10);
                String firstDate = dateFormatter.format(dateManipulator.getTime());
                dateManipulator.setTime(systemDate);
                dateManipulator.add(Calendar.DATE, 1);
                String secondDate = dateFormatter.format(dateManipulator.getTime());
                // Subscriber birthdate
                fields.add(new DateRandomRange(firstDate, secondDate, 1));

                // Service date
                fields.add(new DateRandomRange(firstDate, secondDate, 1));

                // Diagnosis code
                DataElementValuesEqual diagnosis = new DataElementValuesEqual();
                diagnosis.insertValue("100");
                diagnosis.insertValue("101");
                diagnosis.insertValue("102");
                diagnosis.insertValue("110");
                diagnosis.insertValue("200");
                diagnosis.insertValue("210");
                diagnosis.insertValue("211");
                diagnosis.insertValue("215");
                diagnosis.insertValue("300");
                diagnosis.insertValue("301");
                fields.add(diagnosis);

                // Service code
                DataElementValuesEqual service = new DataElementValuesEqual();
                service.insertValue("500");
                service.insertValue("502");
                service.insertValue("503");
                service.insertValue("510");
                service.insertValue("517");
                service.insertValue("520");
                service.insertValue("525");
                service.insertValue("530");
                service.insertValue("531");
                service.insertValue("535");
                fields.add(service);

                // Amount
                fields.add(new RandomRange(-150, 10050, 100));

                /* Validation rules are as follows:
                   1.  Provider category can't be in 300s
                   2.  Subscriber name must be filled in
                   3.  Subscriber birth date must be in past
                   4.  Service date must be in past
                   5.  Diagnosis code can't be in 200s
                   6.  Amount must be positive
                   7.  100 provider + 100 diagnosis can have any service 
                   8.  100 provider + 101,102 diagnosis can have 500,502,503,530,531
                   9. 100 provider + 110,300,301 diagnois can have 510,517,520,525,530
                   10. 150,174 provider + 101,102 diagnosis can have 500,502,510,520,531
                   11. 174 provider + 300 diagnosis can have 500,502,520,525,531
                   12. 201 provider any diagnosis and service
                   13. 250 provider + 100 diagnosis can have 500,502,503,510,517,520,525,530
                   14. 250 provider + 101,110 diagnosis can have 502,503,517,520
                   15. 250 provider + 300,301 diagnosis can have 510, 517, 520, 525, 530, 531, 535

                   Experimental tests show that roughly one out of four records
                   passes validation with this setup.
                */

                ArrayList<ValidationRuleInterface> validation = new ArrayList<ValidationRuleInterface>();

                // Provider category
                SimpleValuesRule providerInvalid = new SimpleValuesRule("300", 4, true);
                providerInvalid.addValue("320");
                validation.add(providerInvalid);
                
                // Subscriber name
                validation.add(new SimpleValuesRule("", 5, true));

                // Subscriber birth date
                validation.add(new DatePastRule(6));

                // Service date
                validation.add(new DatePastRule(7));

                // Diagnosis code
                SimpleValuesRule diagnosisInvalid = new SimpleValuesRule("200", 8, true);
                diagnosisInvalid.addValue("210");
                diagnosisInvalid.addValue("211");
                diagnosisInvalid.addValue("215");
                validation.add(diagnosisInvalid);

                // Amount
                validation.add(new PositiveNumericRule(10));

                // Service code
                CombinationValuesRule serviceValid = new CombinationValuesRule("500", 9, "100", 4, "101", 8, false);
                serviceValid.addValue("502");
                serviceValid.addValue("503");
                serviceValid.addValue("530");
                serviceValid.addValue("531");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("500", 9, "100", 4,
                                                         "102", 8, false);
                serviceValid.addValue("502");
                serviceValid.addValue("503");
                serviceValid.addValue("530");
                serviceValid.addValue("531");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("510", 9, "100", 4,
                                                         "110", 8, false);
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                serviceValid.addValue("525");
                serviceValid.addValue("530");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("510", 9, "100", 4,
                                                         "300", 8, false);
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                serviceValid.addValue("525");
                serviceValid.addValue("530");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("510", 9, "100", 4,
                                                         "301", 8, false);
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                serviceValid.addValue("525");
                serviceValid.addValue("530");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("500", 9, "150", 4,
                                                         "101", 8, false);
                serviceValid.addValue("502");
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                serviceValid.addValue("531");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("500", 9, "150", 4,
                                                         "102", 8, false);
                serviceValid.addValue("502");
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                serviceValid.addValue("531");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("500", 9, "174", 4,
                                                         "101", 8, false);
                serviceValid.addValue("502");
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                serviceValid.addValue("531");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("500", 9, "174", 4,
                                                         "102", 8, false);
                serviceValid.addValue("502");
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                serviceValid.addValue("531");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("500", 9, "174", 4,
                                                         "300", 8, false);
                serviceValid.addValue("502");
                serviceValid.addValue("520");
                serviceValid.addValue("525");
                serviceValid.addValue("531");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("500", 9, "250", 4,
                                                         "100", 8, false);
                serviceValid.addValue("502");
                serviceValid.addValue("503");
                serviceValid.addValue("510");
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                serviceValid.addValue("525");
                serviceValid.addValue("530");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("502", 9, "250", 4,
                                                         "101", 8, false);
                serviceValid.addValue("503");
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("502", 9, "250", 4,
                                                         "110", 8, false);
                serviceValid.addValue("503");
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                validation.add(serviceValid);

                serviceValid = new CombinationValuesRule("510", 9, "250", 4,
                                                         "301", 8, false);
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                serviceValid.addValue("525");
                serviceValid.addValue("530");
                serviceValid.addValue("531");
                serviceValid.addValue("535");
                validation.add(serviceValid);
                serviceValid = new CombinationValuesRule("510", 9, "250", 4,
                                                         "300", 8, false);
                serviceValid.addValue("517");
                serviceValid.addValue("520");
                serviceValid.addValue("525");
                serviceValid.addValue("530");
                serviceValid.addValue("531");
                serviceValid.addValue("535");
                validation.add(serviceValid);

                TestRecordCreator creator = new TestRecordCreator(fields,
                                                                  validation);

                /* To avoid memory problems on huge record sets, generate and
                   output the records in groups */
                int groupSize = 10000;
                int groupCount = recordCount / groupSize;
                int lastCount = recordCount % groupSize;
                int index, outIndex;
                for (index = 0; index < groupCount; index++)
                    outputFile = createRecords(creator, outputFile,
                                               groupSize, debug);
                if (lastCount > 0)
                    outputFile = createRecords(creator, outputFile,
                                               lastCount, debug);
            } // Try block
            catch (Exception e) {
                e.printStackTrace();
                System.out.println("Record generation failed! Error:" + e.getMessage());
            } // Catch block
            finally {
                if (outputFile != null)
                    outputFile.close();
            } // Finally block
        } // Correct number of input arguments
    } // Main method
};