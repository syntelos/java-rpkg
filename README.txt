

RPKG Overview

Alternative design and implementation of the java package concept due
to a design flaw in the java class loader interface.  In this case,
RPKG, sealed packages with individual manifest records are represented
with package information identities.

This package subsystem (RPKG) is able to represent packages in foreign
(child) class loaders by virtue of its initialization proceedure.


Problem statement

A design flaw in the implementation of the java package subsystem
prevents dereferencing a package from a foreign class loader.

The structure of the class loader runtime is a series of class loaders
that incrementally expand the scope of the class path.  This class
loader "chain" is available using the class loader "parent" method.
The "parent" relationship is a singly linked list of class loaders,
from the "boot class path" class loader to the "application class
path" class loader, and then possibly including additional scopes.

Each class loader manages the loading of classes, and packages, from
its class path.  A library managed by a parent is available from its
child, but the library managed by the child is not available to the
parent.

  java/lang/ClassLoader

    java/lang/Package

  # /usr/lib/jvm/java-8-openjdk-amd64/src/java/lang/ClassLoader.java
  1604	    /**
  1605	     * Returns a <tt>Package</tt> that has been defined by this class loader
  1606	     * or any of its ancestors.
  1607	     *
  1608	     * @param  name
  1609	     *         The package name
  1610	     *
  1611	     * @return  The <tt>Package</tt> corresponding to the given name, or
  1612	     *          <tt>null</tt> if not found
  1613	     *
  1614	     * @since  1.2
  1615	     */
  1616	    protected Package getPackage(String name) {
  1617	        Package pkg;
  1618	        synchronized (packages) {
  1619	            pkg = packages.get(name);
  1620	        }
  1621	        if (pkg == null) {
  1622	            if (parent != null) {
  1623	                pkg = parent.getPackage(name);
  1624	            } else {
  1625	                pkg = Package.getSystemPackage(name);
  1626	            }
  1627	            if (pkg != null) {
  1628	                synchronized (packages) {
  1629	                    Package pkg2 = packages.get(name);
  1630	                    if (pkg2 == null) {
  1631	                        packages.put(name, pkg);
  1632	                    } else {
  1633	                        pkg = pkg2;
  1634	                    }
  1635	                }
  1636	            }
  1637	        }
  1638	        return pkg;
  1639	    }

A library using the java package object class is unable to dereference
packages found in a child class loader because access to the java
package subsystem is protected.  


Problem solution

As a result, the design flaw needs to be repaired by replacing the
package concept.


RPKG initialization

Each leaf or tail of the class loader chain needs to be initialized.

  Package.Init(Main.class);

This action creates and stores each sealed package listed in each
manifest in the available class path.


RPKG examination

The effect of initialization may be examined by listing the contents
of the package subsystem.

  Package.List(System.out);


JAR manifest

  Name: syntelos/rpkg/
  Sealed: true
  Implementation-Title: "syntelos.rpkg"
  Implementation-Version: "1.0.1"
  Implementation-Vendor: "John Pritchard, Syntelos"

Each package available to the RPKG package subsystem has a JAR
manifest entry that is sealed.


Update


2018/12 -- 1.0.1

 [DONE] Export package list from user class loader.


2018/12 -- 1.0.2

 [DONE] Export package init class loader.
 
 [TODO] Review package init, which presumes jar files.

