#!/bin/bash
set -e

SOURCES=$(readlink -f $(dirname $0)/..)

function die()
{
	echo "$1"
	exit 1
}

function check_variable()
{
	eval val='$'$1
	[ "$val" != "" ] || die "$1 not defined"
}

check_variable VERSION

RESULT_DIR=`pwd`/dist-src

rm -rf $RESULT_DIR
mkdir -p $RESULT_DIR

echo Preparing $VERSION sources
cd $SOURCES
sed -e "s/^\(re.version\).*/\1=$VERSION/" -i $SOURCES/src/org/executequery/eq.system.properties 
sed -e "s/\(<exe\.version>\)[^<>]*\(<\/exe.version>\)/\1$VERSION\2/" -i $SOURCES/modules/redexpert/pom.xml

echo Archiving sources
ARCHIVE_PREFIX=RedExpert-$VERSION
hash=`git stash create`
[ "$hash" = "" ] && hash=HEAD
git archive --format=tar --prefix=$ARCHIVE_PREFIX/ $hash | gzip > $RESULT_DIR/$ARCHIVE_PREFIX-src.tar.gz
git archive --format=zip --prefix=$ARCHIVE_PREFIX/ $hash > $RESULT_DIR/$ARCHIVE_PREFIX-src.zip
