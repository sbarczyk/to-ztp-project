package pl.edu.agh.to.e2e;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AppProcessFactory {

    private AppProcessFactory() { }

    public static Process startApp(Map<String, String> env) throws IOException {
        String javaBin = System.getProperty("java.home") + "/bin/java";
        String classpath = System.getProperty("java.class.path");

        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add("pl.edu.agh.to.ToZtpApplication");

        ProcessBuilder pb = new ProcessBuilder(cmd);

//        pb.redirectErrorStream(true);

        if (env != null) {
            pb.environment().putAll(env);
        }

        return pb.start();
    }
}