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

/* This utility takes a set of records in CSV format with the classification
   category as the final field. It produces a new set of records, in the same
   order, with the classification removed. These can then form test input to
   a classification algorithm with the original records as the expected
   result */
import java.io.*;

class StripClassification
{
    public static void main(String[] args)
    {
        if (args.length != 2) {
            System.out.println("Args: input file, output file");
            System.exit(1);
        }
        // Open both files. Any error terminates
        BufferedReader input = null;
        PrintWriter output = null;
        try {
            input = new BufferedReader(new FileReader(args[0]));
            output = new PrintWriter(new FileWriter(args[1]));
        }
        catch (Exception e) {
            System.out.println("Error: Specified files invalid, caught exception " + e + " opening files");
            System.exit(1);
        }
        try {
            String line;
            String outLine; 
            while ((line = input.readLine()) != null) {
                /* Fields are deliminated by commas. Take the string up to
                   the last one */
                int cutpos = line.lastIndexOf(",");
                if (cutpos == -1) {
                    System.out.println("WARNING: No comma fields found for file line, result is empty");
                    outLine = new String();
                }
                else
                    outLine = line.substring(0, cutpos);
                output.println(outLine);
            } // While lines of input to process
        }
        catch (Exception e) {
            System.out.println("Error: Exception " + e + " caught processing file");
        }
        finally {
            try {
                if (input != null)
                    input.close();
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