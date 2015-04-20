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

/* This class represents training records for field filter group generation and
   the filters created from those records. As filters are processsed they get
   removed taking their training records with them. The remaining filters can
   then be made more specific based on the remaining records. Processing must
   stop when the class has no records or the filter groups are as specific as
   possible */
import java.util.*;

public class TrainingRecords
{
    /* This class must efficiently generate the initial set of filter groups,
       generate more specific filter groups from less specific ones, and remove
       filter groups plus the records they filter. To avoid repeatedly scanning
       the set of training records to generate filters, the last two items
       require a mapping between the filter groups and the records they effect
       (remember a single record will get selected by multiple filter groups).
       Its implemented as hashmap of FieldFilterGroups to RecordGroups, which
       has efficiency on order of the average number of records selected by
       each filter */
    private HashMap<FieldFilterGroup, RecordGroup> _recordsByFilter;

    /* When removing a training record, it must be removed from all filter
       groups remaining that select it. Regenerating the groups is O(N^F)
       where N is the number of fields per record and F is the number of
       filters per filter group. This rapidly gets expensive, implying the
       need for a reverse index of records to filter groups. Training records
       do not change once inserted in the object, so the default hash on the
       memory address is sufficient */
    private HashMap<ArrayList<String>, FieldFilterCollection> _filtersByRecord;

    /* Filters which are returned for processing should not be processed again.
       Can either mark them as they go or keep a set of them. Resetting the
       state happens regularly and is easier with a set */
    private HashSet<FieldFilterGroup> _ignoredFilters;

    /* The last filter returned for processing. Cached mostly so delete works
       properly */
    private FieldFilterGroup _lastReturnedFilterGroup;

    // The list of record fields that are used for classification
    private int[] _classifyFields;

    /* Field filter groups are generated in such a way that every one in the
       object is the same size, which allows optimizing many operations. This
       tracks the current size */
    private int _filterGroupSize;

    /* Given a single record, returns an array of all single filter groups
       that would select that record */
    private static FieldFilterCollection getFilters(ArrayList<String> record,
                                                    int[] classifyFields)
    {
        // The filters are one for each field in the record
        FieldFilterCollection result = new FieldFilterCollection();

        /* Generate the filters so that the lowest numbered field is first in
           the list. This optimizes operations elsewhere */
        int index;
        for (index = 0; index < classifyFields.length; index++)
            result.add(new FieldFilterGroup(new FieldFilter(record, classifyFields[index])));
        return result;
    }
    
    /* Given a single record and an existing filter, returns an array of all
       filter groups one size larger that would select that record */
    private FieldFilterCollection getFilters(ArrayList<String> record,
                                             FieldFilterGroup filter,
                                             int[] classifyFields)
    {
        /* SANITY CHECK: If the filter does not pass the record, have a huge
           problem */
        if (!filter.passes(record))
            throw new IllegalStateException("Internal state inconsistent, attempt to generate further filters from one that does not pass record");

        /* The filters are one for each field in the record beyond the last
           already in the filter group. Remember that every field used for
           classification must appear in the record somewhere */
        int firstNewField = Arrays.binarySearch(classifyFields,
                                                filter.getLastFilterField() + 1);
        if (firstNewField < 0)
            /* The next field is not part of those used for classification.
               Thankfully, the method returns instead the index of the next
               highest position, although it takes some manipulation to get
               it */
            firstNewField = -(firstNewField + 1);
        
        int filtersNeeded = classifyFields.length - firstNewField;

        /* SANITY CHECK: If the number of filters needed is zero or less, this
           implies the current filter group contains the last field of the
           record */
        if (filtersNeeded <= 0)
            throw new IllegalStateException("Internal state inconsistent, attempt to generate further filters from one that matches last field of record " + classifyFields[classifyFields.length - 1]);

        FieldFilterCollection result = new FieldFilterCollection();

        /* Generate the filters so that the lowest numbered field is first in
           the list. This optimizes operations elsewhere */
        int index;
        for (index = 0; index < filtersNeeded; index++)
            result.add(new FieldFilterGroup(filter, new FieldFilter(record, classifyFields[firstNewField + index])));
        return result;
    }
    
    /* Given a single record and a list of filter groups, insert that record
       into the hash under each of those filter groups. The hash must already
       be initialized. WARNING: Does not verify that the filter groups
       actually pass the record! */
    private void insertRecord(ArrayList<String> record, FieldFilterCollection filters)
    {
        /* SANITY CHECK: The filter array has positive size */
        if (filters.size() == 0)
            throw new IllegalStateException("Internal state inconsistent, set of filters generated from a record is empty");

        Iterator<FieldFilterGroup> index = filters.iterator();
        while (index.hasNext()) {
            FieldFilterGroup nextFilter = index.next();
            if (_recordsByFilter.containsKey(nextFilter))
                // Add the record to the record group for this key
                _recordsByFilter.get(nextFilter).getRecords().add(record);
            else
                // Create a new group and add it with this key
                _recordsByFilter.put(nextFilter, new RecordGroup(record));
        } // For each filter

        // Insert the filters into the reverse index for the record
        if (!_filtersByRecord.containsKey(record))
            _filtersByRecord.put(record, new FieldFilterCollection());
        _filtersByRecord.get(record).add(filters);
    }

