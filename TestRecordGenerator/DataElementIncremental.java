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

/* This class defines a generator for a single field in a single record of
   synthetic data. It generates numbers in increasing order from some seed
   WARNING: Multiple objects will create duplicate numbers */
import java.util.*;

public final class DataElementIncremental implements DataElementInterface
{
    private int _currValue;
    
    // Construct the object with the seed value
    DataElementIncremental(int seed)
    {
        _currValue = seed;
    }

    // Returns the next value in the sequence. WARNING: NOT thread safe
    public String nextValue()
    {
        int value = _currValue;
        _currValue++;
        return Integer.toString(value);
    }
        
    // Debug output: print the contents of the object
    public String toString()
    {
        return "DataElementIncremental: next value " + _currValue;
    }

    /* Code to test the class. Insert some random data and then repeatedly
       generate data and print it */
    public static void main(String[] args)
    {
        DataElementIncremental test = new DataElementIncremental(-10);
        String value1 = Integer.toString(-10);
        String value2 = Integer.toString(-9);
        String value3 = Integer.toString(-8);
        
        System.out.println("Test equals:" + test);
        String testValue1 = test.nextValue();
        if (!testValue1.equals(value1))
            System.out.println("Error, expected " + value1 + " got " + testValue1);
        System.out.println("Test equals:" + test);
        String testValue2 = test.nextValue();
        if (!testValue2.equals(value2))
            System.out.println("Error, expected " + value2 + " got " + testValue2);
        System.out.println("Test equals:" + test);
        String testValue3 = test.nextValue();
        if (!testValue3.equals(value3))
            System.out.println("Error, expected " + value3 + " got " + testValue3);
    }
};