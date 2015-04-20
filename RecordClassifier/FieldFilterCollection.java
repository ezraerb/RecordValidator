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

/* This class represents a collection of field filter groups. If any group
   passes a given record, it passes the collection. In effect, a field filter
   group creates AND conditions on the filters it contains, while the collection
   creates OR conditions on the groups it contains */
import java.util.*;

public class FieldFilterCollection implements Iterable<FieldFilterGroup>
{
    // The actual list this class wraps
    private LinkedList<FieldFilterGroup> _filters;

    // Create the class empty
    public FieldFilterCollection()
    {
        _filters = new LinkedList<FieldFilterGroup>();
    }

    // Create the class from a list of filter groups
    public FieldFilterCollection(LinkedList<FieldFilterGroup> filters)
    {
        if (filters == null)
            throw new IllegalArgumentException("Filter groups to process passed null");
        _filters = filters;
    }

    // Create the class from a single filter group
    public FieldFilterCollection(FieldFilterGroup filter)
    {
        if (filter == null)
            throw new IllegalArgumentException("Filter Group to process passed null");
        _filters = new LinkedList<FieldFilterGroup>();
        _filters.add(filter);
    }

    // Add a new filter to the set.
    public void add(FieldFilterGroup filter)
    {
        if (filter == null)
            throw new IllegalArgumentException("Filter Group to add passed null");
        _filters.add(filter);
    }

    // Merge another filter collection into this one
    public void add(FieldFilterCollection other)
    {
        if (this == other)
            throw new IllegalArgumentException("Filter Group to merge is self!");
        else if (other == null)
            throw new IllegalArgumentException("Filter Group to process passed null");
        _filters.addAll(other._filters);
    }
    
    // Get number of filters.
    public int size()
    {
        return _filters.size();
    }

    // Determine whether any filter group in the collection passes a record
    public boolean passes(List<String> record)
    {
        if (record == null)
            return false; // No record!
        boolean pass = false;
        Iterator<FieldFilterGroup> index = _filters.iterator();
        while (index.hasNext() && (!pass))
            pass = index.next().passes(record);
        return pass;
    }

    // Return an iterator over the list of filters
    @Override
    public Iterator<FieldFilterGroup> iterator()
    {
        return _filters.iterator();
    }

    // Convert the contents of this record group to a multi-line string
    @Override
    public String toString()
    {
        StringBuffer output = new StringBuffer();
        Iterator<FieldFilterGroup> index = _filters.iterator();
        while (index.hasNext()) {
            output.append(index.next().toString());
            // Force a line seperator
            output.append(System.getProperty("line.separator"));
        }
        return output.toString();
    }
};

