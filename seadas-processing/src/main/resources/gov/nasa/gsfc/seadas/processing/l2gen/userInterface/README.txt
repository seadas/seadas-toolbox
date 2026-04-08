To initialize the default xml file for l2gen run the following and put them in seadas-toolbox:

l2gen ifile=PACE_OCI.20250814T175605.L1B.V3.sub.nc prodxmlfile=productInfo.xml

l2gen ifile=PACE_OCI.20250814T175605.L1B.V3.sub.nc --dump_options_xmlfile=paramInfo.xml

mv productInfo.xml seadas-toolbox/seadas-processing/src/main/resources/gov/nasa/gsfc/seadas/processing/l2gen/userInterface/
mv paramInfo.xml seadas-toolbox/seadas-processing/src/main/resources/gov/nasa/gsfc/seadas/processing/l2gen/userInterface/





