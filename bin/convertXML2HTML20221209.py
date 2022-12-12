import argparse
import os
import re
import shutil
import sys
from lxml import etree
import lxml.html

def writeToHtml(rootXml):
    for tocitem1 in rootXml:
        text = tocitem1.get("text")        
        tocitem1Existed = False
        if (text):
            for element in rootHtml:
                for element1 in element: 
                    if(element1.text != None):
    #                     print("element1's text is " + element1.text)
    #                     print("text is " + text)
                        tocitem1Existed = True
                        li1 = element1                                     
            if(not tocitem1Existed):
                li1 = etree.SubElement(body, "li")
                index = tocitem1.get("target")

                if((index) != None):
                    a = etree.SubElement(li1, "a")
                    a.text = text
                    a.set("href", mapDict[index])
                    li1.tail = "\n"
                else: 
                    li1.text = text + "\n"
            for tocitem2 in tocitem1:
                index = tocitem2.get("target")
                text = tocitem2.get("text")
                tocitem2Existed = False
                if (text):
                    if(tocitem1Existed):
                        for element in rootHtml:
                            for element1 in element: 
                                for element2 in element1:
                                    for element3 in element2:
        #                                 print(element3.tag, element3.text)
                                        if(len(element3) and element3.text and (element3.text == (text + "\n"))):
    #                                         print("  element3's text is " + element3.text)
    #                                         print("  text is " + text)
                                            tocitem2Existed = True
                                            li2 = element3
                    if(not tocitem2Existed):
                        ul = etree.SubElement(li1, "ul")
                        ul.text = "\n"
                        ul.tail = "\n"
                        li2 = etree.SubElement(ul, "li")
                        li2.tail = "\n"
                        if((index) != None):
                            a = etree.SubElement(li2, "a")
                            a.text = text
                            a.set("href", mapDict[index])
                            li2.tail = "\n"
                        else:
                            li2.text = text + "\n"
                for tocitem3 in tocitem2:
                    index = tocitem3.get("target")
                    text = tocitem3.get("text")
                    tocitem3Existed = False
                    if(text):
                        for element in rootHtml:
                            for element1 in element: 
                                for element2 in element1:
                                    for element3 in element2:
                                        if (element3.text == (tocitem2.get("text") + "\n")):
                                             for element4 in element3:
                                                for element5 in element4:
                                                    if(len(element5) and element5.text and (element5.text == (text + "\n"))):
    #                                                     print("    element3's text is " + element3.text)
    #                                                     print("    element5's text is " + element5.text)
    #                                                     print("    text is " + text)
                                                        tocitem3Existed = True
                                                        li3 = element5   
                        if (not tocitem3Existed):
                            ul = etree.SubElement(li2, "ul")
                            ul.text = "\n"
                            ul.tail = "\n"
                            li3 = etree.SubElement(ul, "li")
                            li3.tail = "\n"
                            if((index) != None):
                                a = etree.SubElement(li3, "a")
                                a.text = text
                                a.set("href", mapDict[index])
                                li3.tail = "\n"
                            else:
                                li3.text = text + "\n"
                    for tocitem4 in tocitem3:
                        index = tocitem4.get("target")
                        text = tocitem4.get("text")
                        tocitem4Existed = False
                        if(text):
                            for element in rootHtml:
                                for element1 in element: 
                                    for element2 in element1:
                                        for element3 in element2:
                                            if (element3.text == (tocitem2.get("text") + "\n")):
                                                for element4 in element3:
                                                    for element5 in element4:
                                                        for element6 in element5:
                                                            for element7 in element6:
                                                                if(len(element7) and element7.text and (element7.text == (text + "\n"))):
    #                                                                 print("      element7's text is " + element7.text)
    #                                                                 print("      text is " + text)
                                                                    tocitem4Existed = True
                                                                    li4 = element7   
                            if (not tocitem4Existed):
                                ul = etree.SubElement(li3, "ul")
                                ul.text = "\n"
                                ul.tail = "\n"
                                li4 = etree.SubElement(ul, "li")
                                li4.tail = "\n"
                                if((index) != None):
                                    a = etree.SubElement(li4, "a")
                                    a.text = text
    #                                 print(index)
                                    a.set("href", mapDict[index])
                                    li4.tail = "\n"
                                else:
                                    li4.text = text + "\n"
                            for tocitem5 in tocitem4:
                                index = tocitem5.get("target")
                                text = tocitem5.get("text")
    #                             tocitem5Existed = False
                                if (text):
                                    ul = etree.SubElement(li4, "ul")
                                    ul.text = "\n"
                                    ul.tail = "\n"
                                    li5 = etree.SubElement(ul, "li")
                                    li5.tail = "\n"
                                    if((index) != None):
                                        a = etree.SubElement(li5, "a")
                                        a.text = text
                                        a.set("href", mapDict[index])
                                        li5.tail = "\n"
                                    else:
                                        li5.text = text + "\n"
                                for tocitem6 in tocitem5:
                                    index = tocitem6.get("target")
                                    text = tocitem6.get("text")
        #                             tocitem1Existed = False
                                    if (text):
                                        ul = etree.SubElement(li5, "ul")
                                        ul.text = "\n"
                                        ul.tail = "\n"
                                        li6 = etree.SubElement(ul, "li")
                                        li6.tail = "\n"
                                        if((index) != None):
                                            a = etree.SubElement(li6, "a")
                                            a.text = text
                                            a.set("href", mapDict[index])
                                            li6.tail = "\n"
                                        else:
                                            li6.text = text + "\n"
    return

