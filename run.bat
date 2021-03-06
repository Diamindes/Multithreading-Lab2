rmdir out /s /q
mkdir out
set MPJ_HOME=../mpj
javac -cp ./src/main/java;./mpj/lib/mpj.jar -d ./out ./src/main/java/Lab2.java
copy "./machines" "./out/"
cd out
%MPJ_HOME%/bin/mpjrun.bat -np %1 Lab2 %2 %3 %4 %5 %6
cd ..

:: .\run.bat 4 1 "../examples/B-2000.txt" "../examples/X-2000.txt" 0.0001 "../examples/R-2000.txt"
:: %MPJ_HOME%/bin/mpjrun.bat -np %1 -dev niodev -src -wdir /temp/out/tosend Lab2 %2 %3 %4 %5 %6
:: %MPJ_HOME%/bin/mpjrun.bat -np %1 -Xms2096M -Xmx2096M Lab2 %2 %3 %4 %5 %6