    /* Initialze the filter set. Generate one filter for every non-excluded 
       field in each record and insert. If the exclusion field list is 
       passed NULL, every field will be used */
    public TrainingRecords(RecordGroup records, int[] excludeFields)
    {
        if (records == null)
            throw new IllegalArgumentException("Training records for filter generation passed null");
        if (records.getRecords().size() == 0)
            throw new IllegalArgumentException("Training records for filter generation passed empty");

        LinkedList<ArrayList<String> > recordList = records.getRecords();

        /* To optimize class operations, it requires all training records to
           have the same field count. In practice, record sets with mixed
           numbers of fields are so rare they are almost certainly a data error
           anyway */
        boolean valid = true;
        int recordSize = recordList.peekFirst().size();
        ListIterator<ArrayList<String> > recordIndex = recordList.listIterator(0);
        while (recordIndex.hasNext() && valid)
            valid = recordIndex.next().size() == recordSize;
        
        if (!valid)
            throw new IllegalArgumentException("Training records invalid, must all be the same size");
        if (recordSize == 0)
            throw new IllegalArgumentException("Filter generation failed, training records have no fields");

        /* Although the passed field list is those to exclude, the class
           operates much more efficiently on a list of which fields to use.
           Convert one into the other. If no list was passed, simply take
           every field in the record in increasing order */
        if (excludeFields == null) {
            _classifyFields = new int[recordSize];
            int temp;
            for (temp = 0; temp < recordSize; temp++)
                _classifyFields[temp] = temp;
        }
        else {
            Arrays.sort(excludeFields);

            /* Since the final size is unknown, build the field list as an
               ArrayList and convert */
            ArrayList<Integer> fieldList = new ArrayList<Integer>();

            int index = 0;
            int fieldIndex = 0;
            while (fieldIndex < recordSize) {
                while ((index < excludeFields.length) &&
                       (excludeFields[index] < fieldIndex))
                    index++;
                if ((index >= excludeFields.length) ||
                    (excludeFields[index] != fieldIndex))
                    fieldList.add(new Integer(fieldIndex));
                fieldIndex++;
            }
            if (fieldList.size() == 0)
                throw new IllegalArgumentException("Field exclusion list for training data excludes all record fields!");

            // The standard Java pattern for converting to an array of primitives
            _classifyFields = new int[fieldList.size()];
            Iterator<Integer> temp = fieldList.iterator();
            for (index = 0; index < fieldList.size(); index++)
                _classifyFields[index] = temp.next().intValue();
        } // List of classification fields specified
            
        /* The size of the hash needs to be the number of unique filters
           generated by the training set. Its equal to the number of records
           times the number of fields divided by the average number of records
           that share a field value, raised to the power of the typical size
           of the filter set needed to find uniqueness. The third value depends
           on the number of valid values per field and varies significantly.
           The fourth depends on the overlap of field values in each category
           being classified. This code uses a conservative estimate to minimize
           the risk of rehashing. */
        int estSize = recordList.peekFirst().size() * 10;
        _recordsByFilter = new HashMap<FieldFilterGroup, RecordGroup>(estSize); 
        _filtersByRecord = new HashMap<ArrayList<String>, FieldFilterCollection>(recordList.size());
        
        /* Iterate through the list and insert */
        recordIndex = recordList.listIterator(0);
        while (recordIndex.hasNext()) {
            ArrayList<String> procRecord = recordIndex.next();
            insertRecord(procRecord, getFilters(procRecord, _classifyFields));
        }
        _filterGroupSize = 1;

        _ignoredFilters = new HashSet<FieldFilterGroup>();
        _lastReturnedFilterGroup = null;
    }
        
    /* Returns true if there are no more records to process */
    public boolean isEmpty()
    {
        return _recordsByFilter.isEmpty();
    }
    
    /* Returns true if any filter in the set is the same size of at least one
       record that it filters (implying the filter set can not become any more
       specific) */
    public boolean oneFiltersAllFields()
    {
        /* All training records are the same size, and the size of filters
           are all increased in step. This method only needs to compare the
           filter size to the record size.
           TRICKY NOTE: How does this method being false guarentee filters can
           become more specific? Its possible for a filter to filter on the
           last allowable field of a record without filtering every field.
           The existence of this filter in the set implies at least one record
           it filters is still in the training set. The way filters are
           generated, that implies that at least one filter that filters on
           only earlier fields is ALSO in the set, unless the filter filters
           every record field */
        return (_filterGroupSize >= _classifyFields.length);
    }
    
