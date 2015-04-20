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

/* This class represents a collection of record filters on different fields.
   A record must match all of them to pass the filter */
import java.util.*;

public final class FieldFilterGroup
{
    private FieldFilter[] _filters;

    // Contruct from a group of filters
    public FieldFilterGroup(List<FieldFilter> filters)
    {
        // Filters must be non-null
        if (filters == null)
            throw new IllegalArgumentException("set of field filters passed null");
        // Filters must be non-empty
        if (filters.size() <= 0)
            throw new IllegalArgumentException("set of field filters passed empty");
            
        /* Need a copy of the filters seperate from the list, which is
           mutable. The shallow copy of an array conversion handles this
           easily */
        init((FieldFilter[])filters.toArray());
    }
        
    // Construct from a single filter
    public FieldFilterGroup(FieldFilter filter)
    {
        // Filter must be non-null
        if (filter == null)
            throw new IllegalArgumentException("field filter passed null");

        /* Create a single array with this filter in it and initialize the class
           with it */
        FieldFilter[] temp = new FieldFilter[1];
        temp[0] = filter;
        init(temp);
    }

    /* Construct a field filter that finds all records that match the given
       fields of an existing record. The record must have the requested fields
       or an exception is thrown */
    public FieldFilterGroup(List<String> record, int[] fields)
    {
        if (fields == null)
            throw new IllegalArgumentException("List of fields to filter passed null");
        if (fields.length == 0)
            throw new IllegalArgumentException("No fields passed to filter");
        if (record == null)
            throw new IllegalArgumentException("record to match passed null");
        FieldFilter[] filters = new FieldFilter[fields.length];
        int index;
        for (index = 0; index < fields.length; index++)
            // WARNING: If any field is invalid, this method will throw
            filters[index] = new FieldFilter(record, fields[index]);
        init(filters);
    }
            
    /* Construct a field filter group by copying an existing group and adding
       one filter to it */
    public FieldFilterGroup(FieldFilterGroup source, FieldFilter field)
    {
        if (source == null)
            throw new IllegalArgumentException("Base filter group passed null");
        if (field == null)
            throw new IllegalArgumentException("New field for group passed null");
        _filters = new FieldFilter[source._filters.length + 1];
        int index = 0;
        while ((index < source._filters.length) && (source._filters[index].compareTo(field) < 0)) {
            _filters[index] = source._filters[index];
            index++;
        }

        /* At this point, index points to the first entry equal to or beyond the
           field to add. Filter groups require that each filter be for a
           different field. Due to how compareTo works, either the filter before
           or the filter after the wanted slot may cause the new filter to fail
           this requirement */
        if (index > 0) {
            if (source._filters[index - 1].sameField(field))
                throw new IllegalArgumentException("New filter has field matching one already in base group: " + field.getField());
        } // At least one field copied over
        if (index < source._filters.length) {
            if (source._filters[index].sameField(field))
                throw new IllegalArgumentException("New filter has field matching one already in base group: " + field.getField());
        }
        _filters[index] = field;

        // Add remainder
        while (index < source._filters.length) {
            _filters[index + 1] = source._filters[index];
            index++;
        }
    }

    // Initialize the class from an array of filters
    private void init(FieldFilter[] filters)
    {
        // WARNING: This method assumes filters is not null. Caller must check!
        if (filters.length > 1) {
            Arrays.sort(filters);
            /* Only allow one filter per field. Filters sort first on field
               being sorted, so can just compare consecutive elements */
            int index;
            for (index = 0; index < (filters.length - 1); index++)
                if (filters[index].sameField(filters[index + 1]))
                    throw new IllegalArgumentException("Filter group has two filters for field " + filters[index].getField());
        } // More than one filter in the group
        _filters = filters;
    }

    // Returns true if a given record passes the filter
    public boolean passes(List<String> record)
    {
        if (record == null)
            return false; // No record!
        boolean pass = true;
        int index = 0;
        while ((index < _filters.length) && pass) {
            if (!_filters[index].passes(record))
                pass = false;
            else
                index++;
        }
        return pass;
    }

    // Returns the number of defined filters
    public int filterCount()
    {
        return _filters.length;
    }

    // Returns the highest field for which a filter is defined
    public int getLastFilterField()
    {
        /* Filters are sorted by field, so the last filter in the list is
           the wanted value */
        return _filters[_filters.length - 1].getField();
    }
    
