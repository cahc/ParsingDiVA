package org.cc.divaToSciVal;

import cc.analysis.scival.SciValParser;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Shared analytical document-type policy; unknown labels remain eligible. */
final class SciValDocumentTypePolicy {
    enum Group {
        JOURNAL_ARTIFACT_TYPE,
        CONFERENCE_ARTIFACT_TYPE,
        BOOK_AND_CHAPTER_ARTIFACT_TYPE
    }

    private static final Set<String> CURRENT_EXCLUSIONS = Set.of("retracted", "abstract report");
    private static final Set<String> EXCLUDED_TYPES = Set.of(
            "Retracted", "Abstract Report", "Erratum", "Editorial", "Conference Review");
    private static final Set<String> NORMALIZED_EXCLUSIONS = EXCLUDED_TYPES.stream()
            .map(SciValDocumentTypePolicy::normalize).collect(java.util.stream.Collectors.toUnmodifiableSet());
    private static final Map<String, Group> GROUPS = Map.ofEntries(
            Map.entry("article in press", Group.JOURNAL_ARTIFACT_TYPE),
            Map.entry("letter", Group.JOURNAL_ARTIFACT_TYPE),
            Map.entry("data paper", Group.JOURNAL_ARTIFACT_TYPE),
            Map.entry("short survey", Group.JOURNAL_ARTIFACT_TYPE),
            Map.entry("note", Group.JOURNAL_ARTIFACT_TYPE),
            Map.entry("article", Group.JOURNAL_ARTIFACT_TYPE),
            Map.entry("review", Group.JOURNAL_ARTIFACT_TYPE),
            Map.entry("conference paper", Group.CONFERENCE_ARTIFACT_TYPE),
            Map.entry("chapter", Group.BOOK_AND_CHAPTER_ARTIFACT_TYPE),
            Map.entry("book chapter", Group.BOOK_AND_CHAPTER_ARTIFACT_TYPE),
            Map.entry("book", Group.BOOK_AND_CHAPTER_ARTIFACT_TYPE));

    private SciValDocumentTypePolicy() { }

    /** Parser exclusions avoid reading citation fields on explicitly excluded rows. */
    static List<SciValParser.SciValRecord> readEligibleRecords(String file) throws IOException {
        List<SciValParser.SciValRecord> records = SciValParser.getSciValRecords(file,
                EXCLUDED_TYPES);
        records.removeIf(record -> excluded(record.getScivalDocType()));
        return records;
    }

    static String normalize(String type) {
        return type == null ? "" : type.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    static boolean legacyExcluded(String type) {
        return CURRENT_EXCLUSIONS.contains(normalize(type));
    }

    static boolean excluded(String type) {
        return NORMALIZED_EXCLUSIONS.contains(normalize(type));
    }

    static Group group(String type) {
        return GROUPS.get(normalize(type));
    }

    static String disposition(String type) {
        if (excluded(type)) return "EXCLUDED";
        return group(type) == null ? "UNKNOWN_DOCUMENT_TYPE" : "INCLUDED";
    }
}
