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

/* This class represents a group of records. It's primarily a box class to work
   around Java's lack of typedef's and reduce the complexity of other types.
   Subclassing is the other typical anti-pattern, but I find boxing cleaner. It
   also collects together a number of useful utility methods */
import java.util.*;

public class RecordGroup 
{
    // The actual list this class boxes
    private LinkedList<ArrayList<String> > _records;

    // Create the class from a list of records
    public RecordGroup(LinkedList<ArrayList<String> > records)
    {
        if (records == null)
            throw new IllegalArgumentException("RecordGroup to process passed null");
        _records = records;
    }

    // Create the class from a single record
    public RecordGroup(ArrayList<String> record)
    {
        if (record == null)
            throw new IllegalArgumentException("Record to process passed null");
        _records = new LinkedList<ArrayList<String> >();
        _records.add(record);
    }

    public LinkedList<ArrayList<String> > getRecords()
    {
        return _records;
    }

    // Add a new record to the set. Very common operation
    public void add(ArrayList<String> value)
    {
        _records.add(value);
    }

    // Get number of records. Very common operation
    public int size()
    {
        return _records.size();
    }

    // Detemine if the class is empty. Very common operation
    public boolean isEmpty()
    {
        return _records.isEmpty();
    }

    // Debug trace method to output a record to a string buffer
    public static void traceRecord(ArrayList<String> record,
                                   StringBuffer output)
    {
        int index;
        for (index = 0; index < record.size(); index++) {
            if (index > 0)
                output.append(", ");
            output.append(index);
            output.append(":");
            output.append(record.get(index));
        }
    }

    // Convert the contents of this record group to a multi-line string
    @Override
    public String toString()
    {
        StringBuffer output = new StringBuffer();
        int index;
        for (index = 0; index < _records.size(); index++) {
            traceRecord(_records.get(index), output);
            // Force a line seperator
            output.append(System.getProperty("line.separator"));
        }
        return output.toString();
    }
};

