/*
 * Syntelos ENA
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
import java.io.PrintStream;

/**
 * 
 */
public final class Main
    extends Object
{
    /**
     * 
     */
    public static void usage(java.io.PrintStream out){

	out.println();
	out.println("Synopsis");
	out.println();
	out.println("    syntelos.rpkg.Main [-?|-help]");
	out.println();
	out.println("Description");
	out.println();
	out.println("    This message.");
	out.println();
	out.println();
	out.println("Synopsis");
	out.println();
	out.println("    syntelos.rpkg.Main -list");
	out.println();
	out.println("Description");
	out.println();
	out.println("    Dump package list.");
	out.println();
	out.println();
	out.println("Synopsis");
	out.println();
	out.println("    syntelos.rpkg.Main -lookup <substring>");
	out.println();
	out.println("Description");
	out.println();
	out.println("    Print available package information.");
	out.println();
    }
    /**
     * 
     */
    public enum Opt {
        none,
        unknown,
        list,
        lookup,
        help;

        public final static Opt recognize(String s){
	    if (null == s || 1 > s.length()){
		return Opt.none;
	    }
	    else {
		boolean opt = false;
		while (0 < s.length() && '-' == s.charAt(0)){
		    s = s.substring(1);
		    opt = true;
		}

		if (!opt){
		    return Opt.unknown;
		}
		else if (0 < s.length()){
		    if (1 == s.length()){
                        char c = s.charAt(0);
                        switch(c){
                        case 'h':
                        case '?':
                            return Opt.help;
                        default:
                            return Opt.unknown;
                        }
		    }
		    else {
			switch(s.charAt(0)){
			case 'h':
			    if ("help".equals(s)){
				return Opt.help;
			    }
			    else {
				return Opt.unknown;
			    }
			case 'l':
			    if ("list".equals(s)){
				return Opt.list;
			    }
			    else if ("lookup".equals(s)){
				return Opt.lookup;
			    }
			    else {
				return Opt.unknown;
			    }
			default:
			    return Opt.unknown;
			}
		    }
		}
		else {
		    return Opt.none;
		}
	    }
        }
    }
    /**
     * 
     */
    public static void main(String[] argv){
        
	final int argc = argv.length;

	final PrintStream stdout = System.out;

	int argx = 0;
        String arg = null;
        Opt opt = null;
        try {
            Package.Init(Main.class);

            while (argx < argc){
                arg = argv[argx++];
                opt = Opt.recognize(arg);
                switch(opt){
                    /*
                     * LIST
                     */
                case list:
                    Package[] list = Package.getPackages();
                    if (null != list){
                        boolean once = true;
                        for (Package p : list){
                            if (once)
                                once = false;
                            else
                                stdout.println();

                            p.println(stdout);
                        }
                        System.exit(0);
                    }
                    else {
                        stdout.printf("syntelos.rpkg.Main error, package list empty.%n",arg);
                        System.exit(1);
                    }
                    break;
                    /*
                     * LOOKUP
                     */
                case lookup:
                    if (argx < argc){
                        arg = argv[argx++];
                        Package p = Package.getPackage(arg);
                        if (null != p){
                            p.println(stdout);
                            System.exit(0);
                        }
                        else {
                            stdout.printf("syntelos.rpkg.Main error, package '%s' not found.%n",arg);
                            System.exit(1);
                        }
                    }
                    else {
                        stdout.printf("syntelos.rpkg.Main error, option '%s' requires argument.%n",arg);
                        System.exit(1);
                    }
                    break;
                    /*
                     * HELP
                     */
                case help:
                    usage(stdout);
                    System.exit(1);
                    break;
                    /*
                     * ERROR
                     */
                default:
                    stdout.printf("syntelos.rpkg.Main error, unrecognized argument '%s'.%n",arg);
                    System.exit(1);
                    break;
                }
            }
            /*
             * Default invocation
             */
            usage(stdout);
            System.exit(1);
        }
        catch(Throwable t){
            while (null != t){
                t.printStackTrace();
                t = t.getCause();
            }
            System.exit(1);
        }
    }
}
