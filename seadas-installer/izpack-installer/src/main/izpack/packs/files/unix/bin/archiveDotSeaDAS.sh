#!/bin/bash
function GET_RECORDS()
{
   echo -e "Starting\n the process ...";
}

echo $(GET_RECORDS);


echo ""

cd ~/.

if [ -d ".seadas9" ]; then
  rm -rf .seadas_archive
  mv .seadas9 .seadas_archive 2> /dev/null
  echo "Existing ~/.seadas9 directory has been archived"
  echo "in ~/.seadas_archive"

  if [ -d ".seadas9" ]; then
    echo "WARNING: Failed to remove ~/.seadas9"
  fi
fi


