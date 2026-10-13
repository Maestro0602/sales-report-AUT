package edu.itc.salesreport.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthLoaderTest {

    @Test
    void loadsFilesAndPublishesEventsInOrder(@TempDir Path tempDir) throws IOException {
        Path monthDir = Files.createDirectories(tempDir.resolve("2026-09"));
        String header = CsvTransactionParser.HEADER;

        Files.write(monthDir.resolve("PNH-2026-09-01.csv"), List.of(
                header,
                "PNH,2026-09-01,PNH-001,SKU-1001,Rice,Grocery,2,6.50,0.00,KHQR",
                "PNH,2026-09-01,PNH-002,SKU-1002,Fish Sauce,Grocery,1,1.80,0.00,CASH"
        ));

        Files.write(monthDir.resolve("REP-2026-09-01.csv"), List.of(
                header,
                "REP,2026-09-01,REP-001,SKU-1001,Rice,Grocery,1,6.50,0.00,KHQR",
                "REP,2026-09-01,REP-002,SKU-1002,Fish Sauce,Grocery,two,1.80,0.00,CASH"
        ));

        var loader = new MonthLoader(new CsvTransactionParser());
        var events = new ArrayList<LoadEvent>();
        loader.subscribe(events::add);

        var transactions = loader.load(YearMonth.of(2026, 9), tempDir);

        assertEquals(3, transactions.size());
        assertEquals(3, events.size());

        assertInstanceOf(LoadEvent.FileLoaded.class, events.get(0));
        var e1 = (LoadEvent.FileLoaded) events.get(0);
        assertEquals("PNH-2026-09-01.csv", e1.file().getFileName().toString());
        assertEquals(2, e1.accepted());
        assertEquals(0, e1.rejected());
        assertEquals(1, e1.filesDone());
        assertEquals(2, e1.filesTotal());

        assertInstanceOf(LoadEvent.FileLoaded.class, events.get(1));
        var e2 = (LoadEvent.FileLoaded) events.get(1);
        assertEquals("REP-2026-09-01.csv", e2.file().getFileName().toString());
        assertEquals(1, e2.accepted());
        assertEquals(1, e2.rejected());
        assertEquals(2, e2.filesDone());
        assertEquals(2, e2.filesTotal());

        assertInstanceOf(LoadEvent.LoadFinished.class, events.get(2));
        var e3 = (LoadEvent.LoadFinished) events.get(2);
        assertEquals(3, e3.accepted());
        assertEquals(1, e3.rejected());

        assertEquals(1, loader.rejectedRows().size());
        assertTrue(loader.rejectedRows().get(0).contains("REP-2026-09-01.csv:3"));
    }

    @Test
    void unsubscribedListenerReceivesNothing(@TempDir Path tempDir) throws IOException {
        Path monthDir = Files.createDirectories(tempDir.resolve("2026-09"));
        Files.write(monthDir.resolve("PNH-2026-09-01.csv"), List.of(
                CsvTransactionParser.HEADER,
                "PNH,2026-09-01,PNH-001,SKU-1001,Rice,Grocery,1,6.50,0.00,KHQR"
        ));

        var loader = new MonthLoader(new CsvTransactionParser());
        var events = new ArrayList<LoadEvent>();
        Runnable unsub = loader.subscribe(events::add);
        unsub.run();

        loader.load(YearMonth.of(2026, 9), tempDir);

        assertEquals(0, events.size());
    }
}
