# LP_Solver Mavenized and OSGi Ready

This is an example service which mavenizes the Java ILP (java interface to integer linear programming (ILP) solvers) [Java_ILP]
and makes it OSGi ready. The challenge was to make the native library available in OSGi

The current build is platform dependent and only tested with
Ubuntu: 12.04.4 LTS 64bit
Apache Maven 3.0.5
Java version: 1.7.0_51, vendor: Oracle Corporation
Apache Karaf: 2.2.11



## Credits

There are quite a few technologies and libraries used in this bundle

* [Java_ILP][1]:
* [SCP_Solver][2]:
* [LP_Solve][3]:

[1] http://javailp.sourceforge.net/

[2] http://scpsolver.org/

[3] http://lpsolve.sourceforge.net/5.5/
