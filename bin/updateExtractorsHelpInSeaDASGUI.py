import argparse
import os
import re
import shutil
import subprocess
import sys

parser = argparse.ArgumentParser(prog="updateCommandLineHelpInSeaDASGUI")
parser.add_argument("directory", nargs='?',
                      help="The directory that has OCSSW processors' help files", metavar="DIRECTORY")
args = parser.parse_args()
if args.directory is None:
    parser.print_help()
    sys.exit(1)
directory = args.directory
# directory = "/Users/bingyangbyang8/snap-11/seadas-toolbox/seadas-processing/src/main/resources/gov/nasa/gsfc/seadas/processing/docs/processors"
path_html = os.path.join(directory, "ProcessExtractors.html")
file_opened = open(path_html).read()
help_content_old = re.findall(r'<!--AUTOMATED CODE HELP START-->([\s\S]+?)<!--AUTOMATED CODE HELP END-->', file_opened)
if help_content_old:
    command = 'l1aextract_modis'
    useless_cat_call = subprocess.Popen([command], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    output, errors = useless_cat_call.communicate()
    useless_cat_call.wait()
    # print('start of new help')
    # print('start of l1aextract_modis help')
    output1 = '\n<h5>l1aextract_modis</h5>\n' + '<pre>\n' + output + '</pre>\n' 
    # print(output1)
    # print('end of l1aextract_modis help')

    command = 'l1aextract_viirs'
    useless_cat_call = subprocess.Popen([command], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    output, errors = useless_cat_call.communicate()
    useless_cat_call.wait()
    # print('start of l1aextract_viirs help')
    output2 = output1 + '\n<hr>' + '\n<h5>l1aextract_viirs</h5>' + '\n<pre>\n' + output + '\n</pre>\n'
    # print(output2)
    # print('end of l1aextract_viirs help')
    
    command = 'l1aextract_seawifs'
    useless_cat_call = subprocess.Popen([command], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    output, errors = useless_cat_call.communicate()
    useless_cat_call.wait()
    output = output.replace('<', '&lt;')
    output = output.replace('>', '&gt;')
    output = '\nUsage' + output.split('Usage')[1]
    # print('start of l1aextract_seawifs help')
    output3 = output2 + '\n<hr>' + '\n<h5>l1aextract_seawifs</h5>' + '\n<pre>\n' + output + '\n</pre>\n'
    # print(output3)
    # print('end of l1aextract_seawifs help')

    command = 'l2extract'
    useless_cat_call = subprocess.Popen([command], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    output, errors = useless_cat_call.communicate()
    useless_cat_call.wait()
    output = output.replace('<', '&lt;')
    output = output.replace('>', '&gt;')
    # print('start of l2aextract help')
    help_content_new =  output3 + '\n<hr>' + '\n<h5>l2aextract</h5>' + '\n<pre>\n' + output + '\n</pre>\n'
    # print(help_content_new)
    # print('end of l2aextract help')
    Replace = file_opened.replace(help_content_old[0], help_content_new)

    path_tmp_file = os.path.join(directory, 'oldHTMLWithNewHelpContents.html')
    File = open(path_tmp_file,'w')
    File.write(Replace)
    File.close()
    shutil.move(path_tmp_file, path_html)
    
