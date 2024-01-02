import subprocess
import os
import os.path
import requests
import re



def get_command_output(command):

    cmd_output = None
    try:
        process = subprocess.Popen(command, shell=False, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        stdout, stderr = process.communicate()
        cmd_output = stdout.decode("utf-8") + stderr.decode("utf-8")
    except:
        cmd_output = None
    return cmd_output


def get_l1info(l1b_filename, geo_filename):
    """
    Returns l1info output, which is obtained by running l1info
    via subprocess.
    """
    output = None
    cmd = ["l1info", l1b_filename, geo_filename]
    direct_output = get_command_output(cmd)
    if direct_output:
        output = direct_output.splitlines()
    return output


def cleanup_files(file):
  if (os.path.isfile(file) == True):
    print("Deleting file " + file)

  subprocess.run([ "rm", "-f", file ])

  if (os.path.isfile(file) == True):
    print("ERROR: failed to delete file: " + file)




def get_param(lines, param_name):

  param_value = None   

  for line in lines:
    line_split = line.split(param_name + "=")
    if (len(line_split) == 2):
      param_value = line_split[1]

  return param_value





def download_obdaac_file(filename):
  print("Downloading: " + filename)
  url = "https://oceandata.sci.gsfc.nasa.gov/getfile/" + filename
  response = requests.get(url, stream=True)

  with open(filename, mode="wb") as file:
    for chunk in response.iter_content(chunk_size=10 * 1024):
      file.write(chunk)

  if (os.path.isfile(filename) != True):
    print("File " + filename + " DOES NOT EXIST")
    return None



def get_timepart(l1a_filename, viirs_sensor):
  if (viirs_sensor == None or l1a_filename == None):
    return None
  #print(viirs_sensor)
  filename_split1 = l1a_filename.split(viirs_sensor + ".")
  if (len(filename_split1) == 2):
    filename_part2 = filename_split1[1]

    filename_split2 = filename_part2.split(".L1A.nc")
    if (len(filename_split2) == 2):
      timepart = filename_split2[0]
      return timepart

  return None


def get_l1b_filename(viirs_sensor, timepart):
  if (viirs_sensor == None or timepart == None):
    return None
  l1b_filename = viirs_sensor + "." + timepart + ".L1B.nc"
  return l1b_filename


def get_geo_filename(viirs_sensor, timepart):
  if (viirs_sensor == None or timepart == None):
    return None
  geo_filename = viirs_sensor + "." + timepart + ".GEO.nc"
  return geo_filename


def get_viirs_sensor(l1a_filename):
  filename_split = l1a_filename.split("_VIIRS")
  if (len(filename_split) == 2):
    viirs_sensor = filename_split[0] + "_VIIRS"
    return viirs_sensor

  return None


def get_viirs_sensor_from_url(l1a_filename_url):
  filename_split = l1a_filename_url.split("SNPP_VIIRS")
  if (len(filename_split) == 2):
    return "SNPP_VIIRS"

  filename_split = l1a_filename_url.split("JPSS1_VIIRS")
  if (len(filename_split) == 2):
    return "JPSS1_VIIRS"

  filename_split = l1a_filename_url.split("JPSS2_VIIRS")
  if (len(filename_split) == 2):
    return "JPSS2_VIIRS"

  return None




def get_url(filename):
  filename_split = filename.split("https")
  if (len(filename_split) > 1):
    return filename
  else:
    #url = "https://oceandata.sci.gsfc.nasa.gov/getfile/" + filename
    url = "https://oceandata.sci.gsfc.nasa.gov/ob/getfile/" + filename
    return url




def get_l1a_filename_from_url(l1a_filename_url, viirs_sensor):
  filename_split = l1a_filename_url.split(viirs_sensor)
  if (len(filename_split) == 2):
    return  viirs_sensor + filename_split[1]

  return None





def run_calibrate_viirs(l1a_filename, l1b_filename):
  program_name = "calibrate_viirs"
  ifile_arg = "ifile=" + l1a_filename
  ofile_arg = "l1bfile_mod=" + l1b_filename
  
  command = program_name + " " + ifile_arg + " " + ofile_arg
  print("Running: " + command)

  # Example calibrate_viirs ifile=JPSS1_VIIRS.20230812T000000.L1A.nc l1bfile_mod=JPSS1_VIIRS.20230812T000000.L1B.nc

  subprocess.run([program_name, ifile_arg, ofile_arg]) 

  if (os.path.isfile(l1b_filename) != True):
    print("Level-1b file " + l1b_filename + " DOES NOT EXIST")
    return None



def get_l2_filename(viirs_sensor, timepart):
  if (viirs_sensor == None or timepart == None):
    return None

  l2_filename = viirs_sensor + "." + timepart + ".L2.SFREFL.nc"
  return l2_filename



def run_l2gen(l1b_filename, geo_filename, l2_filename, viirs_sensor, daynight, sunzen, satzen):
  if (daynight != "Day" and daynight != "Both"):
    print("Returning due to daynight=" + daynight)
    return 0

  print("daynight=" + daynight)

  program_name = "l2gen"
  ifile_arg = "ifile=" + l1b_filename
  geofile_arg = "geofile=" + geo_filename
  l2prod = ""
  if (viirs_sensor == "JPSS1_VIIRS" or viirs_sensor == "JPSS2_VIIRS"):
    l2prod = "rhos_489,rhos_556,rhos_667,rhot_489,rhot_556,rhot_667,senz,solz"
    #print(l3bprod)
  elif (viirs_sensor == "SNPP_VIIRS"):
    l2prod = "rhos_486,rhos_551,rhos_671,rhot_486,rhot_551,rhot_671,senz,solz"
           #print(l3bprod)
  else:
    print ("unknown viirs sensor " + viirs_sensor)
    return None

  l2prod_arg = "l2prod=" + l2prod
  satzen_arg = "satzen=" + satzen
  sunzen_arg = "sunzen=" + sunzen

#   if (extend_south == True and float(southernmost_lat) < -40):
#     sunzen_arg = "sunzen=82.0"
#
#   if (extend_north == True and float(northernmost_lat) > 40):
#     sunzen_arg = "sunzen=82.0"
#

  suite_arg = "suite=SFREFL"

  ofile_arg = "ofile=" + l2_filename

  par_lines = []
  par_lines.append(ifile_arg)
  par_lines.append(geofile_arg)
  par_lines.append(ofile_arg)
  par_lines.append(l2prod_arg)
  par_lines.append(satzen_arg)
  par_lines.append(sunzen_arg)
  par_lines.append(suite_arg)
  parfile = get_par_filename(l2_filename)

  status = create_parfile_from_list(par_lines, parfile)

  if (status == None): return None


  parfile_arg = "par=" + parfile



#   command = program_name + " " + l2prod_arg + " " + satzen_arg + " " + sunzen_arg + " " + suite_arg + " " + ifile_arg + " " + geofile_arg + " " + ofile_arg
  command = program_name + " " + parfile_arg


# Example:
# l2gen l2prod="rhos_411 rhos_489 rhos_556 rhos_667 rhot_411 rhot_489 rhot_556 rhot_667 senz solz" satzen=48.0 sunzen=76.0 suite=SFREFL ifile=JPSS1_VIIRS.20230812T001800.L1B.nc geofile=JPSS1_VIIRS.20230812T001800.GEO.nc ofile=JPSS1_VIIRS.20230812T001800.L2.SFREFL.nc

  
  print("Running: " + command)

#   subprocess.run([program_name, ifile_arg, geofile_arg, ofile_arg, satzen_arg, sunzen_arg, suite_arg, l2prod_arg])
  subprocess.run([program_name, parfile_arg ])

  if (os.path.isfile(l2_filename) != True):
    print("Level-2 file " + l2_filename + " DOES NOT EXIST")

  return None



def get_l3binned_filename(viirs_sensor, timepart, resolution):
  l3binned_filename = viirs_sensor + "." + timepart + ".L3b." + resolution + "km.nc"
  return l3binned_filename



def get_global_l3binned_filename(viirs_sensor, timepart, resolution, wraplon):
  global_l3binned_filename = None

  global_l3binned_filename = viirs_sensor + "." + timepart + ".L3b." + resolution + "km.wraplon" + wraplon + ".senzmin.nc"
  return global_l3binned_filename

#         ofile=JPSS1.20230808.L3b.2km.wraplon0.senzmin.nc


def get_global_l3mapped_filename(global_l3binned_filename, mapped_resolution, lon_0, lat_0):
  global_l3mapped_filename = None

  global_l3mapped_filename_replace = global_l3binned_filename.replace("L3b", "L3m")

  global_l3binned_filename_split = global_l3mapped_filename_replace.split(".nc")
  if (len(global_l3binned_filename_split) == 2):
    first_part = global_l3binned_filename_split[0]
    global_l3mapped_filename = first_part + ".ortho_lon" + lon_0 + "_lat" + lat_0 + ".nc"

# ifile=JPSS1.202308012.L3b.2km.wraplon0.senzmin.nc
# ofile=JPSS1.202308012.L3m.2km.wraplon0.senzmin.ortho_lon170_lat30.nc
  return global_l3mapped_filename


def get_par_filename(filename):
    print("filename=" + filename)
    parfilename = re.sub('\.nc$', '.par', filename.strip())
    return parfilename


def create_parfile_from_list(par_lines, parfile):
  with open(parfile, 'w') as f:
     for line in par_lines:
       f.write(f"{line}\n")

     if (os.path.isfile(parfile) != True):
       print("ERROR: failed to create file " + parfile)
       return None
  return 0



def run_l2bin(l2_filename, l3_binned_filename, resolution, viirs_sensor):

  l3bprod = ""
  if (viirs_sensor == "JPSS1_VIIRS" or viirs_sensor == "JPSS2_VIIRS"):
    l3bprod = "rhos_489,rhos_556,rhos_667,rhot_489,rhot_556,rhot_667,senz,solz"
    #print(l3bprod)
  elif (viirs_sensor == "SNPP_VIIRS"):
     l3bprod = "rhos_486,rhos_551,rhos_671,rhot_486,rhot_551,rhot_671,senz,solz"
     #print(l3bprod)
  else:
    return None


# ifile=level2_files.txt
# ofile=JPSS1_VIIRS_1orbit.L3b.2km.nc
# flaguse=NAVFAIL,BOWTIEDEL,HISOLZEN
# l3bprod=rhos_489,rhos_556,rhos_667,rhot_489,rhot_556,rhot_667,senz,solz
# prodtype=regional
# resolution=2
# area_weighting=0
# rowgroup=1080
# verbose=1

  program_name = "l2bin"
#   ifile_arg = "ifile=" + l2_filename
#   ofile_arg = "ofile=" + l3_binned_filename
#   l3bprod_arg = "l3bprod=" + l3bprod
#   prodtype_arg = "prodtype=regional"
#   flaguse_arg = "flaguse=NAVFAIL,BOWTIEDEL,HISOLZEN"
#   resolution_arg = "resolution=" + resolution
#   area_weighting_arg = "area_weighting=0"
#   rowgroup_arg = "rowgroup=1080"
#   verbose_arg = "verbose=1"


#   command = program_name + " " + ifile_arg + " " + ofile_arg + " " + flaguse_arg + " " + l3bprod_arg + " " + prodtype_arg + " " + resolution_arg + " " + area_weighting_arg + " "  + rowgroup_arg + " " + verbose_arg

  parfile = get_par_filename(l3_binned_filename)
  parfile_arg = "par=" + parfile

  par_lines = []
  par_lines.append("ifile=" + l2_filename)
  par_lines.append("ofile=" + l3_binned_filename)
  par_lines.append("l3bprod=" + l3bprod)
  par_lines.append("prodtype=regional")
  par_lines.append("flaguse=NAVFAIL,BOWTIEDEL,HISOLZEN")
  par_lines.append("resolution=" + resolution)
  par_lines.append("area_weighting=0")
  par_lines.append("rowgroup=1080")
  par_lines.append("verbose=1")

  status = create_parfile_from_list(par_lines, parfile)
  if (status == None): return None

  command = program_name + " " + parfile_arg
  print("Running: " + command)
  subprocess.run([program_name, parfile_arg ])
#   print("exit_status=" + exit_status)

#   subprocess.run([ program_name, ifile_arg, ofile_arg, flaguse_arg, l3bprod_arg, prodtype_arg, resolution_arg, area_weighting_arg, rowgroup_arg, verbose_arg ])

  if (os.path.isfile(l3_binned_filename) != True):
    print("WARNING!  Level-3binned file " + l3_binned_filename + " DOES NOT EXIST")





def run_l3bin(l3_binned_filename_ifile, l3_binned_filename_ofile, reduce_fac, viirs_sensor):

  prod = ""
  if (viirs_sensor == "JPSS1_VIIRS" or viirs_sensor == "JPSS2_VIIRS"):
    prod = "rhos_489,rhos_556,rhos_667,rhot_489,rhot_556,rhot_667,senz"
    #print(l3bprod)
  elif (viirs_sensor == "SNPP_VIIRS"):
     prod = "rhos_486,rhos_551,rhos_671,rhot_486,rhot_551,rhot_671,senz"
     #print(l3bprod)
  else:
    return None

#
#         cat par/l3bin.JPSS1.20230812.2km.wraplon0.senzmin.par
#         ifile=JPSS1.202308012.L3b.2km.wraplon0.orbitfiles.txt
#         ofile=JPSS1.202308012.L3b.2km.wraplon0.senzmin.nc
#         prod=rhos_489,rhos_556,rhos_667,rhot_489,rhot_556,rhot_667,senz
#         reduce_fac=1
#         composite_scheme=min
#         composite_prod=senz
#         #latnorth=80
#         #latsouth=60
#         #lonwest=15
#         #loneast=65
#         verbose=1

  program_name = "l3bin"
  ifile_arg = "ifile=" + l3_binned_filename_ifile
  ofile_arg = "ofile=" + l3_binned_filename_ofile
  prod_arg = "prod=" + prod
  composite_scheme_arg = "composite_scheme=min"
  composite_prod_arg = "composite_prod=senz"
  reduce_fac_arg = "reduce_fac=" + reduce_fac
  verbose_arg = "verbose=1"

#   command = program_name + " " + ifile_arg + " " + ofile_arg + " " + prod_arg + " " + composite_scheme_arg + " " + composite_prod_arg + " " + reduce_fac_arg + " " + verbose_arg
#
#   print("Running: " + command)
#
#   subprocess.run([ program_name, ifile_arg, ofile_arg, prod_arg, composite_scheme_arg, composite_prod_arg, reduce_fac_arg, verbose_arg ])
#



  parfile = get_par_filename(l3_binned_filename_ofile)
  parfile_arg = "par=" + parfile

  par_lines = []
  par_lines.append(ifile_arg)
  par_lines.append(ofile_arg)
  par_lines.append(prod_arg)
  par_lines.append(composite_scheme_arg)
  par_lines.append(composite_prod_arg)
  par_lines.append(reduce_fac_arg)
  par_lines.append(verbose_arg)

  status = create_parfile_from_list(par_lines, parfile)
  if (status == None): return None

  command = program_name + " " + parfile_arg
  print("Running: " + command)
  subprocess.run([program_name, parfile_arg ])



  if (os.path.isfile(l3_binned_filename_ofile) != True):
    print("WARNING!  Level-3binned file " + l3_binned_filename_ofile + " DOES NOT EXIST")







def run_l3mapgen(l3_binned_filename_ifile, l3_mapped_filename_ofile, resolution, viirs_sensor, lon_0, lat_0):

  product = ""
  if (viirs_sensor == "JPSS1_VIIRS" or viirs_sensor == "JPSS2_VIIRS"):
    product = "rhos_489,rhos_556,rhos_667,rhot_489,rhot_556,rhot_667,senz"
    #print(l3bprod)
  elif (viirs_sensor == "SNPP_VIIRS"):
     product = "rhos_486,rhos_551,rhos_671,rhot_486,rhot_551,rhot_671,senz"
     #print(l3bprod)
  else:
    return None


# ifile=JPSS1.202308012.L3b.2km.wraplon0.senzmin.nc
# ofile=JPSS1.202308012.L3m.2km.wraplon0.senzmin.ortho_lon170_lat30.nc
# product=rhos_489,rhos_556,rhos_667,rhot_489,rhot_556,rhot_667,senz
# projection="+proj=ortho +lon_0=170 +lat_0=30.0"
# resolution=2km
# north=90
# south=-90
# west=-180
# east=180
# num_cache=2000


  program_name = "l3mapgen"
  ifile_arg = "ifile=" + l3_binned_filename_ifile
  ofile_arg = "ofile=" + l3_mapped_filename_ofile
  product_arg = "product=" + product
  projection_arg = "projection=+proj=ortho +lon_0=" + lon_0 + " +lat_0=" + lat_0
  resolution_arg = "resolution=" + resolution + "km"
  north_arg = "north=90"
  south_arg = "south=-90"
  west_arg = "west=-180"
  east_arg = "east=180"
  num_cache_arg = "num_cache=2000"



  parfile = get_par_filename(l3_mapped_filename_ofile)
  parfile_arg = "par=" + parfile

  par_lines = []
  par_lines.append(ifile_arg)
  par_lines.append(ofile_arg)
  par_lines.append(product_arg)
  par_lines.append(projection_arg)
  par_lines.append(resolution_arg)
  par_lines.append(north_arg)
  par_lines.append(south_arg)
  par_lines.append(west_arg)
  par_lines.append(east_arg)
  par_lines.append(num_cache_arg)

  status = create_parfile_from_list(par_lines, parfile)
  if (status == None): return None

  command = program_name + " " + parfile_arg
  print("Running: " + command)
  subprocess.run([program_name, parfile_arg ])


#
#
#   command = program_name + " " + ifile_arg + " " + ofile_arg + " " + product_arg + " " + projection_arg + " " + north_arg + " " + south_arg + " " + west_arg + " " + east_arg + " " + resolution_arg + " " + num_cache_arg
#
#   print("Running: " + command)
#
#   subprocess.run([ program_name, ifile_arg, ofile_arg, product_arg, projection_arg, north_arg, south_arg, west_arg, east_arg, resolution_arg, num_cache_arg ])


  if (os.path.isfile(l3_mapped_filename_ofile) != True):
    print("WARNING!  Level-3 mapped file " + l3_mapped_filename_ofile + " DOES NOT EXIST")
















#l1a_filelist = "level1A_files_test.txt"
#
#if (os.path.isfile(l1a_filelist) != True):
#  print("Make files")
#  subprocess.run(["ls", "-1", "*L1A*"]) 
#  print("Make files")


# def create_scene_files(scene_file_list, sunzen, sunzen_polar, satzen, satzen_polar, binned_list_file, resolution):
def create_scene_files(scene_file_list, binned_list_file, binned_resolution, sunzen_north, sunzen, sunzen_south, satzen_north, satzen, satzen_south ):

    viirs_sensor = None
    timepart = None

    binned_files = []


    with open(scene_file_list) as file:
      filelines = file.read().splitlines()
      for l1a_filename_entry in filelines:
        if (len(l1a_filename_entry.strip()) < 2):
          continue
        if (l1a_filename_entry.strip().startswith("#")):
          continue
        print(" ")
        print(l1a_filename_entry)
        l1a_filename_url = get_url(l1a_filename_entry)
        print(l1a_filename_url)

        viirs_sensor = get_viirs_sensor_from_url(l1a_filename_url)
        if (viirs_sensor == None):
          print("WARNING! not a viirs sensor " + viirs_sensor)
          continue

        l1a_filename =  get_l1a_filename_from_url(l1a_filename_url, viirs_sensor)
        if (l1a_filename == None):
          print("WARNING! viirs level-1A filename not found in URL")
          continue
        print(l1a_filename)



        #viirs_sensor = get_viirs_sensor(l1a_filename)

        timepart = get_timepart(l1a_filename, viirs_sensor)
        if (timepart == None):
          print("WARNING! could not derived timepart from " + l1a_filename)
          continue
        print(timepart)


        l1b_filename = get_l1b_filename(viirs_sensor, timepart)
        if (l1b_filename == None):
          print("WARNING! could not derive l1b_filename")
          continue
        print(l1b_filename)

        geo_filename = get_geo_filename(viirs_sensor, timepart)
        if (geo_filename == None):
          print("WARNING! could not derive geo_filename")
          continue
        print(geo_filename)

        l2_filename = get_l2_filename(viirs_sensor, timepart)
        if (l2_filename == None):
          print("WARNING! could not derive l2_filename")
          continue
        print(l2_filename)


        l3_binned_filename = get_l3binned_filename(viirs_sensor, timepart, binned_resolution)
        if (l3_binned_filename == None):
          print("WARNING! could not derive l3_binned_filename")
          continue
        print(l3_binned_filename)


    #    print("viirs_sensor=" + viirs_sensor)
    #    print("timepart=" + timepart)
    #    print("l1b_filename=" + l1b_filename)
    #    print("geo_filename=" + geo_filename)


        need_l3_binned_file = False
        need_l2_file = False
        need_l1b_file = False
        need_geo_file = False
        need_l1a_file = False

        if (os.path.isfile(l3_binned_filename) != True):
          print("Level-3 Binned file " + l3_binned_filename + " DOES NOT EXIST")
          need_l3_binned_file = True

        if (need_l3_binned_file and os.path.isfile(l2_filename) != True):
          print("Level-2 file " + l2_filename + " DOES NOT EXIST")
          need_l2_file = True

        if (need_l2_file):
          if (os.path.isfile(l1b_filename)!= True):
           print("Level-1B file " + l1b_filename + " DOES NOT EXIST")
           need_l1b_file = True

          if (os.path.isfile(geo_filename) != True):
            print("GEO file " + geo_filename + " DOES NOT EXIST")
            need_geo_file = True

        if (need_l1b_file and os.path.isfile(l1a_filename) != True):
          print("Level-1A file " + l1a_filename + " DOES NOT EXIST")
          need_l1a_file = True


        if (need_l1a_file):
          download_obdaac_file(l1a_filename)
          if (os.path.isfile(l1a_filename) != True):
            print("ERROR: Failed to download Level-1A file " + l1a_filename)
            continue


        if (need_l1b_file):
          run_calibrate_viirs(l1a_filename, l1b_filename)
          if (os.path.isfile(l1b_filename) != True):
            print("ERROR: Failed to generate Level-1B file " + l1b_filename)
            continue


        if (need_geo_file):
          download_obdaac_file(geo_filename)
          if (os.path.isfile(geo_filename) != True):
            print("ERROR: Failed to download GEO file " + geo_filename)
            continue


        if (need_l2_file):

          lines = get_l1info(l1b_filename, geo_filename)
    #      print(lines)
          if (lines == None):
            print("WARNING! could not get info from files " + l1b_filename + " " +  geo_filename)
            continue


          sensor = get_param(lines, "Sensor")
          orbit_number = get_param(lines, "Orbit_Number")
          center_lat = get_param(lines, "Center_Lat")
          center_lon = get_param(lines, "Center_Lon")
          northernmost_lat = get_param(lines, "Northernmost_Lat")
          southernmost_lat = get_param(lines, "Southernmost_Lat")
          daynight = get_param(lines, "Daynight")
          start_date = get_param(lines, "Start_Date")
          end_date = get_param(lines, "End_Date")



    #      print("Sensor=" + sensor)
    #      print("Orbit_Number=" + orbit_number)
    #      print("Center_Lat=" + center_lat)
    #      print("Center_Lon=" + center_lon)
    #      print("Northernmost_Lat=" + northernmost_lat)
    #      print("Southernmost_Lat=" + southernmost_lat)
    #      print("Daynight=" + daynight)
    #      print("Start_Date=" + start_date)
    #      print("End_Date=" + end_date)


          sunzen_curr = sunzen
          satzen_curr = satzen

          if (southernmost_lat is None):
              print("southernmost_lat=" + southernmost_lat)
          else:
              if (float(southernmost_lat) < -40):
                sunzen_curr = sunzen_south
                satzen_curr = satzen_south

          if (northernmost_lat is None):
              print("northernmost_lat=" + northernmost_lat)
          else:
              if (float(northernmost_lat) > 40):
                sunzen_curr = sunzen_north
                satzen_curr = satzen_north



          run_l2gen(l1b_filename, geo_filename, l2_filename, viirs_sensor, daynight, sunzen_curr, satzen_curr)

          if (os.path.isfile(l2_filename) != True):
            print("ERROR: Failed to generate Level-2 file " + l2_filename)
            continue


          if (os.path.isfile(l2_filename)):
            cleanup_files(l1a_filename)
            cleanup_files(l1b_filename)
            cleanup_files(geo_filename)
            need_l2_file = False


        if (need_l2_file != True):
          cleanup_files(l1a_filename)
          cleanup_files(l1b_filename)
          cleanup_files(geo_filename)


        if (need_l3_binned_file):
          run_l2bin(l2_filename, l3_binned_filename, binned_resolution, viirs_sensor)
          if (os.path.isfile(l3_binned_filename) != True):
            print("ERROR: Failed to generate Level-3 binned file " + l3_binned_filename)
            continue



        if (os.path.isfile(l3_binned_filename) == True):
          binned_files.append(l3_binned_filename)


        print("Binned Files List:")
        print(binned_files)


        with open(binned_list_file, 'w') as f:
            for line in binned_files:
                f.write(f"{line}\n")

        if (os.path.isfile(binned_list_file) != True):
          print("ERROR: failed to create file " + binned_list_file)

    return viirs_sensor, timepart



scene_file_list_front = "front_files.txt"
binned_list_file_front = 'binned_files_front.txt'

sunzen_north = "82"
sunzen = "82"
sunzen_south = "82"

satzen_north = "48"
satzen = "48"
satzen_south = "40"

binned_resolution = "2"

viirs_sensor, timepart = create_scene_files(scene_file_list_front, binned_list_file_front, binned_resolution, sunzen_north, sunzen, sunzen_south, satzen_north, satzen, satzen_south )


scene_file_list_back = "back_files.txt"
binned_list_file_back = 'binned_files_back.txt'

sunzen_north = "82"
sunzen = "82"
sunzen_south = "60"

satzen_north = "48"
satzen = "48"
satzen_south = "40"

viirs_sensor2, timepart2 = create_scene_files(scene_file_list_back, binned_list_file_back, binned_resolution, sunzen_north, sunzen, sunzen_south, satzen_north, satzen, satzen_south )

# viirs_sensor, timepart = create_scene_files(scene_file_list_back, sunzen, sunzen_polar, satzen, satzen_polar, binned_list_file_back, binned_resolution)


binned_list_file = 'binned_files.txt'

# file1 = open(binned_list_file_front, 'r')
# file2 = open(binned_list_file_back, 'r')
# content1 = file1.read()
# content2 = file2.read()
# file1.close()
# file2.close()
# destination_file = open(binned_list_file, 'w')
# destination_file.write(content1 + content2)
# destination_file.close()



file1 = open(binned_list_file_front, 'r')
content1 = file1.read()
file1.close()
destination_file = open(binned_list_file, 'w')
destination_file.write(content1)
destination_file.close()

if (viirs_sensor2 is not None):
    file2 = open(binned_list_file_back, 'r')
    content2 = file2.read()
    file2.close()
    destination_file = open(binned_list_file, 'a')
    destination_file.write(content2)
    destination_file.close()




# subprocess.run([ "cat", binned_list_file_front, ">", binned_list_file ])
# subprocess.run([ "cat", binned_list_file_front, ">>", binned_list_file ])

print("viirs_sensor=" + viirs_sensor)
print("timepart=" + timepart)


wraplon = "0"
global_l3binned_filename = get_global_l3binned_filename(viirs_sensor, timepart, binned_resolution, wraplon)
print("global_l3binned_filename=" + global_l3binned_filename)


if (os.path.isfile(global_l3binned_filename) != True):
  print("Creating file: " + global_l3binned_filename)
  reduce_fac = "1"
  run_l3bin(binned_list_file, global_l3binned_filename, reduce_fac, viirs_sensor)

if (os.path.isfile(global_l3binned_filename) != True):
  print("ERROR: Failed to generate file: " + global_l3binned_filename)
  exit(1)



# lon_0 = "170"
# lat_0 = "30"
lon_0 = "-70"
lat_0 = "-30"
mapped_resolution = "2"
global_l3mapped_filename = get_global_l3mapped_filename(global_l3binned_filename, mapped_resolution, lon_0, lat_0)
print("global_l3mapped_filename=" + global_l3mapped_filename)


if (os.path.isfile(global_l3mapped_filename) != True):
  print("Creating file: " + global_l3mapped_filename)
  run_l3mapgen(global_l3binned_filename, global_l3mapped_filename, mapped_resolution, viirs_sensor, lon_0, lat_0)


if (os.path.isfile(global_l3mapped_filename) != True):
  print("ERROR: Failed to generate file: " + global_l3mapped_filename)
  exit(1)
