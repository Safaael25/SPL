CXX = g++
CXXFLAGS = -g -Wall -Weffc++ -std=c++11
SRCDIR = src
INCDIR = include
BINDIR = bin

SOURCES := $(wildcard $(SRCDIR)/*.cpp)
OBJECTS := $(patsubst $(SRCDIR)/%.cpp, $(BINDIR)/%.o, $(SOURCES))
EXECUTABLE = $(BINDIR)/main

all: $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CXX) $(CXXFLAGS) -o $@ $^

$(BINDIR)/%.o: $(SRCDIR)/%.cpp
	$(CXX) $(CXXFLAGS) -I$(INCDIR) -c -o $@ $<

clean:
	rm -f $(OBJECTS) $(EXECUTABLE)

.PHONY: all clean