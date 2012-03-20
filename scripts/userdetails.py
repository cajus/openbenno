#!/usr/bin/python
USERDETAILSDB="/srv/mailarchive/userdb/UserDetails"
from pysqlite2 import dbapi2 as sqlite
import os
import sys

def commit(con, entry):
    try:
        con.execute("insert into userdetails(username,entity,content) values (? ,? ,?)",  entry)
        con.commit()
    except sqlite.Error, e:
        print "ERROR ", e.args[0]
        sys.exit()
    print "SUCCESS"
    sys.exit()
    
def delete(con, entry):
    try:
        con.execute("delete from userdetails where username=(?) and entity=(?) and content=(?)", entry)
        con.commit()
    except sqlite.Error, e:
        print "ERROR ", e.args[0]
        sys.exit()  
    print "SUCCESS"

def main(argv=None):
    if argv is None:
        argv = sys.argv

    if  not os.path.exists(USERDETAILSDB):
        con=sqlite.connect(USERDETAILSDB)
        con.execute("create table userdetails(username varchar(100), entity varchar(10), content varchar(100) )")
        con.close()

    con = sqlite.connect(USERDETAILSDB) 
   
    if (len(argv)<2):
        print "Not enough arguments"
        return -1
    if (argv[1]=="GET"):
	#returns "username password" in cleartext. CHANGE FOR LDAP
        username=argv[2]
        try:
            res=con.execute("select * from userdetails where username=(?)", (username, ))
            found=0
            for row in res:
                found+=1
                print row[1], row[2]
            if (found==0):
                print "NOT_FOUND"
        except sqlite.Error, e:
            print "ERROR ", e.args[0]
            sys.exit()  
        
    
    if (argv[1]=="USERLIST"):
        res=con.execute("select distinct username from userdetails")
        for row in res:
            print row[0]
	        
    if (argv[1]=="ADD"):
        if (argv[2]=="USER"):
            username=argv[3]
            passwd=" ".join(argv[4:])
            entry=(username, "PASSWORD", passwd)
            try:
                con.execute("delete from userdetails where username=(?) AND entity=(?)", (entry[0], entry[1]))
                con.execute("insert into userdetails(username,entity,content) values (? ,? ,?)",  entry)
                con.commit()
                print "SUCCESS"
            except sqlite.Error, e:
                print "ERROR ", e.args[0]
                sys.exit()
            print "SUCCESS"
        elif argv[2]=="MAIL":
            username=argv[3]
            address=argv[4]
            entry=(username, "MAIL", address)
            commit(con, entry)
        elif argv[2]=="QUERY":
            entry=(argv[3], "QUERY", " ".join(argv[4:]))
            commit(con, entry)
        elif argv[2]=="ROLE":
            entry=(argv[3], "ROLE", argv[4])
            commit(con, entry)
    elif (argv[1]=="REMOVE"):
        if argv[2]=="USER":
            username=argv[3]
            try:
                con.execute("delete from userdetails where username=(?)", (username, ))
                con.commit()
            except sqlite.Error, e:
                print "ERROR ", e.args[0]
                sys.exit()
                print "SUCCESS"
        elif argv[2]=="MAIL":
            entry=(argv[3], "MAIL",argv[4] )
            delete(con, entry)
        elif argv[2]=="QUERY":
            entry=(argv[3], "QUERY"," ".join(argv[4:]))
            delete(con, entry)
        elif argv[2]=="ROLE":
            entry=(argv[3], "ROLE",argv[4])
            delete(con, entry)
 
    con.close()
    
if __name__ == "__main__":
    sys.exit(main())
