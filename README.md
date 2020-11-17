# CheckFastq
Checks fastq files for common format and sequencing errors.

This software reads compressed fastq files, which are a typical output from genetic sequencing machines or services, and checks them for common errors. The total number of sequencing reads is also reported.

CheckFastq.java checks for positions stuck on a particular base. This can occur if multiple samples are multiplexed on a single sequencing run, and the wrong length of indexes is removed from the beginning of the read. The software will report any position in the read that is one particular base for more than 45% of the reads, or is unknown ("N") for more than 20% of the reads. The software also reports if the gzip compression of the file is invalid.

CheckFastq2.java assumes that the provided fastq files are paired, which is the case for the majority of modern short read sequencing machines. This allows it to perform additional checks. In addition to the checks made by CheckFastq, this software makes the following checks:
1. The read headers in the paired fastq files match for every read.
2. The third line of each read record contains just a "+" character.
3. The base quality line is the same length as the base sequence line.
4. The fastq file is truncated half-way through a read record.
5. The paired fastq files contain the same number of reads.

The software is written in Java, and it needs to be compiled before it is run. To compile, ensure a Java Development Kit (JDK) is installed, then download the CheckFastq.java and CheckFastq2.java files. Open a terminal in the directory with those files, and run:
```
javac CheckFastq.java CheckFastq2.java
```

To run the software, identify the directory where you compiled the software (for instance /path/to/software). Then the following can be run from anywhere, but preferably where your fastq files are located:
```
java -cp /path/to/software CheckFastq *.fastq.gz
```
or if your fastq files are paired:
```
java -cp /path/to/software CheckFastq2 *.fastq.gz
```

The software will accept multiple fastq files as arguments. CheckFastq will process each file in parallel, while CheckFastq2 will process each file pair in parallel, so it may take twice as long as CheckFastq. If all is okay, the only output will be a number, which is the count of reads in all the fastq files. Any errors found in the files will be added to the output as one line per error, with the file in which the error was found, followed by a tab character, followed by a description of the error that was found. For example:

```
$ java -cp /path/to/software CheckFastq2 file1_R1.fq.gz file1_R2.fq.gz file2_R1.fq.gz file2_R2.fq.gz
file1_R1.fq.gz  Read failure reading fastq file: "Unexpected end of ZLIB input stream"
file1_R2.fq.gz  File appears to be truncated half-way through a read record
file2_R1.fq.gz  File has fewer reads than its pair (76213 versus 1987314)
file2_R2.fq.gz  Read 1 A fraction is 0.879
87649825
```
