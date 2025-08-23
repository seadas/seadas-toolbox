#!/bin/bash


function ARCHIVE_SEADAS9_MSG()
{
   echo -e "Previous SeaDAS 9 configuration directory has been archived in ~/.seadas_archive/.seadas9";
}

function ARCHIVE_SEADAS8_MSG()
{
   echo -e "Previous SeaDAS 8 configuration directory has been archived in ~/.seadas_archive/.seadas8";
}

function ARCHIVE_SEADAS7_MSG()
{
   echo -e "Previous SeaDAS 7 configuration directory has been archived in ~/.seadas_archive/.seadas7";
}

function TRANSFER_SEADAS9_MSG()
{
   echo -e "Moving previous SeaDAS 9 configuration directory ~/.seadas9 to ~/.seadas";
}

function RETAIN_SEADAS_MSG()
{
   echo -e "Retaining previous SeaDAS version configuration directory ~/.seadas";
}



echo -e "Assessing any previous SeaDAS version configuration directories ...";
echo ""


#echo -e "Changing to home directory";
cd ~/.
#pwd


if [ ! -d ".seadas_archive" ]; then
#  echo -e "Making .seadas_archive";
  mkdir .seadas_archive 2> /dev/null
fi



if [ -d ".seadas" ]; then
    if [ -d ".seadas/auxdata" ]; then
      # SeaDAS 10 or later has been previously run, so leave in tact
      # archive any other versions
      echo $(RETAIN_SEADAS_MSG);

      if [ -d ".seadas9" ]; then
        if [ -d ".seadas_archive/.seadas9" ]; then
          rm -rf .seadas_archive/.seadas9 2> /dev/null
        fi
        mv .seadas9 .seadas_archive 2> /dev/null
        echo $(ARCHIVE_SEADAS9_MSG);
      fi
    else
      # This is probably seadas7 so archive it and look for seadas9
      if [ -d ".seadas_archive/.seadas7" ]; then
        if [ -d ".seadas_archive/.seadas7" ]; then
          rm -rf .seadas_archive/.seadas7 2> /dev/null
        fi
      fi
      mv .seadas .seadas_archive/.seadas7 2> /dev/null
      echo $(ARCHIVE_SEADAS7_MSG);


      if [ -d ".seadas9" ]; then
        mv .seadas9 .seadas 2> /dev/null
        rm -rf .seadas9 2> /dev/null
        echo $(TRANSFER_SEADAS9_MSG);
      fi
    fi


else
  # SeaDAS 10 or later not previously run, copy seadas9 if available
  if [ -d ".seadas9" ]; then
     mv .seadas9 .seadas 2> /dev/null
     rm -rf .seadas9 2> /dev/null
     echo $(TRANSFER_SEADAS9_MSG);
  fi
fi



if [ -d ".seadas8" ]; then
   if [ -d ".seadas_archive/.seadas8" ]; then
     rm -rf .seadas_archive/.seadas8 2> /dev/null
   fi
   mv .seadas8 .seadas_archive 2> /dev/null
   echo $(ARCHIVE_SEADAS8_MSG);
fi



