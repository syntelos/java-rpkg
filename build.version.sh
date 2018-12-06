#!/bin/bash

_self_version_major=1
_self_version_minor=1

function usage {
    cat<<EOF>&2
Synopsis

  $0 init [M.m.b]

Description

  Create file 'build.version.txt' with default 
  version "1.0.1", optional version argument.
  Print subsequent version string.

Synopsis

  $0 echo

Description

  Default operation.
  Print current version string.

Synopsis

  $0 inc [major|minor|build]

Description

  Increment one of major, minor, or build.  
  Print subsequent version string.

Synopsis

  $0 rewrite [fext]

Description

  Rewrite version templates found under 
  directory 'ver' to directory 'src'.
  The template strings '@VersionMajor@',
  '@VersionMinor@', '@VersionBuild@' will
  be replaced with their respective version 
  integer number values.

  Optionally source files having filename
  extension 'fext' as "*.fext".

  N.B.  Template rewriting occurs 
  independently from version incrementing.
  Version incrementing does not imply
  template rewriting.  The rewrite step
  must be performed manually.

Synopsis

  $0 self echo

Description

  Print current version string of this script.

Synopsis

  $0 self inc [major|minor|build]

Description

  Increment one of major, minor, or build.  
  Print subsequent version string.

EOF
    exit 1
}

function init_version {
    version_major="1"
    version_minor="0"
    version_build="1"
    if [ -n "${1}" ]&&[ -n "$(echo ${1} | egrep '^[0-9]+\.[0-9]+\.[0-9]+$' )" ]
    then
        if version_major=$(echo "${1}" | sed 's/\..*//') &&
                version_minor=$(echo "${1}" | sed "s/^${version_major}\.//; s/\..*//;")&&
                version_build=$(echo "${1}" | sed "s/^${version_major}\.${version_minor}\.//")
        then
            if write_version && echo_version
            then
                return 0
            else
                return 1
            fi
        else
            return 1
        fi
    else
        if write_version && echo_version
        then
            return 0
        else
            return 1
        fi
    fi
}
function read_version {
    if [ -f build.version.txt ]
    then
	if version_major=$(egrep '^version\.major=' build.version.txt | sed 's/version.major=//')&&[ -n "${version_major}" ]&&
		version_minor=$(egrep '^version\.minor=' build.version.txt | sed 's/version.minor=//')&&[ -n "${version_minor}" ]&&
		version_build=$(egrep '^version\.build=' build.version.txt | sed 's/version.build=//')&&[ -n "${version_build}" ]
	then
	    return 0
	else
	    return 1
	fi
    else
	return 1
    fi
}
function write_version {
    if [ -n "${version_major}" ]&&[ -n "${version_minor}" ]&&[ -n "${version_build}" ]
    then
	if cat<<EOF>build.version.txt
version.major=${version_major}
version.minor=${version_minor}
version.build=${version_build}
EOF
	then
	    return 0
	else
	    return 1
	fi
    else
	return 1
    fi
}   
function echo_version {
    if read_version
    then
	echo "${version_major}.${version_minor}.${version_build}"
	return 0
    else
	return 1
    fi
}
function rewrite_template {
    if read_version && [ -d ver ]&&[ -d src ]
    then
        if [ -n "${1}" ]
        then
            srcf_list=$(find ver -type f -name "*.${1}" )
        else
            srcf_list=$(find ver -type f )
        fi

        for srcf in ${srcf_list}
        do
            tgtf=$(echo ${srcf} | sed "s%ver/%src/%")
            if cat ${srcf} | sed "s%@VersionMajor@%${version_major}%; s%@VersionMinor@%${version_minor}%; s%@VersionBuild@%${version_build}%; " > ${tgtf}
            then
                echo "V ${tgtf}"
            else
                cat<<EOF>&2
$0 error in 'rewrite_template' in 'cat "${srcf}" | sed "s%%%;" > "${tgtf}"'.
EOF
                return 1
            fi
        done
        return 0
    else
        return 1
    fi
}
function inc_major {
    if read_version
    then
	version_major=$(( ${version_major} + 1 ))
	version_minor=0
	version_build=1
	if write_version && echo_version
	then
	    return 0
	else
	    return 1
	fi
    else
	return 1
    fi
}
function inc_minor {
    if read_version
    then
	version_minor=$(( ${version_minor} + 1 ))
	version_build=1
	if write_version && echo_version
	then
	    return 0
	else
	    return 1
	fi
    else
	return 1
    fi
}
function inc_build {
    if read_version
    then
	version_build=$(( ${version_build} + 1 ))
	if write_version && echo_version
	then
	    return 0
	else
	    return 1
	fi
    else
	return 1
    fi
}