    /* Make the set of filters more specific by increasing the size by one, to
       filter all training records remaining. */
    public void incrFilterSpecificity()
    {
        /* If the operation fails for any reason, restore the original state so
           it remains consistent */
        HashMap<FieldFilterGroup, RecordGroup> currentFilters = _recordsByFilter;
        HashMap<ArrayList<String>, FieldFilterCollection> currentRecords = _filtersByRecord;
        HashSet<FieldFilterGroup> ignoredFilters = _ignoredFilters;
        
        try {
            /* The size of the new map will be equal the size of the number of
               filter groups it contains times the number of fields in the
               records used for classification beyond the current size of the
               filter, times the average number of sets the next field will
               divide those records into. The last item is equal to the number
               of valid values per filter. This code takes a conservative
               estimate to reduce the risk of rehashing. */
            int needSize = _classifyFields.length - _filterGroupSize;
            needSize *= (_recordsByFilter.size() * 10);
            _recordsByFilter = new HashMap<FieldFilterGroup, RecordGroup>(needSize);
            _filtersByRecord = new HashMap<ArrayList<String>, FieldFilterCollection>(currentRecords.keySet().size());
            Iterator<Map.Entry<FieldFilterGroup, RecordGroup> > filterIndex = currentFilters.entrySet().iterator();
            while (filterIndex.hasNext()) {
                /* For every record, generate every possible filter one larger
                   and insert into the map. Keep in mind that multiple records
                   can generate the same filter; the insert routine handles
                   collating them properly */
                Map.Entry<FieldFilterGroup, RecordGroup> value = filterIndex.next();
                ListIterator<ArrayList<String> > recordIndex = value.getValue().getRecords().listIterator(0);
                while (recordIndex.hasNext()) {
                    ArrayList<String> record = recordIndex.next();
                    /* If the highest field of the filter equals the last field
                       used for classification, the filter can't be made any
                       more specific so ignore it. */
                    if (value.getKey().getLastFilterField() < _classifyFields[_classifyFields.length - 1])
                        insertRecord(record, getFilters(record, value.getKey(),
                                                        _classifyFields));
                    /* If the size of the curent filter group is equal to the
                       number of fields used for classification, expanding the
                       filter group is illegal because this record will be
                       dropped from the training set unprocessed (Since every
                       field in the record used for classification is filtered
                       by this group, its the only one that currently
                       filters it) */
                    else if (value.getKey().filterCount() == _classifyFields.length)
                        throw new IllegalStateException("Attempt to make filters more specific invalid, at least one training record dropped");
                } // While records for the current key to process
            } // While current filter groups to process

            /* If the set of filters is empty at this point, all training
               records were dropped which is a huge problem. It should have
               been caught by the exception test above, indicating a code
               error */
            if (_recordsByFilter.isEmpty())
                throw new IllegalStateException("Attempt to make filters more specific internal error, no filters generated");
            _filterGroupSize++;

            // Filter groups are now all new, so reset processing state
            _ignoredFilters = new HashSet<FieldFilterGroup>();
            _lastReturnedFilterGroup = null; // Must be last in try block
        } // Try block
        catch (RuntimeException e) { // Interior method should only throw runtime exceptions
            _recordsByFilter = currentFilters;
            _filtersByRecord = currentRecords;
            _ignoredFilters = ignoredFilters;
            throw e;
        }
    }
    
    /* Returns true if a given filter exists within the set of filters in this
       object */
    public boolean hasFilterGroup(FieldFilterGroup filter)
    {
        if (filter == null)
            return false;
        return _recordsByFilter.containsKey(filter);
    }
    
    /* Returns the filter group with the largest number of training records
       to process. Returns NULL if none remain */
    public FieldFilterGroup getLargestFilter()
    {
        // Clear filter processing state and find next largest
        _ignoredFilters = new HashSet<FieldFilterGroup>();
        _lastReturnedFilterGroup = null;
        return getNextLargestFilter();
    }

