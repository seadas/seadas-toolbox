import os
import argparse
import sys
import shutil
import re
import subprocess

parser = argparse.ArgumentParser(prog="updateCommandLineHelpInSeaDASGUI")
parser.add_argument("directory", nargs='?',
                      help="The directory that has OCSSW processors' help files", metavar="DIRECTORY")
args = parser.parse_args()
if args.directory is None:
    parser.print_help()
    sys.exit(1)
directory = args.directory
# directory = "/Users/bingyang/snap-dev/seadas-toolbox/seadas-processing/src/main/resources/gov/nasa/gsfc/seadas/processing/docs/processors"
for filename in os.listdir(directory):
    print(filename)
    if (re.search('ProcessExtractors', filename) == None):
        #    re.search('ProcessMapgen', filename) == None):
        path_html_file = os.path.join(directory, filename)
        if (re.search('ProcessMapgen', filename)):
            File_html = open(path_html_file, 'r', encoding='latin-1')
        else:
            File_html = open(path_html_file, 'r')
        file_opened = File_html.read()
        help_content_old = re.findall(r'<!--AUTOMATED CODE HELP START-->([\s\S]+?)<!--AUTOMATED CODE HELP END-->', file_opened)
        if help_content_old:
#             print('found automated code help start')
#             print(help_content_old[0])
            if (re.search('ProcessL2extract', filename)):
                command = 'l2extract'
            elif (re.search('ProcessL1aextract_seawifs', filename)):
                command = 'l1aextract_seawifs'
            elif (re.search('ProcessL1aextract_modis', filename)):
                command = 'l1aextract_modis'
            elif (re.search('ProcessL1aextract_viirs', filename)):
                command = 'l1aextract_viirs'
            elif (re.search('ProcessGeolocate_hawkeye', filename)):
                command = 'geolocate_hawkeye'
            elif (re.search('InstallOCSSW', filename)):
                command = 'install_ocssw'
            elif (re.search('ProcessL3bin', filename)):
                command = 'l3bin'
            elif (re.search('ProcessUpdateLuts', filename)):
                command = 'update_luts'
            else:    
                command_part = re.split('sage:', help_content_old[0])
                if command_part:
                    command = command_part[1].split()[0]
            useless_cat_call = subprocess.Popen([command, '-h'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            output, errors = useless_cat_call.communicate()
            useless_cat_call.wait()
            if (re.search('l1aextract_seawifs', output)):
                output = '\nUsage' + output.split('Usage')[1]
            output = output.replace('<', '&lt;')
            output = output.replace('>', '&gt;')
            help_content_new = '\n' + '<pre>' + '\n' + output + '\n' + '</pre>' +'\n'
            Replace = file_opened.replace(help_content_old[0], help_content_new)
            path_temp_file = os.path.join(directory, 'temp_file_new_help_content')
            File = open(path_temp_file,'w')
            File.write(Replace)
            File.close()
            File_html.close()
            file_opened = ''
            shutil.move(path_temp_file, path_html_file)
            print('done with ' + filename)
               

