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

/* This class validates a single field against a list of values that are either
   required or prohibited. Not having the field also fails validation. Remember
   that fields are indexed from zero! Construct the object with the first value
   and then add the rest */
import java.util.*;

public class SimpleValuesRule implements ValidationRuleInterface
{
    private ArrayList<String> _testValues;
    private int _fieldNumber;
    /* True, field value must NOT be on the list; false, field value MUST be
       on the list */
    private boolean _prohibited; 
    // True, values are in sorted order
    private boolean _sortValid;
    
    public SimpleValuesRule(String testValue, int fieldNumber,
                            boolean prohibited)
    {
        // Value may not be null (note that an empty string is a valid value!)
        if (testValue == null)
            throw new IllegalArgumentException("Value to test null");

        // Field number must be non-negative
        if (fieldNumber < 0)
            throw new IllegalArgumentException("Field to test negative");
            
        _testValues = new ArrayList<String>();
        _testValues.add(testValue);
        _sortValid = true; // Only one value, sorted by definition

        _fieldNumber = fieldNumber;
        _prohibited = prohibited;
    }
    
    /* Adds another value to the list.
       WARNING: Using this after previous validation will invalidate those
       results */
    public void addValue(String testValue)
    {
        /* If this element falls before last item in the list, the list must be
           resorted before it is used */
        if (testValue.compareTo(_testValues.get(_testValues.size() - 1)) < 0)
            _sortValid = false;
        _testValues.add(testValue);
    }

    // Returns true if a given set of field data is NOT valid
    public boolean failsValidation(List<String> fieldData)
    {
        // No fields always fails
        if (fieldData == null)
            return true;
        // Data not containing the wanted field always fails
        if (_fieldNumber >= fieldData.size())
            return true;

        // Extract the field and search for it in the list
        if (!_sortValid) {
            Collections.sort(_testValues);
            _sortValid = true;
        }
        boolean haveValue = (Collections.binarySearch(_testValues, fieldData.get(_fieldNumber)) >= 0);

        /* Validation fails if the status of the value on the list matches the
           prohibition state */
        return (haveValue == _prohibited);
    }

    public String toString()
    {
        StringBuffer output = new StringBuffer();
        output.append("Validation: field ");
        output.append(_fieldNumber);
        if (_prohibited)
            output.append(" prohibited: ");
        else
            output.append(" required: ");
        output = TestRecordCreator.appendStringArray(_testValues, " ", output);
        return output.toString();
    }

    /* Code to test the class. Insert some random data and then repeatedly
       test data against it */
    public static void main(String[] args)
    {
        SimpleValuesRule test1 = new SimpleValuesRule("test5", 1, true);
        test1.addValue("test1");
        test1.addValue("test3");
        
        SimpleValuesRule test2 = new SimpleValuesRule("test4", 1, false);
        test2.addValue("test6");
        test2.addValue("test2");

        System.out.println("test1: " + test1);
        System.out.println("test2: " + test2);

        StringBuffer output = null;
        ArrayList<String> testData = new ArrayList<String>();
        testData.add("thisDoesntMatter");
        output = new StringBuffer(); // Clunky, but faster than clearing the old value
        output = TestRecordCreator.appendStringArray(testData, " ", output);
        System.out.println(output.toString());
        if (!test1.failsValidation(testData))
            System.out.println("test1 expected fail, passed validation");
        else
            System.out.println("test1 fails validation, test passes");
        if (!test2.failsValidation(testData))
            System.out.println("test2 expected fail, passed validation");
        else
            System.out.println("test2 fails validation, test passes");

        testData.add("test2");
        testData.add("thisHasNoEffect");
        output = new StringBuffer(); // Clunky, but faster than clearing the old value
        output = TestRecordCreator.appendStringArray(testData, " ", output);
        System.out.println(output.toString());
        if (test1.failsValidation(testData))
            System.out.println("test1 expected pass, fail validation");
        else
            System.out.println("test1 passes validation, test passes");
        if (test2.failsValidation(testData))
            System.out.println("test2 expected pass, fail validation");
        else
            System.out.println("test2 passes validation, test passes");


        testData.clear();
        testData.add("thisDoesntMatter");
        testData.add("test5");
        testData.add("thisHasNoEffect");
        output = new StringBuffer(); // Clunky, but faster than clearing the old value
        output = TestRecordCreator.appendStringArray(testData, " ", output);
        System.out.println(output.toString());
        if (!test1.failsValidation(testData))
            System.out.println("test1 expected fail, pass validation");
        else
            System.out.println("test1 fails validation, test passed");
        if (!test2.failsValidation(testData))
            System.out.println("test2 expected fail, pass validation");
        else
            System.out.println("test2 fails validation, test passed");

        testData.clear();
        testData.add("thisDoesntMatter");
        testData.add("test10");
        testData.add("thisHasNoEffect");
        output = new StringBuffer(); // Clunky, but faster than clearing the old value
        output = TestRecordCreator.appendStringArray(testData, " ", output);
        System.out.println(output.toString());
        if (test1.failsValidation(testData))
            System.out.println("test1 expected pass, fail validation");
        else
            System.out.println("test1 passes validation, test passes");
        if (!test2.failsValidation(testData))
            System.out.println("test2 expected fail, pass validation");
        else
            System.out.println("test2 fails validation, test passed");
    }
};