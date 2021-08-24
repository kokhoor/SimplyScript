/*
 * Copyright 2021 SolutionX Software Sdn Bhd &lt;info@solutionx.com.my&gt;.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package my.com.solutionx.simplyscript;

import java.io.*;

/**
 * Class that implements the java.io.FilenameFilter
 * interface.
 *
 * @author <a href="mailto:mjenning@islandnet.com">Mike Jennings</a>
 * @version $Revision: 1.1.1.1 $
 */
public class SimpleFileFilter implements FilenameFilter
{
     private String[] extensions;

     public SimpleFileFilter(String ext)
     {
         this(new String[]{ext});
     }

     public SimpleFileFilter(String[] exts)
     {
         extensions=new String[exts.length];
         for (int i=0;i<exts.length;i++)
         {
             extensions[i]=exts[i].toLowerCase();
         }
     }

     /** filenamefilter interface method */
     public boolean accept(File dir,String _name)
     {
         String name=_name.toLowerCase();
         for (int i=0;i<extensions.length;i++)
         {
             if (name.endsWith(extensions[i]))
                return true;
         }
         return false;
     }

     /** 
      * this method checks to see if an asterisk
      * is imbedded in the filename, if it is, it
      * does an "ls" or "dir" of the parent directory
      * returning a list of files that match
      * eg. /usr/home/mjennings/*.jar
      * would expand out to all of the files with a .jar
      * extension in the /usr/home/mjennings directory 
      */
      public static String[] fileOrFiles(File f)
      {
          if (f==null) return null;
          File parent=new File(f.getParent());
          String fname=f.getName();
          String[] files;
          if (fname.charAt(0)=='*')
          {
              String filter=fname.substring(1,fname.length());
              files=parent.list(new SimpleFileFilter(filter));
              return files;
          }
          else
            return null;
      }
}
