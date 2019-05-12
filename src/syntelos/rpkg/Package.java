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
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Java package manager stores sealed packages found in the class
 * loader chain.  Each package has a single, unique java object.
 * 
 * <pre>
 * boolean isPackage(Package that){
 * 
 *     return (that == Package.getPackage(this.class))
 * }
 * </pre>
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

    private static ClassLoader InitClassLoader = null;

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

        InitClassLoader = UniqueClassLoader(InitClassLoader,tail);

        try {
            Enumeration<URL> enu = tail.getResources("META-INF/MANIFEST.MF");

            while (enu.hasMoreElements()){

                URL src = enu.nextElement();

                InitMF(tail,src);
            }
        }
        catch (IOException iox){
            iox.printStackTrace();
        }
    }
    /**
     * Open a manifest given a manifest reference.
     * 
     * @param tail A class loader chain terminal
     * 
     * @param src An archive manifest reference
     * 
     * @throws java.lang.IllegalStateException Reference argument
     * 'src' is not a pointer to an archive manifest.
     */
    private static void InitMF(ClassLoader tail, URL src)
        throws IOException
    {
        /*
         * Derive a manifest object model from a manifest reference.
         */
        URLConnection con = src.openConnection();
        InputStream in = con.getInputStream();
        try {
            if (con instanceof JarURLConnection){
                JarURLConnection jcon = (JarURLConnection)con;
                JarFile jfi = jcon.getJarFile();
                Manifest man = jfi.getManifest();
                if (null != man){

                    InitMF(tail,src,man);
                }
            }
            else {
                Manifest man = new Manifest(in);

                InitMF(tail,src,man);
            }
        }
        finally {
            in.close();
        }
    }
    /**
     * Store package information found in an archive manifest.
     * 
     * @param tail Class loader chain terminal
     * @param src Archive manifest reference
     * @param man Archive manifest object model
     */
    private static void InitMF(ClassLoader tail, URL src, Manifest man){
        if (null != tail && null != src && null != man){
            /*
             * Read the archive manifest object model into the package
             * store.
             */
            Map<String,Attributes> map = man.getEntries();
            for (Map.Entry<String,Attributes> ent : map.entrySet()){
                String name = ent.getKey();
                if (name.endsWith("/") && (!Package.Store.containsKey(name))){
                    Attributes attr = ent.getValue();

                    Package p = new Package(tail,src,man,name,attr);

                    if (p.sealed){

                        Package.Store.put(p.name,p);
                    }
                }
            }
        }
        else {
            throw new IllegalArgumentException();
        }
    }
    /**
     * Exports the init class loader as the terminal of the
     * application class loader chain.
     * 
     * @return The init class loader, or a derivative representing the
     * init class loader chain.
     * 
     * @throws java.lang.IllegalStateException When not initialized
     * and unable to return the init class loader
     */
    public final static ClassLoader InitClassLoader(){
        if (null != InitClassLoader)
            return InitClassLoader;
        else
            throw new IllegalStateException("Not initialized");
    }
    private final static ClassLoader UniqueClassLoader(ClassLoader a, ClassLoader b){
        if (null == a)
            return b;
        else if (null == b)
            return a;
        else if (a instanceof ChainClassLoaderLink){
            ChainClassLoaderLink c = (ChainClassLoaderLink)a;

            if (IsChild(b,c.parent) && IsChild(b,c.child)){
                /*
                 * Accept join
                 */
                return b;
            }
            else if (IsChild(c.parent,b) || IsChild(c.child,b)){
                /*
                 * Prune call
                 */
                return c;
            }
            else {
                /*
                 * Create join
                 */
                return new ChainClassLoaderLink(a,b);
            }
        }
        else if (IsChild(a,b))
            return a;
        else if (IsChild(b,a))
            return b;
        else
            return new ChainClassLoaderLink(a,b);
    }
    /**
     * The "child" relation reflects the class loader chain "parent"
     * relation.  It affirms a chain leaf or terminal.
     * 
     * @param c Is child of
     * @param p Is parent of
     * 
     * @return Relationship validity
     */
    private final static boolean IsChild(ClassLoader c, ClassLoader p){
        while (null != p){
            if (p == c.getParent())
                return true;
            else {
                p = p.getParent();
            }
        }
        return false;
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
    private final static String[] Namelist(String jpname){
        StringTokenizer strtok = new StringTokenizer(jpname,".$");
        int count = strtok.countTokens();
        String[] list = new String[count];
        for (int cc = 0; cc < count; cc++){

            list[cc] = strtok.nextToken();
        }
        return list;
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

    private final String[] namelist;
    private final int namelist_count;

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

            this.namelist = Namelist(this.name);
            this.namelist_count = this.namelist.length;
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
    public int namelistCount(){

        return this.namelist_count;
    }
    public String namelistHead(){

        return this.namelist[0];
    }
    public String namelistGet(int ix){

        return this.namelist[ix];
    }
    public String namelistTail(){

        return this.namelist[this.namelist_count-1];
    }
    public String[] namelist(){

        return this.namelist.clone();
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