def replace_object_in_file(directory, filename):
    # directory = '/Users/bingyang/seadas8.3.0Help/docs/general/overview'
    # path_html_file = os.path.join(directory, 'SnapDataSources.html')
    path_html_file = os.path.join(directory, filename)
    File_html = open(path_html_file, 'r', encoding='latin-1')
    data = File_html.read()
    path_temp_file = os.path.join(directory, 'temp_file_new_content')
    File = open(path_temp_file,'w')
    content_old = re.findall(r'<object([\s\S]+?)</object>', data)
    i = 0
    if content_old:
        for item in content_old:
            # content_split = item.split('content" value="')[1]
            # link = (content_split.split('<param name="text" value="')[0]).strip()
            # text = content_split.split('<param name="text" value="')[1]
            content_split = (item.split('<param name="content"')[1])
            content_split = content_split.replace('value="', '')
            link = (content_split.split('<param name="text"')[0]).strip()
            text = content_split.split('<param name="text"')[1]
            if re.search('<param name="', text):
                text = text.split('<param name="')[0]
            text = text.replace('">', '').strip()
            content_new = '<a href="' + link + text + '</a>'
            data = data.replace('<object' + item + '</object>', content_new)
            i = i +1
            print('i = ')
            print(i)
            print('replace ' + item + ' with ' + content_new)
        File.write(data)
        File.close()
        File_html.close()
        shutil.move(path_temp_file, path_html_file)
        print("done with " + path_html_file)

def replace_object_dir(directory):
    # directory = '/Users/bingyang/seadas8.3.0Help/docs/general/overview'
    # path_html_file = os.path.join(directory, 'SnapDataSources.html')
    for filename in os.listdir(directory):
        if filename.endswith("html"):
            replace_object_in_file(directory, filename)

def copy_resources():
    for dirname in os.listdir(src):
#         print(dirname)
        src_dir_path = os.path.join(src, dirname)
        if src_dir_path.endswith("html"):
            shutil.copy(src_dir_path, os.path.join(dir_target, 'docs', dirname))
            replace_object_in_file(dir_target, filename)
        if os.path.isdir(src_dir_path):
#             print(src_dir_path)
            if(dirname.endswith("images")):
                # dest_dir_path = "/Users/bingyang/seadas8.2.0Help/docs/images"
                dest_dir_path = os.path.join(dir_target, 'docs', 'images')
                if(not os.path.isdir(dest_dir_path)):
                    os.mkdir(dest_dir_path)
                for filename in os.listdir(src_dir_path):
#                     print(filename)
                    if(not os.path.isfile(os.path.join(dest_dir_path, filename))):
                        shutil.copy(os.path.join(src_dir_path, filename), os.path.join(dest_dir_path, filename))
            else:
                # dest_dir_path = os.path.join("/Users/bingyang/seadas8.2.0Help/docs", dirname)
                dest_dir_path = os.path.join(dir_target, 'docs', dirname)
                if(not os.path.isdir(dest_dir_path)):
                    os.mkdir(dest_dir_path)
                for filename in os.listdir(src_dir_path):
                    if(filename.endswith("images")):
                        src_img_dir = os.path.join(src_dir_path, "images")
                        dest_img_dir = os.path.join(dest_dir_path, "images")
#                         print(src_img_dir)
#                         print(dest_img_dir)
                        if(os.path.isdir(dest_img_dir)):
