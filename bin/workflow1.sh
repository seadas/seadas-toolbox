#!/usr/bin/env bash

# This script starts with a L1A file

usage="Usage example: 
./workflow1.sh A2023016190500.L1A_LAC AQUA_MODIS 20230116T190501 full\n
./workflow1.sh A2023016190500.L1A_LAC AQUA_MODIS 20230116T190501 extract\n
./workflow1.sh A2023016190500.L1A_LAC AQUA_MODIS 20230116T190501 both
"
usage="Usage example: ./workflow1.sh A2023016190500.L1A_LAC AQUA_MODIS 20230116T190501 extract 1 0"

swlon=-85
swlat=24
nelon=-80
nelat=31


make_extract=1
make_full=1
make_extras=1

#dir_delimitor='/';

if [ ! -z $1 ]; then
    level1A_file=$1
    level1A_basename=$(basename "$1")
    working_dir=$(dirname "$1")
else
    echo ${usage}
    exit 1
fi

if [ ! -z $2 ]; then
    basename_mission_part=$2
else
    echo ${usage}
    exit 1
fi

if [ ! -z $3 ]; then
    basename_time_part=$3
else
    echo ${usage}
    exit 1
fi

if [ ! -z $4 ]; then
    if [ $4 == "full" ]; then
        make_full=1
        make_extract=0
        make_extras=0
    elif [ $4 == "full_extras" ]; then
        make_full=1
        make_extract=0
        make_extras=1
    elif [ $4 == "extract" ]; then
        make_full=0
        make_extract=1
        make_extras=0
    elif [ $4 == "extract_extras" ]; then
        make_full=0
        make_extract=1
        make_extras=1
    elif [ $4 == "both" ]; then
        make_full=1
        make_extract=1
        make_extras=0
    elif [ $4 == "both_extras" ]; then
        make_full=1
        make_extract=1
        make_extras=1
    else
        make_full=1
        make_extract=1
        make_extras=1
    fi
fi

if [ ! -z $5 ]; then
    show_commands_only=$5
else
    show_commands_only=0
fi
if [ $show_commands_only -eq 1 ]; then option_c="-c"; else option_c=""; fi

mission=${basename_mission_part}
#echo "make_extract=${make_extract}"
#echo "make_full=${make_full}"
#echo "make_extras=${make_extras}"
#echo " "

echo "#**************************************"
echo "# Showing OCSSWROOT Environment Variable"
echo "#**************************************"
echo 'echo $OCSSWROOT'
echo " "
if [ $show_commands_only -ne 1 ]; then
    echo $OCSSWROOT
    if [ $? -ne 0 ]; then
        echo "ERROR: $OCSSWROOT/OCSSW_bash.env failed to run"
        exit 1
    fi
fi
echo " "

echo "#**************************************"
echo "# Setting up the SeaDAS-OCSSW Processor Environment"
echo "#**************************************"
echo 'source $OCSSWROOT/OCSSW_bash.env'
echo " "
source $OCSSWROOT/OCSSW_bash.env
if [ $show_commands_only -ne 1 ]; then
    if [ $? -ne 0 ]; then
        echo "ERROR"
        exit 1
    fi
fi
echo " "

echo "#**************************************"
echo "# Verifying SeaDAS-OCSSW Processors Exist in Envronment Path"
echo "#**************************************"
echo 'which l2gen'
echo " "
if [ $show_commands_only -ne 1 ]; then
    which l2gen
    if [ $? -ne 0 ]; then
        echo "ERROR"
        exit 1
    fi
fi
echo " "

#script_dir=$(dirname "$0")
#working_dir="${script_dir}"

#product="chlor_a"
#level1A_basename=A2023016190500.L1A_LAC
#basename_time_part=20230116T190501
basename_part=${basename_mission_part}.${basename_time_part}

#echo "level1A_basename=${level1A_basename}"
#echo "basename_time_part=${basename_time_part}"
#echo "basename_part=${basename_part}"
#echo "working_dir=${working_dir}"

