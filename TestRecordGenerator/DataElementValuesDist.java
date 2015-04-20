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
   synthetic data. It chooses between a set of predefined elements with defined
   probability */

import java.util.*;

public class DataElementValuesDist implements DataElementInterface
{
    /* This class divides the range of numbers from the number generator into
       sub-ranges, one for each value to return. Searching this range then gets
       the index of the value to return. The input probabilities are not
       guarenteed to add up to 1, so they are normalized when elements are
       fetched. For performance reasons, the normalized values are cached. */
    private ArrayList<String> _values;
    private ArrayList<Double> _probability; // Probability for each value
    private ArrayList<Double> _normProbability; // Normalized values of above

    private Random _rand; // Random number generator
    
    // Construct the object empty
    DataElementValuesDist()
    {
        _values = new ArrayList<String>();
        _probability = new ArrayList<Double>();
        _normProbability = null;
        _rand = new Random();
    }

    /* Insert a new value into the list. It does not check for dupes, which
       means elements inserted more than once will have higher probability of'
       being chosen. Negative probabilities cause the entry to be ignored, since
       it can't be chosen whith such a probability */
    public void insertValue(String value, double probability)
    {
        if (probability > 0) {
            _values.add(value);
            _probability.add(probability);
            // A new probability invalidates the cache
            _normProbability = null;
        }
    }
    
    // Calculates normalized cumulative probabilities
    private void normalizeProbabilities()
    {
        // If have no probabilities, nothing to do!
        if (!_probability.isEmpty()) {
            double total = 0.0;
            for(Double d: _probability)
                total += d;
            _normProbability = new ArrayList<Double>();
            /* For efficient searching, the normalized probability should be the
               accumulated probability, so the random value can be directly
               used as a search key */
            double valueSoFar = 0.0;
            for(Double d: _probability) {
                valueSoFar += d;
                _normProbability.add(valueSoFar / total);
            }
        }
    }

    // Returns a value from the current list at random
    public String nextValue()
    {
        // If the list is empty, return an empty value
        if (_values.isEmpty())
            return new String();
        else {
            // Normalize probability if needed
            if (_normProbability == null)
                normalizeProbabilities();
            /* Binary search the next random seed in the normalized
               probabilities. In all likelyhood it won't match exactly, but in
               that case it returns where the value would be inserted which is
               just as good */
            int valueIndex = Collections.binarySearch(_normProbability, _rand.nextDouble());
            if (valueIndex < 0)
                /* Does not fall on a boundry. In this case the index is one
                   higher than the first entry greater than the key, negated.
                   In this case, that element is the upper end of the range
                   containing the probability, which is the wanted element */
                valueIndex = -(valueIndex + 1);
            return _values.get(valueIndex);
        } // Have values
    }
        
    // Debug output: print what this object can generate
    public String toString()
    {
        StringBuffer output = new StringBuffer();
        output.append("DataElementValuesDist:");
        int index;
        for (index = 0; index < _values.size(); index++) {
            output.append(" ");
            output.append(_values.get(index));
            output.append(":");
            output.append(_probability.get(index));
        }
        return output.toString();
    }

    /* Code to test the class. Insert some random data and then repeatedly
       generate data and print it */
    public static void main(String[] args)
    {
        DataElementValuesDist test = new DataElementValuesDist();
        test.insertValue("test1", 2.5);
        test.insertValue("test2", 1.0);
        test.insertValue("test3", 0.75);
        test.insertValue("test4", 0.5);
        test.insertValue("test5", 0.25);

        System.out.println("Test equals:" + test);

        int index;
        for (index = 0; index < 25; index++) {
            System.out.println("Next value is: " + test.nextValue());
        }
    }
};