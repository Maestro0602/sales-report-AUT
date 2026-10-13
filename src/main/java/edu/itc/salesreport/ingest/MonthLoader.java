package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public final class MonthLoader {
    private final TransactionParser parser;
    private final List<LoadListener> listeners =
            new CopyOnWriteArrayList<>();
    private final List<String> rejectedRows = new ArrayList<>();

    public MonthLoader(TransactionParser parser) {
        this.parser = parser;
    }

    /** Registers a listener; the returned handle unsubscribes it. */
    public Runnable subscribe(LoadListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public List<String> rejectedRows() {
        return List.copyOf(rejectedRows);
    }

    public List<SaleTransaction> load(YearMonth month, Path dataDir) {
        Instant start = Instant.now();
        Path monthDir = dataDir.resolve(month.toString());
        List<Path> files;
        try (Stream<Path> stream = Files.list(monthDir)) {
            files = stream.filter(p -> p.toString().endsWith(".csv"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var acceptedTransactions = new ArrayList<SaleTransaction>();
        int totalAccepted = 0;
        int totalRejected = 0;
        int filesTotal = files.size();

        for (int i = 0; i < filesTotal; i++) {
            Path file = files.get(i);
            int fileAccepted = 0;
            int fileRejected = 0;
            List<String> lines;
            try {
                lines = Files.readAllLines(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            for (int lineIdx = 1; lineIdx < lines.size(); lineIdx++) {
                String line = lines.get(lineIdx);
                try {
                    SaleTransaction tx = parser.parse(line);
                    acceptedTransactions.add(tx);
                    fileAccepted++;
                } catch (InvalidRowException e) {
                    fileRejected++;
                    rejectedRows.add(file.getFileName() + ":" + (lineIdx + 1) + " " + e.getMessage());
                }
            }

            totalAccepted += fileAccepted;
            totalRejected += fileRejected;
            publish(new LoadEvent.FileLoaded(file, fileAccepted, fileRejected, i + 1, filesTotal));
        }

        Duration took = Duration.between(start, Instant.now());
        publish(new LoadEvent.LoadFinished(totalAccepted, totalRejected, took));
        return List.copyOf(acceptedTransactions);
    }

    private void publish(LoadEvent event) {
        listeners.forEach(l -> l.onEvent(event));
    }
}
