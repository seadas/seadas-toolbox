#!/bin/bash
function GET_RECORDS()
{
   echo -e "Starting\n the process ...";
}

echo $(GET_RECORDS);

echo ""
cd ~
rm -rf ~/.seadas_archive
 mv  ~/.seadas9 ~/.seadas_archive 2> /dev/null
echo "The .seadas9 directory has been moved to"
echo "the .seadas_archive directory."
