#!/bin/bash
function GET_RECORDS()
{
   echo -e "Creating clean ~/.seadas9 configuration directory ...";
}

function ARCHIVE_SEADAS9_MSG()
{
   echo -e "Previous ~/.seadas9 has been archived in ~/.seadas_archive/.seadas9";
}

function ARCHIVE_SEADAS8_MSG()
{
   echo -e "Previous ~/.seadas8 has been archived in ~/.seadas_archive/.seadas8";
}

function RETAIN_SEADAS9_MSG()
{
   echo -e "Retained user custom files in ~/.seadas9";
}

function TRANSFER_SEADAS8_MSG()
{
   echo -e "Transferred user custom files from ~/.seadas8 to ~/.seadas9";
}


echo $(GET_RECORDS);

echo ""

cd ~/.


if [ -d ".seadas9" ]; then

  if [ -d ".seadas_archive" ]; then
    rm -rdf .seadas_archive/.seadas9
  else
    mkdir .seadas_archive
  fi

  mv .seadas9 .seadas_archive/.seadas9 2> /dev/null
  echo $(ARCHIVE_SEADAS9_MSG);

  mkdir .seadas9
  mkdir .seadas9/auxdata

  cp -Rpf .seadas_archive/.seadas9/auxdata/color_palettes .seadas9/auxdata 2> /dev/null
  cp -Rpf .seadas_archive/.seadas9/auxdata/color_schemes .seadas9/auxdata 2> /dev/null
  cp -Rpf .seadas_archive/.seadas9/auxdata/rgb_profiles .seadas9/auxdata 2> /dev/null
  cp -Rpf .seadas_archive/.seadas9/graphs .seadas9 2> /dev/null
  echo $(RETAIN_SEADAS9_MSG);

fi



if [ -d ".seadas8" ]; then

  if [ -d ".seadas_archive" ]; then
    rm -rdf .seadas_archive/.seadas8
  else
    mkdir .seadas_archive
  fi

  mv .seadas8 .seadas_archive/.seadas8 2> /dev/null
  echo $(ARCHIVE_SEADAS8_MSG);

  #  Transfer archived .seadas8 files to .seadas9 (only if .seadas9 not created)
  if [ -d ".seadas9" ]; then
    echo ""
  else
    mkdir .seadas9
    mkdir .seadas9/auxdata

    cp -Rpf .seadas_archive/.seadas8/auxdata/color_palettes .seadas9/auxdata 2> /dev/null
    cp -Rpf .seadas_archive/.seadas8/auxdata/color_schemes .seadas9/auxdata 2> /dev/null
    cp -Rpf .seadas_archive/.seadas8/auxdata/rgb_profiles .seadas9/auxdata 2> /dev/null
    cp -Rpf .seadas_archive/.seadas8/graphs .seadas9 2> /dev/null
    echo $(TRANSFER_SEADAS8_MSG);
  fi
fi


