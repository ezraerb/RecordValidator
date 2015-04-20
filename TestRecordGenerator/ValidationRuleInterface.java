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

/* This interface defines a rule to validate a generated record. It accepts
   an array of field values, and returns whether it is valid or not. These
   classes are designed to be implemented using the classic Chain of
   Responsibility design pattern; apply them in order until the record fails.
   If every one succeeds, the data is valid */
import java.util.List;

public interface ValidationRuleInterface
{
    // Returns true if a given set of field data is NOT valid
    public boolean failsValidation(List<String> fieldData);
}