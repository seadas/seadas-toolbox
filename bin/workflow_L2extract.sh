#!/usr/bin/env bash

show_commands_only=0

Usage() {
    # Display Help
    echo "Usage: workflow_L2extract.sh ifile swlon swlat nelon nelat [-l list of products | -o ofile]"
    echo
    #   echo "Syntax: scriptTemplate [-g|h|v|V]"
    echo "options:"
    echo "o     ofile"
    echo "l     list of products"
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


while getopts ":h:o:cl:" option; do
    case $option in
    h) # display Help
        Usage
        exit
        ;;
    o) ofile=$OPTARG ;;
    l) product=$OPTARG ;;

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
#if [ -z "${ofile}" ]; then echo "ERROR: ($0) ofile not specified"; Usage; exit 1; fi
if [ -z "${swlon}" ]; then echo "ERROR: ($0) swlon not specified"; Usage; exit 1; fi
if [ -z "${swlat}" ]; then echo "ERROR: ($0) swlat not specified"; Usage; exit 1; fi
if [ -z "${nelon}" ]; then echo "ERROR: ($0) nelon not specified"; Usage; exit 1; fi
if [ -z "${nelat}" ]; then echo "ERROR: ($0) nelat not specified"; Usage; exit 1; fi

#echo "swlon=${swlon}"
#echo "swlat=${swlat}"
#echo "nelon=${nelon}"
#echo "nelat=${nelat}"
#echo "ifile=${ifile}"
#echo "ofile=${ofile}"
#echo "show_commands_only=${show_commands_only}"

description="Creating"
if [ ! -z ${mission} ]; then
    description="${description} ${mission}"
fi
if [ ! -z ${suite} ]; then
    description="${description} ${suite}"
fi
description="${description} Level-2 File Extract"


source $OCSSWROOT/OCSSW_bash.env
if [ $show_commands_only -ne 1 ]; then
    if [ $? -ne 0 ]; then
        echo "ERROR: $OCSSWROOT/OCSSW_bash.env failed to run"
        exit 1
    fi
fi
echo " "



command="lonlat2pixline ${ifile} ${swlon} ${swlat} ${nelon} ${nelat}"

echo "#**************************************"
echo "# Getting extract scan line / pixel bounds from geo-coordinates bounds"
echo "# ifile=${ifile}"
echo "# nelon=${nelon}"
echo "# nelat=${nelat}"
echo "# swlon=${swlon}"
echo "# swlat=${swlat}"
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

command="l2extract ifile=$ifile ofile=$ofile spix=${spixl} epix=${epixl} sline=${sline} eline=${eline} ofile=${ofile}"
if [ ! -z ${product} ]; then
    command="${command} product=${product}"
fi
#command="l2extract ifile=$ifile ofile=$ofile spix=309 epix=612 sline=1074 eline=1864"

default_ofile=$(get_output_name ${ifile} l2extract)

echo "#**************************************"
echo "# ${description}"
echo "# Note: scan line / pixel bounds obtained from lonlat2pixline"
echo "# ifile=${ifile}"
echo "# ofile=${ofile}"
if [ ! -z ${product} ]; then
    echo "# product=${product}"
fi
echo "# spix=${spixl}"
echo "# epix=${epixl}"
echo "# sline=${sline}"
echo "# eline=${eline}"
echo "# The suggested ofile from (get_output_name)"
echo "#     Default ${default_ofile}"
echo "#**************************************"
echo "${command}"
echo " "
if [ $show_commands_only -ne 1 ]; then
    ${command}
    exit_status=$?
    if [ ${exit_status} -ne 0 ]; then
        echo "ERROR: command failed: $command"
        echo "exit_status: ${exit_status}"
        exit 1
    fi
fi
echo " "
