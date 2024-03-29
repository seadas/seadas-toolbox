#!/usr/bin/env bash

# This script starts with a L1A file
show_info=0

show_commands_only=0
extras=0

extract=0
make_extras=0
make_extract=0 # hardcoded temporary
structured_directories=0
product_name_part="OC" # default
mission_short="MODIS"

product="chlor_a"
    extract_chlor_a=1


only_1_file=0

Usage() {
    # Display Help
    echo "Usage: workflow_l2bin_l3mapgen.sh"
    #    echo "./workflow2.sh -i Workflow1/AQUA_MODIS.20230116T190501.L2.custom.sub.nc -b AQUA_MODIS -t 20230116T190501   -c -p custom -x"
    #    echo " ./workflow2.sh -i Workflow1/level2_files.txt -b AQUA_MODIS -t 20230116.Composite   -c -p custom -x"
    #    echo " ./workflow2.sh -i Workflow1/level2_files.txt -b AQUA_MODIS -t 20230116.Composite   -c -p OC -x"
    echo " ./workflow2.sh -b AQUA_MODIS -i Workflow2/AQUA_MODIS.20230111T185000.L2.OC.chlor_a.sub.nc -t 20230111T185000 -x -d"
    echo " ./workflow2.sh -i Workflow2/AQUA_MODIS.20230107T191501.L2.OC.nc -b AQUA_MODIS -t 20230107T191501 -x"
    echo " ./workflow2.sh -i Workflow2/level2_files.txt -b CROSS_SENSOR -t 20230116.Composite -p OC -c"
    echo " ./workflow2.sh -i Workflow3/level2_files.txt -b CROSS_SENSOR -t 20230106_20230107.Composite -p OC"
    echo " ./workflow2.sh -i ./Jan07/level2_files.txt -b CROSS_SENSOR -t 20230107.Composite   -p OC"
    echo " ./workflow2.sh -i ./Jan06/level2_files.txt -b CROSS_SENSOR -t 20230106.Composite   -p OC"
    echo " ./workflow2.sh -i Workflow2/AQUA_MODIS.20230116T190501.L2.OC.nc -b AQUA_MODIS -t 20230116T190501 -p "OC" -c"
    echo " ./workflow2.sh -i Workflow2/S3A_OLCI_EFRNT.20230116T151449.L2.OC.nc -b S3A_OLCI_EFRNT -t 20230116T151449 -p "OC" -c"
    echo " ./workflow2.sh -i Workflow2/SNPP_VIIRS.20230116T181800.L2.OC.nc -b SNPP_VIIRS -t 20230116T181800 -p "OC" -c"
    echo
    echo "options:"
    echo "i     ifile"
    echo "b     basename_mission_namepart"
    echo "t     time_namepart"
    echo "p     product_name_part"
    echo "d     structured_directories"
    echo "r     resolution"
    echo "s     suite"
    echo "m     mission_short"
    echo "x     level-2 file(s) are extract(s)" # this just adds the 'sub' extension to output files
    echo "e     make_extra_files"
    echo "E     only_1_file"
    echo "c     show_commands_only"
    echo
    echo "Usage example: Workflow1/workflow1.sh -i A2023016190500.L1A_LAC -b AQUA_MODIS -t 20230116T190501 -e -c"
    echo "Usage example: Workflow1/workflow1.sh -i A2023016190500.L1A_LAC -b AQUA_MODIS -t 20230116T190501 -e -f -x -c -d"
    echo
}

while getopts "hi:b:t:p:s:m:r:eExXcd" option; do
    case $option in
    h)
        Usage
        exit
        ;;
    i) ifile=$OPTARG ;;
    b) basename_mission_namepart=$OPTARG ;;
    t) time_namepart=$OPTARG ;;
    p) product_name_part=$OPTARG ;;
    s) suite="$OPTARG" ;;
    d) structured_directories=1 ;;
    m) mission_short="$OPTARG" ;; # short name (just instrument)
    r) resolution=$OPTARG ;;
    x) extract=1 ;;
    X) make_extract=1 ;;
    e) make_extras=1 ;;
    E) only_1_file=1 ;;
    c) show_commands_only=1 ;;
    \?) # Invalid option
        echo "Error: Invalid option"
        exit
        ;;
    esac
done

if [ ${extract} -eq 1 ]; then
    option_e="-e"
else
    option_e=""
fi

if [ ${mission_short} == "VIIRS" ]; then
    #VIIRS
    nelon=-81.5
    nelat=30.5
    swlon=-85.5
    swlat=23.5
