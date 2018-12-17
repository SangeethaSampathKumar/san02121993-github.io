JFLAGS = -g
JC = javac
RUN = java
ROOTNS = 198.41.0.4
EC2LOC = ./ec2.csv

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java 

CLASSES = \
	edu/wisc/cs/sdn/simpledns/packet/DNS.java \
	edu/wisc/cs/sdn/simpledns/packet/DNSQuestion.java \
	edu/wisc/cs/sdn/simpledns/packet/DNSRdataAddress.java \
	edu/wisc/cs/sdn/simpledns/packet/DNSRdataBytes.java \
	edu/wisc/cs/sdn/simpledns/packet/DNSRdata.java \
	edu/wisc/cs/sdn/simpledns/packet/DNSRdataName.java \
	edu/wisc/cs/sdn/simpledns/packet/DNSResourceRecord.java \
	edu/wisc/cs/sdn/simpledns/ReadCSV.java \
	edu/wisc/cs/sdn/simpledns/SimpleDNS.java 

default: classes

classes: $(CLASSES:.java=.class)

run:
	$(RUN) edu/wisc/cs/sdn/simpledns/SimpleDNS -r $(ROOTNS) -e $(EC2LOC)

clean:
	$(RM) edu/wisc/cs/sdn/simpledns/packet/*.class
	$(RM) edu/wisc/cs/sdn/simpledns/SimpleDNS.class
	$(RM) edu/wisc/cs/sdn/simpledns/ReadCSV.class
