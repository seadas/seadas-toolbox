#!/usr/bin/env bash

show_commands_only=0
extract=0

Usage() {
    # Display Help
    echo "Usage: workflow_l2gen.sh"
    echo
    echo "options:"
    echo "i     ifile"
    echo "g     geofile"
    echo "a     ancfile"
    echo "o     ofile"
    echo "s     suite"
    echo "p     parfile"
    echo "m     mission"
    echo "e     extract"
    echo "c     show_commands_only"
    echo "v     Verbose mode."
    echo
}

while getopts "h:i:g:a:o:s:p:m:ec" option; do
    case $option in
    h) Usage
        exit ;;
    i) ifile=$OPTARG ;;
    g) geofile=$OPTARG ;;
    a) ancfile=$OPTARG ;;
    o) ofile=$OPTARG ;;
    s) suite=$OPTARG ;;
    p) parfile=$OPTARG ;;
    m) mission=$OPTARG ;;
    e) extract=1 ;;
    c) show_commands_only=1 ;;
    \?) # Invalid option
        echo "Error: Invalid option"
        exit ;;
    esac
done


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
description="${description} Level-2 File"
if [ ${extract} -eq 1 ]; then
    description="${description} (Extract)"
else
    description="${description} (Full Scene)"
fi


command="l2gen"

if [ ! -z ${ifile} ]; then
    command="${command}  ifile=${ifile}"
fi

if [ ! -z ${geofile} ]; then
    command="${command}  geofile=${geofile}"
fi

if [ ! -z ${ofile} ]; then
    command="${command}  ofile=${ofile}"
fi

if [ ! -z ${ancfile} ]; then
    command="${command}  par=${ancfile}"
fi

if [ ! -z ${suite} ]; then
    command="${command}  suite=${suite}"
fi

if [ ! -z ${parfile} ]; then
    command="${command}  par=${parfile}"
fi

#    command="l2gen ifile=${ifile} geofile=${geofile} ofile=${ofile} par=${ancfile} suite=\"${suite}\""
#    short_command="l2gen ifile=${ifile} geofile=${geofile} par=${ancfile} suite=\"${suite}\""
default_ofile=$(get_output_name ${ifile} l2gen)
echo "#**************************************"
echo "# ${description}"
if [ ! -z ${ifile} ]; then echo "# ifile=${ifile}"; fi
if [ ! -z ${geofile} ]; then echo "# geofile=${geofile}"; fi
if [ ! -z ${ofile} ]; then echo "# ofile=${ofile}"; fi
if [ ! -z ${ancfile} ]; then echo "# par=${ancfile}"; fi
if [ ! -z ${suite} ]; then echo "# suite=${suite}"; fi
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
    if [ ! -z "${ifile}" ]; then
        if [ ! -e "${ifile}" ]; then
            echo "ifile '${ifile}' does not exist" && exit 1
        fi
    fi
    if [ ! -z "${geofile}" ]; then
        if [ ! -e "${geofile}" ]; then
            echo "geofile '${geofile}' does not exist" && exit 1
        fi
    fi
    if [ ! -z "${ancfile}" ]; then
        if [ ! -e "${ancfile}" ]; then
            echo "ancfile '${ancfile}' does not exist" && exit 1
        fi
    fi
    if [ ! -z "${parfile}" ]; then
        if [ ! -e "${parfile}" ]; then
            echo "parfile '${parfile}' does not exist" && exit 1
        fi
    fi
    ${command}
    if [ $? -ne 0 ]; then
        echo "ERROR"
        exit 1
    fi
fi
echo " "
