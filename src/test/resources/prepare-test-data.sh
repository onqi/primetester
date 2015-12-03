#!/bin/bash
cd "$(dirname "$0")"
echo "Downloading primes..."
wget -qO- -O tmp.zip --no-check-certificate https://primes.utm.edu/lists/small/millions/primes50.zip
echo "Inflating..."
unzip -qq -o tmp.zip && rm tmp.zip
echo "Converting to line-by-line format..."
tail -n+3 primes50.txt | sed 's/\s/\n/g' | sed '/^[ \t]*$/d' > primes50.txt
echo "Done"