elif [ ${mission_short} == "OLCI" ]; then
    #OLCI
    nelon=-81.5
    nelat=28.5
    swlon=-84.0
    swlat=26.5
else
    #MODIS
    nelon=-81.5
    nelat=30.5
    swlon=-85.0
    swlat=23.5
fi

# Override with Jerome values
#nelon=-60.0
#nelat=46.0
#swlon=-75.0
#swlat=40.0

#Override to not extract much and allow Gulf (set at extra 4 degrees over mapped region goal)
    nelon=-76.0
    nelat=35.0
    swlon=14.0
    swlat=23.5

#echo "mission=${mission}"
#echo "swlat=${swlat}"
#exit

#dir_delimitor='/';

if [ ! -z $ifile ]; then
    input_level2_OC_file=$ifile
    input_level2_OC_file_basename=$(basename "$ifile")
    working_dir=$(dirname "$ifile")
else
    echo ${usage}
    exit 1
fi

if [ ! -z ${basename_mission_namepart} ]; then
    basename_mission_part=${basename_mission_namepart}
else
    echo ${usage}
    exit 1
fi

if [ ! -z $time_namepart ]; then
    basename_time_part=$time_namepart
else
    echo ${usage}
    exit 1
fi

echo "####################################"
echo "# The following is a workflow of UNIX commands to generate the SeaDAS Training Demo files"
echo "# The input file is: ${input_level2_OC_file}"
echo "# https://oceandata.sci.gsfc.nasa.gov/getfile/${input_level2_OC_file}"
echo "# Put this Level1A file in an empty directory and then go to this directory at the command line"
echo "# You can cut and paste each of these commands sequentially and run them"
echo "# If you wish to do this from the GUI these commands can help aid you in filling out the GUI"
echo "######################################"

if [ $show_commands_only -eq 1 ]; then option_c="-c"; else option_c=""; fi

mission=${basename_mission_part}

if [ $show_info -eq 1 ]; then
    echo "#**************************************"
    echo "# Showing OCSSWROOT Environment Variable"
    echo "#**************************************"
    echo 'echo $OCSSWROOT'
    echo " "
fi
if [ $show_commands_only -ne 1 ]; then
    echo $OCSSWROOT
    if [ $? -ne 0 ]; then
        echo "ERROR: $OCSSWROOT/OCSSW_bash.env failed to run"
        exit 1
    fi
fi
echo " "

if [ $show_info -eq 1 ]; then

    echo "#**************************************"
    echo "# Setting up the SeaDAS-OCSSW Processor Environment"
    echo "#**************************************"
    echo 'source $OCSSWROOT/OCSSW_bash.env'
    echo " "
fi
source $OCSSWROOT/OCSSW_bash.env
if [ $show_commands_only -ne 1 ]; then
    if [ $? -ne 0 ]; then
        echo "ERROR"
        exit 1
    fi
fi

if [ $show_info -eq 1 ]; then

    echo "#**************************************"
    echo "# Verifying SeaDAS-OCSSW Processors Exist in Environment Path"
    echo "#**************************************"
    echo 'which l2gen'
    echo " "
fi

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

basename_part=${basename_mission_part}.${basename_time_part}

if [ ! -e "${input_level2_OC_file}" ]; then
    echo "ERROR: ${input_level2_OC_file} does not exist"
    exit 1
fi

if [ ! -z ${working_dir} ]; then

    command="cd ${working_dir}"
    if [ $show_info -eq 1 ]; then
        echo "#**************************************"
        echo "# Changing directory to run programs in same directory as level-1A file"
        echo "#**************************************"
        echo "${command}"
        echo " "
    fi

    ${command}
    if [ $? -ne 0 ]; then
        echo "ERROR"
        exit 1
    fi
    echo " "
fi

# Now working dir is the current directory so change the variable
# Showing relative paths to make commands not dependent on any user directory tree
working_dir=""
#echo "working_dir=${working_dir}"
#echo "pwd=`pwd`"
input_level2_OC_file=${working_dir}${input_level2_OC_file_basename}

