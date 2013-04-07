#!/bin/bash

# This script is intended to assist with getting the Triplifier up and running if it is built
# and deployed from within Netbeans.  If you have Netbeans make its own Ant build script (which
# is probably the easiest way to set up the Netbeans project), then a few key files and
# directories will be missing when the project is deployed.  This script will copy the needed
# files and directories to the proper location.  It should only need to be run once after
# deploying the triplifier for the first time, unless you do a "Clean and Build" from Netbeans.
# Prior to running the script, set the value of WEBINFDIR to match your Netbeans project.

# The location of the Netbeans project's WEB-INF directory (typically, this will be in
# "build/web/" inside the Netbeans project's directory).
WEBINFDIR="../triplifier-web-netbeans/build/web/WEB-INF/"

# Note that we use rsync instead of cp for directories to avoid copying any of the SVN stuff.
cp triplifiersettings.props $WEBINFDIR"classes/"
rsync -rv --exclude=.svn vocabularies $WEBINFDIR"classes/"
rsync -rv --exclude=.svn sqlite $WEBINFDIR"classes/"

