#####################################################################################################
#                                                                                                   #
# Makefile for the compilation of a Latex file                                                      #
#                                                                                                   #
# AUTHOR : Brecht Vermeulen (5-02-00)                                                               #
#                                                                                                   #
#####################################################################################################

# $Id: Makefile,v 1.1 2001/11/16 15:02:07 bvermeul Exp $

#############
# {GENERAL} #
#############

LATEX_FILE = boek.tex

CLASS_FILE = 

LATEX = /usr/bin/latex
DVIPS = /usr/bin/dvips
PS2PDF = /usr/bin/ps2pdf
LATEXPDF = /usr/bin/pdflatex

DVI_FILE = $(LATEX_FILE:.tex=.dvi)
PS_FILE = $(LATEX_FILE:.tex=.ps)
PDF_FILE = $(LATEX_FILE:.tex=.pdf)

#############
# {TARGETS} #
#############

all: $(PS_FILE) $(PDF_FILE)

dvi: $(DVI_FILE)

ps: $(PS_FILE)

pdf: $(PDF_FILE)

#################
# {COMPILATION} #
#################

$(DVI_FILE) : $(LATEX_FILE) $(CLASS_FILE)
	$(LATEX) ${LATEX_FILE} && $(LATEX) ${LATEX_FILE} 

$(PS_FILE) : $(DVI_FILE)
	$(DVIPS) $< -o $@

$(PDF_FILE) : $(LATEX_FILE) $(CLASS_FILE)
	$(LATEXPDF) $(LATEX_FILE) && $(LATEXPDF) $(LATEX_FILE)

print: $(PS_FILE)
	lpr $(PS_FILE)

clean:
	$(RM) $(DVI_FILE) $(PS_FILE) $(PDF_FILE) 
	$(RM) *.aux $(LATEX_FILE:.tex=.log)
	$(RM) $(LATEX_FILE:.tex=.toc) 
	$(RM) *~ core

########################################################################################################################