pardir=""
if [ ${structured_directories} -eq 1 ]; then
    pardir="par/"
    extracts_dir="output/"
    extracts_stpeter_dir="${extracts_dir}L3m_SaintPeter/"
    extracts_gulf_dir="${extracts_dir}L3m_Gulf/"
    extracts_florida_dir="${extracts_dir}L3m_WestCoastFlorida/"
    extracts_global_dir="${extracts_dir}L3m_Global/"
    extracts_scene_dir="${extracts_dir}L3m_Scene/"
    extracts_binned_dir="${extracts_dir}L3b/"

    if [ ! -e ${working_dir} ]; then mkdir ${working_dir}; fi
    if [ ! -e ${working_dir}${full_scene_dir} ]; then mkdir ${working_dir}${full_scene_dir}; fi
    if [ ! -e ${working_dir}${extracts_dir} ]; then mkdir ${working_dir}${extracts_dir}; fi
    if [ ! -e ${working_dir}${extracts_stpeter_dir} ]; then mkdir ${working_dir}${extracts_stpeter_dir}; fi
    if [ ! -e ${working_dir}${extracts_gulf_dir} ]; then mkdir ${working_dir}${extracts_gulf_dir}; fi
    if [ ! -e ${working_dir}${extracts_florida_dir} ]; then mkdir ${working_dir}${extracts_florida_dir}; fi
    if [ ! -e ${working_dir}${extracts_global_dir} ]; then mkdir ${working_dir}${extracts_global_dir}; fi
    if [ ! -e ${working_dir}${extracts_scene_dir} ]; then mkdir ${working_dir}${extracts_scene_dir}; fi
    if [ ! -e ${working_dir}${extracts_binned_dir} ]; then mkdir ${working_dir}${extracts_binned_dir}; fi
else
    full_scene_dir=""
    extracts_dir=""
    extracts_stpeter_dir=""
    extracts_gulf_dir=""
    extracts_florida_dir=""
    extracts_global_dir=""
    extracts_scene_dir=""
    extracts_binned_dir=""
    full_scene_binned_dir=""
fi

# Extract files to be created
#level2_LAND_extract_file=${working_dir}/${basename_part}.L2.LAND.sub.nc
#input_level2_OC_file=${working_dir}/${basename_part}.L2.OC.sub.nc

#level3binned_LAND_extract_1km_file=${working_dir}${extracts_binned_dir}/${basename_part}.L3b.LAND.1km.sub.nc

if [ ${extract} -eq 1 ]; then
    extension="sub.nc"
else
    extension="nc"
fi

level3binned_OC_250m_file=${working_dir}${extracts_binned_dir}${basename_part}.L3b.${product_name_part}.${product}.250m.${extension}
#level3binned_OC_500m_file=${working_dir}${extracts_binned_dir}${basename_part}.L3b.${product_name_part}.${product}.500m.${extension}
level3binned_OC_1km_file=${working_dir}${extracts_binned_dir}${basename_part}.L3b.${product_name_part}.${product}.1km.${extension}
level3binned_OC_2km_file=${working_dir}${extracts_binned_dir}${basename_part}.L3b.${product_name_part}.${product}.2km.${extension}
level3binned_OC_4km_file=${working_dir}${extracts_binned_dir}${basename_part}.L3b.${product_name_part}.${product}.4km.${extension}

level3binned_OC_1km_DEFAULTFLAGS_file=${working_dir}${extracts_binned_dir}${basename_part}.L3b.${product_name_part}.${product}.1km.OCFLAGS.${extension}

