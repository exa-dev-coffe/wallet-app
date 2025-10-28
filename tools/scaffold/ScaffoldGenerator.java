import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ScaffoldGenerator {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java ScaffoldGenerator <ModuleName>");
            return;
        }

        String name = args[0];
        String className = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String packageBase = "com.wallet_service.be." + name.toLowerCase();
        String basePath = "src/main/java/" + packageBase.replace('.', '/');

        // Buat direktori utama
        new File(basePath).mkdirs();
        new File(basePath + "/dto").mkdirs();

        // 1️⃣ Model
        writeFile(basePath + "/" + className + "Model.java",
                "package " + packageBase + ";\n\n" +
                        "import com.wallet_service.be.utils.commons.BaseModal;\n" +
                        "import jakarta.persistence.*;\n" +
                        "import lombok.*;\n\n" +
                        "@Getter\n@Setter\n@NoArgsConstructor\n@AllArgsConstructor\n" +
                        "@Entity\n@Table(name = \"tm_" + name.toLowerCase() + "s\")\n" +
                        "public class " + className + "Model extends BaseModal {\n" +
                        "    @Id\n" +
                        "    @GeneratedValue(strategy = GenerationType.IDENTITY)\n" +
                        "    private int id;\n" +
                        "}\n");

        // 2️⃣ Repository
        writeFile(basePath + "/" + className + "Repository.java",
                "package " + packageBase + ";\n\n" +
                        "import org.springframework.data.jpa.repository.JpaRepository;\n" +
                        "import org.springframework.stereotype.Repository;\n\n" +
                        "@Repository\n" +
                        "public interface " + className + "Repository extends JpaRepository<" + className + "Model, Integer> {\n" +
                        "}\n");

        // 3️⃣ Service
        writeFile(basePath + "/" + className + "Service.java",
                "package " + packageBase + ";\n\n" +
                        "import lombok.extern.slf4j.Slf4j;\n" +
                        "import org.springframework.stereotype.Service;\n\n" +
                        "@Slf4j\n@Service\n" +
                        "public class " + className + "Service {\n" +
                        "    private final " + className + "Repository " + name.toLowerCase() + "Repository;\n\n" +
                        "    public " + className + "Service(" + className + "Repository " + name.toLowerCase() + "Repository) {\n" +
                        "        this." + name.toLowerCase() + "Repository = " + name.toLowerCase() + "Repository;\n" +
                        "    }\n" +
                        "}\n");

        // 4️⃣ Route
        writeFile(basePath + "/" + className + "Route.java",
                "package " + packageBase + ";\n\n" +
                        "import com.wallet_service.be.annotation.RequireAuth;\n" +
                        "import com.wallet_service.be.utils.commons.ResponseModel;\n" +
                        "import lombok.extern.slf4j.Slf4j;\n" +
                        "import org.springframework.http.ResponseEntity;\n" +
                        "import org.springframework.web.bind.annotation.*;\n\n" +
                        "@Slf4j\n@RestController\n" +
                        "@RequestMapping(\"/api/1.0/" + name.toLowerCase() + "\")\n" +
                        "public class " + className + "Route {\n" +
                        "    private final " + className + "Service " + name.toLowerCase() + "Service;\n\n" +
                        "    public " + className + "Route(" + className + "Service " + name.toLowerCase() + "Service) {\n" +
                        "        this." + name.toLowerCase() + "Service = " + name.toLowerCase() + "Service;\n" +
                        "    }\n\n" +
                        "}\n");

        System.out.println("✅ Scaffold for module '" + name + "' created successfully!");
    }

    private static void writeFile(String path, String content) throws IOException {
        File f = new File(path);
        if (f.exists()) {
            System.out.println("⚠️  " + path + " already exists, skipped");
            return;
        }
        try (FileWriter writer = new FileWriter(f)) {
            writer.write(content);
        }
    }
}
