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

/* This class represents a record filter on a single field with a defined
   value */
import java.util.*;

public final class FieldFilter implements Comparable<FieldFilter>
{
    private int _field;
    private String _value;

    // Contructor. Field must be non-negative and value not null (empty is valid)
    public FieldFilter(int field, String value)
    {
        if (field < 0)
            throw new IllegalArgumentException("field to filter " + field + " must be non-negative");
        if (value == null)
            throw new IllegalArgumentException("value to filter passed null");
        _field = field;
        _value = value;
    }

    /* Construct a field filter that finds all records that match the given
       field of an existing record. Remember that fields are indexed from zero!
       The record must have the requested field or an exception is thrown */
    public FieldFilter(List<String> record, int field)
    {
        if (field < 0)
            throw new IllegalArgumentException("field to filter " + field + " must be non-negative");
        else if (record == null)
            throw new IllegalArgumentException("record to match passed null");
        else if (field >= record.size())
            throw new IllegalArgumentException("Request field " + field + "; record only has " + record.size());
        else {
            _field = field;
            // Strings are immutable, so this works for setting the filter value
            _value = record.get(field);
        }
    }
            
    // Getters
    public int getField()
    {
        return _field;
    }
    public String getValue()
    {
        return _value;
    }

    // Returns true if a given record passes the filter
    public boolean passes(List<String> record)
    {
        if (record == null)
            return false; // No record!
        else if (record.size() <= _field)
            return false; // Field not in record
        else
            return _value.equals(record.get(_field));
    }

    /* Returns true if two filters are for the same field (implying a given
       record will almost certainly never pass both) */
    public boolean sameField(FieldFilter other)
    {
        return (other._field == _field);
    }
    
    // Comparision method. Needed to sort collections of these objects
    @Override
    public int compareTo(FieldFilter other)
    {
        if (_field != other._field) {
            if (_field < other._field)
                return -1;
            else
                return 1;
        }
        else
            return _value.compareTo(other._value);
    }
        
    // Equality method. Needed for many containers
    @Override
    public boolean equals(Object other)
    {
        if (this == other) // Self
            return true;
        else if (other == null) // Null pointer
            return false;
        else if (getClass() != other.getClass()) // Class mismatch
            return false;
        else {
            FieldFilter otherFilter = (FieldFilter)other;
            return (compareTo(otherFilter) == 0);
        }
    }
        
    // Hash method. Needed to store in hashtables
    @Override
    public int hashCode()
    {
        final int seed = 5;
        final int multiplier = 31; // Often used
        return (((seed * multiplier) + _field) * multiplier) + _value.hashCode();
    }

    public String toString()
    {
        // Field must be first, matches the sort order
        return _field + "->" + _value;
    }

    // Code to test the class
    public static void main(String[] args)
    {
        ArrayList<String> record = new ArrayList<String>();
        record.add("test1");
        record.add("test2");
        record.add("test3");

        System.out.println("Create object with no record, expect exception");
        try {
            FieldFilter test = new FieldFilter((ArrayList<String>)null, 1);
            System.out.println("Failed, got " + test);
        }
        catch (Exception e) {
            System.out.println("Succeeded, caught " + e);
        }
        
        System.out.println("Create object with negative field, expect exception");
        try {
            FieldFilter test = new FieldFilter(record, -1);
            System.out.println("Failed, got " + test);
        }
        catch (Exception e) {
            System.out.println("Succeeded, caught " + e);
        }
        
        System.out.println("Create object with non-existent field, expect exception");
        try {
            FieldFilter test = new FieldFilter(record, 10);
            System.out.println("Failed, got " + test);
        }
        catch (Exception e) {
            System.out.println("Succeeded, caught " + e);
        }
        
        System.out.println("Create object with valid field and record, expect suucess");
        try {
            FieldFilter test = new FieldFilter(record, 1);
            System.out.println("Succeeded, got " + test);
        }
        catch (Exception e) {
            System.out.println("Failed, caught " + e);
        }

        FieldFilter test = new FieldFilter(record, 1);
        System.out.println("Filter null record, expect false");
        if (test.passes(null))
            System.out.println("Test failed, filter passed");
        else
            System.out.println("Test succeeded, filter failed");

        ArrayList<String> testRecord = new ArrayList<String>();

        System.out.println("Filter record too few fields, expect false");
        testRecord.add("doeesn'tmatter");
        if (test.passes(testRecord))
            System.out.println("Test failed, filter passed");
        else
            System.out.println("Test succeeded, filter failed");

        System.out.println("Filter record wrong value, expect false");
        testRecord.add("wrongvalue");
        if (test.passes(testRecord))
            System.out.println("Test failed, filter passed");
        else
            System.out.println("Test succeeded, filter failed");
        
        System.out.println("Filter record correct value, expect true");
        
        testRecord.remove(testRecord.size() - 1);
        testRecord.add("test2");
        if (test.passes(testRecord))
            System.out.println("Test succeeded, filter passed");
        else
            System.out.println("Test failed, filter failed");
        
        System.out.println("Equality with null, expect false");
        if (test.equals(null))
            System.out.println("Test falied, equal");
        else
            System.out.println("Test succeeded, not equal");

        System.out.println("Equality with wrong class, expect false");
        if (test.equals(testRecord))
            System.out.println("Test falied, equal");
        else
            System.out.println("Test succeeded, not equal");

        System.out.println("Equality with diff value, expect false");
        FieldFilter otherFilter1 = new FieldFilter(1, "wrongValue");
        System.out.println("First: " + test + " Second: " + otherFilter1);
        if (test.equals(otherFilter1))
            System.out.println("Test falied, equal");
        else
            System.out.println("Test succeeded, not equal");

        System.out.println("Diff value, confirm comparison is not zero");
        if (test.compareTo(otherFilter1) == 0)
            System.out.println("Test failed, equal");
        else
            System.out.println("Test succeeded, not equal");

        // This is not guarenteed in general, but values chosen so it is
        System.out.println("Diff value, confirm hashcodes different");
        if (test.hashCode() == otherFilter1.hashCode())
            System.out.println("Test failed, hash codes equal");
        else
            System.out.println("Test succeeded, hash codes not equal");
        
        System.out.println("Equality with same value, expect true");
        FieldFilter otherFilter2 = new FieldFilter(1, "test2");
        System.out.println("First: " + test + " Second: " + otherFilter2);
        if (test.equals(otherFilter2))
            System.out.println("Test succeeded, equal");
        else
            System.out.println("Test failed, not equal");

        System.out.println("Same value, confirm comparison is zero");
        if (test.compareTo(otherFilter2) == 0)
            System.out.println("Test succeeded, equal");
        else
            System.out.println("Test failed, not equal");

        System.out.println("Same value, confirm hashcodes same");
        if (test.hashCode() == otherFilter2.hashCode())
            System.out.println("Test succeeded, hash codes equal");
        else
            System.out.println("Test failed, hash codes not equal");
        }
};