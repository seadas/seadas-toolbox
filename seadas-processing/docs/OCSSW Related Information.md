
**September 5, 2020**

For this release, let’s make it easy to get the installer.  The 2 files you need to download to the same directory are here:

https://oceandata.sci.gsfc.nasa.gov/manifest/manifest.py

https://oceandata.sci.gsfc.nasa.gov/manifest/install_ocssw



then



chmod +x install_ocssw



now you can run the installer as a normal unix command.





----------------------------------------------------

Now for the better way to get the proper installer files for the version you are trying to install.  Not sure if this will be worth it, but this is the “right” way to get the installer files.  Here we go:



Assume $TAG=tag you want to install



Download https://oceandata.sci.gsfc.nasa.gov/manifest/tags/$TAG/bin_linux_64/manifest.json

In the manifest.json file look up the record for “manifest.py”

Use the value of “tag” to download the file



As an example, the record for “manifest.py” is:



        "manifest.py": {

            "checksum": "0bb0ed6295fac166e19a50fc0c01aee48539352863c17dd9a1b0857f1f8512d9",

            "mode": 33261,

            "size": 27787,

            "tag": "v3.1.0-rc"

        },



So to download this file you need to download:



https://oceandata.sci.gsfc.nasa.gov/manifest/tags/v3.1.0-rc/bin_linux_64/manifest.json



do the same tag de-reference for install_ocssw.



The reason the manifest.json file has the tag for each file is that newer manifest.json file can just reference files in old tags if the file did not change in the new tag.  The way the installer works, is that it downloads the manifest.json file for the desired tag and looks up the tag for all of the entries in the manifest.json file and downloads those files.  Hope this makes sense.  Let me know what part I failed to explain, because there are probably many.



The first way is easy, and will work until I make radical changes to the installer code which makes it NOT backward compatible.  I try not to do such things, but sometimes it has to happen.



don

