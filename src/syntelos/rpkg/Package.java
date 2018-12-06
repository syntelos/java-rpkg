/*
 * Syntelos RPKG
 * Copyright (C) 2018, John Pritchard, Syntelos
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package syntelos.rpkg;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Java package manager stores sealed packages found in the class
 * loader chain.
 * 
 * @see #Init
 * 
 * @author John Pritchard, syntelos
 */
public final class Package
    extends Object
{
    /**
     * @param out Target output
     * 
     * @return Wrote content to output.
     */
    public static boolean List(PrintStream out){
        Package[] list = Package.getPackages();
        if (null != list){
            boolean once = true;
            for (Package p : list){
                if (once)
                    once = false;
                else
                    out.println();

                p.println(out);
            }
            return true;
        }
        else {
            return false;
        }
    }

    private final static Map<String,Package> Store = new java.util.HashMap();

    /**
     * The required initialization needs to be called on the tail of
     * the class loader chain.  Typically, the "main" class is loaded
     * by the class loader that is the tail (or, "leaf") of the class
     * loader "parent" chain.
     */
    public static void Init(Class c){
        Init(c.getClassLoader());
    }
    /**
     * The required initialization needs to be called on the tail of
     * the class loader chain.  
     */
    public static void Init(ClassLoader tail){

        while (null != tail){
            if (tail instanceof URLClassLoader){
                /*
                 * REVIEW
                 * 
                 * Required technique for a URLClassLoader.
                 */
                URLClassLoader ucl = (URLClassLoader)tail;
                URL[] ucp = ucl.getURLs();
                for (URL src : ucp){

                    Init(tail,src);
                }
            }
            else {
                /*
                 * REVIEW
                 * 
                 * The generic technique fails with a URLClassLoader.
                 */
                URL src = tail.getResource("META-INF/MANIFEST.MF");
                if (null != src){

                    Init(tail,src);
                }
            }

            tail = tail.getParent();
        }
    }
    private static void Init(ClassLoader tail, URL src){
        try {
            URLConnection con = src.openConnection();
            InputStream in = con.getInputStream();
            try {
                if (con instanceof JarURLConnection){
                    JarURLConnection jcon = (JarURLConnection)con;
                    JarFile jfi = jcon.getJarFile();
                    Manifest man = jfi.getManifest();
                    if (null != man){

                        Init(tail,src,man);
                    }
                }
                else {
                    JarInputStream jin = new JarInputStream(in);
                    Manifest man = jin.getManifest();
                    if (null != man){

                        Init(tail,src,man);
                    }
                }
            }
            finally {
                in.close();
            }
        }
        catch (Exception x){
            throw new IllegalStateException(src.toString(),x);
        }
    }
    private static void Init(ClassLoader tail, URL src, Manifest man){
        if (null != tail && null != src && null != man){
            Map<String,Attributes> map = man.getEntries();
            for (Map.Entry<String,Attributes> ent : map.entrySet()){
                String name = ent.getKey();
                if (name.endsWith("/") && (!Package.Store.containsKey(name))){
                    Attributes attr = ent.getValue();

                    Package p = new Package(tail,src,man,name,attr);

                    Package.Store.put(p.name,p);
                }
            }
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    private final static Package[] PARY = new Package[0];

    public static Package getPackage(String name){
        int pxl = -1;

        while (true){
            Package p = Package.Store.get(name);
            if (null != p)
                return p;
            else {
                pxl = name.lastIndexOf('.');
                if (0 < pxl)
                    name = name.substring(0,pxl);
                else
                    break;
            }
        }
        return null;
    }
    public static Package getPackage(Class c){
        String cn = c.getName();
        int cxl = cn.lastIndexOf('.');
        String pn = cn.substring(0,cxl);

        while (true){
            Package p = Package.Store.get(pn);
            if (null != p)
                return p;
            else {
                cxl = pn.lastIndexOf('.');
                if (0 < cxl)
                    pn = pn.substring(0,cxl);
                else
                    break;
            }
        }
        return null;
    }
    public static Package[] getPackages(){
        return (Package[])Package.Store.values().toArray(PARY);
    }
    private final static int HASHKEY = Package.class.hashCode();

    private final static String Name2Java(String name){
        if (name.endsWith("/"))
            return name.replace('/','.').substring(0,name.length()-1);
        else {
            return name;
        }
    }
    private final static String Name2Zip(String name){
        if (name.endsWith("/"))
            return name;
        else {
            return name.replace('.','/').concat("/");
        }
    }

    /**
     * If a package is sealed, the reference and loader fields will be
     * defined with the values passed to the constructor.  Otherwise
     * these two fields will be null.
     */
    public final boolean sealed;
    public final String name;
    public final String entry;
    public final String specTitle;
    public final String specVersion;
    public final String specVendor;
    public final String implTitle;
    public final String implVersion;
    public final String implVendor;
    public final URL reference;
    public final ClassLoader loader;
    public final int hashCode;

    /**
     * Constructor called from {@link Package#Init}.
     */
    private Package(ClassLoader loader, URL url, Manifest man, String name, Attributes attr) {
        super();
        if (null != loader && null != url && null != man && null != name && null != attr){
            String specTitle   = attr.getValue(Name.SPECIFICATION_TITLE);
            String specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            String specVendor  = attr.getValue(Name.SPECIFICATION_VENDOR);
            String implTitle   = attr.getValue(Name.IMPLEMENTATION_TITLE);
            String implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            String implVendor  = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            String sealed      = attr.getValue(Name.SEALED);
            /*
             */
            if (null != sealed && "true".equalsIgnoreCase(sealed)) {
                this.sealed = true;
                this.reference = url;
                this.loader = loader;
            }
            else {
                this.sealed = false;
                this.reference = null;
                this.loader = null;
            }
            this.name = Name2Java(name);
            this.entry = name;
            this.specTitle = specTitle;
            this.specVersion = specVersion;
            this.specVendor = specVendor;
            this.implTitle = implTitle;
            this.implVersion = implVersion;
            this.implVendor = implVendor;

            this.hashCode = (name.hashCode() ^ HASHKEY);
        }
        else {
            throw new IllegalArgumentException();
        }
    }


    /*
     * Drop in replacement for java/lang/Package
     */
    public String getName() {
        return this.name;
    }
    public String getSpecificationTitle() {
        return this.specTitle;
    }
    public String getSpecificationVersion() {
        return this.specVersion;
    }
    public String getSpecificationVendor() {
        return this.specVendor;
    }
    public String getImplementationTitle() {
        return this.implTitle;
    }
    public String getImplementationVersion() {
        return this.implVersion;
    }
    public String getImplementationVendor() {
        return this.implVendor;
    }
    public boolean isSealed() {
        return this.sealed;
    }
    public int hashCode(){

        return this.hashCode;
    }
    public String toString(){

        return this.name;
    }
    public boolean equals(Object that){

        return (this == that);
    }
    /**
     * Write package information in manifest format.
     */
    public void println(PrintStream out){
        out.printf("Name: %s%n",this.entry);

        if (this.sealed)
            out.println("Sealed: true");
        else
            out.println("Sealed: false");

        if (null != this.implTitle)
            out.printf("Implementation-Title: %s%n",this.implTitle);
        if (null != this.implVersion)
            out.printf("Implementation-Version: %s%n",this.implVersion);
        if (null != this.implVendor)
            out.printf("Implementation-Vendor: %s%n",this.implVendor);

        if (null != this.specTitle)
            out.printf("Specification-Title: %s%n",this.specTitle);
        if (null != this.specVersion)
            out.printf("Specification-Version: %s%n",this.specVersion);
        if (null != this.specVendor)
            out.printf("Specification-Vendor: %s%n",this.specVendor);
    }
}
