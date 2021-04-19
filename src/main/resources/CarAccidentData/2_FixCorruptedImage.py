'''
WARNING!!!

This script was run in Google Colab.
And the saved images are saved in Google Drive.
Do not attempt to run this script locally, it has to be loaded in Google Colab.
'''


# -*- coding: utf-8 -*-
"""Fix Corrupted Images.ipynb

Automatically generated by Colaboratory.

Original file is located at
    https://colab.research.google.com/drive/1ZwineXbT_sFtKIF7mvpkV0GFxvANY_sa
"""

allFiles = {"000000/628.jpg","000001/144.jpg","000002/347.jpg","000002/97.jpg","000003/67.jpg","000003/98.jpg","000005/119.jpg","000006/213.jpg","000046/132.jpg","000046/294.jpg","000046/365.jpg","000046/366.jpg","000046/45.jpg","000048/107.jpg","000048/141.jpg","000048/142.jpg","000048/77.jpg","000048/78.jpg","000051/31.jpg","000052/222.jpg","000052/223.jpg","000052/305.jpg","000053/365.jpg","000053/77.jpg","000053/9.jpg","000054/202.jpg","000054/248.jpg","000054/42.jpg","000054/92.jpg","000055/145.jpg","000055/178.jpg","000055/205.jpg","000055/21.jpg","000055/326.jpg","000055/440.jpg","000056/105.jpg","000056/311.jpg","000056/81.jpg","000057/22.jpg","000058/73.jpg","000058/78.jpg","000059/456.jpg","000059/57.jpg","000060/331.jpg","000060/6.jpg","000061/179.jpg","000061/211.jpg","000061/35.jpg","000061/50.jpg","000062/169.jpg","000063/1.jpg","000063/231.jpg","000064/183.jpg","000064/196.jpg","000064/31.jpg","000065/12.jpg","000065/236.jpg","000066/69.jpg","000067/97.jpg","000068/1063.jpg","000068/212.jpg","000068/301.jpg","000068/331.jpg","000068/486.jpg","000068/579.jpg","000068/701.jpg","000068/775.jpg","000069/105.jpg","000069/258.jpg","000069/348.jpg","000069/75.jpg","000074/10.jpg","000074/63.jpg","000079/46.jpg","000081/14.jpg","000081/77.jpg","000082/18.jpg","000082/77.jpg","000088/18.jpg","000089/15.jpg","000089/24.jpg","000092/192.jpg","000092/221.jpg","000092/314.jpg","000093/144.jpg","000093/53.jpg","000096/278.jpg","000097/32.jpg","000098/160.jpg","000098/35.jpg","000099/30.jpg","000106/47.jpg","000106/72.jpg","000108/250.jpg","000111/51.jpg","000113/151.jpg","000113/71.jpg","000114/170.jpg","000116/100.jpg","000116/7.jpg","000118/67.jpg","000122/67.jpg","000122/68.jpg","000130/195.jpg","000131/20.jpg","000132/30.jpg"}
allFiles = {"001387/32.jpg","001388/206.jpg","001389/105.jpg","001390/15.jpg","001390/171.jpg","001390/591.jpg","001390/702.jpg","001410/113.jpg","001410/146.jpg","001410/245.jpg","001410/606.jpg","001410/675.jpg","001411/102.jpg","001411/313.jpg","001411/46.jpg","001412/162.jpg"}
allFiles

from google.colab import drive
drive.mount('/content/drive')

import shutil
import os

sourceRoot = "drive/MyDrive/extracted_frames/"
destRoot = "drive/MyDrive/extracted_frames_OUT/"

for sourceFile in allFiles:
  os.makedirs(os.path.dirname(destRoot + sourceFile), exist_ok=True)
  shutil.copyfile(sourceRoot + sourceFile, destRoot + sourceFile)