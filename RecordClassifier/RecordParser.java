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

/* This class converts CSV files into sets of records and vice versa. Records
   are stored as ArrayLists, not arrays, because the classification can get
   added as they are processed */
import java.util.*;
import java.io.*;


public class RecordParser
{
    /* Convert a set of records into a CSV file. Any existing file with the
       same name is overwritten. If the records have inconsistent field counts,
       this method throws an exception */
    public static void outputRecords(String fileName,
                                     RecordGroup records) throws IOException, FileNotFoundException
    {
        if (fileName == null)
            throw new IllegalArgumentException("Filename for output records must be specified");

        if (records == null)
            throw new IllegalArgumentException("Records to output passed null");

        // Verify field integrity; all records must have same number of fields
        if (records.size() > 0) {
            int fieldCount = records.getRecords().get(0).size();
            Iterator<ArrayList<String> > index = records.getRecords().iterator();
            while (index.hasNext())
                if (index.next().size() != fieldCount)
                    throw new IOException("RecordParser, records to output have inconsistent field counts");
        }

        // Open the file (yes, even in the case with no records!)
        PrintWriter outputFile = new PrintWriter(new FileOutputStream(fileName));
        Iterator<ArrayList<String> > recIndex = records.getRecords().iterator();
        while (recIndex.hasNext()) {
            boolean outputSeperator = false;
            Iterator<String> fieldIndex = recIndex.next().iterator();
            while (fieldIndex.hasNext()) {
                if (outputSeperator)
                    outputFile.print(",");
                else
                    outputSeperator = true;
                outputFile.print(fieldIndex.next());
            }
            outputFile.println();
        }
        outputFile.close();
    }

    /* Read record data from a CSV file. If the file contains no records, or
       the number of fields per record is not consistent, this method throws
       an exception */
    public static RecordGroup readRecords(String fileName) throws IOException, FileNotFoundException
    {
        RecordGroup result = null;
        BufferedReader input = new BufferedReader(new FileReader(fileName));
        String line = null;
        while ((line = input.readLine()) != null) {
            if (line.trim().length() > 0) { // Ignore blank lines
                ArrayList<String> newRecord = new ArrayList<String>(Arrays.asList(line.split(",")));
                if (result != null) {
                    if (newRecord.size() != result.getRecords().get(0).size())
                        throw new IOException("Data for file " + fileName + " inconsistent, expected " + result.getRecords().get(0).size() + " fields, got " + newRecord.size());
                    result.add(newRecord);
                }
                else // First entry
                    result = new RecordGroup(newRecord);
            } // Data on line
        } // Data to read

        // A null pointer at this point signals an empty file
        if (result == null)
            throw new IOException("record File " + fileName + " invalid; contains no records");
        return result;
    }

    /* Test code to write bills and then read them back in. Verify the output
       manually */
    public static void main(String[] args)
    {
        ArrayList<String> testRec1 = new ArrayList<String>();
        testRec1.add("test");
        testRec1.add("test2");
        testRec1.add("test3");
        testRec1.add("test4");
        
        ArrayList<String> testRec2 = new ArrayList<String>();
        testRec2.add("2test");
        testRec2.add("2test2");

        ArrayList<String> testRec3 = new ArrayList<String>();
        testRec3.add("3test");
        testRec3.add("3test2");
        testRec3.add("3test3");
        testRec3.add("3test4");

        RecordGroup testRecords = new RecordGroup(testRec1);
        testRecords.add(testRec2);
        testRecords.add(testRec3);

        System.out.println("Mismatched field counts, expect failure");

        try {
            outputRecords("RecordParserTest.txt", testRecords);
            System.out.println("File created, test failed");
        }
        catch (Exception e) {
            System.out.println("Test succeeded, caught " + e);
        }

        // Fix the broken record
        testRec2.add("2test3");
        testRec2.add("2test4");
        System.out.println("Matched field counts, except success");
        try {
            outputRecords("RecordParserTest.txt", testRecords);
            System.out.println("File created, test succeeded");
        }
        catch (Exception e) {
            System.out.println("Test failed, caught " + e);
        }

        // Read in non-existent file
        System.out.println("Read nonexistent file, expect exception");
        RecordGroup results = null;
        try {
            results = readRecords("BadParserFileName.testtesttest");
            System.out.println("Test failed, file read");
        }
        catch (Exception e) {
            System.out.println("Test succeeded, caught " + e);
        }

        // Read in valid file
        System.out.println("Read valid file, expect success");
        results = null;
        try {
            results = readRecords("RecordParserTest.txt");
            System.out.println("Read: " + results);
            // Validate the size of the results
            if ((results.size() == testRecords.size()) &&
                (results.getRecords().get(0).size() == testRecords.getRecords().get(0).size()) &&
                results.getRecords().get(0).get(0).equals(testRecords.getRecords().get(0).get(0)))
                System.out.println("Test succeeded, file read");
            else
                System.out.println("Test failed, data does not match original");
        }
        catch (Exception e) {
            System.out.println("Test succeeded, caught " + e);
        }

        // Create a deliberately misformatted file, and attempt to read it
        System.out.println("Badly formatted file test, expect read exception");
        results = null;
        try {
            PrintWriter outputFile = new PrintWriter(new FileOutputStream("BadParserFile2.testtesttest"));
            outputFile.println("test1,test2,test3");
            outputFile.println("test4,test5test6"); // Note the missing comma
            outputFile.close();
            try {
                results = readRecords("BadParserFile2.testtesttest");
                System.out.println("Test failed, file read");
            }
            catch (Exception e) {
                System.out.println("Test succeeded, caught " + e);
            }
        }
        catch (Exception e) {
            System.out.println("Test failed, caught " + e + " creating test file");
        }
    } // Main method
};