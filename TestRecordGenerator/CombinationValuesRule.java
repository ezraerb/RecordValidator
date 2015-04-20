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

/* This class validates combinations of values in multiple fields. It states
   that if the first field has one of a set of values, and a second field has
   one of a set of values, the third field is validated against a list of
   values that are either required or prohibited. Not having any of the fields
   at all also fails validation. Remember that fields are indexed from zero!
   Constructt the object with the first value and then add the rest */
import java.util.*;

public class CombinationValuesRule implements ValidationRuleInterface
{
    private ArrayList<String> _testValues;
    private int _firstFieldNumber;
    private int _secondFieldNumber;
    private String _firstFilterValue;
    private String _secondFilterValue;

    private int _testFieldNumber;
    /* True, field value must NOT be on the list; false, field value MUST be
       on the list */
    private boolean _prohibited; 
    // True, values are in sorted order
    private boolean _sortValid;
    
    public CombinationValuesRule(String testValue, int firstFieldNumber,
                                 String firstFilterValue, int secondFieldNumber,
                                 String secondFilterValue, int testFieldNumber,
                                 boolean prohibited)
    {
        // Field numbers must be non-negative
        if (firstFieldNumber < 0)
            throw new IllegalArgumentException("First filter field negative");
        if (secondFieldNumber < 0)
            throw new IllegalArgumentException("Second filer field negative");
        if (testFieldNumber < 0)
            throw new IllegalArgumentException("Field to test negative");

        // All three fields must be different
        if (firstFieldNumber == secondFieldNumber)
            throw new IllegalArgumentException("Same filter field specified twice");
        if ((firstFieldNumber == testFieldNumber) ||
            (secondFieldNumber == testFieldNumber))
            throw new IllegalArgumentException("Field to test also specified as field to filter");

        // Filter values must not be null (empty values are valid
        if (firstFilterValue == null)
            throw new IllegalArgumentException("First field filter value null");
        if (secondFilterValue == null)
            throw new IllegalArgumentException("First field filter value null");
        
        /* Test value must not be null (note that an empty string is a valid
           value!) */
        if (testValue == null)
            throw new IllegalArgumentException("Value to test null");

        _testValues = new ArrayList<String>();
        _testValues.add(testValue);
        _sortValid = true; // Only one value, sorted by definition

        _firstFieldNumber = firstFieldNumber;
        _firstFilterValue = firstFilterValue;
        _secondFieldNumber = secondFieldNumber;
        _secondFilterValue = secondFilterValue;
        
        _testFieldNumber = testFieldNumber;
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
        // Data not containing the wanted fields always fails
        if ((_firstFieldNumber >= fieldData.size()) ||
            (_secondFieldNumber >= fieldData.size()) ||
            (_testFieldNumber >= fieldData.size()))
            return true;

        /* If the first or second fields have values that do not match the
           filter values, this rule does not apply and the field data passes */
        if ((!fieldData.get(_firstFieldNumber).equals(_firstFilterValue)) ||
            (!fieldData.get(_secondFieldNumber).equals(_secondFilterValue)))
            return false; // NOT invalid

        // Extract the field and search for it in the list
        if (!_sortValid) {
            Collections.sort(_testValues);
            _sortValid = true;
        }
        boolean haveValue = (Collections.binarySearch(_testValues, fieldData.get(_testFieldNumber)) >= 0);

        /* Validation fails if the status of the value on the list matches the
           prohibition state */
        return (haveValue == _prohibited);
    }