#                             print("exists")
                            for filename in os.listdir(src_img_dir):
                                shutil.copy(os.path.join(src_img_dir, filename), os.path.join(dest_img_dir, filename))
                        else:
                            shutil.copytree(os.path.join(src_dir_path, filename), os.path.join(dest_dir_path, filename))
                    else:
                        if(not os.path.isfile(os.path.join(dest_dir_path, filename))):
                            if(os.path.isdir(os.path.join(src_dir_path, filename))):
                                shutil.copytree(os.path.join(src_dir_path, filename), os.path.join(dest_dir_path, filename))
                                replace_object_dir(os.path.join(dest_dir_path, filename))
                            else:
                                shutil.copy(os.path.join(src_dir_path, filename), os.path.join(dest_dir_path, filename))
                                if filename.endswith("html"):
                                    replace_object_in_file(dest_dir_path, filename)
    return

parser = argparse.ArgumentParser(prog="convertXML2HTML")
parser.add_argument("dir_src", nargs='?',
                      help="The source directory", metavar="SOURCE DIRECTORY")
parser.add_argument("dir_target", nargs='?',
                      help="The target directory", metavar="TARGET DIRECTORY")
args = parser.parse_args()
if args.dir_src is None or args.dir_target is None:
    parser.print_help()
    sys.exit(1)

dir_src = args.dir_src
dir_target = args.dir_target
rootHtml = etree.Element("html")
head = etree.SubElement(rootHtml, "head")
body = etree.SubElement(rootHtml, "body")

mapDict = dict(target="", url="")
# dir_src = "/Users/bingyang/snap-dev"
# dir_target = "/Users/bingyang/seadas8.3.0Help/"
filename_toc = os.path.join(dir_target)
dir_engine = os.path.join(dir_src, "snap-engine")
dir_desktop = os.path.join(dir_src, "snap-desktop")
dir_s3tbx = os.path.join(dir_src, "s3tbx")
dir_seadas = os.path.join(dir_src, "seadas-toolbox")

for root, dirs, files in os.walk(dir_desktop):
    for file in files:
        if file.endswith("toc.xml"):
            path = os.path.join(root, file)
#             print(path)
            place = path.find("/target/")
#             place2 = path.find("/snap-help/")
#             print("place2 is ")
#             print(place2)
            if(place != -1):
                if(path.find("/snap-help/") != -1):
#                     print("working on snap-help")
                    src = path[0:-8]
                    print(src)
                    copy_resources()
                    # dest = "/Users/bingyang/seadas8.2.0Help/snap-desktop/snap-help"
                    dest = os.path.join(dir_target, 'snap-desktop', 'snap-help')
                    print(dest)

                    if(not os.path.exists(dest)):                
                        shutil.copytree(src, dest)                

                        treeXml = etree.parse(path)
                        rootXml = treeXml.getroot()                
             
                        treeMap = etree.parse(os.path.join(root,"map.jhm"))
                        rootMap = treeMap.getroot()

                        for mapID in rootMap:
                            mapDict[mapID.get('target')] = mapID.get('url')

                        writeToHtml(rootXml)
                        print("snap-help done") 
                        
for root, dirs, files in os.walk(dir_desktop):
    for file in files:
        if file.endswith("toc.xml"):
            path = os.path.join(root, file)
#             print(path)
            place = path.find("/target/")
            if(place != -1):  
                if(path.find("/snap-help/") == -1):
                    src = path[0:-8]
                    print(src)
                    copy_resources()
                    # dest = os.path.join("/Users/bingyang/seadas8.2.0Help/snap-desktop", path[34:place])
                    dest = os.path.join(dir_target, 'snap-desktop', path[38:place])
                    print(dest)

                    if(not os.path.exists(dest)):                
                        shutil.copytree(src, dest)                

                        treeXml = etree.parse(path)
                        rootXml = treeXml.getroot()                

                        map_Path = os.path.join(root,"map.jhm")
                        if (os.path.exists(map_Path)):
                                treeMap = etree.parse(os.path.join(root,"map.jhm"))
                        else:
                            treeMap = etree.parse(os.path.join(root,"map.xml"))
                        rootMap = treeMap.getroot()

                        for mapID in rootMap:
                            mapDict[mapID.get('target')] = mapID.get('url')

                        writeToHtml(rootXml)
                        print(path[34:place] + " done") 
                        
