import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipper extends RecursiveTask<Boolean> {

    private final String uri;

    public Zipper(String uri) {
        super();
        this.uri = uri;
    }

    @Override
    protected Boolean compute() {
        var result = true;

        File file = new File(uri);
        if (file.isDirectory()) {
            List<Zipper> subTasks = new ArrayList<>();

            File[] filesList = file.listFiles();
            if (filesList == null || filesList.length == 0) {
                return true;
            }

            for (File f : filesList) {
                Zipper task = new Zipper(f.getPath());
                task.fork();
                subTasks.add(task);
            }

            for (Zipper task : subTasks) {
                result = result && task.join();
            }
        } else {
            result = zipFile(file);
        }
        return result;
    }

    private boolean zipFile(File fileToZip) {
        String path = fileToZip.getPath();
        try (
                FileOutputStream fos = new FileOutputStream(path + ".zip");
                ZipOutputStream zipOut = new ZipOutputStream(fos);
                FileInputStream fis = new FileInputStream(fileToZip)
        ) {
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
