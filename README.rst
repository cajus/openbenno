What is this?
=============

Open Benno Mailarchive is a GPL based mail archiving solution (http://www.openbenno.org).
Because upstream had no interest in fixing bugs for the version 1.x, I've decided to put
up this repository to collect fixes and information about how get things running smoothly.

The initial state of this repository is 1.1.6.4. I've fixed these issues:

* The range selector (30 days, 60 days, etc.) is fixed to work as expected
* Printing with UTF-8 is fixed
* The printing dialog pops up in a new window
* Sorting of mails works
* Color adjustments for the login throbber
* Typo fixes and unification of vocabulary

Build instructions
==================

Download and install "netbeans 6.8 - JAVA". When installing choose
to install the "tomcat" server somewhere. You need exactly this version,
because you will run into a couple of dependency problems, elseways.

Start netbeans and add the projects "core", "admin" and "search"
projects and you're ready to build. Remember that you need to build
"core" first.

Alternatively you can call "make" and "make install" in every sub
directory - if you don't want to remember how to call "mvn" and "ant".

After running "make install" you find a top level "build" directory
which contains the jar and war files. Additionally there's a "run.sh"
script that can start the core part.

Before running, you should read

http://www.openbenno.org/category/installation/

for basic installation instructions.
