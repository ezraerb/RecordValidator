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

/* This utility takes two sets of records, one with original categories attached
   and one with categories set by a classification algorithm. It reports which
   records have differet results in the two datasets, classification errors */
import java.io.*;

class CompRecordsToBaseline
{
    public static void main(String[] args)
    {
        if (args.length != 3) {
            System.out.println("Args: baseline file, results file, output file");
            System.exit(1);
        }
        // Open all three files. Any error terminates
        BufferedReader baseline = null;
        BufferedReader result = null;
        PrintWriter output = null;
        try {
            baseline = new BufferedReader(new FileReader(args[0]));
            result = new BufferedReader(new FileReader(args[1]));
            output = new PrintWriter(new FileWriter(args[2]));
        }
        catch (Exception e) {
            System.out.println("Error: Specified files invalid, caught exception " + e + " opening files");
            System.exit(1);
        }
        try {
            String baselineLine = baseline.readLine();
            String resultLine = result.readLine();
            boolean haveMismatch = false;
            while ((baselineLine != null) && (resultLine != null)) {
                // If the two lines match, nothing to do
                if (!baselineLine.equals(resultLine)) {
                    /* Split both lines into the record fields and the category.
                       If the record fields don't match, the input is invalid.
                       If they do match, have a mismatch that should be output.
                       Fields are deliminated by commas with the category
                       last */
                    int baselineClasspos = baselineLine.lastIndexOf(",");
                    int resultClasspos = resultLine.lastIndexOf(",");
                    if ((baselineClasspos == -1) || (resultClasspos == -1))
                        // Not strictly correct, but will work
                        throw new IllegalArgumentException("Record of baseline or result has no record fields");
                    String baselineRecord = baselineLine.substring(0, baselineClasspos);
                    String resultRecord = resultLine.substring(0, resultClasspos);
                    if (!baselineRecord.equals(resultRecord))
                        // Not strictly correct, but will work
                        throw new IllegalArgumentException("Records in baseline and result files do not match up");

                    /* At this point, know that records match and the
                       classifications don't. Output the results */
                    output.print("Record: " + resultRecord);
                    output.print(" Baseline: " + baselineLine.substring(baselineClasspos + 1));
                    // Force a newline
                    output.println(" Result: " + resultLine.substring(resultClasspos + 1));
                    haveMismatch = true;
                } // Two file lines are not equal
                // Forcibily read both lines so the final test works properly
                baselineLine = baseline.readLine();
                resultLine = result.readLine();
            } // While lines of input to process
            
            /* At this point. both files must be out of data or they have
               mismatched records. This is an error */
            if ((baselineLine != null) || (resultLine != null))
                throw new IllegalArgumentException("Baseline and result files have unequal number of records");
            
            // Report overall results
            if (haveMismatch)
                System.out.println("Baseline and results have mismatches, see " + args[2]);
            else 
                System.out.println("Baseline and results are identical");
        }
        catch (Exception e) {
            System.out.println("Error: Exception " + e + " caught processing file");
            System.exit(1);
        }
        finally {
            try {
                if (baseline != null)
                    baseline.close();
                if (result != null)
                    result.close();
                if (output != null)
                    output.close();
            }
            catch (IOException e) {
                System.out.println("Error: Exception " + e + " caught closing files");
                e.printStackTrace();
            }
        } // Finally block
    }
}