## Install Level1A file if not in working directory
#level1A_file=${working_dir}/${level1A_basename}
#if [ ! -e "${level1A_file}" ]; then
#    source_dir="${script_dir}/../../OB.DAAC"
#    level1A_source_file=${source_dir}/${level1A_basename}
#    if [ ! -e "${level1A_source_file}" ];  then echo "Missing file ${level1A_source_file}" && exit 1; fi
#
#    command="cp $level1A_source_file ${working_dir}"
#    echo "#**************************************"
#    echo "# Copying L1A file to working directory"
#    echo "#**************************************"
#    echo "${command}"; echo " "
#    ${command};
#    if [ $? -ne 0 ]; then echo "ERROR"; exit 1; fi; echo " ";
#fi

if [ ! -e "${level1A_file}" ]; then
    echo "ERROR: ${level1A_file} does not exist"
    exit 1
fi

command="cd ${working_dir}"
echo "#**************************************"
echo "# Changing directory to run programs in same directory as level-1A file"
echo "#**************************************"
echo "${command}"
echo " "
${command}
if [ $? -ne 0 ]; then
    echo "ERROR"
    exit 1
fi
echo " "

# Now working dir is the current directory so change the variable
# Showing relative paths to make commands not dependent on any user directory tree
working_dir="."
#echo "working_dir=${working_dir}"
#echo "pwd=`pwd`"
level1A_file=${working_dir}/${level1A_basename}

# Full files to be created
geo_full_file=${working_dir}/${basename_part}.GEO.nc
level1B_file=${working_dir}/${basename_part}.L1B.hdf
level1B_qkm_file=${working_dir}/${basename_mission_part}_QKM.${basename_time_part}.L1B.hdf
level1B_hkm_file=${working_dir}/${basename_mission_part}_HKM.${basename_time_part}.L1B.hdf
full_ancfile=${working_dir}/${basename_part}.L1B.hdf.anc
level2_SFREFL_file=${working_dir}/${basename_part}.L2.SFREFL.nc
level2_SFREFL500_file=${working_dir}/${basename_part}.L2.SFREFL500.nc
level2_SFREFL250_file=${working_dir}/${basename_part}.L2.SFREFL250.nc
level2_SST_file=${working_dir}/${basename_part}.L2.SST.nc
level2_IOP_file=${working_dir}/${basename_part}.L2.IOP.sub.nc
level2_LAND_file=${working_dir}/${basename_part}.L2.LAND.sub.nc
level2_OC_file=${working_dir}/${basename_part}.L2.OC.nc
level2_custom_file=${working_dir}/${basename_part}.L2.custom.nc

# Extract files to be created
level1A_extract_file=${working_dir}/${basename_part}.L1A.sub.nc
geo_extract_file=${working_dir}/${basename_part}.GEO.sub.nc
level1B_extract_file=${working_dir}/${basename_part}.L1B.sub.hdf
level1B_qkm_extract_file=${working_dir}/${basename_mission_part}_QKM.${basename_time_part}.L1B.sub.hdf
level1B_hkm_extract_file=${working_dir}/${basename_mission_part}_HKM.${basename_time_part}.L1B.sub.hdf
extract_ancfile=${working_dir}/${basename_part}.L1B.sub.hdf.anc
level2_SFREFL_extract_file=${working_dir}/${basename_part}.L2.SFREFL.sub.nc
level2_SFREFL500_extract_file=${working_dir}/${basename_part}.L2.SFREFL500.sub.nc
level2_SFREFL250_extract_file=${working_dir}/${basename_part}.L2.SFREFL250.sub.nc
level2_SST_extract_file=${working_dir}/${basename_part}.L2.SST.sub.nc
level2_IOP_extract_file=${working_dir}/${basename_part}.L2.IOP.sub.nc
level2_LAND_extract_file=${working_dir}/${basename_part}.L2.LAND.sub.nc
level2_OC_extract_file=${working_dir}/${basename_part}.L2.OC.sub.nc
level2_custom_extract_file=${working_dir}/${basename_part}.L2.custom.sub.nc

program=modis_GEO
command="get_output_name $level1A_file ${program}"
echo "#**************************************"
echo "# Example of getting default program output file name for source file"
echo "# ifile=$level1A_file"
echo "# program=${program}"
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

