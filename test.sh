#!/bin/bash
function GET_RECORDS()
{
   echo -e "Starting\n the process ...";
}

echo $(GET_RECORDS);

echo ""

cd ~/.

SEADAS_DATA_DIR='.seadas9'
SEADAS_ARCHIVE_DIR='.seadas_archive'

if [ -d "$SEADAS_DATA_DIR" ]; then
  rm -rf $SEADAS_ARCHIVE_DIR
  mv $SEADAS_DATA_DIR $SEADAS_ARCHIVE_DIR 2> /dev/null
  echo "Existing ~/$SEADAS_DATA_DIR directory has"
  echo "been archived in ~/$SEADAS_ARCHIVE_DIR"
fi

if [ -d "$SEADAS_DATA_DIR" ]; then
  echo "WARNING: Failed to remove ~/$SEADAS_DATA_DIR"
fi
