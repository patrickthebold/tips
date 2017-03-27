# TIPS

## Quickstart
Assumptions: You have some unix like environment with bash, sbt, postgresql, python3, and virtualenv
installed.

Start the app!  
From the root directory:
```bash
./dev_start.sh
```
Use it! (python bindings for the api)  
Open a new terminal (also in root directory):
```bash
cd python
virtualenv env
. env/bin/activate
pip install -r requirements.txt 
python
```
```python
>>> from tips import Tips
>>> t = Tips("username","password")
>>> t.create_user()
>>> t.new_tip("Something important is happening")
>>> t.tips().json()
```

## Running the app
The app is designed to connect to a postgresql database. The main thing is configuring the
connection to the database. The details for the database connection (amongst other things) need to
be stored in [environmental variables](https://12factor.net/config). See the file `dev-environment`.
In general you should source the environmental variables, as the application expects them to be set,
and run scripts from the project's root director.

It's recommended that you just use postgres locally on a nonstandard port, with the data files it
uses stored within the project, and running as a process owned by your user. 
To accomplish this:
`$TIPS_DB_DIR` should be a path where postgres will store it's data. It's important that this not
exist initially. `$TIPS_DB_HOSTNAME` must be localhost. The `dev_start.sh` script will initialize the
specified directory, start postgresql, add a database, user, and schema for the application. Then
use sbt to start the application.

If you have postgres either running on another server or locally, but as part of a system service
(e.g. you start it with systemd or similar). You should not set `$TIPS_DB_DIR` and configure the
other variables appropriately. Take a look at createdb.sh, and do likewise on the database, so it is
setup with the appropriate user, password, database, and schema. Once the database is setup, `sbt
run` will launch the application.

## Running the tests

Functional tests are written in python to test the api. You will need python3, and you should have
virtualenv. Follow the steps above to setup the virtual environment and download the dependencies. I
always like to separate out code that is just a client api vs code that is testing. So `tips.py` is
a general purpose way to access the API easily. While test_\*.py are the actual tests.
The tests can be run with `pytest <test_name>` (from the python directory).

## Explanation of code
It's been a couple years since I used Scala, but when I did we used Play and Postgresql, so I chose
that mostly due to familiarity. I started with the [seed
template](https://www.playframework.com/download) and reviewed the [Rest Api
Example](https://github.com/playframework/play-scala-rest-api-example/tree/master). And, of course,
consulted the documentation heavily.

### Considering Scale

There are two things to consider:

*  Scaling the web tier
*  Scaling the database tier

Play stores the user session in a cookies. This makes the web app entirely stateless and can scale
out to multiple machines behind a load balancer easily. Of course, this means the entire session is
serialized as a header on each http request, and so care must be taken to not put too much data in the
session. 

I'm less familiar with how to scale postgresql. I know there are projects that help with clustering,
but I am not familiar with them. Since this is basically a CRUD app, http requests are almost one to
one with database calls. Postgres is a great database and can handle pretty large loads, but of you
don't think scaling up will be able to meet the demands of the application, then it's either a bad
choice, or more research would need to be done to scale it out.

So we will likely have an architecture with N webservers behind a load balancer all connected to one
database. The database will have an [optimal number of
connections](https://wiki.postgresql.org/wiki/Number_Of_Database_Connections), and those should be
distributed equally amongst the webservers. If you take a look at the link, it does say there's not
much data on SSDs, but it seems like the optimal number is rather low. My guess is that one
webserver should be enough. Of course, as always with performance, we need to test to find the
optimal configuration. 

I say one webserver is probably enough due to the following. I'm guessing the optimal number of
connections is at most 100. This application is configured so all the database requests are on a
seprate thread pool with the same number of threads as connections. As a resource, each thread has a
stack which takes up memory, but 100 or so threads on the JVM is fine. On the side of HTTP requests,
play using a small thread pool with a non-blocking IO strategy, which let's it handle a large number
of requests effiently. So as the requests come in they queue up until a connection is available.
Scaling out the web tier doesn't change the optimal number of databse connections, so it doesn't how
the length of the queue behaves with respect to the requests coming in and the database fulfilling
those requests. So the main question is how many http servers does it take to saturate the databse. 

Anyway, Play makes it easy to scale out the web tier, but I think we'd likely run into problems
scaling out the database tier rather quickly.  

### Security

I took security to be mostly out of scope. I don't have much experience securing web apps. For the
most part, I stuck with the defaults of the template and example mentioned above. Of course, the
site needs to be run over ssl. There's an environment variable to turn on secure cookes, which
should be on once ssl is enabled. There is the default Csrf protection, so you need to add
Csrf-Token: nocheck to the header, in order to make requests. By default
the cookie is signed with 
[HMAC-SHA1](https://www.playframework.com/documentation/2.5.x/CryptoMigration25#MAC-Algorithm-Independence)
but not encrypted. (HMAC-SHA256 seems to be a better choice at this point in time.) Also the cookie
expiration time in play only causes the browser to stop sending the cookie and doesn't prevent the
cookie form being stored and used later
[Ref](https://www.playframework.com/documentation/2.5.x/ScalaSessionFlash). This app adds an
expiration time to the session and checks it during authentication. It's odd that's not the default
IMHO.

### Testing

This took me a bit longer than I expected, and I did not get to writing unit tests. That is a TODO.
The functional test coverage is quite good IMHO. I never feel confident that my code is correct
without writing functional test, so those are the necessary. If your application has many layers,
then yes there should be testing at each of the layers. For this application, there's almost no code
that's not either callind the database or using the play framework. So testing at the top layer is
the best bang for your buck.

### Documentation

I also didn't get to documenting the api. The main concern here is having quality documentation that
does not get out of date with code changes. I would want to research to tools available to make that
easier. The die-hard Rest fans would say the api should not be documented and be entirely self
discoverable. I made some efforts in that vein, but really there should be quality documention for
an API.