ifile=$level1A_file
ofile=$geo_full_file
command="modis_GEO -o ${ofile} ${ifile}"
short_command="modis_GEO ${ifile}"
default_ofile=$(get_output_name ${ifile} modis_GEO)
echo "#**************************************"
echo "# Creating MODIS GEO File (Full Scene)"
echo "# ifile=$ifile"
echo "# ofile=$ofile"
echo "# Optional command without ofile:"
echo "#     ${short_command}"
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



if [ ${make_extract} -eq 1 ]; then
    ifile=${level1A_file}
    ofile=${level1A_extract_file}
    geofile=${geo_full_file}
    ../workflow_L1Aextract_modis.sh ${ifile} ${swlon} ${swlat} ${nelon} ${nelat} -g ${geofile} -o ${ofile} ${option_c}
    if [ $? -ne 0 ]; then
        echo "ERROR: workflow_L1Aextract_modis.sh failed"
        echo "../workflow_L1Aextract_modis.sh ${ifile} ${swlon} ${swlat} ${nelon} ${nelat} -g ${geofile} -o ${ofile} ${option_c}"
        exit 1
    fi
    echo " "
fi



if [ ${make_extract} -eq 1 ]; then
    ifile=$level1A_extract_file
    ofile=$geo_extract_file
    command="modis_GEO -o ${ofile} ${ifile}"
    short_command="modis_GEO ${ifile}"
    default_ofile=$(get_output_name ${ifile} modis_GEO)
    echo "#**************************************"
    echo "# Creating MODIS GEO File (Extract)"
    echo "# ifile=$ifile"
    echo "# ofile=$ofile"
    echo "# Optional command without ofile:"
    echo "#     ${short_command}"
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
fi

if [ ${make_extract} -eq 1 ]; then
    ifile=$level1A_extract_file
    geofile=$geo_extract_file
    ofile=$level1B_extract_file
    command="modis_L1B -o ${level1B_extract_file} -k ${level1B_hkm_extract_file} -q ${level1B_qkm_extract_file} ${ifile} ${geofile}"
    short_command="modis_L1B ${ifile} ${geofile}"
    default_ofile=$(get_output_name ${ifile} modis_L1B)
    echo "#**************************************"
    echo "# Creating MODIS Level-1B File (Extract)"
    echo "# ifile=${ifile}"
    echo "# geofile=${geofile}"
    echo "# 1kmfile=${level1B_extract_file}"
    echo "# hkmfile=${level1B_hkm_extract_file}"
    echo "# qkmfile=${level1B_qkm_extract_file}"
    echo "# Optional command without ofile:"
    echo "#     ${short_command}"
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
fi

if [ ${make_full} -eq 1 ]; then
    ifile=$level1A_file
    geofile=$geo_full_file
    ofile=$level1B_file
    command="modis_L1B -o ${level1B_file} -k ${level1B_hkm_file} -q ${level1B_qkm_file} ${ifile} ${geofile}"
    short_command="modis_L1B ${ifile} ${geofile}"
    default_ofile=$(get_output_name ${ifile} modis_L1B)
    echo "#**************************************"
    echo "# Creating MODIS Level-1B File (Full Scene)"
    echo "# ifile=${ifile}"
    echo "# geofile=${geofile}"
    echo "# 1kmfile=${level1B_file}"
    echo "# hkmfile=${level1B_file}"
    echo "# qkmfile=${level1B_qkm_file}"
    echo "# Optional command without ofile:"
    echo "#     ${short_command}"
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
fi

if [ ${make_extract} -eq 1 ]; then
    ifile=${level1B_extract_file}
    command="getanc ${ifile}"
    echo "#**************************************"
    echo "# Creating MODIS Ancillary File (Extract)"
    echo "# ifile=${ifile}"
    echo "# ofile=${extract_ancfile}"
    echo "#**************************************"
    echo "${command} > ${extract_ancfile}"
    echo " "
    #    echo "getanc_command=\`${command} > ${extract_ancfile}\` "
    if [ $show_commands_only -ne 1 ]; then
        getanc_command=$(${command} >${extract_ancfile})
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
    fi
    echo " "
fi

