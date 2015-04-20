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
   synthetic data. It chooses between a set of predefined elements with equal
   probability */
import java.util.*;

public final class DataElementValuesEqual implements DataElementInterface
{
    private ArrayList<String> _values;
    private Random _rand; // Random number generator
    
    // Construct the object empty
    DataElementValuesEqual()
    {
        _values = new ArrayList<String>();
        _rand = new Random();
    }

    /* Insert a new value into the list. It does not check for dupes, which
       means elements inserted more than once will have higher probability of'
       being chosen */
    public void insertValue(String value)
    {
        _values.add(value);
    }

    // Returns a value from the current list at random
    public String nextValue()
    {
        // If the list is empty, return an empty value
        if (_values.isEmpty())
            return new String();
        else
            return _values.get(_rand.nextInt(_values.size()));
    }
        
    // Debug output: print what this object can generate
    public String toString()
    {
        StringBuffer output = new StringBuffer();
        output.append("DataElementValuesEqual:");
        Iterator<String> index = _values.iterator();
        while (index.hasNext()) {
            output.append(" ");
            output.append(index.next());
        }
        return output.toString();
    }

    /* Code to test the class. Insert some random data and then repeatedly
       generate data and print it */
    public static void main(String[] args)
    {
        DataElementValuesEqual test = new DataElementValuesEqual();
        test.insertValue("test1");
        test.insertValue("test2");
        test.insertValue("test3");
        test.insertValue("test4");
        test.insertValue("test5");
        test.insertValue("test6");
        test.insertValue("test7");

        System.out.println("Test equals:" + test);

        int index;
        for (index = 0; index < 20; index++) {
            System.out.println("Next value is: " + test.nextValue());
        }
    }
};