function self_read_version {
    if [ -f $0 ]
    then
	if version_major=$(egrep '^_self_version_major=' $0 | sed 's/_self_version_major=//')&&[ -n "${version_major}" ]&&
		version_minor=$(egrep '^_self_version_minor=' $0 | sed 's/_self_version_minor=//')&&[ -n "${version_minor}" ]
	then
	    return 0
	else
	    return 1
	fi
    else
	return 1
    fi
}
function self_write_version {
    if [ -n "${version_major}" ]&&[ -n "${version_minor}" ]
    then
	if cat $0 | sed "s%^_self_version_major=.*%_self_version_major=${version_major}%; s%^_self_version_minor=${version_minor}%^_self_version_minor=${version_minor}%;" > /tmp/tmp &&
		[ -n "$(diff /tmp/tmp $0 )" ]&&
		cp /tmp/tmp $0
	then
	    return 0
	else
	    return 1
	fi
    else
	return 1
    fi
}   
function self_echo_version {
    if self_read_version
    then
	echo "${version_major}.${version_minor}"
	return 0
    else
	return 1
    fi
}
function self_inc_major {
    if self_read_version
    then
	version_major=$(( ${version_major} + 1 ))
	version_minor=0

	if self_write_version && self_echo_version
	then
	    return 0
	else
	    return 1
	fi
    else
	return 1
    fi
}
function self_inc_minor {
    if self_read_version
    then
	version_minor=$(( ${version_minor} + 1 ))

	if self_write_version && self_echo_version
	then
	    return 0
	else
	    return 1
	fi
    else
	return 1
    fi
}

if [ -n "${1}" ]
then
    case "${1}" in
	init)
	    if init_version "${2}"
	    then
		exit 0
	    else
		exit 1
	    fi
	    ;;
	echo)
	    if echo_version
	    then
		exit 0
	    else
		exit 1
	    fi
	    ;;
	inc)
	    if [ -n "${2}" ]
	    then
		case "${2}" in
		    major)
			if inc_major
			then
			    exit 0
			else
			    exit 1
			fi
			;;
		    minor)
			if inc_minor
			then
			    exit 0
			else
			    exit 1
			fi
			;;
		    build)
			if inc_build
			then
			    exit 0
			else
			    exit 1
			fi
			;;
		    *)
			usage
			;;
		esac
	    else
		usage
	    fi
	    ;;
	self)
	    if [ -n "${2}" ]
	    then
		case "${2}" in
		    echo)
			if self_echo_version
			then
			    exit 0
			else
			    exit 1
			fi
			;;
		    inc)
			if [ -n "${3}" ]
			then
			    case "${3}" in
				major)
				    if self_inc_major
				    then
					exit 0
				    else
					exit 1
				    fi
				    ;;
				minor)
				    if self_inc_minor
				    then
					exit 0
				    else
					exit 1
				    fi
				    ;;
				*)
				    usage
				    ;;
			    esac
			else
			    usage
			fi
			;;
		    *)
                        usage
                        ;;
		esac
	    else
                if self_echo_version
		then
		    exit 0
		else
		    exit 1
		fi
	    fi
	    ;;
	rewrite)
	    if rewrite_template "${2}"
	    then
		exit 0
	    else
		exit 1
	    fi
	    ;;
	*)
	    usage
	    ;;
    esac
else
    echo_version
fi