    /* Returns the filter group with the largest number of training records
       smaller than the last filter returned. Returns NULL if none remain */
    public FieldFilterGroup getNextLargestFilter()
    {
        /* This method acts remarkably like an iterator over the filter groups.
           Its not implemented as an iterator because a delete causes changes
           to many filter groups, making concurrency almost impossible

           This method needs to return the filter group NOT on the ignore list
           that has the largest number of records. Can handle this one of two
           ways: scan for it every single time, or keep a sorted heap of
           filters and update it as filters are deleted. The performance works
           out as follows:
           F = number of fields per record
           V = average number of valid values per field
           N = number of records
           D = ratio of filters which are deleted to total filters

           In practice, the filtes will be processed sequentually and some
           will be deleted. The largest number are normally deleted on the
           first pass, so the number of single field filters dominates the run
           time. That number is FV.

           Scan costs for processing all filters is (FV)^2, all on lookups.
           The heap only costs on delete. It must be updated every time a
           record is removed from the list for any filter. Assuming an even
           distribution of possible values, each filter will have N/V records
           Each of those must be removed from F filters, giving FN/V heap
           updates. These happen DFV times when all filters are processed, and
           each costs log(FV). The total cost is DF^2Nlog(FV).
           (FV)^2 vs DF^2Nlog(FV). Multiply both sides by 1/NF and get
           V^2/N vs Dlog(FV)
           As the data set gets big, V and F will grow slowly while N grows
           quickly. D is essentially constant. Scanning the records every time
           will be cheaper than keeping a heap thanks to the number of updates
           it would need */

        /* If the last filter processed is not null, add it to the ignore set
           so it doesn't get returned again */
        if (_lastReturnedFilterGroup != null)
            _ignoredFilters.add(_lastReturnedFilterGroup);
            
        if (isEmpty())
            _lastReturnedFilterGroup = null; // No records!
        // If every filter group should be ignored, can't return any
        else if (_recordsByFilter.size() == _ignoredFilters.size())
            _lastReturnedFilterGroup = null; // Everything should be ignored
        else {
            /* Iterate through the hash map of filters looking for the one with
               the largest number of records that should not be ignored. The
               test above guarentees one exists, so not finding it equals a
               consistency problem */
            Iterator<Map.Entry<FieldFilterGroup, RecordGroup> > index = _recordsByFilter.entrySet().iterator();
            int maxEntryCount = 0;
            _lastReturnedFilterGroup = null;
            while (index.hasNext()) {
                Map.Entry<FieldFilterGroup, RecordGroup> next = index.next();
                if ((!_ignoredFilters.contains(next.getKey())) &&
                    (next.getValue().size() > maxEntryCount)) {
                    _lastReturnedFilterGroup = next.getKey();
                    maxEntryCount = next.getValue().size();
                }
            }
            if (_lastReturnedFilterGroup == null)
                // Nothing found
                throw new IllegalStateException("Mismatch between filters to ignore for processing and training filters remaining");
        } // Filter to return should exist
        return _lastReturnedFilterGroup;
    }

    /* Deletes the last filter group returned, and the training records it
       filters, from the training set. This may also remove other filter groups
       if they have no training records remaining afterward. It then returns
       the next largest filter group after the one deleted, or NULL if none
       remain
       WARNING: This method greatly depends on the cross-index data in the
       class being consistent. If it is not, the method will throw exceptions
       and the data will become even more inconsistent. Any exception should
       be allowed to propagate for that reason. */
    public FieldFilterGroup deleteLastFilterGroup()
    {
        /* Since this method retuns the next filter to process after a delete,
           having no filter at this point implies there are none left. Return
           NULL and quit */
        if (_lastReturnedFilterGroup == null)
            return null;
        
        else {
            /* Extract the list of current records for the entry, and then
               delete it */
            RecordGroup currRecords = _recordsByFilter.get(_lastReturnedFilterGroup);
            if (currRecords == null)
                /* Serious, unrecoverable problem. The object state is not
                   consistent */
                throw new IllegalStateException("Filter group to delete does not exist in training data");

            _recordsByFilter.remove(_lastReturnedFilterGroup);

            /* NOTE: Don't need to remove it from the ignore list, since the
               filter could only be set if it was not on that list */

            /* Now remove the training records for the iterator. Find them in
               the reverse index, remove the record from each of those filters
               lists, and then delete the reverse index entry. This operation
               can fail if the index data is not consistent, in which case it
               will become even more inconsistent. */
            Iterator<ArrayList<String> > recordIter = currRecords.getRecords().iterator();
            FieldFilterCollection recordFilters = null;
            while (recordIter.hasNext()) {
                ArrayList<String> testRecord = recordIter.next();
                /* Find it in the reverse index. Not finding it indicates the
                   two indexes are not syncronized, a serious data corruption */
                recordFilters = _filtersByRecord.get(testRecord);
                if (recordFilters == null)
                    throw new IllegalStateException("Training data invalid; record in filter index missing from record index");
                Iterator<FieldFilterGroup> filterIter = recordFilters.iterator();
                while (filterIter.hasNext()) {
                    FieldFilterGroup testFilter = filterIter.next();
                    /* If this filter is the one being removed, it was already
                       handled above */
                    if (!testFilter.equals(_lastReturnedFilterGroup)) {
                        RecordGroup testRecordGroup = _recordsByFilter.get(testFilter);
                        /* Not finding the record group indicates the two
                           indexes are not in sync and the class is corrupted */
                        if (testRecordGroup == null)
                            throw new IllegalStateException("Training data invalid; filter in record index missing from filter index");
                        if (testRecordGroup.size() > 1)
                            /* Remove the record from the list for this group.
                               Record pointers are duplicated between groups in
                               the filter group map, so just search for it
                               instead of comparing the values */
                            testRecordGroup.getRecords().remove(testRecord);
                        else {
                            // Remove the filter group, last record removed
                            _recordsByFilter.remove(testFilter);
                            // Remove it from the filters to ignore if present
                            _ignoredFilters.remove(testFilter);
                        }
                    } // Test filter group for record is not one being removed
                } // For all possible filter groups for current record to remove
                // Record removed from all filters, delete from record index
                _filtersByRecord.remove(testRecord);
            } // For each training reocrd for filter group to remove

            // Entry deleted, so clear cached value
            _lastReturnedFilterGroup = null;

            // Find the next filter group to process
            return getNextLargestFilter();
        } // Filter group exists to delete
    } 
        
