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
import java.net.URL;
import java.util.Enumeration;

/**
 * A chain link to merge disjoint tails.
 * 
 * When {@link Package} (initialization) is unable to resolve disjoint
 * tails, it merges them using an instance of this class.
 */
public class ChainClassLoaderLink
    extends ClassLoader
{

    public final ClassLoader parent;
    public final ClassLoader child;


    /**
     * A chain link to merge disjoint tails.
     * 
     * @param parent One class loader
     * @param child Another class loader
     */
    public ChainClassLoaderLink(ClassLoader parent, ClassLoader child){
        super(parent);
        if (null != parent && null != child){
            this.parent = parent;
            this.child = child;
        }
        else {
            throw new IllegalArgumentException();
        }
    }


    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return this.parent.loadClass(name);
        }
        catch (ClassNotFoundException cnfx){

            return this.child.loadClass(name);
        }
    }
    @Override
    public URL getResource(String name){
        URL re = this.parent.getResource(name);
        if (null == re){
            re = this.child.getResource(name);
        }
        return re;
    }
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> a = this.parent.getResources(name);
        Enumeration<URL> b = this.child.getResources(name);
        if (null == a || (!a.hasMoreElements()))
            return b;
        else if (null == b || (!b.hasMoreElements()))
            return a;
        else
            return new CombineEnumeration(a,b);
    }


    /**
     * 
     */
    static class CombineEnumeration<E>
        implements Enumeration<E>
    {

        public final Enumeration<E> a, b;

        private boolean usingA = true;


        CombineEnumeration(Enumeration<E> a, Enumeration<E> b){
            super();
            if (null != a && null != b){
                this.a = a;
                this.b = b;
            }
            else {
                throw new IllegalArgumentException();
            }
        }


        public boolean hasMoreElements() {
            if (this.usingA){
                if (this.a.hasMoreElements()){
                    return true;
                }
                else {
                    this.usingA = false;
                }
            }
            return b.hasMoreElements();
        }
        public E nextElement() {
            if (this.usingA)
                return this.a.nextElement();
            else
                return this.b.nextElement();
        }
    }

}
