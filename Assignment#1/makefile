JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC)  $*.java


CLASSES = \
        Iperfer.java \
        Server.java \
        Client.java 

default: classes;

classes: $(CLASSES:.java=.class)

clean:
	rm  *.class