    // Equality method. Needed for hashing to work properly
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
            FieldFilterGroup otherFilter = (FieldFilterGroup)other;
            return Arrays.equals(_filters, otherFilter._filters);
        }
    }
        
    // Hash method. Needed to store in hashtables
    @Override
    public int hashCode()
    {
        final int seed = 7; // Chosen to not clash with FieldFilter
        final int multiplier = 53;
        int result = seed;
        int index;
        for (index = 0; index < _filters.length; index++)
            result = (result * multiplier) + _filters[index].hashCode();
        return result;
    }

    public String toString()
    {
        StringBuffer output = new StringBuffer();
        output.append("[");
        int index;
        for (index = 0; index < _filters.length; index++) {
            if (index != 0)
                output.append(", ");
            output.append(_filters[index]);
        }
        output.append("]");
        return output.toString();
    }

    // Code to test the class
    public static void main(String[] args)
    {
        System.out.println("Create object with no record, expect exceptions");
        try {
            ArrayList<FieldFilter> testList = null;
            FieldFilterGroup test = new FieldFilterGroup(testList);
            System.out.println("Failed, got " + test);
        }
        catch (Exception e) {
            System.out.println("Succeeded, caught " + e);
        }
        try {
            ArrayList<FieldFilter> testData = new ArrayList<FieldFilter>();
            FieldFilterGroup test = new FieldFilterGroup(testData);
            System.out.println("Failed, got " + test);
        }
        catch (Exception e) {
            System.out.println("Succeeded, caught " + e);
        }
        
        ArrayList<String> record = new ArrayList<String>();
        record.add("test1");
        record.add("test2");
        record.add("test3");

        System.out.println("Create object with one filter, expect success and highest field reflects filter");
        FieldFilter testFilter = new FieldFilter(record, 1);
        try {
            FieldFilterGroup test = new FieldFilterGroup(testFilter);
            System.out.println("Got " + test);
            if (test.getLastFilterField() == 1)
                System.out.println("Suceeded, last field as expected");
            else
                System.out.println("Failed, last field set wrong");
        }
        catch (Exception e) {
            System.out.println("Failed, caught " + e);
        }

        System.out.println("Create object with two filters for the same field, expect exception");
        try {
            FieldFilter testFilter2 = new FieldFilter(record, 1);
            ArrayList<FieldFilter> testData = new ArrayList<FieldFilter>();
            testData.add(testFilter);
            testData.add(testFilter2);
            FieldFilterGroup test = new FieldFilterGroup(testData);
            System.out.println("Failed, got " + test);
        }
        catch (Exception e) {
            System.out.println("Succeeded, caught " + e);
        }
        
        System.out.println("Create object with two fields, expect success and highest field reflects filter");
        try {
            int [] testFields = new int[2];
            testFields[0] = 2;
            testFields[1] = 1;
            FieldFilterGroup test = new FieldFilterGroup(record, testFields);
            System.out.println("Got " + test);
            if (test.getLastFilterField() == 2)
                System.out.println("Suceeded, last field as expected");
            else
                System.out.println("Failed, last field set wrong");
        }
        catch (Exception e) {
            System.out.println("Failed, caught " + e);
        }

        System.out.println("Create object with two fields where one is invalid, exepct exception");
        try {
            int [] testFields = new int[2];
            testFields[0] = -1;
            testFields[1] = 1;
            FieldFilterGroup test = new FieldFilterGroup(record, testFields);
            System.out.println("Failed, got " + test);
        }
        catch (Exception e) {
            System.out.println("Succeeded, caught " + e);
        }

        System.out.println("Create from existing filter, valid field");
        FieldFilterGroup testGroup = new FieldFilterGroup(testFilter);
        try {
            FieldFilter testFilter2 = new FieldFilter(record, 0);
            FieldFilterGroup test = new FieldFilterGroup(testGroup, testFilter2);
            System.out.println("Got " + test);
            if (test.getLastFilterField() == 1)
                System.out.println("Suceeded, last field as expected");
            else
                System.out.println("Failed, last field set wrong");
        }
        catch (Exception e) {
            System.out.println("Failed, caught " + e);
        }

        int [] testFields = new int[2];
        testFields[0] = 2;
        testFields[1] = 1;
        testGroup = new FieldFilterGroup(record, testFields);
        try {
            FieldFilterGroup test = new FieldFilterGroup(testGroup, testFilter);
            System.out.println("Failed, got " + test);
        }
        catch (Exception e) {
            System.out.println("Succeeded, caught " + e);
        }

        ArrayList<String> testRecord = new ArrayList<String>();

        System.out.println("Filter record too few fields, expect false");
        testRecord.add("doeesn'tmatter");
        testRecord.add("alsodoeesn'tmatter");
        if (testGroup.passes(testRecord))
            System.out.println("Test failed, filter passed");
        else
            System.out.println("Test succeeded, filter failed");

        System.out.println("Filter record one wrong value, expect false");
        testRecord.add(record.get(2));
        if (testGroup.passes(testRecord))
            System.out.println("Test failed, filter passed");
        else
            System.out.println("Test succeeded, filter failed");
        
        System.out.println("Filter record correct value, expect true");
        
        testRecord.remove(testRecord.size() - 1);
        testRecord.remove(testRecord.size() - 2);
        testRecord.add(record.get(1));
        testRecord.add(record.get(2));
        if (testGroup.passes(testRecord))
            System.out.println("Test succeeded, filter passed");
        else
            System.out.println("Test failed, filter failed");

        System.out.println("Equality with null, expect false");
        if (testGroup.equals(null))
            System.out.println("Test falied, equal");
        else
            System.out.println("Test succeeded, not equal");

        System.out.println("Equality with wrong class, expect false");
        if (testGroup.equals(testRecord))
            System.out.println("Test falied, equal");
        else
            System.out.println("Test succeeded, not equal");

        System.out.println("Equality with diff value, expect false");
        FieldFilterGroup otherGroup1 = new FieldFilterGroup(testFilter);
        System.out.println("First: " + testGroup + " Second: " + otherGroup1);
        if (testGroup.equals(otherGroup1))
            System.out.println("Test falied, equal");
        else
            System.out.println("Test succeeded, not equal");

        // This is not guarenteed in general, but values chosen so it is
        System.out.println("Diff value, confirm hashcodes different");
        if (testGroup.hashCode() == otherGroup1.hashCode())
            System.out.println("Test failed, hash codes equal");
        else
            System.out.println("Test succeeded, hash codes not equal");
        
        System.out.println("Equality with same value, expect true");
        FieldFilterGroup otherGroup2 = new FieldFilterGroup(record, testFields);
        System.out.println("First: " + testGroup + " Second: " + otherGroup2);
        if (testGroup.equals(otherGroup2))
            System.out.println("Test succeeded, equal");
        else
            System.out.println("Test failed, not equal");

        System.out.println("Same value, confirm hashcodes same");
        if (testGroup.hashCode() == otherGroup2.hashCode())
            System.out.println("Test succeeded, hash codes equal");
        else
            System.out.println("Test failed, hash codes not equal");
    }
};