    public String toString()
    {
        StringBuffer output = new StringBuffer();
        output.append("Validation: filter field ");
        output.append(_firstFieldNumber);
        output.append(" value: ");
        output.append(_firstFilterValue);
        output.append(" Filter field ");
        output.append(_secondFieldNumber);
        output.append(" value: ");
        output.append(_secondFilterValue);
        output.append(" test field ");
        output.append(_testFieldNumber);
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
        CombinationValuesRule test1 = new CombinationValuesRule("test5", 1,
                                                                "filter1", 2,
                                                                "filter2", 3,
                                                                true);
        test1.addValue("test1");
        test1.addValue("test3");

        CombinationValuesRule test2 = new CombinationValuesRule("test4", 1,
                                                                "filter1", 2,
                                                                "filter2", 3,
                                                                false);
        test2.addValue("test6");
        test2.addValue("test2");

        System.out.println("test1: " + test1);
        System.out.println("test2: " + test2);

        StringBuffer output = null;
        ArrayList<String> testData = new ArrayList<String>();
        // Wrong filter value
        testData.add("thisDoesntMatter");
        testData.add("wrongValue");
        testData.add("filter2");
        testData.add("test3"); // Must normally be prohibited
        testData.add("thisHasNoEffect");

        output = new StringBuffer(); // Clunky, but faster than clearing the old value
        output = TestRecordCreator.appendStringArray(testData, " ", output);
        System.out.println(output.toString());
        if (test1.failsValidation(testData))
            System.out.println("test1 expected pass, failed validation");
        else
            System.out.println("test1 passes validation, test passed");

        if (test2.failsValidation(testData))
            System.out.println("test2 expected pass, failed validation");
        else
            System.out.println("test2 passes validation, test passed");
        
        // Too few values
        testData.clear();
        testData.add("thisDoesntMatter");
        testData.add("filter1");
        testData.add("filter2");
        output = new StringBuffer(); // Clunky, but faster than clearing the old value
        output = TestRecordCreator.appendStringArray(testData, " ", output);
        System.out.println(output.toString());
        if (!test1.failsValidation(testData))
            System.out.println("test1 expected fail, passed validation");
        else
            System.out.println("test1 failed validation, test passed");
        if (!test2.failsValidation(testData))
            System.out.println("test2 expected fail, passed validation");
        else
            System.out.println("test2 failed validation, test passed");

        // Value on prohibit list/not on accept list
        testData.add("test3");
        testData.add("thisHasNoEffect");
        output = new StringBuffer(); // Clunky, but faster than clearing the old value
        output = TestRecordCreator.appendStringArray(testData, " ", output);
        System.out.println(output.toString());
        if (!test1.failsValidation(testData))
            System.out.println("test1 expected fail, pass validation");
        else
            System.out.println("test1 failed validation, test passed");

        if (!test2.failsValidation(testData))
            System.out.println("test2 expected fail, pass validation");
        else
            System.out.println("test2 failed validation, test passed");

        // Value on accept list/not on prohibit list
        testData.clear();
        testData.add("thisDoesntMatter");
        testData.add("filter1");
        testData.add("filter2");
        testData.add("test4");
        testData.add("thisHasNoEffect");
        output = new StringBuffer(); // Clunky, but faster than clearing the old value
        output = TestRecordCreator.appendStringArray(testData, " ", output);
        System.out.println(output.toString());
        if (test1.failsValidation(testData))
            System.out.println("test1 expected pass, fails validation");
        else
            System.out.println("test1 passes validation, test passed");

        if (test2.failsValidation(testData))
            System.out.println("test2 expected pass, fails validation");
        else
            System.out.println("test2 passes validation, test passed");

        // Value not on either list
        testData.clear();
        testData.add("thisDoesntMatter");
        testData.add("filter1");
        testData.add("filter2");
        testData.add("test10");
        testData.add("thisHasNoEffect");
        output = new StringBuffer(); // Clunky, but faster than clearing the old value
        output = TestRecordCreator.appendStringArray(testData, " ", output);
        System.out.println(output.toString());
        if (test1.failsValidation(testData))
            System.out.println("test1 expected pass, fail validation");
        else
            System.out.println("test1 passes validation, test passed");

        if (!test2.failsValidation(testData))
            System.out.println("test2 expected fail, pass validation");
        else
            System.out.println("test2 failed validation, test passed");

    }
};