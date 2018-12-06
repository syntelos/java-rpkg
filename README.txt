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
