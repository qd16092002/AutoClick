package org.quangdao.tools;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Log4j2
public class FileIO {
    private final Path outputFile, inputFolder, dumpFile;
    private Path currentFile;
    private BufferedReader br;
    private final File[] files;
    private int fileIndex = 0;
    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    private List<MailPass> queue = new ArrayList<>();

    public FileIO(String inputFolder, String outputFolder, String dump) throws IOException {
        this.outputFile = Path.of(outputFolder);
        this.inputFolder = Path.of(inputFolder);
        this.dumpFile = Path.of(dump);

        files = this.inputFolder.toFile().listFiles();
        if(files == null || files.length == 0) {
            log.error("Folder empty");
        }
        else nextFile();

        scheduledExecutorService.scheduleAtFixedRate(this::flush, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void nextFile() throws IOException {
        if(br!=null) br.close();
        if(fileIndex == files.length) br = null;
        else br = new BufferedReader(new FileReader(files[fileIndex++]));
    }

    public List<MailPass> getEmail(int count) throws IOException {
        log.info(files);
        log.info(fileIndex);
        log.info(br);
        ArrayList<MailPass> mailPass = new ArrayList<>();
        if(br == null) return mailPass;
        while (mailPass.size() < count) {
                String line = br.readLine();
                while(line == null) {
                    br.close();
                    nextFile();
                    if(br == null) return mailPass;
                    line = br.readLine();
                }
                String[] tok = line.split(":");
                if (tok.length != 2)
                    dumpFailLine(line);
                tok[0] = tok[0].trim();
                tok[1] = tok[1].trim();
                if (tok[0].endsWith("@gmail.com"))
                    mailPass.add(new MailPass(tok[0], tok[1]));
        }
        return mailPass;
    }

    void dumpFailLine(String line)  {
        try (FileWriter fileWriter = new FileWriter(dumpFile.toFile(), StandardCharsets.UTF_8, true)) {
            fileWriter.write(line+'\n');
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            log.info("Fail write: "+line);
        }
    }

    public void addEmail(MailPass mailPass) {
        synchronized (this) {
            queue.add(mailPass);
        }
    }

    private void flush() {
        List<MailPass> tmp;
        synchronized (this) {
            tmp = queue;
            queue = new ArrayList<>();
        }

        try(BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile.toFile(), StandardCharsets.UTF_8, true))) {
            for (var i : tmp) {
                bufferedWriter.write(i.mail());
                bufferedWriter.write(":");
                bufferedWriter.write(i.pass());
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            log.info("Fail write: "+tmp);
        }
    }
}
