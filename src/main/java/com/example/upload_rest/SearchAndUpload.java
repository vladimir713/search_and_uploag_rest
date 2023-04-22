package com.example.upload_rest;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import org.springframework.core.io.FileSystemResource;

public class SearchAndUpload {

        private static final String POST_URL = "http://localhost:8080/upload";

        public static List<Path> list= new CopyOnWriteArrayList<>();

        public static void main(String[] args) {

            searchFile("*chm", "c:\\Windows\\Help");
        }

        /**
         * Могу составить вариант программы с использованием Nio2 с применением FileVisitor
         * В данной реализации для каждой директории создается свой поток.
         * Для путей, не содержащих файлы, тоже создаются потоки. Могу доработать программу
         * и не запускать такие потоки.
         * Используется интерфейс Runnable.
         * Также возможно реализовать многопоточность с использованием пула потоков ExecuteServiсe
         *
         * @param file Строка с именем файла для поиска. Используется шаблон * - любое количество символов.
         *             В последствии * меняется на регулярное выражение.
         * @param dir Директория, где будет осуществляться поиск файлов
         * @latch Счетчик рабочих потоков
         */
        public static void searchFile(String file, String dir) {
            try {
                List<Path> listDirs = Files.walk(Paths.get(dir))
                        .filter(Files::isDirectory)
                        .toList();

                file = file.replace("*", ".*");

                CountDownLatch latch = new CountDownLatch(listDirs.size());

                for (Path p:listDirs) {
                    new Thread(new MyRunnable(p, file, latch)).start();
                }

                latch.await();

                for (Path p:list) {
                    System.out.println(p + " размер = " + Files.size(p));
                }
                System.out.print("Всего найдено файлов: " + list.size() + "\n\n" + "Копировать их по сети? (y/n) ");
                Scanner sc = new Scanner(System.in);
                if (sc.nextLine().equals("y")) {
                    transferFile(list);
                };


            } catch (UncheckedIOException e) {                      // Может быть очень круто,
                System.out.println("Ошибка доступа к " + dir);      // лучше поискать более специализированные исключения :)
            } catch (IOException e) {
                System.out.println("Путь " + dir + " не существует");                   // Можно и логгер использовать
            } catch (InterruptedException e) {
                System.out.println("Проблемы со счетчиком потоков");
            }
        }

        /**
         * Реализовал с помощью RestTemplate. Тестовый Spring Rest server для приема файлов
         * - тут <a href="https://github.com/vladimir713/test_Spring_Rest_server_for_upload_files.git">...</a>
         *
         * @param list Список файлов для отправки
         *
         */

        public static void transferFile(List<Path> list) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            for (Path p:list) {
                body.add("files", new FileSystemResource(p.toFile()));
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(POST_URL, requestEntity, String.class);
            System.out.println("Response code: " + response.getStatusCode());
        }
}
