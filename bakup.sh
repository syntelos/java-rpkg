#!/bin/bash

name=$(basename $(pwd))

archive=~/src/syntelos/archive/phone/dbnbooks

if [ -d ${archive} ]&&[ -x ${archive}/bakup.sh ]
then
    if [ -n "${1}" ]
    then
	echo OK
	exit 0

    elif cd ${archive} && ./bakup.sh ${name}
    then
	exit 0
    else
	cat<<EOF>&2
$0 error from 'cd ${archive} && ./backup.sh ${name}'.
EOF
	exit 1
    fi
else
    cat<<EOF>&2
$0 error from '[ -d ${archive} ]&&[ -x ${archive}/bakup.sh ]'.
EOF
    exit 1
fi
