#!/usr/bin/env bash

show_commands_only=0
extract=0

Usage() {
    # Display Help
    echo "Usage: workflow_l2bin.sh"
    echo
    #   echo "Syntax: scriptTemplate [-g|h|v|V]"
    echo "options:"
    echo "i     ifile"
    echo "o     ofile"
    echo "p     parfile"
    echo "r     resolution"
    echo "s     suite"
    echo "m     mission"
    echo "e     extract"
    echo "c     show_commands_only"
    echo
}

while getopts "h:i:o:s:p:m:r:ec" option; do
    case $option in
    h) # display Help
        Usage
        exit
        ;;
    i) ifile=$OPTARG ;;
    o) ofile=$OPTARG ;;
    p) parfile=$OPTARG ;;
    s) suite="$OPTARG" ;;
    m) mission="$OPTARG" ;;
    r) resolution=$OPTARG ;;
    e) extract=1 ;;
    c) show_commands_only=1 ;;
    \?) # Invalid option
        echo "Error: Invalid option"
        exit
        ;;
    esac
done



if [ ! -z "${ifile}" ]; then
    if [ ! -e "${ifile}" ]; then
        echo "ifile '${ifile}' does not exist" && exit 1
    fi
fi

if [ ! -z "${parfile}" ]; then
    if [ ! -e "${parfile}" ]; then
        echo "parfile '${parfile}' does not exist" && exit 1
    fi
fi

source $OCSSWROOT/OCSSW_bash.env
if [ $show_commands_only -ne 1 ]; then
    if [ $? -ne 0 ]; then
        echo "ERROR: $OCSSWROOT/OCSSW_bash.env failed to run"
        exit 1
    fi
fi
echo " "




description="Creating"
if [ ! -z ${mission} ]; then
    description="${description} ${mission}"
fi
if [ ! -z ${suite} ]; then
    description="${description} ${suite}"
fi
description="${description} Level-3 Binned File"
if [ ${extract} -eq 1 ]; then
    description="${description} (Extract)"
else
    description="${description} (Full Scene)"
fi


command="l2bin"

if [ ! -z ${ifile} ]; then
    command="${command}  ifile=${ifile}"
fi

if [ ! -z ${ofile} ]; then
    command="${command}  ofile=${ofile}"
fi

if [ ! -z ${suite} ]; then
    command="${command}  suite=${suite}"
fi

if [ ! -z ${parfile} ]; then
    command="${command}  par=${parfile}"
fi

if [ ! -z ${resolution} ]; then
    command="${command}  resolution=${resolution}"
fi


default_ofile=$(get_output_name ${ifile} l2bin)
echo "#**************************************"
echo "# ${description}"
if [ ! -z ${ifile} ]; then echo "# ifile=${ifile}"; fi
if [ ! -z ${ofile} ]; then echo "# ofile=${ofile}"; fi
if [ ! -z ${suite} ]; then echo "# suite=${suite}"; fi
if [ ! -z ${resolution} ]; then echo "# suite=${resolution}"; fi
if [ ! -z ${parfile} ]; then
    echo "# par=${parfile}"
    sed 's/^/#     /g' ${parfile}
fi

echo "# The suggested ofile from (get_output_name)"
echo "#     Default ${default_ofile}"
echo "#**************************************"
echo "${command}"
echo " "
if [ $show_commands_only -ne 1 ]; then
    ${command}
    if [ $? -ne 0 ]; then
        echo "ERROR"
        exit 1
    fi
fi
echo " "