level3mapped_OC_1km_file=${working_dir}${extracts_scene_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.smi.${extension}
level3mapped_OC_2km_file=${working_dir}${extracts_scene_dir}${basename_part}.L3m.${product_name_part}.${product}.2km.smi.${extension}
level3mapped_OC_4km_file=${working_dir}${extracts_scene_dir}${basename_part}.L3m.${product_name_part}.${product}.4km.smi.${extension}
level3mapped_OC_extract_1km_DEFAULTFLAGS_smi_file=${working_dir}${extracts_scene_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.OCFLAGS.smi.${extension}

level3mapped_chlor_a_1km_500m_smi_scene_file=${working_dir}${extracts_scene_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.500m.f2.smi.${extension}
level3mapped_chlor_a_1km_500m_aea_scene_file=${working_dir}${extracts_scene_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.500m.f2.aea.${extension}

level3mapped_OC_250m_smi_gulf_file=${working_dir}${extracts_gulf_dir}${basename_part}.L3m.${product_name_part}.${product}.250m.smi.GulfMexico.${extension}
level3mapped_OC_250m_f1_5_smi_gulf_file=${working_dir}${extracts_gulf_dir}${basename_part}.L3m.${product_name_part}.${product}.250m.f1.5.smi.GulfMexico.${extension}
level3mapped_OC_250m_f4_smi_gulf_file=${working_dir}${extracts_gulf_dir}${basename_part}.L3m.${product_name_part}.${product}.250m.f4.smi.GulfMexico.${extension}
level3mapped_OC_1km_smi_gulf_file=${working_dir}${extracts_gulf_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.smi.GulfMexico.${extension}
level3mapped_OC_1km_aea_gulf_file=${working_dir}${extracts_gulf_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.aea.GulfMexico.${extension}
level3mapped_OC_2km_smi_gulf_file=${working_dir}${extracts_gulf_dir}${basename_part}.L3m.${product_name_part}.${product}.2km.smi.GulfMexico.${extension}
level3mapped_OC_2km_aea_gulf_file=${working_dir}${extracts_gulf_dir}${basename_part}.L3m.${product_name_part}.${product}.2km.aea.GulfMexico.${extension}

level3mapped_chlor_a_1km_500m_smi_westcoastflorida_file=${working_dir}${extracts_florida_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.500m.f2.smi.FLwestcoast.${extension}
level3mapped_chlor_a_250m_smi_WestCoastFlorida_file=${working_dir}${extracts_florida_dir}${basename_part}.L3m.${product_name_part}.${product}.250m.smi.FLwestcoast.${extension}
level3mapped_chlor_a_1km_smi_westcoastflorida_file=${working_dir}${extracts_florida_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.smi.FLwestcoast.${extension}
level3mapped_chlor_a_1km_aea_westcoastflorida_file=${working_dir}${extracts_florida_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.aea.FLwestcoast.${extension}

level3mapped_chlor_a_250m_150m_smi_StPeter_file=${working_dir}${extracts_stpeter_dir}${basename_part}.L3m.${product_name_part}.${product}.250m.150m.smi.FLstpeter.${extension}
level3mapped_chlor_a_250m_smi_StPeter_file=${working_dir}${extracts_stpeter_dir}${basename_part}.L3m.${product_name_part}.${product}.250m.smi.FLstpeter.${extension}
level3mapped_chlor_a_250m_smi_scene_file=${working_dir}${extracts_scene_dir}${basename_part}.L3m.${product_name_part}.${product}.250m.smi.${extension}
level3mapped_OC_extract_1km_smi_stpeter_file=${working_dir}${extracts_stpeter_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.smi.FLstpeter.${extension}
level3mapped_OC_extract_1km_100m_smi_stpeter_file=${working_dir}${extracts_stpeter_dir}${basename_part}.L3m.${product_name_part}.${product}.1km.100m.smi.FLstpeter.${extension}

level3mapped_OC_extract_18km_cea_global_file=${working_dir}${extracts_global_dir}${basename_part}.L3m.${product_name_part}.${product}.18km.cea.global.${extension}
level3mapped_OC_extract_18km_smi_global_file=${working_dir}${extracts_global_dir}${basename_part}.L3m.${product_name_part}.${product}.18km.smi.global.${extension}
level3mapped_OC_extract_9km_cea_global_file=${working_dir}${extracts_global_dir}${basename_part}.L3m.${product_name_part}.${product}.9km.cea.global.${extension}
level3mapped_OC_extract_9km_smi_global_file=${working_dir}${extracts_global_dir}${basename_part}.L3m.${product_name_part}.${product}.9km.smi.global.${extension}

ifile=${input_level2_OC_file}

if [ ${make_extract} -eq 1 ]; then

    if [ ${extract_chlor_a} -eq 1 ]; then
        level2_OC_extract_file=${working_dir}${basename_part}.L2.OC.chlor_a.sub.nc

        ofile=${level2_OC_extract_file}
        ../workflow_L2extract.sh ${ifile} ${swlon} ${swlat} ${nelon} ${nelat} -o ${ofile} ${option_c} -l "chlor_a"
        if [ $? -ne 0 ]; then
            echo "ERROR: workflow_2extract.sh failed"
            echo "../workflow_2extract.sh ${ifile} ${swlon} ${swlat} ${nelon} ${nelat}  -o ${ofile} ${option_c} -l chlor_a"
            exit 1
        fi
        echo " "

    else
              level2_OC_extract_file=${working_dir}${basename_part}.L2.OC.sub.nc

              ofile=${level2_OC_extract_file}
              ../workflow_L2extract.sh ${ifile} ${swlon} ${swlat} ${nelon} ${nelat} -o ${ofile} ${option_c}
              if [ $? -ne 0 ]; then
                  echo "ERROR: workflow_2extract.sh failed"
                  echo "../workflow_2extract.sh ${ifile} ${swlon} ${swlat} ${nelon} ${nelat} -o ${ofile} ${option_c}"
                  exit 1
              fi
    fi

    input_level2_OC_file=${ofile}

    ifile=${input_level2_OC_file}

fi

###########
# BINNING
###########

ifile=${input_level2_OC_file}
ofile=${level3binned_OC_1km_file}
parfile="${pardir}l2bin_${product}_1km.par"
../workflow_l2bin.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
if [ $? -ne 0 ]; then exit 1; fi
if [ ${make_extras} -eq 1 ]; then
    ifile=${input_level2_OC_file}
    ofile=${level3binned_OC_1km_DEFAULTFLAGS_file}
    parfile="${pardir}l2bin_${product}_OCFLAGS_1km.par"
    ../workflow_l2bin.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi
fi

if [ ${make_extras} -eq 1 ]; then

    ifile=${input_level2_OC_file}
    ofile=${level3binned_OC_2km_file}
    parfile="${pardir}l2bin_${product}_2km.par"
    ../workflow_l2bin.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ifile=${input_level2_OC_file}
    ofile=${level3binned_OC_4km_file}
    parfile="${pardir}l2bin_${product}_4km.par"
    ../workflow_l2bin.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi
fi

######
# OLCI
######
if [ ${mission_short} == "OLCI" ]; then

    ifile=${input_level2_OC_file}
    ofile=${level3binned_OC_250m_file}
    parfile="${pardir}l2bin_${product}_250m.par"
    ../workflow_l2bin.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ifile=${level3binned_OC_250m_file}
    ofile=${level3mapped_OC_250m_smi_gulf_file}
    parfile="${pardir}l3mapgen_chlor_a_250m_smi.par"
    parfile2="${pardir}l3mapgen_region_GulfMexico.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ifile=${level3binned_OC_250m_file}
    ofile=${level3mapped_OC_250m_f1_5_smi_gulf_file}
    parfile="${pardir}l3mapgen_chlor_a_250m_f1.5_smi.par"
    parfile2="${pardir}l3mapgen_region_GulfMexico.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

        ifile=${level3binned_OC_250m_file}
        ofile=${level3mapped_OC_250m_f4_smi_gulf_file}
        parfile="${pardir}l3mapgen_chlor_a_250m_f4_smi.par"
        parfile2="${pardir}l3mapgen_region_GulfMexico.par"
        ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then exit 1; fi



    ifile=${level3binned_OC_1km_file}
    ofile=${level3mapped_OC_1km_smi_gulf_file}
    parfile="${pardir}l3mapgen_chlor_a_1km_smi.par"
    parfile2="${pardir}l3mapgen_region_GulfMexico.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi


    ifile=${level3binned_OC_250m_file}
    ofile=${level3mapped_chlor_a_250m_150m_smi_StPeter_file}
    parfile="${pardir}l3mapgen_chlor_a_150m_f2_smi.par"
    parfile2="${pardir}l3mapgen_region_FLstpeter.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ifile=${level3binned_OC_250m_file}
    ofile=${level3mapped_chlor_a_250m_smi_StPeter_file}
    parfile="${pardir}l3mapgen_chlor_a_250m_f1.5_smi.par"
    parfile2="${pardir}l3mapgen_region_FLstpeter.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ifile=${level3binned_OC_250m_file}
    ofile=${level3mapped_chlor_a_250m_smi_scene_file}
    parfile="${pardir}l3mapgen_chlor_a_250m_smi.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi
fi

if [ ${make_extras} -eq 1 ]; then
    ifile=${level3binned_OC_1km_DEFAULTFLAGS_file}
    ofile=${level3mapped_OC_extract_1km_DEFAULTFLAGS_smi_file}
    parfile="${pardir}l3mapgen_chlor_a_1km_smi.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi
fi

if [ ${mission_short} != "OLCI" ]; then



    ifile=${level3binned_OC_1km_file}
    ofile=${level3mapped_chlor_a_1km_500m_smi_westcoastflorida_file}
    parfile="${pardir}l3mapgen_chlor_a_500m_f2_smi.par"
    parfile2="${pardir}l3mapgen_region_FLwestcoast.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    if [ ${only_1_file} -eq 1 ]; then
        exit
    fi

    ifile=${level3binned_OC_1km_file}
    ofile=${level3mapped_chlor_a_1km_smi_westcoastflorida_file}
    parfile="${pardir}l3mapgen_chlor_a_1km_smi.par"
    parfile2="${pardir}l3mapgen_region_FLwestcoast.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ifile=${level3binned_OC_1km_file}
    ofile=${level3mapped_chlor_a_1km_500m_smi_scene_file}
    parfile="${pardir}l3mapgen_chlor_a_500m_f2_smi.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ifile=${level3binned_OC_1km_file}
    ofile=${level3mapped_OC_1km_file}
    parfile="${pardir}l3mapgen_chlor_a_1km_smi.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ifile=${level3binned_OC_1km_file}
    ofile=${level3mapped_chlor_a_1km_500m_aea_scene_file}
    parfile="${pardir}l3mapgen_chlor_a_500m_f2_aea.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi
fi

if [ ${make_extras} -eq 1 ]; then

    ifile=${level3binned_OC_1km_file}
    ofile=${level3mapped_chlor_a_1km_aea_westcoastflorida_file}
    parfile="${pardir}l3mapgen_chlor_a_1km_aea.par"
    parfile2="${pardir}l3mapgen_region_FLwestcoast.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi
    if [ ${mission_short} != "OLCI" ]; then

        ifile=${level3binned_OC_1km_file}
        ofile=${level3mapped_OC_extract_1km_smi_stpeter_file}
        parfile="${pardir}l3mapgen_chlor_a_1km_smi.par"
        parfile2="${pardir}l3mapgen_region_FLstpeter.par"
        ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P${parfile2} -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then exit 1; fi

        ifile=${level3binned_OC_1km_file}
        ofile=${level3mapped_OC_extract_1km_100m_smi_stpeter_file}
        parfile="${pardir}l3mapgen_chlor_a_100m_f20_smi.par"
        parfile2="${pardir}l3mapgen_region_FLstpeter.par"
        ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P${parfile2} -m ${mission} ${option_e} ${option_c}
        if [ $? -ne 0 ]; then exit 1; fi
    fi
    ifile=${level3binned_OC_1km_file}
    ofile=${level3mapped_OC_1km_aea_gulf_file}
    parfile="${pardir}l3mapgen_chlor_a_1km_aea.par"
    parfile2="${pardir}l3mapgen_region_GulfMexico.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ifile=${level3binned_OC_1km_file}
    ofile=${level3mapped_OC_1km_smi_gulf_file}
    parfile="${pardir}l3mapgen_chlor_a_1km_smi.par"
    parfile2="${pardir}l3mapgen_region_GulfMexico.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ######
    ifile=${level3binned_OC_2km_file}
    ######

    ofile=${level3mapped_OC_2km_file}
    parfile="${pardir}l3mapgen_chlor_a_2km_smi.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ofile=${level3mapped_OC_2km_aea_gulf_file}
    parfile="${pardir}l3mapgen_chlor_a_2km_aea.par"
    parfile2="${pardir}l3mapgen_region_GulfMexico.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ofile=${level3mapped_OC_2km_smi_gulf_file}
    parfile="${pardir}l3mapgen_chlor_a_2km_smi.par"
    parfile2="${pardir}l3mapgen_region_GulfMexico.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -P ${parfile2} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ######
    ifile=${level3binned_OC_4km_file}
    ######

    ofile=${level3mapped_OC_4km_file}
    parfile="${pardir}l3mapgen_chlor_a_4km_smi.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ofile=${level3mapped_OC_extract_18km_cea_global_file}
    parfile="${pardir}l3mapgen_chlor_a_18km_cea_global.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

    ofile=${level3mapped_OC_extract_18km_smi_global_file}
    parfile="${pardir}l3mapgen_chlor_a_18km_smi_global.par"
    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
    if [ $? -ne 0 ]; then exit 1; fi

#    ofile=${level3mapped_OC_extract_9km_cea_global_file}
#    parfile="${pardir}l3mapgen_chlor_a_9km_cea_global.par"
#    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
#    if [ $? -ne 0 ]; then exit 1; fi
#
#    ofile=${level3mapped_OC_extract_9km_smi_global_file}
#    parfile="${pardir}l3mapgen_chlor_a_9km_smi_global.par"
#    ../workflow_l3mapgen.sh -i ${ifile} -o ${ofile} -p ${parfile} -m ${mission} ${option_e} ${option_c}
#    if [ $? -ne 0 ]; then exit 1; fi
fi
