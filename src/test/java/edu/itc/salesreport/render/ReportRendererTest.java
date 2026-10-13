package edu.itc.salesreport.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ReportRendererTest {

    @ParameterizedTest
    @ValueSource(strings = {"txt", "html", "csv"})
    void rendersBranchCodesAndTotal(String ext) {
        var renderer = ReportRenderer.forExtension(ext);
        var report = TestReports.september();
        String output = new String(renderer.render(report), StandardCharsets.UTF_8);

        assertTrue(output.contains("PNH"), "missing PNH in " + ext);
        assertTrue(output.contains("REP"), "missing REP in " + ext);
        assertTrue(output.contains("BTB"), "missing BTB in " + ext);
        assertTrue(output.contains("2300.50"), "missing total 2300.50 in " + ext);
    }

    @Test
    void exactCsvLine() {
        var renderer = ReportRenderer.forExtension("csv");
        var report = TestReports.september();
        String output = new String(renderer.render(report), StandardCharsets.UTF_8);

        assertTrue(output.contains("2026-09,REP,800.50,0.00,50,16.01\n"));
    }

    @Test
    void forExtensionPdfThrows() {
        assertThrows(IllegalArgumentException.class, () -> ReportRenderer.forExtension("pdf"));
    }

    @Test
    void exhaustiveSwitchWithoutDefault() {
        ReportRenderer renderer = ReportRenderer.forExtension("txt");
        String name = switch (renderer) {
            case TextReportRenderer t -> "text";
            case HtmlReportRenderer h -> "html";
            case CsvReportRenderer c -> "csv";
        };
        assertEquals("text", name);
    }

    @Test
    void declaredExtensionsMatch() {
        assertEquals("txt", new TextReportRenderer().extension());
        assertEquals("html", new HtmlReportRenderer().extension());
        assertEquals("csv", new CsvReportRenderer().extension());
    }
}
