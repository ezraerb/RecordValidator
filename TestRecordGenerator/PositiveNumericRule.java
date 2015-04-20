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

/* This class validates that a field is a positive integer. If the field is not
   an integer or does not exist, it also fails */
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class PositiveNumericRule implements ValidationRuleInterface
{
    private int _fieldNumber;
    
    public PositiveNumericRule(int fieldNumber)
    {
        // Field number must be non-negative
        if (fieldNumber < 0)
            throw new IllegalArgumentException("Field to test negative");

        _fieldNumber = fieldNumber;
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

        /* Extract the field and convert it. Any exception causes validation
           failure */
        try {
            int value = Integer.parseInt(fieldData.get(_fieldNumber));
            return value <= 0;
        }
        catch (NumberFormatException e) {
            return true; // Failed
        }
    }

    public String toString()
    {
        return "Validation: field " + _fieldNumber + " must be a positive number";
    }

    /* Code to test the class. Insert some random data and then repeatedly
       test data against it */
    public static void main(String[] args)
    {
        ValidationRuleInterface test = new PositiveNumericRule(1);
        System.out.println(test);
        ArrayList<String> testData = new ArrayList<String>();
        testData.add("thisDoesntMatter");

        System.out.println("Test data: " + testData.get(0));
        if (!test.failsValidation(testData))
            System.out.println("Expected fail, passed validation");
        else
            System.out.println("Validation fails, test passed");
        
        testData.add("30");
        testData.add("thisHasNoEffect");
        System.out.println("Test data: " + testData.get(0) + " " + testData.get(1) + " " + testData.get(2));
        if (test.failsValidation(testData))
            System.out.println("Expected pass, fail validation");
        else
            System.out.println("Validation passes, test passed");

        testData.clear();
        testData.add("thisDoesntMatter");
        testData.add("0");
        testData.add("thisHasNoEffect");
        System.out.println("Test data: " + testData.get(0) + " " + testData.get(1) + " " + testData.get(2));
        if (!test.failsValidation(testData))
            System.out.println("Expected fail, pass validation");
        else
            System.out.println("Validation fails, test passed");

        testData.clear();
        testData.add("thisDoesntMatter");
        testData.add("test");
        testData.add("thisHasNoEffect");
        System.out.println("Test data: " + testData.get(0) + " " + testData.get(1) + " " + testData.get(2));
        if (!test.failsValidation(testData))
            System.out.println("Expected fail, pass validation");
        else
            System.out.println("Validation fails, test passed");
    }
};