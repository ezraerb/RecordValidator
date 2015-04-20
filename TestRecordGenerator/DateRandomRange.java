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
   synthetic data. It chooses between dates defined by a range with equal
   probability */
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public final class DateRandomRange implements DataElementInterface
{
    private Calendar _startDate;
    private int _stepDays;
    private int _valueCount;
    private Random _rand; // Random number generator
    private SimpleDateFormat _sdf; // String to date converter

    // Public so validation objects have access
    static public String _dateFormat = new String("MM/dd/yyyy"); // m means minutes
    /* The object is defined by a range of dates, each of which are some step
       amount apart. Dates will be output as strings, so this class takes them
       as strings. Date format is defined above in _dateFormat */
    DateRandomRange(String startDate, String endDate, int stepAmt) throws ParseException
    {
        _sdf = new SimpleDateFormat(_dateFormat); // m means minutes
        Calendar firstDate = Calendar.getInstance();
        Calendar secondDate = Calendar.getInstance();
        firstDate.setTime(_sdf.parse(startDate));
        secondDate.setTime(_sdf.parse(endDate));

        // First, check for a common configuration error
        if (secondDate.before(firstDate)) {
            Calendar temp = secondDate;
            secondDate = firstDate;
            firstDate = temp;
        }

        // Second, another common configuration error
        if (stepAmt < 1)
            stepAmt = 1;

        /* Due to how the random number generator works, storing the number of
           possible increments is much better than storing the actual range */
        _startDate = firstDate;
        _stepDays = stepAmt;

        /* The clunky Java pattern of getting the number of days between two
           dates. It works because the dates have no time component */
        long diffInMillis = secondDate.getTimeInMillis() - firstDate.getTimeInMillis();
        int dateRange = (int)TimeUnit.MILLISECONDS.toDays(diffInMillis);

        // Add one for the lower limit itself
        _valueCount = (dateRange / stepAmt) + 1;

        _rand = new Random();
    }

    // Returns a value from the current list at random
    public String nextValue()
    {
        Calendar newValue = (Calendar)_startDate.clone();
        // If only one value is possible, just return it
        if (_valueCount > 0)
            newValue.add(Calendar.DATE, _rand.nextInt(_valueCount) * _stepDays);
        
        /* NOTE: String conversions are expensive, should probably memoize the
           the mapping of random values to dates as a future project */
        return _sdf.format(newValue.getTime());
    }
        
    // Debug output: print what this object can generate
    public String toString()
    {
        StringBuffer output = new StringBuffer();
        output.append("DateRandomRange: start: ");
        /* A clunky anti-pattern for inseting a date into a string buffer.
           Java makes inserting dates directly difficult */
        output.append(_sdf.format(_startDate.getTime()));
        output.append(" end: ");
        Calendar endDate = (Calendar)_startDate.clone();
        endDate.add(Calendar.DATE, _stepDays * (_valueCount - 1));
        output.append(_sdf.format(endDate.getTime()));
        output.append(" values count: " + _valueCount);
        return output.toString();
    }

    /* Code to test the class. Set a range and then repeatedly generate data
       and print it */
    public static void main(String[] args) throws ParseException
    {
        DateRandomRange test = new DateRandomRange("1/23/2010","2/6/2010", 3);
        System.out.println("Test equals:" + test);

        int index;
        for (index = 0; index < 20; index++) {
            System.out.println("Next value is: " + test.nextValue());
        }
    }
};