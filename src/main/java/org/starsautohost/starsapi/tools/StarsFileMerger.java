package org.starsautohost.starsapi.tools;

import java.util.ArrayList;
import java.util.List;

public class StarsFileMerger {
    private static boolean wantsHelp(String[] args) {
        if (args.length == 0) return true;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-help") || arg.equalsIgnoreCase("--help")) {
                return true;
            }
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("-h")) return true;
        boolean foundOpt = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-m")) {
                if (foundOpt) return true;
                foundOpt = true;
            }
        }
        if (!foundOpt) return true;
        return false;
    }
    
    public static void main(String[] args) throws Exception {
        if (wantsHelp(args)) {
            System.out.println("Usage: java -jar StarsFileMerger.jar -m file...");
            System.out.println("       java -jar StarsFileMerger.jar -h file...");
            System.out.println();
            System.out.println("StarsFileMerger will merge data among allies' M files or H files.");
            System.out.println("Use -m to merge M files, -h to merge H files.");
            System.out.println();
            System.out.println("If merging M files:");
            System.out.println("All M files supplied on the command line will have their data augmented");
            System.out.println("with the data on each planet, player, design, fleet, minefield, packet,");
            System.out.println("salvage, or wormhole from any of the files.");
            System.out.println();
            System.out.println("Backups of each input M file will be retained with suffix .backup-m#.");
            System.out.println();
            System.out.println("If merging H files:");
            System.out.println("All H files supplied on the command line will have their data replaced");
            System.out.println("with the newest data on each planet, player, and design from any of the files.");
            System.out.println();
            System.out.println("M files supplied on the command line will have their data incorporated");
            System.out.println("but will not be changed.  M files are needed for accurately determining");
            System.out.println("the latest ship designs.");
            System.out.println();
            System.out.println("Backups of each input H file will be retained with suffix .backup-h#.");
            System.out.println();
            System.out.println("Usage notes:");
            System.out.println("If allies merge M files each turn, merging H files should not be necessary.");
            System.out.println("If any ally skips a turn, merging H files might become necessary.");
            System.out.println("H file merge can also be used for a single player to fill a new H file");
            System.out.println("from a collection of M file backups.");
            return;
        }
        
        List<String> filenames = new ArrayList<String>();
        boolean isM = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-m")) {
                isM = true;
            } else if (arg.equalsIgnoreCase("-h")) {
                isM = false;
            } else {
                filenames.add(arg);
            }
        }
        String[] filenameArray = filenames.toArray(new String[filenames.size()]);
        if (isM) {
            new MFileMerger().run(filenameArray);
        } else {
            new HFileMerger().run(filenameArray);
        }
    }

}
