#!/bin/bash

set -e

# Check sudo privilege before executing
SUDO=''
if (( $EUID != 0 )); then
    SUDO='sudo'
fi

# Install the packages in 
# Dependencies/packages/
echo "[install.sh]  Installing dependencies...."
echo ""
$SUDO dpkg -i Dependencies/packages/*.deb


# Copy the m2 packages into ~/.m2
echo ""
echo "[install.sh]  Importing maven packages...."
echo ""
cp -r Dependencies/m2 ~/.m2

# Move the pre-compiled z3 
# into /usr/bin and /usr/include
echo ""
echo "[install.sh]  Installing z3-4.7 ...."
echo ""

z3Path="Dependencies/z3/"
list="$(ls -l $z3Path/bin | awk '{print $9}')"
for VAR in $list
do
	$SUDO cp $z3Path/bin/$VAR /usr/bin/
	$SUDO chmod 777 /usr/bin/$VAR
done

list="$(ls -l $z3Path/include | awk '{print $9}')"
for VAR in $list
do
	$SUDO cp $z3Path/include/$VAR /usr/include/
	$SUDO chmod 777 /usr/include/$VAR
done

echo ""
echo "[install.sh]  Compile a reducer for maven cache.."

./J-ReCoVer/j-Compile J-ReCoVer/sample_reducer.java | grep Success

echo ""
echo "[install.sh]  Done."
echo ""