if [ ${make_full} -eq 1 ]; then
    ifile=${level1B_file}
    command="getanc ${ifile}"
    echo "#**************************************"
    echo "# Creating MODIS Ancillary File (Full Scene)"
    echo "# ifile=${ifile}"
    echo "# ofile=${full_ancfile}"
    echo "#**************************************"
    echo "${command} > ${full_ancfile}"
    echo " "
    #    echo "getanc_command=\`${command} > ${full_ancfile}\` "
    if [ $show_commands_only -ne 1 ]; then
        getanc_command=$(${command} >${full_ancfile})
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
    fi
    echo " "
fi

#  Run Level-2 Gen (Extracts)

if [ ${make_extract} -eq 1 ]; then
    option_e="-e"

    ifile=$level1B_extract_file
    geofile=$geo_extract_file
    ancfile=${extract_ancfile}
    ofile=$level2_OC_extract_file

    ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "OC" -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then
        echo "ERROR"
        exit 1
    fi
    echo " "
fi

if [ ${make_extract} -eq 1 ]; then
    option_e="-e"
    parfile="../l2gen_custom.par"
    ifile=$level1B_extract_file
    geofile=$geo_extract_file
    ofile=$level2_custom_extract_file
    ancfile=${extract_ancfile}

    ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then
        echo "ERROR"
        exit 1
    fi
    echo " "
fi

#  Run Level-2 Gen (Extracts)

if [ ${make_extract} -eq 1 ]; then
    option_e="-e"

    ifile=$level1B_extract_file
    geofile=$geo_extract_file
    ancfile=${extract_ancfile}

    if [ ${make_extras} -eq 1 ]; then
        ofile=$level2_SST_extract_file
        ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "SST" -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
        echo " "

        ofile=$level2_IOP_extract_file
        ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "IOP" -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
        echo " "

        ofile=$level2_LAND_extract_file
        ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "LAND" -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
        echo " "

        ofile=$level2_SFREFL_extract_file
        ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "SFREFL" -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
        echo " "

        ofile=$level2_SFREFL500_extract_file
        ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "SFREFL500" -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
        echo " "

        ofile=$level2_SFREFL250_extract_file
        ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "SFREFL250" -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
        echo " "
    fi

fi

#  Run Level-2 Gen (Full Scene)

if [ ${make_full} -eq 1 ]; then
    option_e=""

    ifile=$level1B_file
    geofile=$geo_full_file
    ancfile=${full_ancfile}

    ofile=$level2_OC_file
    ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "OC" -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then
        echo "ERROR"
        exit 1
    fi
    echo " "
fi

if [ ${make_full} -eq 1 ]; then
    option_e=""

    parfile="../l2gen_custom.par"
    ifile=$level1B_file
    geofile=$geo_full_file
    ofile=$level2_custom_file
    ancfile=${full_ancfile}

    ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then
        echo "ERROR"
        exit 1
    fi
    echo " "
fi

#  Run Level-2 Gen (Full Scene)

if [ ${make_full} -eq 1 ]; then
    option_e=""

    ifile=$level1B_file
    geofile=$geo_full_file
    ancfile=${full_ancfile}

    if [ ${make_extras} -eq 1 ]; then
        ofile=$level2_SST_file
        ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "SST" -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
        echo " "

        ofile=$level2_IOP_file
        ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "IOP" -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
        echo " "

        ofile=$level2_LAND_file
        ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "LAND" -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
        echo " "

        ofile=$level2_SFREFL_file
        ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "SFREFL" -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then
            echo "ERROR"
            exit 1
        fi
        echo " "

        #        ofile=$level2_SFREFL500_file
        #         ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "SFREFL500" -m ${mission} ${option_e} ${option_c}
        #        if [ $? -ne 0 ]; then echo "ERROR"; exit 1; fi; echo " "
        #
        #        ofile=$level2_SFREFL250_file
        #         ../workflow_l2gen.sh -i ${ifile} -g ${geofile} -o ${ofile} -a ${ancfile} -s "SFREFL250" -m ${mission} ${option_e} ${option_c}
        #        if [ $? -ne 0 ]; then echo "ERROR"; exit 1; fi; echo " "
    fi
fi