    // Print the filters the class contains with record counts
    /* NOTE: The records themselves are omitted to keep the output size
       resonable. To get full data fetch filter groups one by one and call
       debugRecordsForFilter() on each one */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        Iterator<Map.Entry<FieldFilterGroup, RecordGroup> > index = _recordsByFilter.entrySet().iterator();
        while (index.hasNext()) {
            Map.Entry<FieldFilterGroup, RecordGroup> value = index.next();
            buffer.append(value.getKey());
            buffer.append(":");
            buffer.append(value.getValue().getRecords().size());
            buffer.append(" records ");
        }
        buffer.append("]");
        return buffer.toString();
    }
    
    // If a filter group exists within the training data, print its records
    public String debugRecordsForFilter(FieldFilterGroup filter)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Filter: ");
        if (filter == null)
            buffer.append("none");
        else {
            buffer.append(filter);
            // Force a newline
            buffer.append(System.getProperty("line.separator"));
            buffer.append(" Records: ");
            RecordGroup records = _recordsByFilter.get(filter);
            if (records == null)
                buffer.append("None");
            else
                buffer.append(records);
        } // Filter passed
        return buffer.toString();
    } 

    // Print the records for the last filter retuned to process
    public String debugLastReturnedFilterRecords()
    {
        return debugRecordsForFilter(_lastReturnedFilterGroup);
    }

    // Utility method to create a record with four fields in it, in order
    private static ArrayList<String> createTestRecord(String value1,
                                                      String value2,
                                                      String value3,
                                                      String value4)
    {
        ArrayList<String> result = new ArrayList<String>();
        result.add(value1);
        result.add(value2);
        result.add(value3);
        result.add(value4);
        return result;
    }

    // Class test code
    public static void main(String[] args)
    {
        /* Test records. They must have the same number of fields. Need to cover
           all of the following:
           1. Field where one record has a unique value
           2. Field where multiple records have the same value
           3. Field that has the same value as another field. */
        ArrayList<String> record1 = createTestRecord("test1", "test6", "test1",
                                                     "test8");
        ArrayList<String> record2 = createTestRecord("test5", "test6", "test3",
                                                     "test4");
        ArrayList<String> record3 = createTestRecord("test5", "test6", "test1", 
                                                     "test9");
        
        RecordGroup testData = new RecordGroup(createTestRecord("test1", "test2",
                                                                "test3", "test4"));
        testData.add(record1);
        testData.add(record2);
        testData.add(record3);
        testData.add(createTestRecord("test5", "test7", "test3", "test4"));
        
        // Construct training data with no records. Expect exception
        System.out.println("Training data with no records. Expect exception");
        try {
            TrainingRecords badTest = new TrainingRecords(null, null);
            System.out.println("Records: " + badTest);
            System.out.println("Test failed");
        }
        catch (Exception e) {
            System.out.println("Test passed, caught exception: " + e);
        }

        // Construct training data where all fields are excluded. Expect exception
        int [] excludeFields = new int[4];
        excludeFields[0] = 0;
        excludeFields[1] = 1;
        excludeFields[2] = 2;
        excludeFields[3] = 3;
        System.out.println("Training data with all fields excluded. Expect exception");
        try {
            TrainingRecords badTest = new TrainingRecords(testData, excludeFields);
            System.out.println("Records: " + badTest);
            System.out.println("Test failed");
        }
        catch (Exception e) {
            System.out.println("Test passed, caught exception: " + e);
        }

        TrainingRecords test = new TrainingRecords(testData, null);

        System.out.println("Test group: " + test);

        System.out.println("Is empty, epect false");
        if (!test.isEmpty())
            System.out.println("Not empty, succeeded");
        else
            System.out.println("Empty, failed");

        // Generate some filter groups and test for them
        FieldFilterGroup testFilter1 = new FieldFilterGroup(new FieldFilter(record1, 1));
        FieldFilterGroup testFilter2 = new FieldFilterGroup(new FieldFilter(record3, 2));
        FieldFilterGroup testFilterMult1 = new FieldFilterGroup(testFilter1, new FieldFilter(record1, 0));

        System.out.println("test for " + testFilter1 + " expect find it");
        if (test.hasFilterGroup(testFilter1))
            System.out.println("Filter found, succeeded");
        else
            System.out.println("Filter not found, failed");
        System.out.println("test for " + testFilter2 + " expect find it");
        if (test.hasFilterGroup(testFilter2))
            System.out.println("Filter found, succeeded");
        else
            System.out.println("Filter not found, failed");

        System.out.println("test for " + testFilterMult1 + " expect NOT find it");
        if (!test.hasFilterGroup(testFilterMult1))
            System.out.println("Filter not found, succeeded");
        else
            System.out.println("Filter found, failed");

        // The filter can become more specific at this point
        System.out.println("Test if filters can become more specific, expect yes");
        if (test.oneFiltersAllFields())
            System.out.println("Test fails, states all fields are filtered for one record");
        else
            System.out.println("Test succeeded, not all fields are filtered for all records");

        // Make fields more specific.
        System.out.println("Make filters more specific, expect valid result");
        try {
            test.incrFilterSpecificity();
            System.out.println("Records filter state: " + test);
        }
        catch (RuntimeException e) {
            System.out.println("Test failed, caught " + e);
            // Remaining tests will fail at this point, so quit
            System.exit(1);
        }

        /* Look up entires. This time the single field group fails and the 
           multi-field group succeeds */
        System.out.println("test for " + testFilter1 + " expect NOT find it");
        if (!test.hasFilterGroup(testFilter1))
            System.out.println("Filter not found, succeeded");
        else
            System.out.println("Filter found, failed");

        System.out.println("test for " + testFilterMult1 + " expect find it");
        if (test.hasFilterGroup(testFilterMult1))
            System.out.println("Filter found, succeeded");
        else
            System.out.println("Filter not found, failed");

        /* Iterate through the class using the access methods above. The filter
           tested above must appear in the results. Remember that order is not
           guarenteed */
        System.out.println("Iterate through class searching for filter group " + testFilterMult1);
        FieldFilterGroup filterIndex1 = test.getLargestFilter();
        while ((filterIndex1 != null) && (!filterIndex1.equals(testFilterMult1))) {
            System.out.println(test.debugLastReturnedFilterRecords());
            filterIndex1 = test.getNextLargestFilter();
        }
        
        // Record what was ultimately found
        System.out.println(test.debugLastReturnedFilterRecords());

        if (filterIndex1 != null)
            System.out.println("Test successful, found filter");
        else
            System.out.println("Test failed, filter not found");

        System.out.println("Current test state " + test);
        System.out.println("Delete current record");
        test.deleteLastFilterGroup();
        System.out.println("After test state " + test);
        if (test.hasFilterGroup(testFilterMult1))
            System.out.println("Filter found, delete failed");
        else {
            System.out.println("Deleted filter not found");
            /* The removal of the filter removed the record as well. That caused
               certain filters to disappear because they only filtered that
               record. Test for a few of them. */
            int[] testFields = new int[2];
            testFields[0] = 0;
            testFields[1] = 2;
            FieldFilterGroup testFilterMult2 = new FieldFilterGroup(record1, testFields);
            testFields[1] = 3;
            FieldFilterGroup testFilterMult3 = new FieldFilterGroup(record1, testFields);
            testFields[0] = 1;
            testFields[1] = 2;

            /* One filter originally had two records, including the one
               deleted above. Confirm it still exists */
            FieldFilterGroup testFilterMult4 = new FieldFilterGroup(record1, testFields);

            System.out.println("Expect that the delete removed filters " + testFilterMult2 + " and " + testFilterMult3);
            System.out.println("Expect that the delete kept filter " + testFilterMult4);
            if (test.hasFilterGroup(testFilterMult2))
                System.out.println("First filter found, delete failed");
            else if (test.hasFilterGroup(testFilterMult3))
                System.out.println("Second filter found, delete failed");
            else if (!test.hasFilterGroup(testFilterMult4))
                System.out.println("Third filter NOT found, delete failed");
            else {
                System.out.println("Filter as expected after delete");

                /* Now, delete a DIFFERENT filter referencing the other (now
                   sole remaining) record for the third filter and confirm that
                   this filter is also deleted. A failure implies that the
                   first delete did not update the record list properly */
                testFields[0] = 0;
                testFields[1] = 1;
                FieldFilterGroup testFilterMult5 = new FieldFilterGroup(record3, testFields);
                System.out.println("Expect that the delete of " + testFilterMult5 + " also removes " + testFilterMult4);

                FieldFilterGroup filterIndex2 = test.getLargestFilter();
                while ((filterIndex2 != null) && (!filterIndex2.equals(testFilterMult5)))
                    filterIndex2 = test.getNextLargestFilter();
        
                if (filterIndex2 == null)
                    System.out.println("Test failed, filter " + testFilterMult5 + " not found");
                else {
                    test.deleteLastFilterGroup();
                    System.out.println("Training records state " + test);
                    if (test.hasFilterGroup(testFilterMult4))
                        System.out.println("Third filter found, original delete failed");
                    else
                        System.out.println("Delete works successfully");
                    /* At this point, the object has filters of size two. Make
                       the filters more specific twice and test if they can
                       be made more specific yet. The answer should be no. */
                    try {
                        System.out.println("training record state: " + test);
                        test.incrFilterSpecificity();
                        test.incrFilterSpecificity();
                        System.out.println("training record state: " + test);
                        if (test.oneFiltersAllFields())
                            System.out.println("Test succeeds, all fields filtered for records");
                        else
                            System.out.println("Test failed, states not all fields filtered");
                        try {
                            System.out.println("Make filters more specific, expect exception");
                            test.incrFilterSpecificity();
                            System.out.println("training record state: " + test);
                            System.out.println("Test fails, exception should have been thrown");
                        }
                        catch (RuntimeException e) {
                            System.out.println("Test succeeded, caught " + e);
                        }
                    } // Outer try block
                    catch (RuntimeException e) {
                        System.out.println("Test failed, caught unexpected exception " + e);
                    }
                } // Found second group to delete
            } // Results for other filter groups as expected
        } // First filter group deleted as expected

        /* Create a second training data that only considers some of the
           fields. Test which have filters.
           NOTE: The last field in the records must NOT be included for this
           test to work properly */
        excludeFields[0] = 3;
        excludeFields[1] = -1;
        excludeFields[2] = 3;
        excludeFields[3] = 1;
        TrainingRecords test2 = new TrainingRecords(testData, excludeFields);
        FieldFilterGroup testFilter3 = new FieldFilterGroup(new FieldFilter(record2, 2));
        FieldFilterGroup testFilter4 = new FieldFilterGroup(new FieldFilter(record1, 1));

        System.out.println("Construct training data based only in a subset of record fields");
        System.out.println("Current data state: " + test2);
        System.out.println("Expect filter to exist: " + testFilter3);
        if (test2.hasFilterGroup(testFilter3))
            System.out.println("Filter found, succeeded");
        else
            System.out.println("Filter not found, failed");
        
        System.out.println("Expect filter to NOT exist: " + testFilter4);
        if (!test2.hasFilterGroup(testFilter4))
            System.out.println("Filter not found, succeeded");
        else
            System.out.println("Filter found, failed");
        System.out.println("Test can make filters more specific, expect success");
        if (test2.oneFiltersAllFields())
            System.out.println("Test fails, states all fields are filtered for one record");
        else
            System.out.println("Test succeeded, not all fields are filtered for all records");

        // Make fields more specific.
        System.out.println("Make filters more specific, expect valid result");
        try {
            test2.incrFilterSpecificity();
            System.out.println("Records filter state: " + test2);
        }
        catch (RuntimeException e) {
            System.out.println("Test failed, caught " + e);
            // Remaining tests will fail at this point, so quit
            System.exit(1);
        }
        // Test for two field filters
        FieldFilterGroup testFilter5 = new FieldFilterGroup(testFilter3, new FieldFilter(record2, 0));
        FieldFilterGroup testFilter6 = new FieldFilterGroup(testFilter4, new FieldFilter(record1, 0));

        System.out.println("Expect filter to exist: " + testFilter5);
        if (test2.hasFilterGroup(testFilter5))
            System.out.println("Filter found, succeeded");
        else
            System.out.println("Filter not found, failed");
        
        System.out.println("Expect filter to NOT exist: " + testFilter6);
        if (!test2.hasFilterGroup(testFilter6))
            System.out.println("Filter not found, succeeded");
        else
            System.out.println("Filter found, failed");

        /* Only two filters were considered for classification, so the filters
           can't be made more specific at this point */
        System.out.println("Test can make filters more specific, expect they can't");
        if (!test2.oneFiltersAllFields())
            System.out.println("Test fails, not all fields are filtered for all records");
        else
            System.out.println("Test succeeds, states all fields are filtered for one record");

        // Make fields more specific.
        System.out.println("Make filters more specific, expect exception");
        try {
            test2.incrFilterSpecificity();
            System.out.println("Records filter state: " + test2);
            System.out.println("Test fails, exception should have been thrown");
        }
        catch (RuntimeException e) {
            System.out.println("Test succeeded, caught " + e);
        }

        /* Create records where certain filters have a specific number of
           records. Fetch them in order and ensure they get returned in the
           correct order. Make the filters more specific and test again */
        System.out.println("Construct test data where filters have specific record counts, and ensure they are returned in decreasing order");
        ArrayList<String> record4 = createTestRecord("test1", "test2", "test3", 
                                                     "test4");
        
        RecordGroup testData2 = new RecordGroup(record4);
        testData2.add(createTestRecord("test1", "test2", "test3", "test5"));
        testData2.add(createTestRecord("test1", "test2", "test6", "test7"));
        testData2.add(createTestRecord("test1", "test8", "test10", "test11"));
        
        FieldFilterGroup testFilter7 = new FieldFilterGroup(new FieldFilter(record4, 0));
        FieldFilterGroup testFilter8 = new FieldFilterGroup(new FieldFilter(record4, 1));
        FieldFilterGroup testFilter9 = new FieldFilterGroup(new FieldFilter(record4, 2));
        TrainingRecords test3 = new TrainingRecords(testData2, null);
        System.out.println("Training data state: " + test3);

        System.out.println("Expect first filter: " + testFilter7);
        FieldFilterGroup testFilter = test3.getLargestFilter();
        System.out.println("Read " + testFilter);
        if (testFilter.equals(testFilter7))
            System.out.println("Test succeeded");
        else
            System.out.println("Test failed");

        System.out.println("Expect second filter: " + testFilter8);
        testFilter = test3.getNextLargestFilter();
        System.out.println("Read " + testFilter);
        if (testFilter.equals(testFilter8))
            System.out.println("Test succeeded");
        else
            System.out.println("Test failed");

        System.out.println("Expect second filter: " + testFilter9);
        testFilter = test3.getNextLargestFilter();
        System.out.println("Read " + testFilter);
        if (testFilter.equals(testFilter9))
            System.out.println("Test succeeded");
        else
            System.out.println("Test failed");

        test3.incrFilterSpecificity();
        System.out.println("Training data state: " + test3);
        
        FieldFilterGroup testFilterMult6 = new FieldFilterGroup(testFilter7, new FieldFilter(record4, 1));
        FieldFilterGroup testFilterMult7 = new FieldFilterGroup(testFilter7, new FieldFilter(record4, 2));
        FieldFilterGroup testFilterMult8 = new FieldFilterGroup(testFilter8, new FieldFilter(record4, 2));

        /* At this point, have one filter with 3 records and two filters with
           two each. The first filter should be first, the next two can appear
           in either order */
        System.out.println("Expect first filter: " + testFilterMult6);
        testFilter = test3.getNextLargestFilter();
        System.out.println("Read " + testFilter);
        if (testFilter.equals(testFilterMult6))
            System.out.println("Test succeeded");
        else
            System.out.println("Test failed");

        System.out.println("Expect next two filters to be " + testFilterMult7 + " and " + testFilterMult8);
        testFilter = test3.getNextLargestFilter();
        FieldFilterGroup testOtherFilter = test3.getNextLargestFilter();
        System.out.println("Read " + testFilter + " and " + testOtherFilter);
        if ((testFilter.equals(testFilterMult7) &&
             testOtherFilter.equals(testFilterMult8)) ||
            (testOtherFilter.equals(testFilterMult7) &&
             testFilter.equals(testFilterMult8)))
            System.out.println("Test succeeded");
        else
            System.out.println("Test failed");
        
        /* Create a record filters with uneven record sizes. It should throw
           an exception */
        System.out.println("record filter group with uneven records, expect exception");
        ArrayList<String> record10 = new ArrayList<String>();
        record10.add("test1");
        record10.add("false");

        ArrayList<String> record11 = new ArrayList<String>();
        record11.add("test1");
        record11.add("test5");
        record11.add("false");

        ArrayList<String> record12 = new ArrayList<String>();
        record12.add("test2");
        record12.add("test5");
        record12.add("false");

        RecordGroup testData3 = new RecordGroup(record2);
        testData3.add(record10);
        testData3.add(record11);
        testData3.add(record12);

        try {
            TrainingRecords unevenTest = new TrainingRecords(testData3, null);
            System.out.println("result: " + unevenTest);
            System.out.println("Test failed, expected exception");
        }
        catch (RuntimeException e) {
            System.out.println("Test succeeded, caught " + e);
        }
    } // Main method
}