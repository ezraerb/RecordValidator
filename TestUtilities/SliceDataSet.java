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

/* This utility extracts a set of records from a much larger set. Its used to
   create learning data sets for cross-verification of machine learning
   algorithms. The arguments are the input file, output file for the slice,
   output file for the remainder, first line to slice, and number to slice.
   A missing file, or a file smaller than the first line generates an error */
import java.io.*;

class SliceDataSet
{
    public static void main(String[] args)
    {
        if (args.length != 5) {
            System.out.println("Args: input file, slice output file, remainder output file, first slice line, number of lines in slice");
            System.exit(1);
        }
        // Convert last two arguments. Failure causes termination
        int sliceLine = 0;
        int sliceCount = 0;
        try {
            sliceLine = Integer.parseInt(args[3]);
            sliceCount = Integer.parseInt(args[4]);
        }
        catch (Exception e) {
            System.out.println("Error: caught exception " + e + ". First slice line and slice count must be positive integers");
            System.exit(1);
        }
        // Confirm input is valid
        if ((sliceLine <= 0) || (sliceCount <= 0)) {
            System.out.println("Error: First slice line and slice count must be positive integers");
            System.exit(1);
        }

        // Open all files. Any error terminates
        BufferedReader input = null;
        PrintWriter slice = null;
        PrintWriter other = null;
        try {
            input = new BufferedReader(new FileReader(args[0]));
            slice = new PrintWriter(new FileWriter(args[1]));
            other = new PrintWriter(new FileWriter(args[2]));
        }
        catch (Exception e) {
            System.out.println("Error: Specified files invalid, caught exception " + e + " opening files");
            System.exit(1);
        }
        int lineCount = 0;
        int firstNonSliceLine = sliceLine + sliceCount;
        String line;
        try {
            while ((line = input.readLine()) != null) {
                lineCount++;
                if ((lineCount >= sliceLine) && (lineCount < firstNonSliceLine))
                    slice.println(line);
                else
                    other.println(line);
            } // While lines of input to process
            if (lineCount < sliceLine)
                // Input file was too short
                // NOTE: Will be caught by enclosing block, this is deliberate
                throw new IllegalArgumentException("Input file has only " + lineCount + " lines, not sliced");
        }
        catch (Exception e) {
            System.out.println("Error: Exception " + e + " caught reading input");
            e.printStackTrace();
        }
        finally {
            try {
                if (input != null)
                    input.close();
                if (slice != null)
                    slice.close();
                if (other != null)
                    other.close();
            }
            catch (IOException e) {
                System.out.println("Error: Exception " + e + " caught closing files");
                e.printStackTrace();
            }
        } // Finally block
    }
}