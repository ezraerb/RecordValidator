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

/* This class generates record data at random based on its field value
   generation objects. It then sets a validity status based on the its
   validation objects. Field value generation objects must be passed in order
   of the fields to generate */
import java.util.*;

class TestRecordCreator
{
    private List<DataElementInterface> _fields;
    private List<ValidationRuleInterface> _validation;

    public TestRecordCreator(List<DataElementInterface> fields,
                             List<ValidationRuleInterface> validation)
    {
        // The list of fields can't be null and must have at least one field
        if (fields == null)
            throw new IllegalArgumentException("Fields to generate passed null");
        else if (fields.size() <= 0)
            throw new IllegalArgumentException("Fields to generate passed empty");
        /* Validation can not be null, but may be empty. In that case everything
           will be valid */
        if (validation == null)
            throw new IllegalArgumentException("Validation rules passed null");

        /* To be completely through, should clone both lists so this object is
           not subject to outside manipulation of the lists. In practice, it
           won't be worth it */
        _fields = fields;
        _validation = validation;
    }

    /* Generates the number of records specified by count and validates them.
       Set the debug flag to get a trace of which rule failed the record,
       highly useful for debugging validation setups */
    public ArrayList<ArrayList<String> > generateRecords(int recCount,
                                                         boolean debugTrace)
    {
        ArrayList<ArrayList<String> > results = new ArrayList<ArrayList<String> >();
        if (recCount <= 0)
            return results; // No records requested
        int recIndex;
        for (recIndex = 0; recIndex < recCount; recIndex++) {
            ArrayList<String> nextRecord = new ArrayList<String>();
            // Fields are generated in the order specified
            int index;
            for (index = 0; index < _fields.size(); index++)
                nextRecord.add(_fields.get(index).nextValue());
            /* Validate the record and insert the result as the last field.
               A record is valid until flagged overwise by a validator */
            boolean valid = true;
            for (index = 0; index < _validation.size(); index++)
                if (_validation.get(index).failsValidation(nextRecord)) {
                    valid = false;
                    if (debugTrace) {
                        /* Print the record data with numbered fields and the
                           rule that failed the record.
                           WARNING: This will bias the trace toward rules
                           earlier in the list because a record could fail
                           due to several */
                        System.out.println("Record number: " + recIndex);
                        int debugIndex;
                        for (debugIndex = 0; debugIndex < nextRecord.size();
                             debugIndex++)
                            System.out.println(debugIndex + ": " + nextRecord.get(debugIndex));
                        System.out.println(_validation.get(index));
                    }
                    break; // No point testing any further
                }
            nextRecord.add(String.valueOf(valid));
            results.add(nextRecord);
        }
        return results;
    }
            
    // Output the contents of a list of strings, from the standard Java pattern
    public static StringBuffer appendStringArray(List<String> data,
                                                 String seperator,
                                                 StringBuffer buffer)
    {
        int index;
        for (index = 0; index < data.size(); index++) {
            if (index > 0)
                buffer.append(seperator);
            buffer.append(data.get(index));
        }
        return buffer;
    }

    // Debug output: print the field generation and validation objects
    public String toString()
    {
        StringBuffer output = new StringBuffer();
        output.append("TestRecordCreator:\n");
        int index;
        for (index = 0; index < _fields.size(); index++) {
            output.append(_fields.get(index).toString());
            output.append("\n");
        }
        for (index = 0; index < _validation.size(); index++) {
            output.append(_validation.get(index).toString());
            output.append("\n");
        }
        output.append("\n");
        return output.toString();
    }
    
    /* Test code to generate a simple set of records and output them.
       Unfortunately the output is random so it must be verified manually */
    public static void main(String[] args)
    {
        /* The records for this test have three fields, each of which has
           three possible values. For the first and second fields, one field
           value is invalid. For the third, the valid values depend on the other
           two field values. This gives 27 possible combinations of values, only
           eight of which are valid */
        ArrayList<DataElementInterface> fields = new ArrayList<DataElementInterface>();
        DataElementValuesEqual testValues1 = new DataElementValuesEqual();
        testValues1.insertValue("test11");
        testValues1.insertValue("test12");
        testValues1.insertValue("test13");
        fields.add(testValues1);
        
        DataElementValuesEqual testValues2 = new DataElementValuesEqual();
        testValues2.insertValue("test21");
        testValues2.insertValue("test22");
        testValues2.insertValue("test23");
        fields.add(testValues2);

        DataElementValuesEqual testValues3 = new DataElementValuesEqual();
        testValues3.insertValue("test31");
        testValues3.insertValue("test32");
        testValues3.insertValue("test33");
        fields.add(testValues3);

        ArrayList<ValidationRuleInterface> validation = new ArrayList<ValidationRuleInterface>();
        validation.add(new SimpleValuesRule("test12", 0, true));
        validation.add(new SimpleValuesRule("test22", 1, true));


        CombinationValuesRule testRule1 = new CombinationValuesRule("test31",
                                                                    0, "test11",
                                                                    1, "test21",
                                                                    2, false);
        testRule1.addValue("test32");
        validation.add(testRule1);

        CombinationValuesRule testRule2 = new CombinationValuesRule("test31",
                                                                    0, "test13",
                                                                    1, "test23",
                                                                    2, false);
        testRule2.addValue("test32");
        validation.add(testRule2);

        CombinationValuesRule testRule3 = new CombinationValuesRule("test32",
                                                                    0, "test11",
                                                                    1, "test23",
                                                                    2, false);
        testRule3.addValue("test33");
        validation.add(testRule3);

        CombinationValuesRule testRule4 = new CombinationValuesRule("test31",
                                                                    0, "test13",
                                                                    1, "test21",
                                                                    2, false);
        testRule4.addValue("test33");
        validation.add(testRule4);

        TestRecordCreator test = new TestRecordCreator(fields, validation);
        ArrayList<ArrayList<String> > results = test.generateRecords(50, true);
        int index;
        StringBuffer buffer = null;
        for (index = 0; index < results.size(); index++) {
            buffer = new StringBuffer();
            buffer = appendStringArray(results.get(index), ",", buffer);
            System.out.println(buffer.toString());
        }
    }
};