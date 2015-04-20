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
   synthetic data. It chooses between values defined by a range with equal
   probability */
import java.util.*;

public final class RandomRange implements DataElementInterface
{
    private int _startValue;
    private int _stepAmt;
    private int _valueCount;
    private Random _rand; // Random number generator
    
    /* The object is defined by a range of values, each of which are some step
       amount apart. */
    RandomRange(int lowerLimit, int upperLimit, int stepAmt)
    {
        // First, check for a common configuration error
        // NOTE: Remember that parameters were passed by value
        if (upperLimit < lowerLimit) {
            int temp = upperLimit;
            upperLimit = lowerLimit;
            lowerLimit = temp;
        }

        // Second, another common configuration error
        if (stepAmt < 1)
            stepAmt = 1;

        /* Due to how the random number generator works, storing the number of
           possible increments is much better than storing the actual range */
        _startValue = lowerLimit;
        _stepAmt = stepAmt;
        // Add one for the lower limit itself
        _valueCount = ((upperLimit - lowerLimit) / stepAmt) + 1;

        _rand = new Random();
    }

    // Returns a value from the current list at random
    public String nextValue()
    {
        int newValue = _startValue;
        // If only one value is possible, just return it
        if (_valueCount > 0)
            newValue += (_rand.nextInt(_valueCount) * _stepAmt);
        return Integer.toString(newValue);
    }
        
    // Debug output: print what this object can generate
    public String toString()
    {
        return "RandomRange: start: " + _startValue + " end: " + (_startValue + (_stepAmt * (_valueCount - 1))) + " values count: " + _valueCount;
    }

    /* Code to test the class. Set a range and then repeatedly generate data
       and print it */
    public static void main(String[] args)
    {
        RandomRange test = new RandomRange(10, 24, 3);
        System.out.println("Test equals:" + test);

        int index;
        for (index = 0; index < 20; index++) {
            System.out.println("Next value is: " + test.nextValue());
        }
    }
};