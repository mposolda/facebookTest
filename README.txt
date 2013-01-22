To try the app, you need to:

1) Add virtual host "server.local.network" to your "/etc/hosts" (assuming you are on linux).

192.168.2.8 server.local.network

2) Build application with "mvn clean install" and deploy to your favourite app server (Tested on JBoss AS7)

3) Start JBoss AS7 with "./standalone.sh -b server.local.network"

4) Access "http://server.local.network:8080/facebookTest/test1" with your browser