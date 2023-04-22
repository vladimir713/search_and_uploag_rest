package com.example.upload_rest;

/* Copyright (c) 2023, Vladimir Chugunov
 */
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MyRunnable implements Runnable{
    private Path dir;
    private String file;
    CountDownLatch latch;
    public MyRunnable(Path dir, String file, CountDownLatch latch) {
        this.dir = dir;
        this.file = file;
        this.latch = latch;
    }

    @Override
    public void run() {
        List<Path> listFiles;
        try {
            listFiles = Files.walk(Paths.get(dir.toString()))
                    .filter(Files::isRegularFile)
                    .filter(Files -> Files.getParent().equals(dir)) // Чтобы не было повторов найденных файлов.
                    .toList();                                     // Пока не нашел метода выделить в текущей папке
        } catch (IOException e) {                                 // только файлы из этой директории, но не из вложенных
            throw new RuntimeException(e);
        }

        for (Path f : listFiles) {
            if (f.getFileName().toString().matches(file)) {
//                System.out.println(f);
                SearchAndUpload.list.add(f);
//                Main.countSmallFiles.incrementAndGet();
            }
        }
        latch.countDown();
    }
}
