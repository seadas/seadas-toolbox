#!/usr/bin/env bash

show_commands_only=0
extract=0

Usage()
{
   # Display Help
   echo "Add description of the script functions here."
   echo
   echo "Syntax: scriptTemplate [-g|h|v|V]"
   echo "options:"
   echo "i     ifile"
   echo "g     geofile"
   echo "a     ancfile"
   echo "o     ofile"
   echo "s     suite"
   echo "p     parfile"
   echo "e     extract"
   echo "c     show_commands_only"
   echo "v     Verbose mode."
   echo
}



while getopts "h:i:g:a:o:s:p:ec" option; do
   case $option in
      h) # display Help
         Usage
         exit;;
      i) ifile=$OPTARG;;
      g) geofile=$OPTARG;;
      a) ancfile=$OPTARG;;
      o) ofile=$OPTARG;;
      s) suite=$OPTARG;;
      p) parfile=$OPTARG;;
      e) extract=1;;
      c) show_commands_only=1;;
     \?) # Invalid option
         echo "Error: Invalid option"
         exit;;
   esac
done


if [ -z "${ifile}" ] || [ -z "${geofile}" ] || [ -z "${ofile}" ]; then
    echo "ERROR in call to $0"
    Usage
    exit 1;
fi


#echo "ifile=${ifile}"
#echo "geofile=${geofile}"
#echo "ancfile=${ancfile}"
#echo "ofile=${ofile}"
#echo "suite=${suite}"
#echo "parfile=${parfile}"
#echo "extract=${extract}"
#echo "show_commands_only=${show_commands_only}"


if [ ${extract} -eq 1 ]; then
    description="Creating MODIS Level-2 ${suite} File (Extract)"
else
    description="Creating MODIS Level-2 ${suite} File (Full Scene)"
fi







if [ ! -e "${ifile}" ];  then echo "ifile '${ifile}' does not exist" && exit 1; fi
if [ ! -e "${geofile}" ];  then echo "geofile '${ifile}' does not exist" && exit 1; fi
if [ ! -e "${ancfile}" ];  then echo "ifile '${ancfile}' does not exist" && exit 1; fi



source $OCSSWROOT/OCSSW_bash.env
if [ $show_commands_only -ne 1 ]; then 
    if [ $? -ne 0 ]; then echo "ERROR"; exit 1; fi
fi; echo " ";




    command="l2gen ifile=${ifile} geofile=${geofile} ofile=${ofile} par=${ancfile} suite=\"${suite}\""
    short_command="l2gen ifile=${ifile} geofile=${geofile} par=${ancfile} suite=\"${suite}\""
    default_ofile=`get_output_name ${ifile} l2gen`
    echo "#**************************************"
    echo "# Creating ${description}"
    echo "# ifile=${ifile}"
    echo "# geofile=${geofile}"
    echo "# ofile=${ofile}"
    echo "# par=${ancfile}"
    echo "# suite=${suite}"
    echo "# Optional command without ofile:"
    echo "#     ${short_command}"
    echo "#     Default ${default_ofile}"
    echo "#**************************************"
    echo "${command}"; echo " "
    if [ $show_commands_only -ne 1 ]; then ${command};
        if [ $? -ne 0 ]; then echo "ERROR"; exit 1; fi
    fi; echo " ";



