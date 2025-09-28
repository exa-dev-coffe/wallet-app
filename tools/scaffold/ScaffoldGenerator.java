import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ScaffoldGenerator {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java ScaffoldGenerator <ModuleName>");
            return;
        }

        String name = args[0].toLowerCase();
        String className = Character.toUpperCase(name.charAt(0)) + name.substring(1);

        String basePath = "src/main/java/com/time_tracker/be/" + name;
        new File(basePath).mkdirs();

        writeFile(basePath + "/" + className + "Model.java",
                "package com.time_tracker.be." + name + ";\n\n" +
                        "public class " + className + "Model {\n}\n");

        writeFile(basePath + "/" + className + "Repository.java",
                "package com.time_tracker.be." + name + ";\n\n" +
                        "public interface " + className + "Repository {\n}\n");

        writeFile(basePath + "/" + className + "Service.java",
                "package com.time_tracker.be." + name + ";\n\n" +
                        "public class " + className + "Service {\n}\n");

        writeFile(basePath + "/" + className + "Route.java",
                "package com.time_tracker.be." + name + ";\n\n" +
                        "public class " + className + "Controller {\n}\n");

        System.out.println("✅ Scaffold for module '" + name + "' created!");
    }

    private static void writeFile(String path, String content) throws IOException {
        File f = new File(path);
        if (f.exists()) {
            System.out.println("Warn " + path + " already exists, skipped");
            return;
        }
        try (FileWriter writer = new FileWriter(f)) {
            writer.write(content);
        }
    }
}
