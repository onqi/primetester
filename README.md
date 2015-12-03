Prime tester
======================


# Test data preparation
Pre-calculated prime numbers are used as test data set.
Numbers are available online but need additional processing. 
If another data set is to be used (instead of provided), it can be prepared using following script:

```
cd test/resources \
&& wget -qO- -O tmp.zip --no-check-certificate https://primes.utm.edu/lists/small/millions/primes50.zip \
&& unzip tmp.zip \
&& rm tmp.zip \
&& tail -n+3 primes50.txt | sed 's/\s/\n/g' | sed '/^[ \t]*$/d' > primes50.txt
```

This process can be automated, but left as is for now because of the following reasons:

- there's no way to bypass cert issues without fixing keychain (running `keytool`) on the machine where code runs
- `exec-maven-plugin` is platform-dependent