for root, dirs, files in os.walk(dir_seadas):
    for file in files:
        if file.endswith("toc.xml"):
            path = os.path.join(root, file)
            place = path.find("/target/")
            if(place != -1):
                if(path.find("seadas-processing") != -1):
                    src = path[0:-8]
                    print(src)
                    copy_resources()
                    # dest = "/Users/bingyang/seadas8.2.0Help/seadas-toolbox/seadas-processing"
                    dest = os.path.join(dir_target, 'seadas-toolbox', 'seadas-processing')
                    print(dest)

                    if(not os.path.exists(dest)):                
                        shutil.copytree(src, dest)                

                        treeXml = etree.parse(path)
                        rootXml = treeXml.getroot()                
             
                        treeMap = etree.parse(os.path.join(root,"map.jhm"))
                        rootMap = treeMap.getroot()

                        for mapID in rootMap:
                            mapDict[mapID.get('target')] = mapID.get('url')

                        writeToHtml(rootXml)
                        print("seadas-processing done") 
                        
for root, dirs, files in os.walk(dir_seadas):
    for file in files:
        if file.endswith("toc.xml"):
            path = os.path.join(root, file)
            place = path.find("/target/")
            if(place != -1):
                if(path.find("seadas-processing") == -1):
                    src = path[0:-8]
                    print(src)
                    copy_resources()           
                    # dest = os.path.join("/Users/bingyang/seadas8.2.0Help/seadas-toolbox", path[36:place])
                    dest = os.path.join(dir_target, 'seadas-toolbox', path[40:place])
                    print(dest)
                    if(not os.path.exists(dest)):
                        shutil.copytree(src, dest)                

                        treeXml = etree.parse(path)
                        rootXml = treeXml.getroot()                

                        map_Path = os.path.join(root,"map.jhm")
                        if (os.path.exists(map_Path)):
                            treeMap = etree.parse(os.path.join(root,"map.jhm"))
                        else:
                            treeMap = etree.parse(os.path.join(root,"map.xml"))
                        rootMap = treeMap.getroot()

                        for mapID in rootMap:
                            mapDict[mapID.get('target')] = mapID.get('url')

                        writeToHtml(rootXml)
                        print(path[36:place] + " done") 

for root, dirs, files in os.walk(dir_engine):
    for file in files:
        if file.endswith("toc.xml"):
            path = os.path.join(root, file)
            place = path.find("/target/")
            if(place != -1):
                src = path[0:-8]
                print(src)
                copy_resources()
                dest = os.path.join(dir_target, 'snap-engine', path[37:place])
                print(dest)
            
                if(not os.path.exists(dest)):
                    shutil.copytree(src, dest)    
                    treeXml = etree.parse(path)
                    rootXml = treeXml.getroot()                

                    map_Path = os.path.join(root,"map.jhm")
                    if (os.path.exists(map_Path)):
                        treeMap = etree.parse(os.path.join(root,"map.jhm"))
                    else:
                        treeMap = etree.parse(os.path.join(root,"map.xml"))
                    rootMap = treeMap.getroot()

                    for mapID in rootMap:
                        mapDict[mapID.get('target')] = mapID.get('url')

                    writeToHtml(rootXml)
                    print(path[33:place] + " done") 

for root, dirs, files in os.walk(dir_s3tbx):
    for file in files:
        if (file.endswith("toc.xml")):
            path = os.path.join(root, file)
            if(path.find("s3tbx-dos") == -1):
                place = path.find("/target/")
                if(place != -1):
                    src = path[0:-8]
                    print(src)
                    copy_resources()
                    dest = os.path.join(dir_target, 's3tbx', path[31:place])
                    print(dest)

                    if(not os.path.exists(dest)):
                        shutil.copytree(src, dest)  
                        treeXml = etree.parse(path)
                        rootXml = treeXml.getroot()                

                        map_Path = os.path.join(root,"map.jhm")
                        if (os.path.exists(map_Path)):
                            treeMap = etree.parse(os.path.join(root,"map.jhm"))
                        else:
                            treeMap = etree.parse(os.path.join(root,"map.xml"))
                        rootMap = treeMap.getroot()

                        for mapID in rootMap:
                            mapDict[mapID.get('target')] = mapID.get('url')

                        writeToHtml(rootXml)
                        print(path[31:place] + " done") 

treeHtml = etree.ElementTree(rootHtml)
treeHtml.write(os.path.join(dir_target, 'docs', 'toc.html'),pretty_print=True)


