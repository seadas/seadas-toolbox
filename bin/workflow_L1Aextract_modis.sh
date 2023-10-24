#!/usr/bin/env bash

show_commands_only=0
extract=0

Usage() {
    # Display Help
    echo "Usage: workflow_l2gen.sh ifile swlon swlat nelon nelat [-g geofile | -o ofile]"
    echo
    #   echo "Syntax: scriptTemplate [-g|h|v|V]"
    echo "options:"
    echo "g     geofile"
    echo "o     ofile"
    echo "c     show_commands_only"
    echo "v     Verbose mode."
    echo
}

if [ ! -z $1 ]; then ifile=$1; else echo ${usage}; exit 1; fi
if [ ! -z $2 ]; then swlon=$2; else echo ${usage}; exit 1; fi
if [ ! -z $3 ]; then swlat=$3; else echo ${usage}; exit 1; fi
if [ ! -z $4 ]; then nelon=$4; else echo ${usage}; exit 1; fi
if [ ! -z $5 ]; then nelat=$5; else echo ${usage}; exit 1; fi



shift 5


while getopts ":h:g:o:c" option; do
    case $option in
    h) # display Help
        Usage
        exit
        ;;
    g) geofile=$OPTARG ;;
    o) ofile=$OPTARG ;;

    c) show_commands_only=1 ;;
    \?) # Invalid option
        echo "Error: Invalid option"
        exit
        ;;
    esac
done

#shift $((OPTIND - 1))

#${swlon} ${swlat} ${nelon} ${nelat}



if [ -z "${ifile}" ]; then echo "ERROR: ($0) ifile not specified"; Usage; exit 1; fi
if [ ! -e "${ifile}" ]; then echo "ERROR: ($0) ifile '${ifile} does not exist"; Usage; exit 1; fi
if [ -z "${geofile}" ]; then echo "ERROR: ($0) geofile not specified"; Usage; exit 1; fi
if [ ! -e "${geofile}" ]; then echo "ERROR: ($0) geofile '${geofile} does not exist"; Usage; exit 1; fi
if [ -z "${ofile}" ]; then echo "ERROR: ($0) ofile not specified"; Usage; exit 1; fi
if [ -z "${swlon}" ]; then echo "ERROR: ($0) swlon not specified"; Usage; exit 1; fi
if [ -z "${swlat}" ]; then echo "ERROR: ($0) swlat not specified"; Usage; exit 1; fi
if [ -z "${nelon}" ]; then echo "ERROR: ($0) nelon not specified"; Usage; exit 1; fi
if [ -z "${nelat}" ]; then echo "ERROR: ($0) nelat not specified"; Usage; exit 1; fi

#echo "swlon=${swlon}"
#echo "swlat=${swlat}"
#echo "nelon=${nelon}"
#echo "nelat=${nelat}"
#echo "ifile=${ifile}"
#echo "geofile=${geofile}"
#echo "ofile=${ofile}"
#echo "show_commands_only=${show_commands_only}"

description="Creating"
if [ ! -z ${mission} ]; then
    description="${description} ${mission}"
fi
if [ ! -z ${suite} ]; then
    description="${description} ${suite}"
fi
description="${description} Level-1A File Extract"


source $OCSSWROOT/OCSSW_bash.env
if [ $show_commands_only -ne 1 ]; then
    if [ $? -ne 0 ]; then
        echo "ERROR: $OCSSWROOT/OCSSW_bash.env failed to run"
        exit 1
    fi
fi
echo " "



command="lonlat2pixline ${geofile} ${swlon} ${swlat} ${nelon} ${nelat}"

echo "#**************************************"
echo "# Example of getting extract pixel bounds from geo-coordinates"
echo "# ifile=${geofile}"
echo "# swlon=${swlon}"
echo "# swlat=${swlat}"
echo "# nelon=${nelon}"
echo "# nelat=${nelat}"
echo "#**************************************"
echo "${command}"
echo " "
values=$(${command})
    exit_status=$?
    if [ ${exit_status} -ne 0 ]; then
            echo "ERROR: lonlat2pixline return status=${exit_status}"
        exit 1
    fi
echo " "

IFS=$'\n'
for i in $values; do
    if [ $show_commands_only -ne 1 ]; then echo $i; fi
    IFS='=' read -ra vals <<<"$i"
    unset IFS
    if [ ${#vals[@]} -eq 2 ]; then
        read -rd '' name <<<"${vals[0]}"
        read -rd '' value <<<"${vals[1]}"
        if [ $name == "spixl" ]; then
            spixl=$value
        fi
        if [ $name == "epixl" ]; then
            epixl=$value
        fi
        if [ $name == "sline" ]; then
            sline=$value
        fi
        if [ $name == "eline" ]; then
            eline=$value
        fi
    fi
done

#spixl=309
#epixl=612
#sline=1074
#eline=1864
#command="${extract_program} ifile=\"$ifile\" ofile=\"$ofile\" spix=309 epix=612 sline=1074 eline=1864"

command="l1aextract_modis $ifile ${spixl} ${epixl} ${sline} ${eline} $ofile"

default_ofile=$(get_output_name ${ifile} l1aextract_modis)

echo "#**************************************"
echo "# ${description}"
echo "# Note: pixel and line info obtained from lonlat2pixline"
echo "# infile=$ifile"
echo "# outfile=$ofile"
echo "# spixl=$spixl"
echo "# epixl=$epixl"
echo "# sline=$sline"
echo "# eline=$eline"
echo "# Required ofile but get_output_name suggests:"
echo "#     Default ${default_ofile}"
echo "#**************************************"
echo "${command}"
echo " "
if [ $show_commands_only -ne 1 ]; then
    ${command}
    exit_status=$?
    if [ ${exit_status} -ne 0 ]; then
            echo "ERROR: l1aextract_modis return status=${exit_status}"
        exit 1
    fi
fi
echo " "
